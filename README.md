# 🚀 Mini-MyBatis - 从0到1手写MyBatis框架

## 📖 项目介绍

这是一个用于深入学习MyBatis框架原理的项目。通过手写一个简化版的MyBatis框架，深入理解MyBatis的核心设计思想和实现原理。

## 🎯 学习目标

1. **理解MyBatis的核心架构**
2. **掌握ORM框架的设计思路**
3. **熟悉常用设计模式的实际应用**
4. **达到面试级别的源码理解能力**

## 🏗️ 架构设计

```
MyBatis核心架构
├── 配置层 (Configuration)
│   ├── XML解析
│   ├── 配置加载
│   └── 环境配置
├── 接口层 (SqlSession)
│   ├── SqlSessionFactory
│   ├── SqlSession
│   └── Mapper接口
├── 核心处理层
│   ├── Executor (执行器)
│   ├── StatementHandler (语句处理器)
│   ├── ParameterHandler (参数处理器)
│   └── ResultSetHandler (结果集处理器)
├── 基础支撑层
│   ├── 数据源管理
│   ├── 事务管理
│   ├── 缓存机制
│   └── 插件机制
└── 动态SQL解析
    ├── OGNL表达式
    ├── 动态标签解析
    └── SQL生成
```

## 📚 实现阶段

### 第一阶段：基础架构 ✅
- [x] 项目初始化
- [x] 核心包结构设计
- [x] 基础配置类

### 第二阶段：配置解析
- [x] XML配置文件解析
- [x] Configuration配置类
- [x] SqlSessionFactory构建

### 第三阶段：SQL执行器
- [ ] Executor接口设计
- [ ] SimpleExecutor实现
- [ ] StatementHandler实现

### 第四阶段：结果映射
- [ ] ResultSetHandler实现
- [ ] 类型转换器TypeHandler
- [ ] 结果集映射逻辑

### 第五阶段：Mapper代理
- [ ] MapperProxy动态代理
- [ ] MapperProxyFactory
- [ ] MapperRegistry注册中心

### 第六阶段：插件机制
- [ ] Interceptor拦截器接口
- [ ] InterceptorChain责任链
- [ ] Plugin动态代理

### 第七阶段：缓存机制
- [ ] Cache接口设计
- [ ] PerpetualCache一级缓存
- [ ] 二级缓存实现

### 第八阶段：动态SQL
- [ ] SqlNode抽象
- [ ] IfSqlNode条件判断
- [ ] ForeachSqlNode循环
- [ ] OGNL表达式解析

### 第九阶段：事务管理
- [ ] Transaction接口
- [ ] JdbcTransaction实现
- [ ] TransactionFactory

### 第十阶段：完善测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] 性能测试

## 🔑 核心知识点

### 1. 设计模式应用
- **工厂模式**: SqlSessionFactory, MapperProxyFactory
- **建造者模式**: SqlSessionFactoryBuilder
- **单例模式**: Configuration
- **代理模式**: MapperProxy, Plugin
- **模板方法模式**: Executor
- **责任链模式**: InterceptorChain
- **装饰器模式**: Cache

### 2. 核心流程
```
初始化阶段：
SqlSessionFactoryBuilder → 解析配置 → 创建Configuration → 生成SqlSessionFactory

执行阶段：
SqlSession → MapperProxy → Executor → StatementHandler → JDBC → ResultSetHandler
```

### 3. 关键技术
- **JDK动态代理**: Mapper接口代理
- **JDBC封装**: 数据库操作
- **XML解析**: 配置文件和Mapper文件解析
- **反射机制**: 对象创建和属性赋值
- **类型处理**: Java类型与JDBC类型转换

## 🎓 面试重点

### 高频面试题
1. MyBatis的执行流程是什么？
2. MyBatis的一级缓存和二级缓存？
3. MyBatis如何实现Mapper接口？
4. MyBatis的插件原理？
5. MyBatis如何处理SQL注入？
6. MyBatis的延迟加载原理？
7. #{} 和 ${} 的区别？

### 源码阅读建议
1. 从SqlSessionFactory的创建开始
2. 跟踪一次完整的SQL执行流程
3. 理解四大对象的创建时机
4. 分析插件的拦截点
5. 研究缓存的实现细节

## 📝 使用示例

```java
// 1. 加载配置，创建SqlSessionFactory
InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

// 2. 获取SqlSession
SqlSession sqlSession = sqlSessionFactory.openSession();

// 3. 获取Mapper代理对象
UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

// 4. 执行SQL
User user = userMapper.selectById(1L);

// 5. 提交事务，关闭会话
sqlSession.commit();
sqlSession.close();
```

## 🔧 技术栈

- Java 8
- JDBC
- DOM4J (XML解析)
- CGLib (字节码增强)
- SLF4J + Logback (日志)
- JUnit (测试)

## 📂 项目结构

```
mini-mybatis/
├── src/main/java/com/mybatis/
│   ├── session/          # 会话层
│   ├── executor/         # 执行器层
│   ├── mapping/          # 映射层
│   ├── builder/          # 构建器
│   ├── parsing/          # 解析器
│   ├── scripting/        # 脚本引擎
│   ├── cache/            # 缓存
│   ├── transaction/      # 事务
│   ├── datasource/       # 数据源
│   ├── plugin/           # 插件
│   ├── reflection/       # 反射工具
│   ├── type/             # 类型处理
│   └── exceptions/       # 异常
├── src/main/resources/
│   ├── mybatis-config.xml    # 主配置文件
│   └── mapper/               # Mapper映射文件
└── src/test/                 # 测试代码
```

## 🚀 快速开始

### 1. 创建数据库表
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    create_time DATETIME
);
```

### 2. 配置数据库连接
编辑 `mybatis-config.xml` 配置文件

### 3. 运行测试
```bash
mvn clean test
```

## 📖 参考资料

- [MyBatis官方文档](https://mybatis.org/mybatis-3/zh/index.html)
- [MyBatis源码](https://github.com/mybatis/mybatis-3)
- 《MyBatis技术内幕》
- 《MyBatis从入门到精通》

## 💡 学习建议

1. **边写边调试**: 不要只看代码，要实际运行和调试
2. **对比源码**: 实现一个功能后，对比MyBatis源码的实现
3. **画流程图**: 把复杂的流程用图形化方式表达
4. **写测试用例**: 通过测试来验证理解
5. **做笔记**: 记录关键点和疑问

---

**作者**: 无牙仔  
**目标**: 深入理解MyBatis，达到面试级别的源码熟悉度  
**日期**: 2026-01-07

