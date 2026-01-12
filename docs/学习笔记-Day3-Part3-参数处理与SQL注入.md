# Day 3 学习笔记 - Part 3: 参数处理与SQL注入防护

## ⚠️ 第三课：#{} vs ${} 和SQL注入

### 这是什么？

这是MyBatis中**最重要的安全话题**，也是面试必问的内容！

---

## 🔍 #{} 和 ${} 的本质区别

### 一句话总结

```
#{}  =  安全的参数占位符  =  PreparedStatement  =  防SQL注入
${}  =  字符串替换       =  Statement          =  有SQL注入风险
```

---

## 📖 详细对比

### 1. #{} - 预编译参数（推荐）✅

**示例**：
```xml
<select id="selectById" parameterType="java.lang.Long" resultType="User">
    SELECT * FROM user WHERE id = #{id}
</select>
```

**处理过程**：
```java
// 步骤1: MyBatis处理
原始SQL: SELECT * FROM user WHERE id = #{id}
参数: 1L

// 步骤2: 替换为?
预编译SQL: SELECT * FROM user WHERE id = ?

// 步骤3: 创建PreparedStatement
PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE id = ?");

// 步骤4: 设置参数
ps.setLong(1, 1L);

// 步骤5: 执行
ResultSet rs = ps.executeQuery();
```

**特点**：
- ✅ 使用?作为占位符
- ✅ 参数通过setObject()绑定
- ✅ 参数会被转义
- ✅ 防止SQL注入
- ✅ 类型安全
- ✅ 可以复用（预编译）

---

### 2. ${} - 字符串替换（危险）⚠️

**示例**：
```xml
<select id="selectByTable" resultType="User">
    SELECT * FROM ${tableName}
</select>
```

**处理过程**：
```java
// 步骤1: MyBatis处理
原始SQL: SELECT * FROM ${tableName}
参数: "user"

// 步骤2: 直接替换
最终SQL: SELECT * FROM user
// 注意：直接把"user"拼接进去了！

// 步骤3: 创建Statement（不是PreparedStatement）
Statement stmt = conn.createStatement();

// 步骤4: 直接执行
ResultSet rs = stmt.executeQuery("SELECT * FROM user");
```

**特点**：
- ❌ 直接字符串替换
- ❌ 没有参数绑定
- ❌ 参数不会被转义
- ❌ 有SQL注入风险
- ❌ 不能复用
- ⚠️ 只在特定场景使用（表名、列名、Order By等）

---

## ⚠️ SQL注入攻击详解

### 什么是SQL注入？

通过在输入中插入恶意SQL代码，改变原SQL的逻辑，从而获取敏感数据或破坏数据。

### 攻击示例1：使用${}的危险

**错误的代码**：
```xml
<select id="login" resultType="User">
    SELECT * FROM user 
    WHERE username = '${username}' AND password = '${password}'
</select>
```

**正常使用**：
```java
// 参数
username = "admin"
password = "123456"

// 最终SQL
SELECT * FROM user 
WHERE username = 'admin' AND password = '123456'

// 结果：正常查询
```

**恶意攻击**：
```java
// 恶意参数
username = "admin' OR '1'='1"
password = "anything"

// 最终SQL（被注入了！）
SELECT * FROM user 
WHERE username = 'admin' OR '1'='1' AND password = 'anything'
//                      ↑ 这里形成了永真条件！

// 结果：返回所有用户，绕过了密码验证！
```

**更严重的攻击**：
```java
// 删库跑路！
username = "admin'; DROP TABLE user; --"
password = "anything"

// 最终SQL
SELECT * FROM user 
WHERE username = 'admin'; 
DROP TABLE user; 
-- ' AND password = 'anything'
//  ↑ 这部分被注释掉了

// 结果：查询用户 + 删除表！！！
```

---

### 攻击示例2：使用#{}的安全

**正确的代码**：
```xml
<select id="login" resultType="User">
    SELECT * FROM user 
    WHERE username = #{username} AND password = #{password}
</select>
```

**恶意攻击尝试**：
```java
// 恶意参数
username = "admin' OR '1'='1"
password = "anything"

// MyBatis处理后
预编译SQL: SELECT * FROM user 
          WHERE username = ? AND password = ?

// PreparedStatement设置参数
ps.setString(1, "admin' OR '1'='1");  // 会被转义！
ps.setString(2, "anything");

// 实际执行的效果相当于
SELECT * FROM user 
WHERE username = 'admin\' OR \'1\'=\'1\'' AND password = 'anything'
//                      ↑ 单引号被转义了！

// 结果：找不到用户，攻击失败！✅
```

**关键**：
- PreparedStatement会自动转义特殊字符
- 单引号 `'` 会被转义为 `\'`
- 恶意代码被当作普通字符串
- 不会改变SQL的语义

---

## 📊 #{} 和 ${} 对比表

| 特性 | #{} | ${} |
|-----|-----|-----|
| 处理方式 | 预编译 | 字符串替换 |
| JDBC类型 | PreparedStatement | Statement |
| 占位符 | ? | 无 |
| 参数绑定 | setObject() | 直接拼接 |
| 参数转义 | 是 | 否 |
| SQL注入 | 防御 ✅ | 危险 ⚠️ |
| 性能 | 好（可复用） | 差 |
| 类型转换 | 自动 | 无 |
| 适用场景 | 参数值 | 表名、列名、Order By |

---

## 🎯 使用场景

### #{} 使用场景（99%的情况）

```xml
<!-- 1. WHERE条件 -->
<select id="selectById">
    SELECT * FROM user WHERE id = #{id}
</select>

<!-- 2. INSERT值 -->
<insert id="insert">
    INSERT INTO user (name, age) VALUES (#{name}, #{age})
</insert>

<!-- 3. UPDATE设置值 -->
<update id="update">
    UPDATE user SET name = #{name} WHERE id = #{id}
</update>

<!-- 4. LIKE查询 -->
<select id="searchByName">
    SELECT * FROM user WHERE name LIKE CONCAT('%', #{keyword}, '%')
</select>
```

**总结**：只要是**值**，就用 #{}

---

### ${} 使用场景（1%的情况）

```xml
<!-- 1. 表名（动态表） -->
<select id="selectFromTable">
    SELECT * FROM ${tableName}
</select>

<!-- 2. 列名（动态列） -->
<select id="selectColumn">
    SELECT ${columnName} FROM user
</select>

<!-- 3. ORDER BY排序字段 -->
<select id="selectOrderBy">
    SELECT * FROM user ORDER BY ${orderColumn} ${orderDirection}
</select>

<!-- 4. SQL片段 -->
<select id="complexQuery">
    SELECT * FROM user ${whereClause}
</select>
```

**注意**：
- ⚠️ 必须验证输入（白名单）
- ⚠️ 不能直接用用户输入
- ⚠️ 最好通过枚举限制值

**安全示例**：
```java
// ❌ 不安全（直接用用户输入）
String column = request.getParameter("column");
List<User> users = mapper.selectOrderBy(column);

// ✅ 安全（白名单验证）
String column = request.getParameter("column");
if (!Arrays.asList("id", "name", "age").contains(column)) {
    throw new IllegalArgumentException("Invalid column");
}
List<User> users = mapper.selectOrderBy(column);
```

---

## 🛡️ 防SQL注入最佳实践

### 1. 优先使用 #{}

```xml
<!-- ✅ 正确 -->
<select id="login">
    SELECT * FROM user 
    WHERE username = #{username} AND password = #{password}
</select>

<!-- ❌ 错误 -->
<select id="login">
    SELECT * FROM user 
    WHERE username = '${username}' AND password = '${password}'
</select>
```

### 2. ${} 必须验证输入

```java
// ✅ 正确：使用枚举
public enum OrderColumn {
    ID("id"),
    NAME("name"),
    AGE("age");
    
    private String column;
    OrderColumn(String column) {
        this.column = column;
    }
    public String getColumn() {
        return column;
    }
}

// 使用
String orderBy = OrderColumn.NAME.getColumn();
List<User> users = mapper.selectOrderBy(orderBy);
```

### 3. LIKE查询的安全方式

```xml
<!-- ❌ 错误 -->
<select id="search">
    SELECT * FROM user WHERE name LIKE '%${keyword}%'
</select>

<!-- ✅ 正确方式1：CONCAT函数 -->
<select id="search">
    SELECT * FROM user WHERE name LIKE CONCAT('%', #{keyword}, '%')
</select>

<!-- ✅ 正确方式2：在Java代码中拼接 -->
<!-- Java: String param = "%" + keyword + "%"; -->
<select id="search">
    SELECT * FROM user WHERE name LIKE #{keyword}
</select>
```

### 4. 分页查询

```xml
<!-- ✅ 正确：使用LIMIT #{offset}, #{pageSize} -->
<select id="selectByPage">
    SELECT * FROM user 
    LIMIT #{offset}, #{pageSize}
</select>

<!-- ❌ 错误：使用${} -->
<select id="selectByPage">
    SELECT * FROM user 
    LIMIT ${offset}, ${pageSize}
</select>
```

### 5. IN查询

```xml
<!-- ✅ 正确：使用foreach -->
<select id="selectByIds">
    SELECT * FROM user WHERE id IN
    <foreach collection="ids" item="id" open="(" close=")" separator=",">
        #{id}
    </foreach>
</select>

<!-- ❌ 错误：使用${} -->
<select id="selectByIds">
    SELECT * FROM user WHERE id IN (${ids})
</select>
```

---

## 🧪 实践：测试SQL注入

让我们创建一个测试案例来演示SQL注入：

### 不安全的实现（演示用）

```java
// ⚠️ 警告：这只是演示，实际项目中永远不要这样做！

// Mapper接口
public interface UnsafeMapper {
    @Select("SELECT * FROM user WHERE username = '${username}'")
    User loginUnsafe(@Param("username") String username);
}

// 测试
public void testSqlInjection() {
    // 正常使用
    User user1 = mapper.loginUnsafe("admin");
    // SQL: SELECT * FROM user WHERE username = 'admin'
    // 结果：返回admin用户
    
    // 恶意攻击
    User user2 = mapper.loginUnsafe("admin' OR '1'='1");
    // SQL: SELECT * FROM user WHERE username = 'admin' OR '1'='1'
    // 结果：返回所有用户！
}
```

### 安全的实现

```java
// ✅ 安全的实现
public interface SafeMapper {
    @Select("SELECT * FROM user WHERE username = #{username}")
    User loginSafe(@Param("username") String username);
}

// 测试
public void testSafe() {
    // 恶意攻击尝试
    User user = mapper.loginSafe("admin' OR '1'='1");
    // SQL: SELECT * FROM user WHERE username = ?
    // 参数: "admin' OR '1'='1" (会被转义)
    // 结果：找不到用户，攻击失败！
}
```

---

## 🤔 思考题

### 1. 为什么ORDER BY要用${}？

<details>
<summary>点击查看答案</summary>

因为ORDER BY后面跟的是**列名**，不是**值**。

```sql
-- ✅ 正确
ORDER BY name ASC

-- ❌ 错误
ORDER BY 'name' ASC
-- 这会被当作字符串'name'排序，而不是按name列排序
```

所以ORDER BY必须用${}直接替换：
```xml
ORDER BY ${columnName} ${direction}
```

但必须验证输入！
</details>

### 2. 如何安全地处理动态表名？

<details>
<summary>点击查看答案</summary>

方案1：使用枚举
```java
public enum TableName {
    USER("user"),
    PRODUCT("product"),
    ORDER("order");
    // ...
}

String tableName = TableName.USER.name();
```

方案2：白名单验证
```java
List<String> allowedTables = Arrays.asList("user", "product", "order");
if (!allowedTables.contains(tableName)) {
    throw new SecurityException("Invalid table name");
}
```

方案3：使用映射
```java
Map<String, String> tableMapping = new HashMap<>();
tableMapping.put("users", "t_user");
tableMapping.put("products", "t_product");

String realTable = tableMapping.get(userInput);
```
</details>

### 3. PreparedStatement如何防止注入？

<details>
<summary>点击查看答案</summary>

PreparedStatement的防注入原理：

1. **预编译**：
   - SQL结构在编译时就确定了
   - ?只是占位符，不能改变SQL结构

2. **参数绑定**：
   - 参数通过setObject()绑定
   - 参数会被当作数据，不会被当作代码

3. **自动转义**：
   - 特殊字符（如单引号）会被转义
   - 恶意SQL代码变成普通字符串

4. **类型检查**：
   - 参数类型由PreparedStatement处理
   - 类型不匹配会报错
</details>

---

## 🎯 核心要点总结

### 1. 记住这个原则

```
值用 #{}   （99%的情况）
名用 ${}   （1%的情况，且要验证）
```

### 2. #{} 的特点

- ✅ 预编译
- ✅ 防注入
- ✅ 类型安全
- ✅ 性能好

### 3. ${} 的特点

- ⚠️ 字符串替换
- ⚠️ 有注入风险
- ⚠️ 必须验证输入
- ⚠️ 只用于表名、列名等

### 4. 面试重点

- 能说出#{} 和 ${} 的区别
- 能解释SQL注入的原理
- 能举例说明安全实践
- 知道什么时候用${}

---

**第三课完成！准备最后一课：实践案例！** 🚀

