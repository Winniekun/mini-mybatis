# Day 2 学习笔记 - Configuration与配置解析

## 🧠 第一课：Configuration - MyBatis的大脑

### Configuration是什么？

Configuration是MyBatis的**核心配置中心**，就像一个超级大仓库，存储了MyBatis运行所需的所有信息。

### 类比理解

想象你开了一家餐厅（MyBatis框架）：

```
Configuration = 餐厅总部
├── 菜单（所有SQL语句）
├── 厨师名单（Mapper接口注册表）
├── 供应商信息（数据源配置）
├── 厨房设备（Executor、StatementHandler等）
└── 特殊服务（插件、缓存等）

当服务员（SqlSession）接到订单（SQL请求）时：
1. 去总部查菜单（查MappedStatement）
2. 找对应的厨师（找Mapper）
3. 按照菜谱做菜（执行SQL）
4. 上菜（返回结果）
```

---

## 📖 Configuration的内部结构

让我们看看Configuration里面都有什么：

```java
public class Configuration {
    
    // 1. Mapper注册中心 ⭐核心
    private MapperRegistry mapperRegistry;
    // 存储所有Mapper接口
    // UserMapper, OrderMapper, ProductMapper...
    
    // 2. SQL语句仓库 ⭐核心
    private Map<String, MappedStatement> mappedStatements;
    // key: "com.mybatis.mapper.UserMapper.selectById"
    // value: MappedStatement（包含SQL、参数类型、返回类型等）
    
    // 3. 拦截器链
    private InterceptorChain interceptorChain;
    // 存储所有插件
    
    // 4. 数据库连接配置
    private String jdbcDriver;    // 驱动类名
    private String jdbcUrl;       // 数据库地址
    private String jdbcUsername;  // 用户名
    private String jdbcPassword;  // 密码
    
    // 5. 全局设置
    private boolean cacheEnabled = true;        // 是否开启缓存
    private boolean lazyLoadingEnabled = false; // 是否延迟加载
    private String defaultExecutorType = "SIMPLE"; // 执行器类型
}
```

---

## 🔍 深入理解：MapperRegistry

MapperRegistry是管理所有Mapper接口的注册中心。

### 工作原理

```
┌──────────────────────────────────────┐
│        MapperRegistry                │
├──────────────────────────────────────┤
│  Map<Class<?>, MapperProxyFactory>   │
│                                      │
│  UserMapper.class → Factory1         │
│  OrderMapper.class → Factory2        │
│  ProductMapper.class → Factory3      │
└──────────────────────────────────────┘
```

### 核心方法

```java
// 1. 注册Mapper接口
public <T> void addMapper(Class<T> type) {
    // 创建对应的代理工厂
    MapperProxyFactory<T> factory = new MapperProxyFactory<>(type);
    knownMappers.put(type, factory);
}

// 2. 获取Mapper代理对象
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    // 从注册表中找到对应的工厂
    MapperProxyFactory<T> factory = knownMappers.get(type);
    // 使用工厂创建代理对象
    return factory.newInstance(sqlSession);
}
```

### 注册时机

```
何时注册？在XML解析阶段！

XMLMapperBuilder.parse()
  └─> 解析namespace: com.mybatis.mapper.UserMapper
      └─> 尝试加载类: Class.forName("com.mybatis.mapper.UserMapper")
          └─> 如果类存在且是接口
              └─> configuration.addMapper(UserMapper.class)
                  └─> mapperRegistry.addMapper(UserMapper.class)
```

---

## 🗂️ 深入理解：MappedStatement

MappedStatement是SQL语句的完整描述。

### 一个MappedStatement包含什么？

```java
MappedStatement {
    id: "com.mybatis.test.mapper.UserMapper.selectById"
    sqlCommandType: SELECT
    sql: "SELECT id, username, password, email, create_time 
          FROM user WHERE id = #{id}"
    parameterType: java.lang.Long
    resultType: com.mybatis.test.entity.User
    useCache: true
    configuration: Configuration对象引用
}
```

### 它从哪里来？

```xml
<!-- UserMapper.xml -->
<select id="selectById" 
        parameterType="java.lang.Long" 
        resultType="com.mybatis.test.entity.User">
    SELECT * FROM user WHERE id = #{id}
</select>

                ↓ 解析
                
MappedStatement对象创建
                
                ↓ 存储
                
Configuration.mappedStatements.put(
    "com.mybatis.test.mapper.UserMapper.selectById",
    mappedStatement
)
```

### 如何使用？

```java
// 1. 执行SQL时，通过statementId获取
String statementId = "com.mybatis.test.mapper.UserMapper.selectById";
MappedStatement ms = configuration.getMappedStatement(statementId);

// 2. 从MappedStatement中获取所需信息
String sql = ms.getSql();              // 获取SQL
Class<?> resultType = ms.getResultType(); // 获取返回类型
SqlCommandType type = ms.getSqlCommandType(); // 获取SQL类型

// 3. 执行SQL
```

---

## 🎯 Configuration的生命周期

```
阶段1: 创建
  XMLConfigBuilder构造函数
    └─> new Configuration()

阶段2: 填充（解析配置文件）
  XMLConfigBuilder.parse()
    ├─> 解析数据源配置
    ├─> 解析Mapper文件
    └─> 填充Configuration各个字段

阶段3: 使用（贯穿整个应用生命周期）
  ├─> SqlSession需要它
  ├─> Executor需要它
  ├─> StatementHandler需要它
  └─> MapperProxy需要它

阶段4: 销毁
  应用关闭时自动回收
```

---

## 💡 为什么Configuration要设计成这样？

### 1. 集中管理
```
✅ 所有配置都在一个地方
✅ 组件间通过Configuration共享信息
✅ 修改配置不影响其他代码
```

### 2. 单一职责
```
Configuration只负责存储配置
不负责解析（由Builder完成）
不负责执行（由Executor完成）
```

### 3. 全局唯一
```
一个应用只需要一个Configuration
所有SqlSession共享同一个Configuration
避免重复解析配置文件
```

---

## 🤔 思考题

### 1. 为什么Configuration不使用单例模式？

<details>
<summary>点击查看答案</summary>

虽然一般情况下一个应用只有一个Configuration，但：
- 可能需要连接多个数据库（多个Configuration）
- 测试时可能需要创建多个实例
- 单例会增加测试难度

所以没有强制单例，而是：
- SqlSessionFactory持有Configuration
- 通过SqlSessionFactory保证Configuration的唯一性
</details>

### 2. Configuration是线程安全的吗？

<details>
<summary>点击查看答案</summary>

是的！因为：
- Configuration在初始化后就不再修改（只读）
- 所有的Map都是在初始化阶段填充的
- 运行期间只有读操作，没有写操作

所以可以被多个线程安全地共享。
</details>

### 3. 如果有两个SQL的id相同会怎样？

<details>
<summary>点击查看答案</summary>

会报错！因为：
- statementId必须唯一
- Map的key不能重复
- XMLMapperBuilder解析时会检查

这就是为什么要用：namespace.id 作为完整标识
- namespace: com.mybatis.mapper.UserMapper
- id: selectById
- 完整ID: com.mybatis.mapper.UserMapper.selectById

不同的namespace可以有相同的id！
</details>

---

## 📊 Configuration在架构中的位置

```
       ┌─────────────────────┐
       │ SqlSessionFactory   │
       │   持有Configuration │
       └──────────┬──────────┘
                  │
       ┌──────────▼──────────┐
       │   Configuration     │ ← 核心！
       │  (配置中心)         │
       └──────────┬──────────┘
                  │
      ┌───────────┼───────────┐
      ▼           ▼           ▼
┌─────────┐ ┌─────────┐ ┌─────────┐
│Mapper   │ │Mapped   │ │Executor │
│Registry │ │Statement│ │ etc.    │
└─────────┘ └─────────┘ └─────────┘
```

所有组件都通过Configuration获取信息！

---

## 🎯 小结

Configuration是MyBatis的核心：
- ✅ 存储所有配置信息
- ✅ 管理Mapper注册
- ✅ 管理SQL语句映射
- ✅ 提供全局访问点
- ✅ 贯穿整个生命周期

下一课我们学习：Configuration是如何被创建和填充的（XML解析）

---

**休息5分钟，准备进入第二课！** ☕

