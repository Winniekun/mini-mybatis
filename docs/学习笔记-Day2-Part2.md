# Day 2 学习笔记 - Part 2: XML配置解析

## 🔍 第二课：XML配置解析全流程

### 配置解析的完整链路

```
用户代码
  SqlSessionFactoryBuilder.build(inputStream)
                ↓
         XMLConfigBuilder创建
                ↓
         解析mybatis-config.xml
                ↓
    ┌───────────┴───────────┐
    ↓                       ↓
parseEnvironments      parseMappers
    ↓                       ↓
 数据源配置          加载Mapper.xml
    ↓                       ↓
填充Configuration    XMLMapperBuilder
                            ↓
                      解析SQL语句
                            ↓
                    创建MappedStatement
                            ↓
                   注册到Configuration
```

---

## 📖 阶段1：主配置文件解析

### mybatis-config.xml的结构

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<configuration>                    <!-- 根标签 -->
    
    <!-- 1. 环境配置 -->
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/test"/>
                <property name="username" value="root"/>
                <property name="password" value="123456"/>
            </dataSource>
        </environment>
    </environments>
    
    <!-- 2. Mapper文件配置 -->
    <mappers>
        <mapper resource="mapper/UserMapper.xml"/>
        <mapper resource="mapper/OrderMapper.xml"/>
    </mappers>
    
</configuration>
```

### 解析流程详解

#### Step 1: 创建DOM解析器

```java
SAXReader reader = new SAXReader();
Document document = reader.read(inputStream);
Element root = document.getRootElement(); // <configuration>
```

**技术点**：
- 使用DOM4J库解析XML
- SAXReader：基于事件的解析器
- Document：整个XML文档的对象模型
- Element：XML元素节点

#### Step 2: 解析<environments>

```java
private void parseEnvironments(Element environments) {
    // 1. 获取默认环境
    String defaultEnv = environments.attributeValue("default");
    // defaultEnv = "development"
    
    // 2. 获取所有<environment>标签
    List<Element> environmentList = environments.elements("environment");
    
    // 3. 找到默认环境并解析
    for (Element environment : environmentList) {
        String id = environment.attributeValue("id");
        if (id.equals(defaultEnv)) {
            // 解析这个环境的数据源配置
            parseDataSource(environment.element("dataSource"));
            break;
        }
    }
}
```

**关键理解**：
- MyBatis支持多环境配置（开发、测试、生产）
- 但同时只使用一个环境（由default指定）
- 这就是为什么要用`if (id.equals(defaultEnv))`

#### Step 3: 解析<dataSource>

```java
private void parseDataSource(Element dataSource) {
    // 获取所有<property>标签
    List<Element> properties = dataSource.elements("property");
    
    for (Element property : properties) {
        String name = property.attributeValue("name");
        String value = property.attributeValue("value");
        
        // 根据name设置到Configuration
        switch (name) {
            case "driver":
                configuration.setJdbcDriver(value);
                break;
            case "url":
                configuration.setJdbcUrl(value);
                break;
            // ...
        }
    }
}
```

**此时Configuration的状态**：
```java
configuration {
    jdbcDriver: "com.mysql.cj.jdbc.Driver"
    jdbcUrl: "jdbc:mysql://localhost:3306/mybatis_learn..."
    jdbcUsername: "root"
    jdbcPassword: "kwk961202"
}
```

#### Step 4: 解析<mappers>

```java
private void parseMappers(Element mappers) {
    List<Element> mapperList = mappers.elements("mapper");
    
    for (Element mapper : mapperList) {
        String resource = mapper.attributeValue("resource");
        // resource = "mapper/UserMapper.xml"
        
        // 1. 加载Mapper文件
        InputStream inputStream = Resources.getResourceAsStream(resource);
        
        // 2. 创建Mapper文件解析器
        XMLMapperBuilder mapperBuilder = 
            new XMLMapperBuilder(inputStream, configuration);
        
        // 3. 解析Mapper文件（这是重点！）
        mapperBuilder.parse();
    }
}
```

---

## 📖 阶段2：Mapper文件解析

现在我们进入最核心的部分：如何解析Mapper.xml文件

### UserMapper.xml的结构

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<mapper namespace="com.mybatis.test.mapper.UserMapper">
    
    <select id="selectById" 
            parameterType="java.lang.Long" 
            resultType="com.mybatis.test.entity.User">
        SELECT * FROM user WHERE id = #{id}
    </select>
    
    <select id="selectAll" 
            resultType="com.mybatis.test.entity.User">
        SELECT * FROM user ORDER BY id
    </select>
    
    <insert id="insert" 
            parameterType="com.mybatis.test.entity.User">
        INSERT INTO user (username, password, email, create_time)
        VALUES (#{username}, #{password}, #{email}, #{createTime})
    </insert>
    
</mapper>
```

### XMLMapperBuilder.parse()详解

```java
public void parse() {
    SAXReader reader = new SAXReader();
    Document document = reader.read(inputStream);
    Element root = document.getRootElement();
    
    // 1. 获取namespace（重要！）
    String namespace = root.attributeValue("namespace");
    // namespace = "com.mybatis.test.mapper.UserMapper"
    
    // 2. 注册Mapper接口
    registerMapper(namespace);
    
    // 3. 解析所有SQL标签
    parseStatements(root.elements("select"), namespace, SELECT);
    parseStatements(root.elements("insert"), namespace, INSERT);
    parseStatements(root.elements("update"), namespace, UPDATE);
    parseStatements(root.elements("delete"), namespace, DELETE);
}
```

### 关键步骤分解

#### Step 1: 注册Mapper接口

```java
private void registerMapper(String namespace) {
    try {
        // 1. 加载接口类
        Class<?> mapperClass = Class.forName(namespace);
        // mapperClass = UserMapper.class
        
        // 2. 检查是否已注册
        if (!configuration.hasMapper(mapperClass)) {
            // 3. 注册到Configuration
            configuration.addMapper(mapperClass);
            //    └─> mapperRegistry.addMapper(mapperClass)
            //        └─> 创建MapperProxyFactory
        }
    } catch (ClassNotFoundException e) {
        logger.warn("未找到Mapper接口: {}", namespace);
    }
}
```

**关键理解**：
- namespace必须是完整的类名
- 这个类必须存在且是接口
- 注册后，就可以通过`session.getMapper()`获取代理对象

#### Step 2: 解析SQL语句

```java
private void parseStatements(List<Element> elements, 
                             String namespace, 
                             SqlCommandType sqlCommandType) {
    for (Element element : elements) {
        // 1. 获取属性
        String id = element.attributeValue("id");
        // id = "selectById"
        
        String parameterType = element.attributeValue("parameterType");
        // parameterType = "java.lang.Long"
        
        String resultType = element.attributeValue("resultType");
        // resultType = "com.mybatis.test.entity.User"
        
        String sql = element.getTextTrim();
        // sql = "SELECT * FROM user WHERE id = #{id}"
        
        // 2. 构建statementId
        String statementId = namespace + "." + id;
        // statementId = "com.mybatis.test.mapper.UserMapper.selectById"
        
        // 3. 创建MappedStatement
        createMappedStatement(statementId, sqlCommandType, 
                             parameterType, resultType, sql);
    }
}
```

#### Step 3: 创建MappedStatement

```java
private void createMappedStatement(String statementId, 
                                   SqlCommandType sqlType,
                                   String parameterType, 
                                   String resultType, 
                                   String sql) {
    // 1. 使用Builder创建MappedStatement
    MappedStatement.Builder builder = 
        new MappedStatement.Builder(configuration, statementId, sqlType);
    
    // 2. 设置SQL
    builder.sql(sql);
    
    // 3. 设置参数类型
    if (parameterType != null) {
        Class<?> paramClass = Class.forName(parameterType);
        builder.parameterType(paramClass);
    }
    
    // 4. 设置返回类型
    if (resultType != null) {
        Class<?> resultClass = Class.forName(resultType);
        builder.resultType(resultClass);
    }
    
    // 5. 构建并注册
    MappedStatement ms = builder.build();
    configuration.addMappedStatement(statementId, ms);
}
```

---

## 🎯 解析完成后的Configuration状态

```java
Configuration {
    // 数据源配置
    jdbcDriver: "com.mysql.cj.jdbc.Driver"
    jdbcUrl: "jdbc:mysql://localhost:3306/mybatis_learn..."
    jdbcUsername: "root"
    jdbcPassword: "kwk961202"
    
    // Mapper注册表
    mapperRegistry: {
        knownMappers: {
            UserMapper.class → MapperProxyFactory实例
        }
    }
    
    // SQL语句映射
    mappedStatements: {
        "com.mybatis.test.mapper.UserMapper.selectById" → MappedStatement {
            id: "com.mybatis.test.mapper.UserMapper.selectById"
            sqlCommandType: SELECT
            sql: "SELECT * FROM user WHERE id = #{id}"
            parameterType: Long.class
            resultType: User.class
        },
        "com.mybatis.test.mapper.UserMapper.selectAll" → MappedStatement {...},
        "com.mybatis.test.mapper.UserMapper.insert" → MappedStatement {...}
    }
}
```

---

## 🔄 完整的初始化流程

```
┌──────────────────────────────────────┐
│  1. 用户调用                          │
│  SqlSessionFactoryBuilder.build()    │
└────────────┬─────────────────────────┘
             ↓
┌──────────────────────────────────────┐
│  2. 创建XMLConfigBuilder              │
│  new XMLConfigBuilder(inputStream)   │
└────────────┬─────────────────────────┘
             ↓
┌──────────────────────────────────────┐
│  3. 创建Configuration                 │
│  this.configuration = new Config()   │
└────────────┬─────────────────────────┘
             ↓
┌──────────────────────────────────────┐
│  4. 解析mybatis-config.xml           │
│  parse(inputStream)                  │
└────────────┬─────────────────────────┘
             ↓
      ┌──────┴──────┐
      ↓             ↓
┌─────────┐   ┌─────────┐
│解析环境  │   │解析Mapper│
│配置     │   │列表     │
└─────────┘   └────┬────┘
                   ↓
          ┌────────────────┐
          │ 遍历每个Mapper │
          └────────┬───────┘
                   ↓
      ┌────────────────────────┐
      │  5. 创建XMLMapperBuilder│
      │  new XMLMapperBuilder() │
      └────────────┬─────────────┘
                   ↓
      ┌────────────────────────┐
      │  6. 解析UserMapper.xml │
      │  mapperBuilder.parse()  │
      └────────────┬─────────────┘
                   ↓
          ┌────────┴────────┐
          ↓                 ↓
    ┌─────────┐      ┌─────────┐
    │注册Mapper│      │解析SQL  │
    │接口     │      │标签     │
    └─────────┘      └────┬────┘
                          ↓
                 ┌────────────────┐
                 │创建MappedStatement│
                 └────────┬───────┘
                          ↓
                 ┌────────────────┐
                 │注册到Configuration│
                 └────────────────┘
                          ↓
      ┌────────────────────────────┐
      │  7. 返回Configuration       │
      │  builder.getConfiguration() │
      └────────────┬───────────────┘
                   ↓
      ┌────────────────────────────┐
      │  8. 创建SqlSessionFactory   │
      │  new DefaultSqlSessionFactory│
      └────────────────────────────┘
```

---

## 💡 关键设计点

### 1. 为什么要分两个Builder？

```
XMLConfigBuilder     → 解析主配置文件
XMLMapperBuilder     → 解析Mapper文件

优点：
✅ 职责单一
✅ 代码清晰
✅ 易于维护
✅ 可以独立测试
```

### 2. 为什么用Builder模式？

```java
// 不用Builder（代码丑陋）
MappedStatement ms = new MappedStatement();
ms.setId("...");
ms.setSql("...");
ms.setParamType(...);
ms.setResultType(...);

// 使用Builder（代码优雅）
MappedStatement ms = new MappedStatement.Builder(config, id, type)
    .sql(sql)
    .parameterType(paramType)
    .resultType(resultType)
    .build();
```

### 3. namespace的作用

```
namespace有三重作用：

1. 作为statementId的前缀
   namespace.id → 完整的statementId

2. 关联Mapper接口
   namespace必须是接口的全限定名

3. 避免id冲突
   不同namespace可以有相同的id
```

---

## 🤔 思考题

### 1. 如果namespace写错了会怎样？

```xml
<mapper namespace="com.mybatis.test.mapper.UserMapperXXX">
```

<details>
<summary>点击查看答案</summary>

分两种情况：

情况1：类不存在
- registerMapper时Class.forName会抛异常
- 捕获后只是警告，不会中断
- 但是无法通过getMapper获取代理对象

情况2：类存在但不匹配
- 接口是UserMapper，namespace写成OrderMapper
- 可以解析成功
- 但调用时会找不到对应的SQL
</details>

### 2. 如果两个Mapper文件有相同的statementId？

```xml
<!-- UserMapper.xml -->
<select id="selectById">...</select>

<!-- OrderMapper.xml -->
<select id="selectById">...</select>
```

<details>
<summary>点击查看答案</summary>

不会冲突！因为：
- UserMapper的完整ID：com.mybatis.mapper.UserMapper.selectById
- OrderMapper的完整ID：com.mybatis.mapper.OrderMapper.selectById

namespace保证了唯一性！
</details>

### 3. 解析的顺序重要吗？

<details>
<summary>点击查看答案</summary>

对于基本功能，顺序不重要：
- 数据源配置 vs Mapper配置
- 不同Mapper文件的顺序

但对于高级功能可能重要：
- 如果有Mapper引用其他Mapper
- 如果有resultMap的继承关系
</details>

---

## 📊 配置解析的性能

```
解析阶段                     时间占比
────────────────────────────────────
读取XML文件                   10%
DOM解析                       20%
创建Java对象                  30%
反射加载类                    30%
注册到Configuration           10%
```

**优化点**：
- 配置只解析一次（应用启动时）
- 解析结果缓存在Configuration中
- 运行期间不再解析

---

**第二课完成！休息一下，准备第三课！** ☕

