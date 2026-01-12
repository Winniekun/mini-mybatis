# StatementHandler vs 原始JDBC - 完整对比

## 🎯 原始JDBC操作数据库

### 示例1：查询单个用户

```java
package com.example.jdbc;

import java.sql.*;

public class JdbcExample {
    
    /**
     * 原始JDBC方式查询用户
     */
    public User selectUserById(Long id) {
        // 1. 定义JDBC连接参数
        String url = "jdbc:mysql://localhost:3306/mybatis_test";
        String username = "root";
        String password = "root";
        String sql = "SELECT id, username, password, email, create_time FROM user WHERE id = ?";
        
        // 2. 声明资源
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        User user = null;
        
        try {
            // 3. 加载驱动（新版本可以省略）
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 4. 获取数据库连接 ⚠️ 每次都要创建
            conn = DriverManager.getConnection(url, username, password);
            
            // 5. 创建PreparedStatement ⚠️ 手动创建
            ps = conn.prepareStatement(sql);
            
            // 6. 设置参数 ⚠️ 手动设置，容易出错
            ps.setLong(1, id);  // 第1个?的值
            
            // 7. 执行查询
            rs = ps.executeQuery();
            
            // 8. 处理结果集 ⚠️ 手动映射，繁琐！
            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                // 如果有更多字段，需要一个一个set...
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("找不到驱动: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("SQL异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 9. 关闭资源 ⚠️ 必须手动关闭，顺序还不能错！
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return user;
    }
    
    /**
     * 原始JDBC方式查询用户列表
     */
    public List<User> selectAllUsers() {
        String url = "jdbc:mysql://localhost:3306/mybatis_test";
        String username = "root";
        String password = "root";
        String sql = "SELECT id, username, password, email, create_time FROM user";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<User> users = new ArrayList<>();
        
        try {
            // 获取连接
            conn = DriverManager.getConnection(url, username, password);
            
            // 创建Statement
            ps = conn.prepareStatement(sql);
            
            // 执行查询
            rs = ps.executeQuery();
            
            // 遍历结果集 ⚠️ 需要循环处理
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                users.add(user);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return users;
    }
    
    /**
     * 原始JDBC方式插入用户
     */
    public int insertUser(User user) {
        String url = "jdbc:mysql://localhost:3306/mybatis_test";
        String username = "root";
        String password = "root";
        String sql = "INSERT INTO user (username, password, email, create_time) VALUES (?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement ps = null;
        int rows = 0;
        
        try {
            // 获取连接
            conn = DriverManager.getConnection(url, username, password);
            
            // 创建PreparedStatement，设置返回生成的主键
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            // 设置参数 ⚠️ 参数顺序必须和SQL中的?一一对应
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());
            ps.setTimestamp(4, Timestamp.valueOf(user.getCreateTime()));
            
            // 执行插入
            rows = ps.executeUpdate();
            
            // 获取自增主键 ⚠️ 手动获取
            if (rows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
                generatedKeys.close();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return rows;
    }
    
    /**
     * 原始JDBC方式更新用户
     */
    public int updateUser(User user) {
        String url = "jdbc:mysql://localhost:3306/mybatis_test";
        String username = "root";
        String password = "root";
        String sql = "UPDATE user SET username = ?, password = ?, email = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        int rows = 0;
        
        try {
            conn = DriverManager.getConnection(url, username, password);
            ps = conn.prepareStatement(sql);
            
            // 设置参数 ⚠️ 顺序很重要
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());
            ps.setLong(4, user.getId());
            
            // 执行更新
            rows = ps.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return rows;
    }
    
    /**
     * 原始JDBC方式删除用户
     */
    public int deleteUser(Long id) {
        String url = "jdbc:mysql://localhost:3306/mybatis_test";
        String username = "root";
        String password = "root";
        String sql = "DELETE FROM user WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        int rows = 0;
        
        try {
            conn = DriverManager.getConnection(url, username, password);
            ps = conn.prepareStatement(sql);
            ps.setLong(1, id);
            rows = ps.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return rows;
    }
}
```

---

## 🔧 MyBatis StatementHandler方式

### 对比：查询单个用户

```java
// MyBatis方式（StatementHandler内部实现）
public <E> List<E> query(String statementId, Object parameter) {
    // 1. 获取SQL配置（从XML或注解）
    MappedStatement ms = configuration.getMappedStatement(statementId);
    
    // 2. 创建StatementHandler
    StatementHandler handler = new StatementHandler(configuration);
    
    // 3. 准备Statement（自动替换#{} → ?）
    Statement statement = handler.prepare(connection, ms.getSql());
    
    // 4. 设置参数（自动判断类型）
    handler.parameterize(statement, parameter);
    
    // 5. 执行查询并自动映射结果
    List<E> result = handler.query(statement, ms.getResultType());
    
    // 6. 自动关闭Statement
    statement.close();
    
    return result;
}

// 用户使用
User user = mapper.selectById(1L);  // 一行代码搞定！
```

---

## 📊 详细对比表

| 方面 | 原始JDBC | MyBatis StatementHandler |
|-----|---------|-------------------------|
| **连接管理** | ⚠️ 每次手动创建和关闭 | ✅ 自动管理（连接池） |
| **SQL定义** | ⚠️ 硬编码在Java中 | ✅ 集中在XML/注解中 |
| **参数设置** | ⚠️ 手动调用setXxx() | ✅ 自动判断类型设置 |
| **结果映射** | ⚠️ 手动从ResultSet提取并set | ✅ 自动映射到对象 |
| **资源关闭** | ⚠️ 必须手动finally关闭 | ✅ 自动关闭 |
| **异常处理** | ⚠️ 到处都是try-catch | ✅ 统一异常处理 |
| **代码量** | ⚠️ 50-100行 | ✅ 10行左右 |
| **易错性** | ⚠️ 容易忘记关闭资源 | ✅ 不易出错 |
| **可维护性** | ⚠️ SQL分散，难维护 | ✅ 集中管理，易维护 |
| **动态SQL** | ⚠️ 需要手动拼接字符串 | ✅ 提供<if>等标签 |

---

## 🎯 核心区别分析

### 1. 连接管理

**原始JDBC**：
```java
// 每次都要写这些
Connection conn = DriverManager.getConnection(url, username, password);
// 用完必须关闭
conn.close();

问题：
❌ 频繁创建连接，性能差
❌ 忘记关闭导致连接泄露
❌ 没有连接池管理
```

**MyBatis**：
```java
// 配置一次
<dataSource type="POOLED">
    <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
    <property name="url" value="jdbc:mysql://localhost:3306/mybatis_test"/>
</dataSource>

// 自动从连接池获取
Connection conn = dataSource.getConnection();

优势：
✅ 连接池管理，性能好
✅ 自动归还连接
✅ 配置化管理
```

---

### 2. SQL管理

**原始JDBC**：
```java
// SQL硬编码在代码中
String sql = "SELECT id, username, password, email, create_time FROM user WHERE id = ?";

问题：
❌ SQL分散在各处
❌ 修改SQL需要改代码、重新编译
❌ 不支持动态SQL
❌ 复杂SQL可读性差
```

**MyBatis**：
```xml
<!-- SQL集中在XML中 -->
<select id="selectById" resultType="User">
    SELECT id, username, password, email, create_time 
    FROM user 
    WHERE id = #{id}
</select>

<!-- 支持动态SQL -->
<select id="selectByCondition" resultType="User">
    SELECT * FROM user
    WHERE 1=1
    <if test="username != null">
        AND username = #{username}
    </if>
    <if test="email != null">
        AND email = #{email}
    </if>
</select>

优势：
✅ SQL集中管理
✅ 不需要重新编译
✅ 支持动态SQL
✅ 可读性好
```

---

### 3. 参数设置

**原始JDBC**：
```java
// 需要手动设置每个参数
ps.setLong(1, id);
ps.setString(2, username);
ps.setTimestamp(3, timestamp);

问题：
❌ 必须记住参数顺序
❌ 容易设置错类型
❌ 代码重复
❌ 对象参数需要手动提取属性
```

**MyBatis**：
```java
// StatementHandler自动处理
public void parameterize(Statement statement, Object parameter) {
    if (isSimpleType(parameter)) {
        // 简单类型：直接设置
        ps.setObject(1, parameter);
    } else {
        // 复杂类型：自动提取属性
        // 根据#{propName}自动调用getPropName()
    }
}

优势：
✅ 自动判断类型
✅ 自动设置参数
✅ 支持对象参数
✅ 代码简洁
```

---

### 4. 结果映射

**原始JDBC**：
```java
// 手动从ResultSet提取数据
if (rs.next()) {
    user = new User();
    user.setId(rs.getLong("id"));
    user.setUsername(rs.getString("username"));
    user.setPassword(rs.getString("password"));
    user.setEmail(rs.getString("email"));
    user.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
    // 每个字段都要写一遍！
}

问题：
❌ 代码重复
❌ 容易写错字段名
❌ 类型转换麻烦
❌ 修改字段需要改代码
```

**MyBatis**：
```java
// ResultSetHandler自动映射
public <E> List<E> handleResultSet(ResultSet rs, Class<?> resultType) {
    List<E> result = new ArrayList<>();
    
    while (rs.next()) {
        // 自动创建对象
        E obj = (E) resultType.newInstance();
        
        // 自动映射字段
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnLabel(i);
            Object value = rs.getObject(i);
            // 自动调用setXxx()方法
            setProperty(obj, columnName, value);
        }
        
        result.add(obj);
    }
    
    return result;
}

优势：
✅ 自动映射
✅ 自动类型转换
✅ 减少80%代码
✅ 不易出错
```

---

### 5. 资源关闭

**原始JDBC**：
```java
// 必须在finally中手动关闭，顺序还不能错
finally {
    try {
        if (rs != null) rs.close();
    } catch (SQLException e) {
        e.printStackTrace();
    }
    try {
        if (ps != null) ps.close();
    } catch (SQLException e) {
        e.printStackTrace();
    }
    try {
        if (conn != null) conn.close();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

问题：
❌ 代码冗长
❌ 容易忘记
❌ 顺序容易错
❌ 每个方法都要写
```

**MyBatis**：
```java
// StatementHandler自动关闭
public <E> List<E> query(...) {
    Statement stmt = null;
    try {
        stmt = handler.prepare(...);
        // ... 执行查询
        return result;
    } finally {
        if (stmt != null) {
            stmt.close();  // 自动关闭
        }
        // Connection由SqlSession管理
    }
}

// 用户只需关闭SqlSession
try (SqlSession session = sqlSessionFactory.openSession()) {
    // 使用session
}  // 自动关闭

优势：
✅ 自动管理资源
✅ 不易遗漏
✅ 代码简洁
✅ 使用try-with-resources
```

---

## 💡 实际代码量对比

### 场景：查询用户列表

**原始JDBC - 约60行代码**：
```java
public List<User> selectAllUsers() {
    String url = "jdbc:mysql://localhost:3306/mybatis_test";
    String username = "root";
    String password = "root";
    String sql = "SELECT id, username, password, email, create_time FROM user";
    
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    List<User> users = new ArrayList<>();
    
    try {
        conn = DriverManager.getConnection(url, username, password);
        ps = conn.prepareStatement(sql);
        rs = ps.executeQuery();
        
        while (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setEmail(rs.getString("email"));
            user.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            users.add(user);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
    } finally {
        try {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    return users;
}

// 调用
JdbcExample jdbc = new JdbcExample();
List<User> users = jdbc.selectAllUsers();  // 约60行支撑代码
```

**MyBatis - 约5行代码**：
```xml
<!-- UserMapper.xml -->
<select id="selectAll" resultType="User">
    SELECT id, username, password, email, create_time FROM user
</select>
```

```java
// UserMapper.java
public interface UserMapper {
    List<User> selectAll();
}

// 调用
List<User> users = mapper.selectAll();  // 1行代码！
```

**代码量减少 92%！**

---

## 🎯 StatementHandler的价值

### 1. 封装JDBC模板代码
```
原始JDBC的模板代码：
1. 获取连接
2. 创建Statement
3. 设置参数
4. 执行SQL
5. 处理结果
6. 关闭资源
7. 异常处理

StatementHandler封装了2、3、4、5步，
SqlSession封装了1、6步，
让用户只需关注业务逻辑！
```

### 2. 提供统一的接口
```java
// 无论是查询、插入、更新、删除
// StatementHandler提供统一的方法

prepare()      - 准备Statement
parameterize() - 设置参数
query()        - 查询
update()       - 更新
```

### 3. 自动化处理
```
自动替换#{} → ?
自动设置参数
自动映射结果
自动关闭资源
自动异常转换
```

### 4. 可扩展性
```
通过插件机制可以：
- 拦截SQL执行
- 修改参数
- 处理结果
- 性能监控
- 日志记录
```

---

## 🤔 思考题

### 1. 为什么需要StatementHandler？

<details>
<summary>点击查看答案</summary>

因为原始JDBC有太多重复的模板代码：
- 创建Statement
- 设置参数
- 执行SQL
- 处理结果
- 关闭资源

StatementHandler封装了这些通用逻辑，让开发者只需关注：
- SQL写什么
- 参数是什么
- 结果要什么

这就是**封装的价值**！
</details>

### 2. MyBatis是如何减少代码量的？

<details>
<summary>点击查看答案</summary>

三个层次的封装：

**第一层：StatementHandler封装JDBC操作**
- 不用手动创建Statement
- 不用手动设置参数
- 不用手动映射结果

**第二层：Executor封装执行流程**
- 不用管理Statement生命周期
- 不用处理异常
- 不用关闭资源

**第三层：SqlSession封装会话管理**
- 不用管理Connection
- 不用处理事务
- 不用写try-catch

**第四层：MapperProxy封装接口调用**
- 只需定义接口
- 自动生成实现
- 一行代码调用

从60行代码 → 1行代码！
</details>

### 3. 如果没有MyBatis，我们会遇到什么问题？

<details>
<summary>点击查看答案</summary>

**代码问题**：
- 大量重复的模板代码
- SQL分散在各处，难以管理
- 容易忘记关闭资源，导致连接泄露

**维护问题**：
- 修改SQL需要改代码、重新编译
- 字段改变需要修改多处
- 难以统一管理数据库配置

**性能问题**：
- 没有连接池，频繁创建连接
- 没有预编译缓存
- 没有二级缓存

**安全问题**：
- 容易写出SQL注入漏洞
- 没有统一的参数验证

**开发效率**：
- 大量时间花在重复劳动上
- 简单的CRUD也要写很多代码
- 调试和测试困难

这就是为什么需要ORM框架！
</details>

---

## 📊 总结对比图

```
┌─────────────────────────────────────────────────┐
│              原始JDBC (60行代码)                 │
├─────────────────────────────────────────────────┤
│ 1. 手动获取连接                                  │
│    Connection conn = DriverManager.get...       │
│                                                 │
│ 2. 手动创建Statement                            │
│    PreparedStatement ps = conn.prepare...       │
│                                                 │
│ 3. 手动设置参数                                  │
│    ps.setLong(1, id);                          │
│    ps.setString(2, name);                      │
│                                                 │
│ 4. 手动执行SQL                                   │
│    ResultSet rs = ps.executeQuery();           │
│                                                 │
│ 5. 手动处理结果                                  │
│    while(rs.next()) {                          │
│        user.setId(rs.getLong("id"));           │
│        user.setName(rs.getString("name"));     │
│        ...                                     │
│    }                                           │
│                                                 │
│ 6. 手动关闭资源                                  │
│    finally {                                   │
│        rs.close();                             │
│        ps.close();                             │
│        conn.close();                           │
│    }                                           │
└─────────────────────────────────────────────────┘
                        ↓
                    封装简化
                        ↓
┌─────────────────────────────────────────────────┐
│         MyBatis StatementHandler (1行代码)       │
├─────────────────────────────────────────────────┤
│ User user = mapper.selectById(1L);              │
│                                                 │
│ 背后自动完成：                                   │
│ ✅ 自动获取连接                                  │
│ ✅ 自动创建Statement                             │
│ ✅ 自动设置参数                                  │
│ ✅ 自动执行SQL                                   │
│ ✅ 自动映射结果                                  │
│ ✅ 自动关闭资源                                  │
└─────────────────────────────────────────────────┘
```

---

## 🎊 核心启示

### MyBatis的本质：
```
MyBatis = JDBC的优雅封装

不是替代JDBC，而是：
✅ 封装模板代码
✅ 提供更好的API
✅ 自动化重复工作
✅ 让开发者专注业务

底层还是JDBC！
```

### StatementHandler的定位：
```
StatementHandler = JDBC操作的直接封装层

职责：
✅ 封装Statement的创建
✅ 封装参数的设置
✅ 封装SQL的执行
✅ 委托结果的映射

价值：
✅ 统一接口
✅ 简化代码
✅ 易于扩展
```

---

**现在你明白MyBatis的价值了吗？它把60行的重复代码变成了1行！** 🎉

