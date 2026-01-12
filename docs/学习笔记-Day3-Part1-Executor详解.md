# Day 3 学习笔记 - Part 1: Executor执行器详解

## 🎯 第一课：理解Executor

### Executor是什么？

Executor是MyBatis的**SQL执行引擎**，负责真正执行SQL语句。

### 类比理解

```
如果把MyBatis比作餐厅：

SqlSession      = 服务员（接待客人）
Executor        = 厨师（真正做菜）
StatementHandler = 厨具（锅碗瓢盆）
JDBC            = 原材料（数据库连接）

客人点菜（调用方法）
  ↓
服务员记录（SqlSession）
  ↓
转交给厨师（Executor）
  ↓
厨师使用厨具（StatementHandler）
  ↓
制作菜品（执行SQL）
  ↓
上菜（返回结果）
```

---

## 📚 Executor的类型

MyBatis提供了3种Executor：

```
                    Executor (接口)
                         │
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
    SimpleExecutor  ReuseExecutor  BatchExecutor
     (简单执行)      (复用执行)      (批量执行)
```

### 1. SimpleExecutor（默认）

**特点**：每次执行SQL都创建新的Statement

```java
// 执行流程
public <E> List<E> query(String statementId, Object parameter) {
    // 1. 获取SQL配置
    MappedStatement ms = configuration.getMappedStatement(statementId);
    
    // 2. 创建StatementHandler（每次都新建）
    StatementHandler handler = new StatementHandler(configuration);
    
    // 3. 准备Statement（每次都新建）
    Statement stmt = handler.prepare(connection, ms.getSql());
    
    // 4. 设置参数
    handler.parameterize(stmt, parameter);
    
    // 5. 执行查询
    List<E> result = handler.query(stmt, ms.getResultType());
    
    // 6. 立即关闭Statement
    stmt.close();
    
    return result;
}
```

**优点**：
- ✅ 简单直观
- ✅ 不会有状态问题
- ✅ 容易理解

**缺点**：
- ❌ 每次都创建新Statement（有性能开销）
- ❌ 不能复用预编译的SQL

**适用场景**：
- 一般的CRUD操作
- SQL语句不重复执行
- 我们的mini-mybatis就用这个

---

### 2. ReuseExecutor（复用执行器）

**特点**：复用相同SQL的Statement

```java
public class ReuseExecutor implements Executor {
    // 缓存Statement
    private Map<String, Statement> statementMap = new HashMap<>();
    
    public <E> List<E> query(String statementId, Object parameter) {
        MappedStatement ms = configuration.getMappedStatement(statementId);
        String sql = ms.getSql();
        
        // 检查是否已有预编译的Statement
        Statement stmt = statementMap.get(sql);
        
        if (stmt == null) {
            // 第一次：创建并缓存
            stmt = connection.prepareStatement(sql);
            statementMap.put(sql, stmt);
        } else {
            // 后续：直接复用
            // 不需要重新编译SQL
        }
        
        // 设置参数并执行
        handler.parameterize(stmt, parameter);
        return handler.query(stmt, ms.getResultType());
    }
    
    @Override
    public void close() {
        // 关闭所有缓存的Statement
        for (Statement stmt : statementMap.values()) {
            stmt.close();
        }
        statementMap.clear();
    }
}
```

**优点**：
- ✅ 复用Statement，避免重复编译
- ✅ 提高性能（特别是复杂SQL）

**缺点**：
- ❌ 需要管理Statement缓存
- ❌ 占用更多内存

**适用场景**：
- 同一个SQL会多次执行
- SQL比较复杂（编译耗时）
- 内存充足

**示例**：
```java
// 假设在一个事务中多次查询
for (int i = 0; i < 100; i++) {
    User user = mapper.selectById(i);
    // 使用ReuseExecutor，Statement只编译一次
}
```

---

### 3. BatchExecutor（批量执行器）

**特点**：批量执行SQL，一次性提交

```java
public class BatchExecutor implements Executor {
    private Statement currentStatement;
    private String currentSql;
    
    @Override
    public int update(String statementId, Object parameter) {
        MappedStatement ms = configuration.getMappedStatement(statementId);
        String sql = ms.getSql();
        
        // 检查是否需要创建新的Statement
        if (currentStatement == null || !sql.equals(currentSql)) {
            // SQL变了，需要新Statement
            currentStatement = connection.prepareStatement(sql);
            currentSql = sql;
        }
        
        // 设置参数
        handler.parameterize(currentStatement, parameter);
        
        // 添加到批次（不立即执行）
        currentStatement.addBatch();
        
        return 0; // 暂时返回0，真正的影响行数在flush时返回
    }
    
    public List<BatchResult> flushStatements() {
        // 执行所有批次
        int[] results = currentStatement.executeBatch();
        // 返回批量结果
        return convertResults(results);
    }
}
```

**优点**：
- ✅ 减少网络往返次数
- ✅ 大幅提升批量操作性能
- ✅ 适合批量插入/更新

**缺点**：
- ❌ 不能立即获取影响行数
- ❌ 调试困难
- ❌ 出错时难以定位

**适用场景**：
- 批量插入数据
- 批量更新数据
- 数据导入

**示例**：
```java
// 批量插入1000条数据
SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH);
try {
    UserMapper mapper = session.getMapper(UserMapper.class);
    
    for (int i = 0; i < 1000; i++) {
        User user = new User();
        user.setUsername("user" + i);
        mapper.insert(user);  // 只是添加到批次
    }
    
    session.commit();  // 真正执行
} finally {
    session.close();
}
```

---

## 🔍 三种Executor对比

| 特性 | SimpleExecutor | ReuseExecutor | BatchExecutor |
|-----|----------------|---------------|---------------|
| Statement管理 | 每次新建 | 复用缓存 | 批量累积 |
| 性能 | 中等 | 较好 | 最好（批量） |
| 内存占用 | 低 | 中等 | 低 |
| 适用场景 | 一般CRUD | 重复SQL | 批量操作 |
| 复杂度 | 简单 | 中等 | 复杂 |
| 默认 | ✅ 是 | ❌ 否 | ❌ 否 |

---

## 📖 Executor的生命周期

```
创建阶段
  DefaultSqlSession构造函数
    └─> new SimpleExecutor(configuration, connection)

使用阶段
  SqlSession.selectOne/selectList/update/insert/delete
    └─> executor.query() / executor.update()

关闭阶段
  SqlSession.close()
    └─> executor.close()
        └─> 关闭Statement
        └─> 不关闭Connection（由SqlSession管理）
```

---

## 💡 Executor的核心职责

### 1. 获取配置信息
```java
// 从Configuration获取MappedStatement
MappedStatement ms = configuration.getMappedStatement(statementId);

// MappedStatement包含：
// - SQL语句
// - 参数类型
// - 返回类型
// - 缓存配置
// - ...
```

### 2. 创建StatementHandler
```java
// StatementHandler负责JDBC操作
StatementHandler handler = new StatementHandler(configuration);

// StatementHandler会：
// - 准备Statement
// - 设置参数
// - 执行SQL
// - 处理结果
```

### 3. 执行SQL
```java
// 准备Statement
Statement stmt = handler.prepare(connection, sql);

// 设置参数
handler.parameterize(stmt, parameter);

// 执行查询
ResultSet rs = stmt.executeQuery();

// 或执行更新
int rows = stmt.executeUpdate();
```

### 4. 处理结果
```java
// 查询：调用ResultSetHandler
List<E> result = resultSetHandler.handleResultSet(rs, resultType);

// 更新：返回影响行数
return rows;
```

---

## 🎯 深入SimpleExecutor源码

让我们看看我们实现的SimpleExecutor：

```java
public <E> List<E> query(String statementId, Object parameter) {
    logger.debug("SimpleExecutor执行查询: {}", statementId);
    
    // 步骤1: 获取MappedStatement ⭐
    MappedStatement ms = configuration.getMappedStatement(statementId);
    if (ms == null) {
        throw new MyBatisException("未找到SQL语句: " + statementId);
    }
    // 此时ms包含了：
    // - SQL: "SELECT * FROM user WHERE id = #{id}"
    // - resultType: User.class
    // - parameterType: Long.class
    
    // 步骤2: 创建StatementHandler ⭐
    StatementHandler handler = new StatementHandler(configuration);
    // StatementHandler将负责后续的JDBC操作
    
    // 步骤3: 准备Statement ⭐
    Statement statement = handler.prepare(connection, ms.getSql());
    // 内部会：
    // - 将 #{id} 替换为 ?
    // - 调用 connection.prepareStatement(sql)
    // - 返回 PreparedStatement
    
    // 步骤4: 设置参数 ⭐
    handler.parameterize(statement, parameter);
    // 内部会：
    // - 判断参数类型
    // - 调用 ps.setObject(1, parameter)
    
    // 步骤5: 执行查询 ⭐
    List<E> result = handler.query(statement, ms.getResultType());
    // 内部会：
    // - 执行 ps.executeQuery()
    // - 获取 ResultSet
    // - 调用 ResultSetHandler 映射结果
    // - 返回 List<User>
    
    // 步骤6: 关闭Statement ⭐
    statement.close();
    // 释放JDBC资源
    
    logger.debug("查询完成，返回{}条记录", result.size());
    return result;
}
```

**关键理解**：
1. Executor只是**协调者**
2. 真正的工作由**StatementHandler**完成
3. 每个步骤职责清晰

---

## 🤔 思考题

### 1. 为什么默认使用SimpleExecutor？

<details>
<summary>点击查看答案</summary>

因为：
- ✅ 最简单，不容易出错
- ✅ 适合大多数场景（CRUD操作）
- ✅ 不会有Statement缓存问题
- ✅ 内存占用最小

ReuseExecutor和BatchExecutor需要额外管理，只在特定场景有优势。
</details>

### 2. ReuseExecutor什么时候会有问题？

<details>
<summary>点击查看答案</summary>

问题场景：
- 长时间持有Statement可能导致连接问题
- 如果SQL很多，缓存Map会占用大量内存
- 参数类型不同但SQL相同，可能导致参数绑定错误

所以要在close时清理所有缓存的Statement。
</details>

### 3. 如何选择Executor类型？

<details>
<summary>点击查看答案</summary>

选择依据：

**SimpleExecutor**（默认）:
- 一般的CRUD操作
- SQL不重复执行
- 没有特殊性能要求

**ReuseExecutor**:
- 同一SQL会多次执行
- SQL编译成本高
- 内存充足

**BatchExecutor**:
- 批量插入/更新
- 对实时性要求不高
- 需要高吞吐量
</details>

---

## 📊 执行流程图

```
用户调用
  mapper.selectById(1L)
         ↓
MapperProxy拦截
  构建statementId
         ↓
SqlSession.selectOne()
         ↓
┌─────────────────────────┐
│    Executor.query()     │ ← 我们在这里！
└────────────┬────────────┘
             │
    ┌────────┼────────┐
    ↓        ↓        ↓
  获取MS   创建Handler  准备Statement
    ↓        ↓        ↓
  设置参数  执行SQL   处理结果
    ↓        ↓        ↓
             └────────┘
                 ↓
            返回结果
```

---

## 🎯 核心要点

1. **Executor是协调者**
   - 不直接操作JDBC
   - 协调各个组件完成SQL执行

2. **三种类型各有特点**
   - Simple：简单直接
   - Reuse：复用Statement
   - Batch：批量执行

3. **生命周期跟随SqlSession**
   - SqlSession创建时创建
   - SqlSession关闭时关闭

4. **职责单一**
   - 只负责执行流程
   - 具体工作委托给Handler

---

**第一课完成！休息3分钟，准备第二课！** ☕

