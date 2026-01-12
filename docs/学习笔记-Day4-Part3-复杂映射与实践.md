# Day 4 学习笔记 - Part 3&4: 复杂映射与完整实践

## 🚀 第三课：复杂映射场景

### 什么是复杂映射？

当简单的自动映射无法满足需求时，就需要复杂映射：
- 一对一关联（用户 → 详情）
- 一对多关联（用户 → 订单列表）
- 多对多关联（学生 ↔ 课程）
- 嵌套对象映射

---

## 📚 映射场景分类

###场景1：简单映射（已掌握）✅

```
一张表 → 一个对象

SELECT id, username, email FROM user
  ↓
User {
    id, username, email
}
```

---

### 场景2：一对一关联

```
两张表 → 一个对象（包含关联对象）

user表                 user_detail表
┌────┬──────────┐     ┌─────────┬──────────┐
│ id │ username │     │ user_id │  address │
├────┼──────────┤     ├─────────┼──────────┤
│ 1  │  admin   │  ←  │   1     │ 北京市   │
└────┴──────────┘     └─────────┴──────────┘

  ↓ 映射为

User {
    id = 1,
    username = "admin",
    detail = UserDetail {
        address = "北京市"
    }
}
```

**SQL方式1：JOIN查询**
```sql
SELECT 
    u.id,
    u.username,
    d.address
FROM user u
LEFT JOIN user_detail d ON u.id = d.user_id
WHERE u.id = 1
```

**Java类定义**：
```java
public class User {
    private Long id;
    private String username;
    private UserDetail detail;  // 关联对象
}

public class UserDetail {
    private String address;
    private String phone;
}
```

**真实MyBatis配置**：
```xml
<resultMap id="userWithDetail" type="User">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <!-- 一对一关联 -->
    <association property="detail" javaType="UserDetail">
        <result column="address" property="address"/>
        <result column="phone" property="phone"/>
    </association>
</resultMap>
```

---

### 场景3：一对多关联

```
两张表 → 一个对象（包含集合）

user表                 order表
┌────┬──────────┐     ┌────┬─────────┬──────────┐
│ id │ username │     │ id │ user_id │  amount  │
├────┼──────────┤     ├────┼─────────┼──────────┤
│ 1  │  admin   │  ←  │ 1  │   1     │  100.00  │
└────┴──────────┘     ├────┼─────────┼──────────┤
                      │ 2  │   1     │  200.00  │
                      └────┴─────────┴──────────┘

  ↓ 映射为

User {
    id = 1,
    username = "admin",
    orders = [
        Order { id=1, amount=100.00 },
        Order { id=2, amount=200.00 }
    ]
}
```

**SQL查询**：
```sql
SELECT 
    u.id AS user_id,
    u.username,
    o.id AS order_id,
    o.amount
FROM user u
LEFT JOIN `order` o ON u.id = o.user_id
WHERE u.id = 1
```

**Java类定义**：
```java
public class User {
    private Long id;
    private String username;
    private List<Order> orders;  // 关联集合
}

public class Order {
    private Long id;
    private BigDecimal amount;
}
```

**真实MyBatis配置**：
```xml
<resultMap id="userWithOrders" type="User">
    <id column="user_id" property="id"/>
    <result column="username" property="username"/>
    <!-- 一对多关联 -->
    <collection property="orders" ofType="Order">
        <id column="order_id" property="id"/>
        <result column="amount" property="amount"/>
    </collection>
</resultMap>
```

---

### 场景4：嵌套查询

```
分两次查询（避免JOIN）

第1次：查询用户
SELECT id, username FROM user WHERE id = 1

第2次：查询订单（根据用户ID）
SELECT id, amount FROM `order` WHERE user_id = 1

合并结果：
User {
    id = 1,
    username = "admin",
    orders = [...]
}
```

**真实MyBatis配置**：
```xml
<resultMap id="userWithOrders" type="User">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <!-- 嵌套查询 -->
    <collection 
        property="orders" 
        ofType="Order"
        select="selectOrdersByUserId"
        column="id"/>
</resultMap>

<select id="selectById" resultMap="userWithOrders">
    SELECT id, username FROM user WHERE id = #{id}
</select>

<select id="selectOrdersByUserId" resultType="Order">
    SELECT id, amount FROM `order` WHERE user_id = #{userId}
</select>
```

---

## 🎯 我们的简化实现

我们的mini-mybatis目前只实现了：
- ✅ 简单映射（单表 → 单对象）
- ✅ 自动驼峰转换
- ✅ 基本类型转换

**不支持**（但真实MyBatis支持）：
- ❌ 一对一关联（<association>）
- ❌ 一对多关联（<collection>）
- ❌ 嵌套查询
- ❌ 懒加载

**为什么不支持？**
```
因为实现复杂关联需要：
1. 解析<resultMap>标签
2. 处理多表JOIN结果
3. 对象去重和聚合
4. 懒加载代理

这些是MyBatis的高级特性，
我们focus on核心原理。
```

---

## 💡 解决方案：两种方式

### 方式1：分次查询（推荐新手）

```java
// 1. 查询用户
User user = userMapper.selectById(1L);

// 2. 查询订单
List<Order> orders = orderMapper.selectByUserId(user.getId());

// 3. 手动设置
user.setOrders(orders);
```

**优点**：
- ✅ 简单直观
- ✅ 灵活可控
- ✅ 容易调试

**缺点**：
- ❌ 需要多次查询
- ❌ 手动组装数据

---

### 方式2：SQL JOIN + 手动映射

```sql
-- SQL
SELECT 
    u.id AS user_id,
    u.username,
    o.id AS order_id,
    o.amount
FROM user u
LEFT JOIN `order` o ON u.id = o.user_id
WHERE u.id = 1
```

```java
// Mapper接口
List<Map<String, Object>> selectUserWithOrders(@Param("id") Long id);

// Service层处理
public User getUserWithOrders(Long id) {
    List<Map<String, Object>> rows = mapper.selectUserWithOrders(id);
    
    // 手动组装
    User user = new User();
    List<Order> orders = new ArrayList<>();
    
    for (Map<String, Object> row : rows) {
        if (user.getId() == null) {
            user.setId((Long) row.get("user_id"));
            user.setUsername((String) row.get("username"));
        }
        
        Order order = new Order();
        order.setId((Long) row.get("order_id"));
        order.setAmount((BigDecimal) row.get("amount"));
        orders.add(order);
    }
    
    user.setOrders(orders);
    return user;
}
```

---

## 🧪 第四课：完整实践案例

让我们通过实际案例来巩固今天的知识！

---

## 📝 实践1：完整的用户CRUD

### 需求

实现用户管理的完整功能：
- 新增用户
- 删除用户
- 更新用户
- 查询单个用户
- 查询所有用户
- 条件查询

---

### 步骤1：数据库表

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    email VARCHAR(100) COMMENT '邮箱',
    age INT COMMENT '年龄',
    status TINYINT DEFAULT 1 COMMENT '状态：1=正常，0=禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

---

### 步骤2：Java实体类

```java
package com.mybatis.test.entity;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private Integer age;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // getter/setter...
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
}
```

**关键点**：
- ✅ 属性名使用驼峰：createTime, updateTime
- ✅ 数据库字段用下划线：create_time, update_time
- ✅ 自动映射会处理转换

---

### 步骤3：Mapper接口

```java
package com.mybatis.test.mapper;

import com.mybatis.test.entity.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface UserMapper {
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 根据ID删除
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新用户
     */
    int update(User user);
    
    /**
     * 根据ID查询
     */
    User selectById(@Param("id") Long id);
    
    /**
     * 查询所有
     */
    List<User> selectAll();
    
    /**
     * 根据用户名查询
     */
    User selectByUsername(@Param("username") String username);
    
    /**
     * 条件查询
     */
    List<User> selectByCondition(@Param("username") String username,
                                  @Param("minAge") Integer minAge,
                                  @Param("maxAge") Integer maxAge);
    
    /**
     * 统计数量
     */
    Long count();
}
```

---

### 步骤4：Mapper XML

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mybatis.test.mapper.UserMapper">
    
    <!-- 插入用户 -->
    <insert id="insert" parameterType="User">
        INSERT INTO user (username, password, email, age, status)
        VALUES (#{username}, #{password}, #{email}, #{age}, #{status})
    </insert>
    
    <!-- 删除用户 -->
    <delete id="deleteById">
        DELETE FROM user WHERE id = #{id}
    </delete>
    
    <!-- 更新用户 -->
    <update id="update">
        UPDATE user 
        SET username = #{username},
            email = #{email},
            age = #{age},
            status = #{status}
        WHERE id = #{id}
    </update>
    
    <!-- 根据ID查询 -->
    <select id="selectById" resultType="User">
        SELECT id, username, password, email, age, status, create_time, update_time
        FROM user 
        WHERE id = #{id}
    </select>
    
    <!-- 查询所有 -->
    <select id="selectAll" resultType="User">
        SELECT id, username, email, age, status, create_time
        FROM user
        ORDER BY create_time DESC
    </select>
    
    <!-- 根据用户名查询 -->
    <select id="selectByUsername" resultType="User">
        SELECT id, username, email, age, status, create_time
        FROM user
        WHERE username = #{username}
    </select>
    
    <!-- 条件查询 -->
    <select id="selectByCondition" resultType="User">
        SELECT id, username, email, age, status, create_time
        FROM user
        WHERE 1=1
        <if test="username != null and username != ''">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="minAge != null">
            AND age >= #{minAge}
        </if>
        <if test="maxAge != null">
            AND age &lt;= #{maxAge}
        </if>
        ORDER BY create_time DESC
    </select>
    
    <!-- 统计数量 -->
    <select id="count" resultType="Long">
        SELECT COUNT(*) FROM user
    </select>
    
</mapper>
```

---

### 步骤5：测试代码

```java
package com.mybatis.test;

import com.mybatis.test.entity.User;
import com.mybatis.test.mapper.UserMapper;
import com.mybatis.session.SqlSession;
import com.mybatis.session.SqlSessionFactory;
import com.mybatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

public class UserCrudTest {
    
    private SqlSessionFactory sqlSessionFactory;
    
    @Before
    public void setUp() throws Exception {
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }
    
    @Test
    public void testInsert() {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            User user = new User();
            user.setUsername("testuser");
            user.setPassword("123456");
            user.setEmail("test@example.com");
            user.setAge(25);
            user.setStatus(1);
            
            int rows = mapper.insert(user);
            System.out.println("插入成功，影响 " + rows + " 行");
            
            session.commit();
        } finally {
            session.close();
        }
    }
    
    @Test
    public void testSelectById() {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            User user = mapper.selectById(1L);
            System.out.println("查询结果: " + user);
            
            // 验证自动映射
            System.out.println("createTime自动映射: " + user.getCreateTime());
        } finally {
            session.close();
        }
    }
    
    @Test
    public void testSelectAll() {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            List<User> users = mapper.selectAll();
            System.out.println("查询到 " + users.size() + " 个用户:");
            users.forEach(System.out::println);
        } finally {
            session.close();
        }
    }
    
    @Test
    public void testUpdate() {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 先查询
            User user = mapper.selectById(1L);
            System.out.println("更新前: " + user);
            
            // 修改
            user.setEmail("newemail@example.com");
            user.setAge(30);
            
            // 更新
            int rows = mapper.update(user);
            System.out.println("更新成功，影响 " + rows + " 行");
            
            // 再次查询验证
            user = mapper.selectById(1L);
            System.out.println("更新后: " + user);
            
            session.commit();
        } finally {
            session.close();
        }
    }
    
    @Test
    public void testDelete() {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            int rows = mapper.deleteById(1L);
            System.out.println("删除成功，影响 " + rows + " 行");
            
            session.commit();
        } finally {
            session.close();
        }
    }
    
    @Test
    public void testSelectByCondition() {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            // 条件1：用户名包含"user"
            List<User> users1 = mapper.selectByCondition("user", null, null);
            System.out.println("用户名包含'user'的用户: " + users1.size() + " 个");
            
            // 条件2：年龄在20-30之间
            List<User> users2 = mapper.selectByCondition(null, 20, 30);
            System.out.println("年龄20-30的用户: " + users2.size() + " 个");
            
            // 条件3：组合条件
            List<User> users3 = mapper.selectByCondition("admin", 18, 50);
            System.out.println("组合条件的用户: " + users3.size() + " 个");
            
        } finally {
            session.close();
        }
    }
    
    @Test
    public void testCount() {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            
            Long count = mapper.count();
            System.out.println("用户总数: " + count);
        } finally {
            session.close();
        }
    }
}
```

---

## 🎯 关键知识点总结

### 1. ResultSetHandler的核心能力

```
✅ 遍历ResultSet
✅ 创建Java对象
✅ 自动映射字段
✅ 类型转换
✅ 支持驼峰命名转换
```

### 2. 自动映射的三个层次

```
Level 1: 直接匹配
  username → username

Level 2: 驼峰转换
  user_name → userName

Level 3: 手动配置
  <resultMap> (真实MyBatis)
```

### 3. 映射流程

```
ResultSet
  ↓ 遍历
每一行
  ↓ 创建对象
Object bean = new User()
  ↓ 建立映射表
Map<String, Field> fieldMap
  ↓ 遍历每列
for each column
  ↓ 查找字段
Field field = fieldMap.get(columnName)
  ↓ 类型转换
Object value = convertType(...)
  ↓ 设置值
field.set(bean, value)
  ↓ 返回
List<User>
```

---

## 🤔 面试题

### Q1: ResultSetHandler的作用是什么？

**答案**：
ResultSetHandler负责将JDBC的ResultSet转换为Java对象。主要包括：
1. 遍历ResultSet的每一行
2. 根据resultType创建对象实例
3. 将数据库字段映射到对象属性
4. 处理类型转换
5. 支持驼峰命名自动转换

---

### Q2: MyBatis如何实现驼峰命名转换？

**答案**：
通过建立两种映射：
1. 原始名称映射：username → username字段
2. 下划线名称映射：user_name → username字段

转换算法：遇到大写字母就加下划线
- userName → user_name
- createTime → create_time

这样无论数据库字段是username还是user_name，都能找到对应的Java属性。

---

### Q3: MyBatis的四大核心对象是什么？

**答案**：
1. **SqlSession** - 门面接口，用户直接使用
2. **Executor** - 执行器，协调SQL执行流程
3. **StatementHandler** - JDBC封装，执行SQL
4. **ResultSetHandler** - 结果映射，ResultSet → Object

---

**Day 4所有内容完成！** 🎉

