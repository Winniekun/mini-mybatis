# Day 5 学习笔记 - Part 1: 完整项目回顾

## 🎯 第一课：从0到1的完整回顾

### 我们构建了什么？

**一个简化版的MyBatis框架**，实现了ORM的核心功能：

```
从用户调用 → 执行SQL → 返回结果

全自动化：
✅ SQL不用写在Java代码里
✅ 参数不用手动设置
✅ 结果不用手动映射
✅ 连接不用手动管理
```

---

## 📚 完整架构回顾

### 整体架构图

```
┌─────────────────────────────────────────────────────┐
│                   用户层                             │
│  UserMapper mapper = session.getMapper(...);        │
│  User user = mapper.selectById(1L);                 │
└────────────────────┬────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────┐
│                 接口层 (Day 1)                       │
│  ┌──────────────┐        ┌─────────────────┐       │
│  │ SqlSession   │        │  MapperProxy    │       │
│  │ - selectOne  │   ←──  │  (动态代理)     │       │
│  │ - selectList │        │  JDK Proxy      │       │
│  │ - update     │        └─────────────────┘       │
│  │ - commit     │                                   │
│  └──────────────┘                                   │
└────────────────────┬────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────┐
│                配置层 (Day 2)                        │
│  ┌──────────────────────────────────────────┐      │
│  │          Configuration                   │      │
│  │  - 数据库配置                             │      │
│  │  - MappedStatement管理                   │      │
│  │  - MapperRegistry                        │      │
│  │  - InterceptorChain                      │      │
│  └──────────────────────────────────────────┘      │
│                                                     │
│  ┌──────────────────┐  ┌──────────────────┐       │
│  │XMLConfigBuilder  │  │XMLMapperBuilder  │       │
│  │ (解析config.xml) │  │ (解析Mapper.xml) │       │
│  └──────────────────┘  └──────────────────┘       │
└────────────────────┬────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────┐
│                执行层 (Day 3)                        │
│  ┌──────────────┐                                   │
│  │  Executor    │                                   │
│  │  - query()   │                                   │
│  │  - update()  │                                   │
│  └──────┬───────┘                                   │
│         ↓                                           │
│  ┌────────────────────┐                             │
│  │ StatementHandler   │                             │
│  │  - prepare()       │  #{} → ?                   │
│  │  - parameterize()  │  设置参数                   │
│  │  - query()         │  执行SQL                    │
│  │  - update()        │                             │
│  └────────────────────┘                             │
└────────────────────┬────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────┐
│                映射层 (Day 4)                        │
│  ┌────────────────────────┐                         │
│  │  ResultSetHandler      │                         │
│  │  - handleResultSet()   │  ResultSet → Object    │
│  │  - 自动字段映射         │                         │
│  │  - 类型转换            │                         │
│  │  - 驼峰命名转换         │                         │
│  └────────────────────────┘                         │
└─────────────────────────────────────────────────────┘
```

---

## 🔍 核心组件回顾

### 1. MapperProxy - 动态代理 (Day 1)

**作用**：拦截Mapper接口方法调用

```java
public class MapperProxy<T> implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 1. 构建statementId
        String statementId = mapperInterface.getName() + "." + method.getName();
        // com.mybatis.test.mapper.UserMapper.selectById
        
        // 2. 获取参数
        Object parameter = args != null && args.length > 0 ? args[0] : null;
        
        // 3. 判断方法类型，调用SqlSession
        Class<?> returnType = method.getReturnType();
        if (Collection.class.isAssignableFrom(returnType)) {
            return sqlSession.selectList(statementId, parameter);
        } else if (returnType == void.class || returnType == int.class) {
            return sqlSession.update(statementId, parameter);
        } else {
            return sqlSession.selectOne(statementId, parameter);
        }
    }
}
```

**关键点**：
- ✅ 用户调用接口方法
- ✅ 代理拦截，构建statementId
- ✅ 委托给SqlSession
- ✅ 用户无感知

---

### 2. Configuration - 配置中心 (Day 2)

**作用**：管理所有配置信息

```java
public class Configuration {
    // 数据库配置
    private String driver;
    private String url;
    private String username;
    private String password;
    
    // MappedStatement管理
    private Map<String, MappedStatement> mappedStatements = new HashMap<>();
    
    // Mapper注册表
    private MapperRegistry mapperRegistry = new MapperRegistry(this);
    
    // 拦截器链
    private InterceptorChain interceptorChain = new InterceptorChain();
    
    // 添加MappedStatement
    public void addMappedStatement(String id, MappedStatement ms) {
        mappedStatements.put(id, ms);
    }
    
    // 获取MappedStatement
    public MappedStatement getMappedStatement(String id) {
        return mappedStatements.get(id);
    }
}
```

**管理的内容**：
- ✅ 数据库连接信息
- ✅ 所有SQL语句（MappedStatement）
- ✅ 所有Mapper接口（MapperRegistry）
- ✅ 所有拦截器（InterceptorChain）

---

### 3. XMLConfigBuilder - 配置解析器 (Day 2)

**作用**：解析mybatis-config.xml

```java
public class XMLConfigBuilder {
    public Configuration parse() {
        Configuration configuration = new Configuration();
        
        // 1. 解析<environments>标签
        Element environments = root.element("environments");
        Element dataSource = environments.element("dataSource");
        
        // 设置数据库配置
        configuration.setDriver(getProperty(dataSource, "driver"));
        configuration.setUrl(getProperty(dataSource, "url"));
        configuration.setUsername(getProperty(dataSource, "username"));
        configuration.setPassword(getProperty(dataSource, "password"));
        
        // 2. 解析<mappers>标签
        Element mappers = root.element("mappers");
        List<Element> mapperList = mappers.elements("mapper");
        
        for (Element mapper : mapperList) {
            String resource = mapper.attributeValue("resource");
            // 解析每个Mapper XML
            parseMapperXml(configuration, resource);
        }
        
        return configuration;
    }
}
```

**解析流程**：
```
mybatis-config.xml
    ↓
<environments> → 数据库配置
    ↓
<mappers> → Mapper列表
    ↓
逐个解析Mapper XML
    ↓
生成MappedStatement
    ↓
注册到Configuration
```

---

### 4. XMLMapperBuilder - Mapper解析器 (Day 2)

**作用**：解析Mapper XML文件

```java
public class XMLMapperBuilder {
    public void parse() {
        // 1. 获取namespace
        String namespace = root.attributeValue("namespace");
        
        // 2. 注册Mapper接口
        Class<?> mapperClass = Class.forName(namespace);
        configuration.addMapper(mapperClass);
        
        // 3. 解析<select>标签
        List<Element> selects = root.elements("select");
        for (Element select : selects) {
            String id = select.attributeValue("id");
            String resultType = select.attributeValue("resultType");
            String sql = select.getTextTrim();
            
            // 构建MappedStatement
            MappedStatement ms = new MappedStatement(
                namespace + "." + id,
                SqlCommandType.SELECT,
                sql,
                resultType
            );
            
            // 注册到Configuration
            configuration.addMappedStatement(ms.getId(), ms);
        }
        
        // 4. 解析<insert>、<update>、<delete>标签
        // ...
    }
}
```

**解析内容**：
- ✅ namespace → Mapper接口
- ✅ <select> → SELECT语句
- ✅ <insert> → INSERT语句
- ✅ <update> → UPDATE语句
- ✅ <delete> → DELETE语句

---

### 5. Executor - 执行协调器 (Day 3)

**作用**：协调SQL执行流程

```java
public class SimpleExecutor implements Executor {
    public <E> List<E> query(String statementId, Object parameter) {
        // 1. 获取MappedStatement
        MappedStatement ms = configuration.getMappedStatement(statementId);
        
        // 2. 创建StatementHandler
        StatementHandler handler = new StatementHandler(configuration);
        
        // 3. 准备Statement
        Statement statement = handler.prepare(connection, ms.getSql());
        
        // 4. 设置参数
        handler.parameterize(statement, parameter);
        
        // 5. 执行查询
        List<E> result = handler.query(statement, ms.getResultType());
        
        // 6. 关闭Statement
        statement.close();
        
        return result;
    }
}
```

**职责**：
- ✅ 获取SQL配置
- ✅ 创建StatementHandler
- ✅ 协调执行流程
- ✅ 管理资源生命周期

---

### 6. StatementHandler - JDBC封装 (Day 3)

**作用**：封装JDBC操作

```java
public class StatementHandler {
    // 准备Statement
    public Statement prepare(Connection connection, String sql) {
        // #{id} → ?
        String preparedSql = sql.replaceAll("#\\{[^}]+\\}", "?");
        return connection.prepareStatement(preparedSql);
    }
    
    // 设置参数
    public void parameterize(Statement statement, Object parameter) {
        PreparedStatement ps = (PreparedStatement) statement;
        if (isSimpleType(parameter)) {
            ps.setObject(1, parameter);
        }
    }
    
    // 执行查询
    public <E> List<E> query(Statement statement, Class<?> resultType) {
        PreparedStatement ps = (PreparedStatement) statement;
        ResultSet resultSet = ps.executeQuery();
        return resultSetHandler.handleResultSet(resultSet, resultType);
    }
    
    // 执行更新
    public int update(Statement statement) {
        PreparedStatement ps = (PreparedStatement) statement;
        return ps.executeUpdate();
    }
}
```

**三大方法**：
- ✅ prepare() - 创建PreparedStatement
- ✅ parameterize() - 设置参数
- ✅ query()/update() - 执行SQL

---

### 7. ResultSetHandler - 结果映射 (Day 4)

**作用**：ResultSet → Java对象

```java
public class ResultSetHandler {
    public <E> List<E> handleResultSet(ResultSet rs, Class<?> resultType) {
        List<E> resultList = new ArrayList<>();
        
        // 获取元数据
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // 遍历结果集
        while (rs.next()) {
            // 创建对象
            Object bean = resultType.newInstance();
            
            // 建立字段映射表
            Map<String, Field> fieldMap = buildFieldMap(resultType);
            
            // 遍历每列，设置值
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object columnValue = rs.getObject(i);
                
                Field field = fieldMap.get(columnName.toLowerCase());
                if (field != null && columnValue != null) {
                    Object value = convertType(columnValue, field.getType());
                    field.set(bean, value);
                }
            }
            
            resultList.add((E) bean);
        }
        
        return resultList;
    }
}
```

**核心能力**：
- ✅ 自动字段映射
- ✅ 驼峰命名转换
- ✅ 类型自动转换

---

## 🔄 完整执行流程

### 示例：查询用户

```java
// 用户代码
User user = mapper.selectById(1L);
```

### 详细执行步骤

```
第1步：MapperProxy拦截
─────────────────────────
mapper.selectById(1L)
    ↓
MapperProxy.invoke()
    ↓
构建statementId = "com.mybatis.test.mapper.UserMapper.selectById"
获取参数 = 1L
判断返回类型 = User.class (不是集合)
    ↓
调用 sqlSession.selectOne(statementId, parameter)


第2步：SqlSession处理
─────────────────────────
DefaultSqlSession.selectOne()
    ↓
调用 selectList(statementId, parameter)
    ↓
取第一个元素返回


第3步：Executor执行
─────────────────────────
SimpleExecutor.query()
    ↓
① 获取MappedStatement
   ms = configuration.getMappedStatement(
       "com.mybatis.test.mapper.UserMapper.selectById"
   )
   得到：
   - SQL: "SELECT * FROM user WHERE id = #{id}"
   - resultType: User.class
    ↓
② 创建StatementHandler
   handler = new StatementHandler(configuration)
    ↓
③ 准备Statement
   handler.prepare(connection, ms.getSql())


第4步：StatementHandler处理
─────────────────────────
① prepare()
   原始SQL: "SELECT * FROM user WHERE id = #{id}"
   替换后: "SELECT * FROM user WHERE id = ?"
   创建: PreparedStatement ps = conn.prepareStatement(sql)
    ↓
② parameterize()
   ps.setObject(1, 1L)  // 设置第1个?的值为1L
    ↓
③ query()
   ResultSet rs = ps.executeQuery()
   // 现在rs包含查询结果


第5步：ResultSetHandler处理
─────────────────────────
ResultSetHandler.handleResultSet(rs, User.class)
    ↓
① 获取元数据
   columnCount = 5
   columns = ["id", "username", "password", "email", "create_time"]
    ↓
② 遍历结果集
   rs.next()  // 移到第1行
    ↓
③ 创建对象
   User user = new User()
    ↓
④ 建立字段映射表
   fieldMap = {
       "id" → id字段,
       "username" → username字段,
       "create_time" → createTime字段,
       ...
   }
    ↓
⑤ 遍历每列设置值
   列1: id = 1L
       field.set(user, 1L)
       // user.id = 1L
   
   列2: username = "admin"
       field.set(user, "admin")
       // user.username = "admin"
   
   列5: create_time = Timestamp(...)
       value = convertType(...)
       field.set(user, value)
       // user.createTime = LocalDateTime(...)
    ↓
⑥ 返回结果
   return List<User> [user]


第6步：返回结果
─────────────────────────
SimpleExecutor → DefaultSqlSession → MapperProxy → 用户
    ↓
User user = mapper.selectById(1L);
// user.id = 1L
// user.username = "admin"
// user.createTime = ...
```

---

## 📊 数据流转图

```
┌─────────────┐
│   用户      │
│ selectById  │
│    (1L)     │
└──────┬──────┘
       ↓ 参数: 1L
┌──────────────┐
│ MapperProxy  │
└──────┬───────┘
       ↓ statementId + parameter
┌──────────────┐
│ SqlSession   │
└──────┬───────┘
       ↓ statementId + parameter
┌──────────────┐
│  Executor    │
└──────┬───────┘
       ↓ MappedStatement + parameter
┌────────────────────┐
│ StatementHandler   │
│  prepare()         │
│  parameterize()    │
│  query()           │
└──────┬─────────────┘
       ↓ ResultSet
┌────────────────────┐
│ ResultSetHandler   │
│  handleResultSet() │
└──────┬─────────────┘
       ↓ List<User>
┌──────────────┐
│   用户       │
│  User user   │
└──────────────┘
```

---

## 🎯 关键设计模式

### 1. 代理模式 (Proxy Pattern)

```
MapperProxy实现InvocationHandler
用户 → 代理 → 真实对象(SqlSession)

优势：
- 用户只需定义接口
- 不需要写实现类
- 代理自动处理调用
```

### 2. 建造者模式 (Builder Pattern)

```
SqlSessionFactoryBuilder
    .build(inputStream)
        ↓
    XMLConfigBuilder.parse()
        ↓
    Configuration
        ↓
    SqlSessionFactory

优势：
- 分步构建复杂对象
- 隐藏构建细节
```

### 3. 模板方法模式 (Template Method)

```
Executor.query() {
    getMappedStatement()  // 步骤1
    createHandler()       // 步骤2
    prepare()            // 步骤3
    parameterize()       // 步骤4
    executeQuery()       // 步骤5
}

优势：
- 固定流程
- 部分步骤可定制
```

### 4. 策略模式 (Strategy Pattern)

```
ResultSetHandler.createResultObject() {
    if (简单类型) {
        策略1
    } else if (Map类型) {
        策略2
    } else {
        策略3
    }
}

优势：
- 根据类型选择策略
- 易于扩展新策略
```

---

## 🎯 核心要点总结

### 1. 分层架构

```
接口层 → 配置层 → 执行层 → 映射层
    ↓       ↓       ↓       ↓
MapperProxy Configuration Executor ResultSetHandler
```

### 2. 职责清晰

```
每个组件只做一件事：
- MapperProxy: 拦截调用
- Configuration: 管理配置
- Executor: 协调执行
- StatementHandler: JDBC操作
- ResultSetHandler: 结果映射
```

### 3. 自动化

```
用户只需要：
1. 定义Mapper接口
2. 写Mapper XML
3. 调用方法

框架自动：
1. 生成实现
2. 解析配置
3. 执行SQL
4. 映射结果
```

---

## 💡 我们实现了什么？

### 核心功能 ✅

```
1. 动态代理
   ✅ MapperProxy
   ✅ 接口方法拦截
   ✅ 自动路由

2. 配置管理
   ✅ XML解析
   ✅ MappedStatement管理
   ✅ 数据源配置

3. SQL执行
   ✅ Executor
   ✅ StatementHandler
   ✅ PreparedStatement
   ✅ 参数设置

4. 结果映射
   ✅ ResultSetHandler
   ✅ 自动字段映射
   ✅ 驼峰命名转换
   ✅ 类型转换

5. 基础设施
   ✅ 连接管理
   ✅ 事务管理
   ✅ 资源关闭
```

---

**第一课完成！我们已经回顾了整个项目！** 🎉

**休息3分钟，准备第二课：高级特性探索！** ☕

