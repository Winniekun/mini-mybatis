# Day 1 学习笔记 - MyBatis入门

## ✅ 今日学习目标
- [x] 理解MyBatis的基本概念
- [x] 了解项目结构
- [x] 准备数据库环境
- [x] 运行第一个测试
- [x] 理解基本使用流程

---

## 📚 核心概念

### 1. MyBatis是什么？
**半自动ORM框架**，介于JDBC和Hibernate之间：
- 比JDBC简单：自动处理连接、映射
- 比Hibernate灵活：完全控制SQL

### 2. 核心组件（5个）

```
SqlSessionFactoryBuilder  →  构建工厂
         ↓
SqlSessionFactory        →  创建会话（全局唯一）
         ↓
SqlSession               →  执行SQL（每次请求）
         ↓
MapperProxy              →  动态代理（核心魔法！）
         ↓
Executor + Handler       →  真正执行SQL
```

---

## 🏗️ 项目结构

```
src/main/java/com/mybatis/
├── session/          会话层（SqlSession、Factory）
├── executor/         执行层（执行SQL）
├── binding/          代理层（Mapper动态代理）⭐核心
├── builder/          解析层（XML解析）
├── mapping/          映射层（存储SQL）
├── plugin/           插件层（拦截器）
└── cache/            缓存层

src/test/java/com/mybatis/test/
├── entity/User.java          实体类
├── mapper/UserMapper.java    Mapper接口
└── MybatisTest.java          测试类
```

---

## 🎯 实践步骤

### Step 1: 准备数据库 ⚠️ 你需要做这步

```sql
-- 1. 打开MySQL客户端（Navicat/MySQL Workbench/命令行）
-- 2. 执行以下SQL

CREATE DATABASE mybatis_learn 
DEFAULT CHARACTER SET utf8mb4;

USE mybatis_learn;

CREATE TABLE `user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO `user` (`username`, `password`, `email`) VALUES
('zhangsan', '123456', 'zhangsan@example.com'),
('lisi', '123456', 'lisi@example.com'),
('wangwu', '123456', 'wangwu@example.com');

-- 验证数据
SELECT * FROM user;
```

### Step 2: 检查配置文件

配置文件位置：`src/main/resources/mybatis-config.xml`

```xml
<property name="url" value="jdbc:mysql://localhost:3306/mybatis_learn..."/>
<property name="username" value="root"/>
<property name="password" value="你的密码"/>  <!-- 修改这里 -->
```

### Step 3: 编译项目

```bash
cd /Users/weikunkun/IdeaProjects/cursor-learn/mybatis
mvn clean compile
```

### Step 4: 运行测试

在IDE中运行 `MybatisTest.testMapperProxy()` 方法

或者命令行：
```bash
mvn test -Dtest=MybatisTest#testMapperProxy
```

---

## 📖 代码阅读顺序

### 第一遍：看使用（从上往下）

1. **MybatisTest.java** - 看如何使用
2. **UserMapper.java** - Mapper接口长什么样
3. **UserMapper.xml** - SQL如何配置
4. **User.java** - 实体类

### 第二遍：看流程（从入口开始）

1. **SqlSessionFactoryBuilder** - 入口
2. **XMLConfigBuilder** - 配置解析
3. **Configuration** - 配置存储
4. **DefaultSqlSessionFactory** - 工厂实现
5. **DefaultSqlSession** - 会话实现

### 第三遍：看核心（重点！）

1. **MapperRegistry** - Mapper注册
2. **MapperProxyFactory** - 代理工厂
3. **MapperProxy** - 动态代理 ⭐⭐⭐
4. **SimpleExecutor** - SQL执行
5. **StatementHandler** - JDBC封装
6. **ResultSetHandler** - 结果映射

---

## 💡 核心原理预告

### Mapper接口如何工作？（明天详细讲）

```java
// 1. 你写的接口（没有实现类！）
public interface UserMapper {
    User selectById(Long id);
}

// 2. 你的调用
UserMapper mapper = session.getMapper(UserMapper.class);
User user = mapper.selectById(1L);  // ← 这里发生了什么？

// 3. 背后的魔法
// JDK动态代理创建了一个代理对象
// 当你调用selectById时：
//   a) MapperProxy拦截方法调用
//   b) 构建statementId: "com.xxx.UserMapper.selectById"
//   c) 找到对应的SQL: SELECT * FROM user WHERE id = #{id}
//   d) 执行SQL，参数1L
//   e) 结果映射为User对象
//   f) 返回
```

---

## ❓ 今日思考题

1. **为什么SqlSessionFactory是全局唯一的？**
   - 提示：想想它里面包含什么

2. **为什么SqlSession不能共享？**
   - 提示：想想线程安全

3. **Mapper接口明明没有实现类，为什么能调用方法？**
   - 提示：动态代理

---

## 🎯 明天预告

**Day 2: 深入Configuration和配置解析**
- MyBatis如何解析XML配置文件
- MappedStatement是如何创建的
- Configuration为什么是核心

---

## ✍️ 学习记录

**今日收获：**
- SQLSession、SqlSessionFactory


**今日疑问：**
- 


**明日计划：**
- 

---

**加油！你已经迈出了第一步！** 🚀

