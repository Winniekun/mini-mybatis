# Day 1 深入学习 - 执行流程分析

## 🎯 刚才发生了什么？

### 日志分析

当你运行 `testMapperProxy()` 时，看到的日志可以分为**3个阶段**：

#### 阶段1：初始化阶段（@Before方法）
```
SqlSessionFactory初始化完成！
```

这一步做了什么？
```java
InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(inputStream);
```

**背后的流程**：
```
1. Resources.getResourceAsStream()
   └─> 从classpath加载配置文件
   
2. SqlSessionFactoryBuilder.build()
   └─> 创建XMLConfigBuilder
       └─> 解析<environments>标签（数据源配置）
       └─> 解析<mappers>标签
           └─> 加载 UserMapper.xml
               └─> 创建XMLMapperBuilder
                   └─> 解析namespace: com.mybatis.test.mapper.UserMapper
                   └─> 解析<select>标签
                       └─> 创建MappedStatement对象
                           - id: com.mybatis.test.mapper.UserMapper.selectById
                           - sql: SELECT * FROM user WHERE id = #{id}
                           - resultType: User.class
                   └─> 注册Mapper接口到MapperRegistry
   
3. 返回DefaultSqlSessionFactory对象
```

---

#### 阶段2：执行阶段
```
========== 测试：Mapper接口代理 ==========
Mapper代理对象: com.sun.proxy.$Proxy5
```

**这里是核心！** `$Proxy5` 是什么？

```java
UserMapper mapper = session.getMapper(UserMapper.class);
```

**背后的魔法**：
```
1. SqlSession.getMapper(UserMapper.class)
   └─> Configuration.getMapper(UserMapper.class, sqlSession)
       └─> MapperRegistry.getMapper(UserMapper.class, sqlSession)
           └─> MapperProxyFactory.newInstance(sqlSession)
               └─> JDK动态代理创建代理对象
                   new MapperProxy(sqlSession, UserMapper.class)
                   ↓
                   Proxy.newProxyInstance(
                       classLoader,
                       new Class[]{UserMapper.class},  // 实现UserMapper接口
                       mapperProxy                      // InvocationHandler
                   )
                   ↓
                   返回 $Proxy5（实现了UserMapper接口的代理对象）
```

**关键理解**：
- `$Proxy5` 是JDK动态代理生成的类
- 它实现了 `UserMapper` 接口
- 所有方法调用都会被 `MapperProxy` 拦截

---

#### 阶段3：方法调用
```java
User user = mapper.selectById(1L);
```

**完整的执行链路**：

```
1. 调用 mapper.selectById(1L)
   ↓
2. JDK代理拦截，调用 MapperProxy.invoke()
   ↓
3. MapperProxy.invoke() 中：
   - 构建statementId: "com.mybatis.test.mapper.UserMapper.selectById"
   - 获取参数: 1L
   - 判断返回类型: User（单个对象）
   - 调用 sqlSession.selectOne(statementId, 1L)
   ↓
4. DefaultSqlSession.selectOne()
   - 调用 selectList()
   - 检查结果数量（必须是1条）
   ↓
5. SimpleExecutor.query()
   - 从Configuration获取MappedStatement
   - 创建StatementHandler
   ↓
6. StatementHandler.prepare()
   - 获取SQL: "SELECT * FROM user WHERE id = #{id}"
   - 替换占位符: "SELECT * FROM user WHERE id = ?"
   - 创建PreparedStatement
   ↓
7. StatementHandler.parameterize()
   - 设置参数: ps.setObject(1, 1L)
   ↓
8. StatementHandler.query()
   - 执行SQL: ps.executeQuery()
   - 得到ResultSet
   ↓
9. ResultSetHandler.handleResultSet()
   - 遍历ResultSet
   - 创建User对象: new User()
   - 映射字段:
     * id列 → user.setId(1L)
     * username列 → user.setUsername("zhangsan")
     * password列 → user.setPassword("123456")
     * email列 → user.setEmail("zhangsan@example.com")
     * create_time列 → user.setCreateTime(LocalDateTime)
   ↓
10. 返回User对象
```

---

## 🎯 核心原理：Mapper接口的动态代理

这是MyBatis最精彩的部分！让我们深入理解：

### 为什么不需要实现类？

```java
// 你只定义了接口
public interface UserMapper {
    User selectById(Long id);
}

// 没有这个类！
public class UserMapperImpl implements UserMapper {
    public User selectById(Long id) {
        // 不需要写这个实现
    }
}

// 但是可以直接调用
UserMapper mapper = session.getMapper(UserMapper.class);
User user = mapper.selectById(1L);  // 为什么能执行？
```

### 动态代理原理

**Step 1: 创建代理对象**
```java
// MapperProxyFactory.newInstance()
MapperProxy<UserMapper> mapperProxy = new MapperProxy<>(sqlSession, UserMapper.class);

UserMapper mapper = (UserMapper) Proxy.newProxyInstance(
    UserMapper.class.getClassLoader(),
    new Class[]{UserMapper.class},
    mapperProxy
);
// 返回的mapper实际上是 $Proxy5
```

**Step 2: 拦截方法调用**
```java
// MapperProxy.invoke()
public Object invoke(Object proxy, Method method, Object[] args) {
    // method = selectById方法
    // args = [1L]
    
    // 1. 构建statementId
    String statementId = "com.mybatis.test.mapper.UserMapper.selectById";
    
    // 2. 获取参数
    Object parameter = args[0]; // 1L
    
    // 3. 根据返回类型选择执行方法
    if (Collection.class.isAssignableFrom(method.getReturnType())) {
        return sqlSession.selectList(statementId, parameter);
    } else {
        return sqlSession.selectOne(statementId, parameter);
    }
}
```

### 类比理解

想象你是一个**接线员**：

```
用户打电话（调用方法）
    ↓
你接听（MapperProxy拦截）
    ↓
你问：要找谁？（解析方法名）
用户说：selectById，参数是1L
    ↓
你查电话簿（Configuration.getMappedStatement）
找到：UserMapper.selectById → SELECT * FROM user WHERE id = ?
    ↓
你转接电话（SqlSession.selectOne）
    ↓
执行查询，返回结果
```

---

## 🔬 实践：调试代码

现在，让我们通过调试来验证这个流程！

### 调试步骤

1. **在关键位置打断点**：
   - `MapperProxy.invoke()` - 第98行
   - `DefaultSqlSession.selectOne()` - 第86行
   - `SimpleExecutor.query()` - 第48行
   - `StatementHandler.query()` - 第75行
   - `ResultSetHandler.handleResultSet()` - 第45行

2. **Debug模式运行测试**

3. **观察变量值**：
   - method对象（方法名、返回类型）
   - statementId的值
   - SQL语句的变化
   - ResultSet的内容
   - 最终的User对象

---

## 💡 设计模式分析

这个流程中用到了多个设计模式：

### 1. 工厂模式
```java
// SqlSessionFactory
SqlSession session = factory.openSession();

// MapperProxyFactory
UserMapper mapper = factory.newInstance(sqlSession);
```

### 2. 代理模式
```java
// JDK动态代理
Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
```

### 3. 模板方法模式
```java
// Executor定义流程
query() {
    prepare();     // 准备Statement
    parameterize(); // 设置参数
    execute();      // 执行SQL
    handleResult(); // 处理结果
}
```

### 4. 建造者模式
```java
// SqlSessionFactoryBuilder
new SqlSessionFactoryBuilder().build(inputStream);

// MappedStatement.Builder
new MappedStatement.Builder(config, id, sqlType)
    .sql(sql)
    .resultType(User.class)
    .build();
```

---

## 🎓 重要概念总结

### 1. statementId（重要！）

```
格式: namespace.methodId
示例: com.mybatis.test.mapper.UserMapper.selectById

作用: SQL语句的唯一标识符
     Configuration通过它找到对应的MappedStatement
     MappedStatement包含了SQL和所有配置信息
```

### 2. MappedStatement（核心！）

```java
public class MappedStatement {
    private String id;              // statementId
    private SqlCommandType sqlType; // SELECT/INSERT/UPDATE/DELETE
    private String sql;             // SQL语句
    private Class<?> resultType;    // 返回类型
    private Class<?> parameterType; // 参数类型
    // ... 其他配置
}
```

### 3. 三层映射关系

```
Mapper接口方法  →  statementId  →  MappedStatement  →  数据库SQL

selectById()  →  UserMapper.selectById  →  MappedStatement  →  SELECT * FROM user...
```

---

## 🤔 思考题

### 1. 如果我有两个方法名相同但参数不同，会怎样？

```java
public interface UserMapper {
    User selectById(Long id);
    User selectById(String username);  // ❌ 会有问题吗？
}
```

<details>
<summary>答案</summary>

会有问题！因为statementId的构建只用了：
```java
String statementId = namespace + "." + method.getName();
```

不包含参数类型，所以两个方法的statementId相同，会冲突！

**解决方案**：
- MyBatis不支持方法重载（除非使用注解指定不同的statementId）
- 应该用不同的方法名：`selectById()` 和 `selectByUsername()`
</details>

### 2. 如果XML中的方法在接口中不存在，会怎样？

```xml
<select id="selectById">...</select>
```

```java
// 接口中没有selectById方法
public interface UserMapper {
    User findById(Long id);  // 名字不一样
}
```

<details>
<summary>答案</summary>

不会报错！因为：
1. XML解析时只是创建MappedStatement
2. 只有当你调用`findById()`时才会报错
3. 报错信息：找不到statementId为"UserMapper.findById"的SQL

所以：XML和接口方法名必须一致！
</details>

---

## 📝 今日学习成果

你现在理解了：

✅ MyBatis的完整执行流程（10步）
✅ Mapper接口的动态代理原理
✅ statementId的作用和重要性
✅ MappedStatement的结构
✅ JDK动态代理的应用
✅ 5个核心设计模式

---

## 🎯 明天预告

明天我们会深入学习：

1. **Configuration的秘密**
   - 如何存储所有配置信息
   - 为什么是单例模式

2. **XML解析过程**
   - XMLConfigBuilder如何工作
   - XMLMapperBuilder如何解析SQL

3. **实践**：
   - 自己写一个Mapper
   - 配置一个复杂的SQL

---

**今天到此结束！休息一下，消化今天的内容！** 💪

