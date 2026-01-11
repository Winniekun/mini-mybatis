# 🎊 Day 2 完整总结

## 今日学习成果

恭喜你完成了Day 2的学习！今天的内容非常充实！

---

## ✅ 今天学到的内容

### 1. Configuration深入理解

#### 核心概念
```
Configuration = MyBatis的大脑
├── MapperRegistry（Mapper注册表）
├── MappedStatements（SQL语句映射）
├── InterceptorChain（拦截器链）
└── 数据源配置
```

#### 关键理解
- ✅ Configuration是全局唯一的配置中心
- ✅ 所有组件都通过它获取信息
- ✅ 在初始化阶段创建，运行期间只读
- ✅ 线程安全，可以被多个线程共享

---

### 2. XML配置解析全流程

#### 解析链路
```
SqlSessionFactoryBuilder.build()
  ↓
XMLConfigBuilder
  ├─> 解析mybatis-config.xml
  │   ├─> <environments> (数据源配置)
  │   └─> <mappers> (Mapper文件列表)
  ↓
XMLMapperBuilder (for each Mapper)
  ├─> 解析namespace
  ├─> 注册Mapper接口
  └─> 解析SQL标签
      ├─> <select>
      ├─> <insert>
      ├─> <update>
      └─> <delete>
          ↓
      创建MappedStatement
          ↓
      注册到Configuration
```

#### 关键步骤
1. **DOM解析XML**：使用DOM4J库
2. **提取配置信息**：遍历XML节点
3. **创建Java对象**：MappedStatement等
4. **注册到Configuration**：存入Map

---

### 3. MappedStatement详解

#### 结构
```java
MappedStatement {
    id: "完整的statementId"
    sqlCommandType: SELECT/INSERT/UPDATE/DELETE
    sql: "SQL语句（含占位符）"
    parameterType: 参数类型
    resultType: 返回类型
    configuration: Configuration引用
}
```

#### 作用
- ✅ 存储一条SQL的完整信息
- ✅ 通过statementId唯一标识
- ✅ 执行时从Configuration中获取

---

### 4. 实践：创建ProductMapper

#### 完成的工作
1. ✅ 创建数据库表（product）
2. ✅ 创建实体类（Product.java）
3. ✅ 创建Mapper接口（ProductMapper.java）
4. ✅ 创建XML配置（ProductMapper.xml）
5. ✅ 注册到配置文件
6. ✅ 编写测试代码

#### 三个文件的对应关系
```
Product.java (实体)
    ↕ 字段对应
ProductMapper.java (接口)
    ↕ 方法 ←→ id
ProductMapper.xml (配置)
    ↕ namespace ←→ 接口全限定名
```

---

## 🎯 重点知识回顾

### 1. namespace的三重作用

```
1. 关联接口和XML
   namespace = "com.mybatis.test.mapper.ProductMapper"
   对应 ProductMapper.java

2. 构建statementId
   namespace + "." + id
   = "com.mybatis.test.mapper.ProductMapper.selectById"

3. 避免冲突
   不同namespace可以有相同的id
```

### 2. 参数占位符 #{参数名}

```java
// 简单类型
SELECT * FROM user WHERE id = #{id}
// id直接取参数值

// 对象类型
INSERT INTO product (product_name, price) 
VALUES (#{productName}, #{price})
// 调用 product.getProductName()
// 调用 product.getPrice()
```

### 3. 字段名自动转换

```
数据库（下划线）     Java（驼峰）
────────────────────────────────
product_name    →   productName
create_time     →   createTime
user_id         →   userId
```

MyBatis自动处理！

---

## 📊 完整的初始化流程（复习）

```
用户代码
  ↓
SqlSessionFactoryBuilder.build()
  ↓
XMLConfigBuilder
  ├─> new Configuration()
  ├─> 解析数据源配置
  │   └─> 填充Configuration字段
  └─> 解析Mapper列表
      ↓
  For each Mapper:
    XMLMapperBuilder
      ├─> 解析namespace
      ├─> 注册Mapper接口
      │   └─> MapperRegistry.addMapper()
      │       └─> 创建MapperProxyFactory
      └─> 解析SQL标签
          └─> 创建MappedStatement
              └─> Configuration.addMappedStatement()
  ↓
返回配置好的Configuration
  ↓
创建DefaultSqlSessionFactory
  ↓
返回给用户
```

---

## 💡 设计模式应用

### 1. 建造者模式
```java
// XMLConfigBuilder
Configuration config = new XMLConfigBuilder(inputStream)
    .getConfiguration();

// MappedStatement.Builder
MappedStatement ms = new MappedStatement.Builder(...)
    .sql(sql)
    .parameterType(...)
    .resultType(...)
    .build();
```

### 2. 工厂模式
```java
// MapperProxyFactory
MapperProxyFactory<UserMapper> factory = ...;
UserMapper mapper = factory.newInstance(sqlSession);
```

### 3. 注册表模式
```java
// MapperRegistry
mapperRegistry.addMapper(UserMapper.class);
UserMapper mapper = mapperRegistry.getMapper(UserMapper.class, session);
```

---

## 🤔 Day 2 思考题答案

### 1. Configuration为什么不用单例？
**答案**：
- 可能需要连接多个数据库
- 测试时需要多个实例
- 通过SqlSessionFactory保证唯一性

### 2. namespace写错了会怎样？
**答案**：
- 如果类不存在：警告，但不中断
- 无法通过getMapper()获取代理对象
- SQL执行时找不到Mapper

### 3. 字段名不匹配怎么办？
**答案**：
- 方案1：MyBatis自动转换（下划线↔驼峰）
- 方案2：SQL中使用别名
- 方案3：使用<resultMap>手动映射

---

## 📚 学到的技能

### 技能1：阅读XML配置
- ✅ 理解每个标签的作用
- ✅ 知道配置项的含义
- ✅ 能够排查配置错误

### 技能2：创建Mapper
- ✅ 设计实体类
- ✅ 定义Mapper接口
- ✅ 编写XML配置
- ✅ 测试CRUD操作

### 技能3：调试能力
- ✅ 理解初始化流程
- ✅ 知道在哪里打断点
- ✅ 能够跟踪配置解析过程

---

## 🎯 明天预告：Day 3

明天我们将学习：

### 1. Executor深入
- SimpleExecutor vs ReuseExecutor vs BatchExecutor
- SQL执行的完整流程
- StatementHandler详解

### 2. 参数处理
- 简单参数 vs 复杂参数
- ParameterHandler的作用
- #{} 和 ${} 的本质区别

### 3. 实践
- 实现一个带复杂参数的查询
- 理解SQL注入问题
- 学习防SQL注入的最佳实践

---

## 📋 Day 2 作业检查

### 必做任务
- [x] 理解Configuration的结构和作用
- [x] 掌握XML配置解析流程
- [x] 创建完整的ProductMapper
- [x] 运行并验证所有功能

### 选做任务
- [ ] 尝试添加价格区间查询
- [ ] 创建OrderMapper
- [ ] 思考关联查询的实现

---

## 📊 学习进度

```
Week 1: 基础入门
  Day 1: ✅ 基本使用和执行流程
  Day 2: ✅ Configuration和配置解析
  Day 3: ⏳ Executor和参数处理
  Day 4: ⏳ ResultSetHandler深入
  Day 5: ⏳ 完整项目实战
```

---

## 💪 激励时刻

今天的内容很有挑战性，你坚持下来了！

你现在已经理解了：
- ✅ MyBatis的初始化过程（从配置文件到可用的框架）
- ✅ Configuration的核心作用（配置中心）
- ✅ MappedStatement的创建和存储
- ✅ 如何创建自己的Mapper

**这些都是面试的重点内容！** 🎯

---

## 🌙 今晚的任务

1. **复习今天的笔记**
   - 重点：Configuration的结构
   - 重点：XML解析流程
   - 重点：三个文件的对应关系

2. **运行Product相关代码**
   - 确保能成功执行
   - 观察日志输出
   - 理解执行流程

3. **思考问题**
   - 如果要实现关联查询（User关联Order），应该如何设计？
   - 如何实现动态SQL（条件查询）？
   - 如何实现分页？

---

**Day 2圆满完成！好好休息，明天见！** 😴💤

**记住**：理解原理比记住API更重要！💡

