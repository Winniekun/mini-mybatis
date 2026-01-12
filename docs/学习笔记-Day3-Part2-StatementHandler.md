# Day 3 学习笔记 - Part 2: StatementHandler深入剖析

## 🔧 第二课：StatementHandler - JDBC的封装者

### StatementHandler是什么？

StatementHandler是MyBatis**四大对象之一**，负责封装JDBC的Statement操作。

### 类比理解

```
如果Executor是厨师：

StatementHandler = 厨具（锅碗瓢盆）
PreparedStatement = 具体的锅
参数 = 食材
SQL = 菜谱

厨师（Executor）告诉你要做什么菜
你（StatementHandler）：
1. 准备锅（prepare）
2. 放食材（parameterize）
3. 开火做菜（query/update）
4. 上菜（返回结果）
```

---

## 📚 Statement的三种类型

JDBC提供了三种Statement：

```
          Statement (接口)
               │
    ┌──────────┼──────────┐
    ↓          ↓          ↓
Statement  PreparedStatement  CallableStatement
(普通)      (预编译)           (存储过程)
```

### 1. Statement（普通）

```java
// 不带参数，直接执行SQL
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM user");
```

**特点**：
- ❌ SQL直接拼接，容易SQL注入
- ❌ 不能复用，每次都要编译
- ❌ 不推荐使用

---

### 2. PreparedStatement（预编译）⭐ 推荐

```java
// 使用?占位符
PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE id = ?");
ps.setLong(1, 1L);  // 设置第1个参数
ResultSet rs = ps.executeQuery();
```

**特点**：
- ✅ 预编译，可以复用
- ✅ 使用?占位符，防止SQL注入
- ✅ 类型安全
- ✅ MyBatis默认使用

**工作原理**：
```
1. 编译阶段（prepare）
   SQL: SELECT * FROM user WHERE id = ?
   编译成执行计划，?是占位符

2. 绑定阶段（parameterize）
   ps.setLong(1, 1L)
   将参数1L绑定到第1个?

3. 执行阶段（executeQuery）
   数据库执行预编译好的计划
   替换?为实际值
```

---

### 3. CallableStatement（存储过程）

```java
// 调用存储过程
CallableStatement cs = conn.prepareCall("{call getUserById(?)}");
cs.setLong(1, 1L);
ResultSet rs = cs.executeQuery();
```

**特点**：
- 用于调用数据库存储过程
- 可以有输出参数
- 不常用

---

## 🔍 StatementHandler的核心方法

### 方法1: prepare() - 准备Statement

```java
public Statement prepare(Connection connection, String sql) throws SQLException {
    // 步骤1: 将#{param}替换为?
    String preparedSql = sql.replaceAll("#\\{[^}]+\\}", "?");
    //  输入: SELECT * FROM user WHERE id = #{id}
    //  输出: SELECT * FROM user WHERE id = ?
    
    logger.debug("原始SQL: {}", sql);
    logger.debug("预编译SQL: {}", preparedSql);
    
    // 步骤2: 创建PreparedStatement
    return connection.prepareStatement(preparedSql);
}
```

**详细解析**：

#### 占位符替换
```java
// 正则表达式: #\\{[^}]+\\}
// 匹配模式:
//   #\{      - 匹配 #{
//   [^}]+    - 匹配一个或多个非}字符
//   \}       - 匹配 }

// 示例：
"SELECT * FROM user WHERE id = #{id}"
// 替换为：
"SELECT * FROM user WHERE id = ?"

"INSERT INTO user (name, age) VALUES (#{name}, #{age})"
// 替换为：
"INSERT INTO user (name, age) VALUES (?, ?)"

"SELECT * FROM user WHERE name = #{name} AND age > #{age}"
// 替换为：
"SELECT * FROM user WHERE name = ? AND age > ?"
```

#### 为什么要替换？
```
MyBatis的SQL       →  JDBC的SQL
─────────────────────────────────
#{id}              →  ?
#{name}            →  ?
#{user.id}         →  ?

原因：
1. JDBC的PreparedStatement只认识?
2. #{param}是MyBatis的语法糖
3. 需要转换成JDBC能理解的格式
```

---

### 方法2: parameterize() - 设置参数

```java
public void parameterize(Statement statement, Object parameter) throws SQLException {
    if (parameter == null) {
        return;
    }
    
    PreparedStatement ps = (PreparedStatement) statement;
    
    // 简单类型直接设置
    if (isSimpleType(parameter)) {
        logger.debug("设置参数: [1] = {}", parameter);
        ps.setObject(1, parameter);
        // 将parameter绑定到第1个?的位置
    } else {
        // 复杂类型需要特殊处理
        logger.warn("暂不支持复杂类型参数: {}", parameter.getClass());
    }
}
```

**参数类型判断**：

```java
// 简单类型
- String
- Integer, Long, Double, Float
- Boolean, Byte, Short, Character
- Date, Timestamp
- 等等

// 复杂类型  
- JavaBean（User, Product等）
- Map
- List
- Array
```

**简单类型处理**：
```java
// SQL: SELECT * FROM user WHERE id = ?
// 参数: 1L

ps.setObject(1, 1L);  // 绑定到第1个?

// 等价于：
ps.setLong(1, 1L);
```

**复杂类型处理（真实MyBatis）**：
```java
// SQL: INSERT INTO user (name, age) VALUES (#{name}, #{age})
// 参数: User对象

// 真实MyBatis会：
// 1. 解析SQL，找到#{name}和#{age}
// 2. 通过反射调用user.getName()和user.getAge()
// 3. 依次绑定到?的位置

// 我们的简化版暂时不支持，需要自己实现这个功能
```

---

### 方法3: query() - 执行查询

```java
public <E> List<E> query(Statement statement, Class<?> resultType) throws SQLException {
    // 步骤1: 执行查询
    PreparedStatement ps = (PreparedStatement) statement;
    ResultSet resultSet = ps.executeQuery();
    // 此时SQL已经发送到数据库并执行
    // resultSet包含了查询结果
    
    logger.debug("SQL执行成功，开始处理结果集");
    
    // 步骤2: 处理结果集
    List<E> result = resultSetHandler.handleResultSet(resultSet, resultType);
    // ResultSetHandler负责：
    // - 遍历ResultSet
    // - 创建Java对象
    // - 映射字段到属性
    // - 返回对象列表
    
    // 步骤3: 关闭结果集
    resultSet.close();
    
    return result;
}
```

**执行流程**：
```
PreparedStatement (已经设置好参数)
        ↓
   executeQuery()
        ↓
发送SQL到数据库
        ↓
数据库执行查询
        ↓
返回ResultSet
        ↓
ResultSetHandler处理
        ↓
返回List<E>
```

---

### 方法4: update() - 执行更新

```java
public int update(Statement statement) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    
    // 执行更新（INSERT/UPDATE/DELETE）
    int rows = ps.executeUpdate();
    // rows是影响的行数
    
    logger.debug("SQL执行成功，影响{}行", rows);
    
    return rows;
}
```

**与query()的区别**：
```
query()               update()
────────────────────────────────────
executeQuery()        executeUpdate()
返回ResultSet         返回int（行数）
SELECT语句            INSERT/UPDATE/DELETE
需要处理结果集         只需要返回影响行数
```

---

## 💡 StatementHandler的工作流程

完整的执行过程：

```
1. Executor调用handler.prepare()
   ↓
   将 #{id} 替换为 ?
   ↓
   创建 PreparedStatement
   ↓
   SQL发送到数据库编译

2. Executor调用handler.parameterize()
   ↓
   判断参数类型
   ↓
   调用 ps.setObject(1, parameter)
   ↓
   参数绑定到?

3. Executor调用handler.query()/update()
   ↓
   执行 ps.executeQuery() 或 executeUpdate()
   ↓
   数据库执行SQL
   ↓
   返回结果
```

---

## 🎯 关键设计点

### 1. 为什么要预编译？

```
不预编译（Statement）:
  每次都要：解析SQL → 编译 → 执行
  ❌ 慢
  ❌ 容易SQL注入

预编译（PreparedStatement）:
  第一次：解析SQL → 编译 → 保存执行计划
  后续：直接执行
  ✅ 快
  ✅ 防SQL注入
  ✅ 可以复用
```

### 2. 占位符的作用

```
#{id} vs直接拼接

直接拼接：
String sql = "SELECT * FROM user WHERE id = " + id;
❌ SQL注入风险
❌ 类型不安全
❌ 不能复用

使用占位符：
String sql = "SELECT * FROM user WHERE id = #{id}";
✅ 安全（参数会被转义）
✅ 类型安全（PreparedStatement处理）
✅ 可以复用
```

### 3. 职责分离

```
StatementHandler     只负责JDBC操作
ResultSetHandler     只负责结果映射
ParameterHandler     只负责参数处理（真实MyBatis）

每个类职责单一，易于维护和扩展
```

---

## 🔍 深入理解：SQL预编译

### 数据库视角

```
第一次执行：
  Client → Server
  "SELECT * FROM user WHERE id = ?"
            ↓
  Server解析SQL
  Server编译SQL
  Server保存执行计划
  Server执行（替换?为1）
            ↓
  返回结果

第二次执行（相同SQL，不同参数）：
  Client → Server
  "执行已编译的计划，参数=2"
            ↓
  Server直接执行（不用再编译）
            ↓
  返回结果

性能提升：省去了解析和编译时间
```

### PreparedStatement的优势

```
优势1：性能
  编译一次，执行多次

优势2：安全
  参数会被正确转义
  不会被当作SQL代码执行

优势3：类型安全
  参数类型由PreparedStatement处理
  自动转换类型
```

---

## 🤔 思考题

### 1. 为什么#{id}要替换成?

<details>
<summary>点击查看答案</summary>

因为：
- JDBC的PreparedStatement只认识?作为占位符
- #{id}是MyBatis的语法，JDBC不理解
- 需要转换成JDBC能理解的格式

这是MyBatis对JDBC的封装，让我们写SQL更方便。
</details>

### 2. 如果有多个参数怎么办？

<details>
<summary>点击查看答案</summary>

多个参数的情况：

```sql
SELECT * FROM user 
WHERE name = #{name} AND age > #{age}

替换后：
SELECT * FROM user 
WHERE name = ? AND age > ?

设置参数：
ps.setString(1, "张三");  // 第1个?
ps.setInt(2, 18);         // 第2个?
```

关键：
- 参数顺序很重要
- 第1个#{} 对应第1个?
- 第2个#{} 对应第2个?
</details>

### 3. 简单类型 vs 复杂类型

<details>
<summary>点击查看答案</summary>

**简单类型**（我们已实现）:
```java
// 只有一个参数
mapper.selectById(1L);

// StatementHandler直接设置
ps.setObject(1, 1L);
```

**复杂类型**（需要增强）:
```java
// 参数是对象
mapper.insert(user);

// 需要：
// 1. 解析SQL中的#{name}, #{age}
// 2. 通过反射获取user.getName(), user.getAge()
// 3. 依次设置参数
ps.setString(1, user.getName());
ps.setInt(2, user.getAge());
```

我们的简化版暂不支持复杂类型。
</details>

---

## 📊 StatementHandler在架构中的位置

```
Executor（调度者）
    ↓
创建StatementHandler
    ↓
StatementHandler（执行者）
    ├─> prepare() → PreparedStatement
    ├─> parameterize() → 设置参数
    ├─> query()/update() → 执行SQL
    └─> ResultSetHandler → 处理结果
```

---

## 🎯 核心要点

1. **StatementHandler封装JDBC操作**
   - 创建Statement
   - 设置参数
   - 执行SQL
   - 不负责结果处理（委托给ResultSetHandler）

2. **PreparredStatement是核心**
   - 预编译提升性能
   - ?占位符防注入
   - 类型安全

3. **#{} 的处理**
   - 替换为?
   - 让MyBatis语法转换为JDBC语法

4. **职责单一**
   - 只管JDBC操作
   - 其他工作委托出去

---

**第二课完成！休息3分钟，准备第三课：参数处理和SQL注入！** ☕

