# Day 4 学习笔记 - Part 2: 自动映射机制深入剖析

## 🔧 第二课：自动映射机制

### 什么是自动映射？

**自动映射（Auto Mapping）**是MyBatis的核心特性之一，它能自动将ResultSet的列映射到Java对象的属性，无需手动编写映射代码。

###类比理解

```
想象你在填表格：

表格（ResultSet）：
┌────────────┬─────────────┐
│   姓名     │    年龄     │
├────────────┼─────────────┤
│   张三     │     25      │
└────────────┴─────────────┘

对象（User）：
class User {
    String name;
    int age;
}

自动映射就是：
自动把"姓名"的值填到name属性
自动把"年龄"的值填到age属性

不需要你手动写：
user.setName("张三");
user.setAge(25);
```

---

## 📚 映射的三个层次

```
层次1：完全匹配
  数据库字段：username
  Java属性：username
  ✅ 直接匹配

层次2：驼峰转换
  数据库字段：user_name
  Java属性：userName
  ✅ 自动转换匹配

层次3：手动配置
  数据库字段：user_id
  Java属性：id
  ⚠️ 需要<resultMap>配置
```

---

## 🔍 自动映射的工作原理

### 步骤1：建立字段映射表

```java
// 获取Java类的所有字段
Field[] fields = User.class.getDeclaredFields();
// fields = [id, username, password, email, createTime]

// 建立映射表
Map<String, Field> fieldMap = new HashMap<>();

for (Field field : fields) {
    field.setAccessible(true);  // 允许访问private字段
    
    // 策略1：原始名称（小写）
    fieldMap.put(field.getName().toLowerCase(), field);
    // "username" → username字段
    
    // 策略2：下划线名称（小写）
    fieldMap.put(camelToUnderscore(field.getName()).toLowerCase(), field);
    // "user_name" → username字段
}

// 最终的映射表：
// {
//   "id"          → id字段,
//   "username"    → username字段,
//   "user_name"   → username字段,  ← 支持下划线
//   "password"    → password字段,
//   "email"       → email字段,
//   "createtime"  → createTime字段,
//   "create_time" → createTime字段  ← 支持下划线
// }
```

---

### 步骤2：遍历ResultSet的列

```java
// 获取ResultSet的元数据
ResultSetMetaData metaData = rs.getMetaData();
int columnCount = metaData.getColumnCount();  // 假设5列

// 遍历每一列
for (int i = 1; i <= columnCount; i++) {
    // 获取列名
    String columnName = metaData.getColumnLabel(i);
    // 列1: "id"
    // 列2: "username"
    // 列3: "password"
    // 列4: "email"
    // 列5: "create_time"
    
    // 获取列值
    Object columnValue = rs.getObject(i);
    // 1L, "admin", "123456", "a@xx.com", Timestamp对象
}
```

---

### 步骤3：查找对应的字段

```java
for (int i = 1; i <= columnCount; i++) {
    String columnName = metaData.getColumnLabel(i);
    Object columnValue = rs.getObject(i);
    
    // 查找字段（不区分大小写）
    Field field = fieldMap.get(columnName.toLowerCase());
    
    if (field != null) {
        // 找到了！
        // columnName="create_time" → field=createTime字段
    } else {
        // 没找到，跳过这列
        logger.trace("未找到字段映射: {}", columnName);
    }
}
```

---

### 步骤4：类型转换和赋值

```java
if (field != null && columnValue != null) {
    // 类型转换
    Object value = convertType(columnValue, field.getType());
    // 例如：Timestamp → LocalDateTime
    
    // 设置字段值
    field.set(bean, value);
    // 等价于：user.setCreateTime(value)
    
    logger.trace("设置属性: {} = {}", field.getName(), value);
}
```

---

## 🎯 命名转换详解

### 驼峰命名 vs 下划线命名

```
Java规范（驼峰命名 camelCase）：
  类名：User, Product, OrderInfo
  属性名：id, userName, createTime

数据库规范（下划线命名 snake_case）：
  表名：user, product, order_info
  字段名：id, user_name, create_time

问题：
  Java属性 userName 对应 数据库字段 user_name
  如果不转换，无法自动匹配！

解决：
  MyBatis自动转换
  userName ↔ user_name
```

---

### 转换算法

```java
private String camelToUnderscore(String camelCase) {
    // userName → user_name
    
    StringBuilder result = new StringBuilder();
    
    // 第一个字符小写
    result.append(Character.toLowerCase(camelCase.charAt(0)));
    // 'u'
    
    // 遍历剩余字符
    for (int i = 1; i < camelCase.length(); i++) {
        char ch = camelCase.charAt(i);
        
        if (Character.isUpperCase(ch)) {
            // 遇到大写字母，加下划线
            result.append('_');
            result.append(Character.toLowerCase(ch));
            // 'N' → '_n'
        } else {
            // 小写字母直接加
            result.append(ch);
            // 's', 'e', 'r' → 'ser'
        }
    }
    
    return result.toString();
    // "user_name"
}
```

---

### 转换示例

| Java属性 | 转换结果 | 说明 |
|---------|---------|------|
| id | id | 无大写字母，不变 |
| username | username | 无大写字母，不变 |
| userName | user_name | 'N'是大写，变成'_n' |
| createTime | create_time | 'T'是大写，变成'_t' |
| isActive | is_active | 'A'是大写，变成'_a' |
| userId | user_id | 'I'是大写，变成'_i' |
| userDetailInfo | user_detail_info | 两个大写，各加下划线 |

---

## 📊 映射策略对比

### 策略1：直接匹配（无转换）

```
数据库字段：username
Java属性：username
匹配：✅ 成功

数据库字段：user_name
Java属性：username
匹配：❌ 失败（无法映射）
```

---

### 策略2：驼峰转换（推荐）⭐

```
数据库字段：username
Java属性：username
匹配：✅ 成功（直接匹配）

数据库字段：user_name
Java属性：userName
转换：userName → user_name
匹配：✅ 成功（转换后匹配）
```

---

### 策略3：手动配置（高级）

```xml
<!-- 当自动映射无法满足时 -->
<resultMap id="userMap" type="User">
    <id column="user_id" property="id"/>
    <result column="user_name" property="name"/>
    <result column="user_email" property="email"/>
</resultMap>

<select id="selectById" resultMap="userMap">
    SELECT user_id, user_name, user_email FROM user WHERE user_id = #{id}
</select>
```

---

## 🔍 类型转换详解

### 为什么需要类型转换？

```
问题：
JDBC返回的类型可能与Java属性类型不一致

示例1：
  数据库：BIGINT
  JDBC返回：Long
  Java属性：int
  需要转换：Long → int

示例2：
  数据库：VARCHAR("123")
  JDBC返回：String
  Java属性：Integer
  需要转换：String → Integer

示例3：
  数据库：TINYINT(1)
  JDBC返回：Integer(1)
  Java属性：Boolean
  需要转换：Integer → Boolean (1 → true)
```

---

### 常见类型转换

```java
private Object convertType(Object value, Class<?> targetType) {
    // 转换1：同类型，直接返回
    if (targetType.isAssignableFrom(value.getClass())) {
        return value;
    }
    
    // 转换2：转为String
    if (targetType == String.class) {
        return value.toString();
        // 任何类型 → String
    }
    
    // 转换3：转为Integer
    if (targetType == Integer.class || targetType == int.class) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
            // Long/Double → Integer
        }
        return Integer.parseInt(value.toString());
        // String → Integer
    }
    
    // 转换4：转为Long
    if (targetType == Long.class || targetType == long.class) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }
    
    // 转换5：转为Boolean
    if (targetType == Boolean.class || targetType == boolean.class) {
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
            // 1 → true, 0 → false
        }
        return Boolean.parseBoolean(value.toString());
        // "true" → true
    }
    
    // 其他类型暂不支持
    return value;
}
```

---

### 类型转换矩阵

| 源类型 | 目标类型 | 转换方式 | 示例 |
|-------|---------|---------|------|
| Long | int | .intValue() | 123L → 123 |
| Integer | Long | .longValue() | 123 → 123L |
| String | Integer | parseInt() | "123" → 123 |
| Integer | String | toString() | 123 → "123" |
| Integer | Boolean | != 0 | 1 → true |
| String | Boolean | parseBoolean() | "true" → true |
| BigDecimal | Double | .doubleValue() | 123.45 → 123.45 |

---

## 🎯 完整映射案例

### 案例：用户查询

**数据库表结构**：
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(50),
    password VARCHAR(100),
    email VARCHAR(100),
    is_active TINYINT(1),
    create_time DATETIME
);
```

**Java类**：
```java
public class User {
    private Long id;
    private String userName;
    private String password;
    private String email;
    private Boolean isActive;
    private LocalDateTime createTime;
    
    // getter/setter...
}
```

**SQL查询**：
```sql
SELECT id, user_name, password, email, is_active, create_time
FROM user
WHERE id = 1
```

**ResultSet**：
```
┌────┬───────────┬──────────┬────────────┬───────────┬─────────────────────┐
│ id │ user_name │ password │   email    │ is_active │     create_time     │
├────┼───────────┼──────────┼────────────┼───────────┼─────────────────────┤
│ 1  │  admin    │  123456  │ a@xx.com   │     1     │ 2024-01-01 10:00:00 │
└────┴───────────┴──────────┴────────────┴───────────┴─────────────────────┘
```

---

**映射过程**：

#### 第1列：id
```
列名：id
列值：1L (Long类型)
查找：fieldMap.get("id") → id字段
类型：Long → Long (相同，不需要转换)
赋值：user.id = 1L
```

#### 第2列：user_name
```
列名：user_name
列值："admin" (String类型)
查找：fieldMap.get("user_name") → userName字段 ⭐ 驼峰转换
类型：String → String (相同)
赋值：user.userName = "admin"
```

#### 第3列：password
```
列名：password
列值："123456"
查找：fieldMap.get("password") → password字段
类型：String → String
赋值：user.password = "123456"
```

#### 第4列：email
```
列名：email
列值："a@xx.com"
查找：fieldMap.get("email") → email字段
类型：String → String
赋值：user.email = "a@xx.com"
```

#### 第5列：is_active
```
列名：is_active
列值：1 (Integer类型)
查找：fieldMap.get("is_active") → isActive字段 ⭐ 驼峰转换
类型：Integer → Boolean ⭐ 类型转换
      1 != 0 → true
赋值：user.isActive = true
```

#### 第6列：create_time
```
列名：create_time
列值：Timestamp("2024-01-01 10:00:00")
查找：fieldMap.get("create_time") → createTime字段 ⭐ 驼峰转换
类型：Timestamp → LocalDateTime ⭐ 类型转换
      (JDBC驱动自动转换)
赋值：user.createTime = LocalDateTime.parse("2024-01-01T10:00:00")
```

---

**最终结果**：
```java
User {
    id = 1L,
    userName = "admin",
    password = "123456",
    email = "a@xx.com",
    isActive = true,
    createTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
}
```

---

## 🤔 常见问题

### 问题1：字段映射不上怎么办？

**现象**：
```java
// 数据库有数据，但对象属性为null
User user = mapper.selectById(1L);
System.out.println(user.getUserName());  // null ❌
```

**排查步骤**：

1. **检查列名**
```sql
-- 查看实际返回的列名
SELECT * FROM user;
-- 是 username 还是 user_name？
```

2. **检查Java属性名**
```java
private String userName;  // 驼峰
// 或
private String username;  // 小写
```

3. **检查日志**
```
TRACE - 未找到字段映射: user_name
```

**解决方案**：

方案1：统一命名
```java
// 数据库字段：username (全小写)
// Java属性：username (全小写)
private String username;  // ✅ 直接匹配
```

方案2：使用驼峰转换
```java
// 数据库字段：user_name (下划线)
// Java属性：userName (驼峰)
private String userName;  // ✅ 自动转换匹配
```

方案3：SQL别名
```sql
-- 数据库字段叫 uname
SELECT id, uname AS userName FROM user
-- 别名userName可以匹配Java属性
```

---

### 问题2：类型转换失败怎么办？

**现象**：
```java
// 数据库：VARCHAR("abc")
// Java属性：Integer
java.lang.NumberFormatException: For input string: "abc"
```

**原因**：
- 数据类型不兼容
- 无法将"abc"转换为Integer

**解决方案**：

方案1：修改Java类型
```java
// 改为String
private String age;  // ✅
```

方案2：在SQL中转换
```sql
SELECT CAST(age AS SIGNED) AS age FROM user
```

方案3：自定义TypeHandler（高级）

---

### 问题3：性能问题

**问题**：
```
每次查询都要：
1. 获取所有Field
2. 建立fieldMap
3. 反射赋值

会不会很慢？
```

**答案**：

实际性能影响不大：

1. **Field获取可以缓存**
```java
// 真实MyBatis会缓存
private static Map<Class<?>, Map<String, Field>> fieldCache;
```

2. **反射性能已优化**
```
现代JVM对反射做了很多优化
对于简单的get/set操作，性能接近直接调用
```

3. **瓶颈在数据库**
```
查询1000行数据：
- SQL执行：100ms
- 网络传输：50ms
- 结果映射：5ms  ← 占比很小

瓶颈在SQL和网络，不在映射
```

---

## 🎯 自动映射的优势

### 1. 减少配置
```xml
<!-- 不需要写 -->
<result column="id" property="id"/>
<result column="username" property="username"/>
<result column="email" property="email"/>
<!-- 自动映射！ -->
```

### 2. 减少代码
```java
// 不需要写
User user = new User();
user.setId(rs.getLong("id"));
user.setUsername(rs.getString("username"));
// 自动映射！
```

### 3. 适应变化
```
添加新字段：
1. 数据库加字段
2. Java类加属性
3. 自动映射 ✅

不需要修改映射配置！
```

### 4. 提高效率
```
开发效率提升 80%
从100行代码 → 20行配置
```

---

## 🎯 核心要点

1. **自动映射的两个策略**
   - 直接匹配：username → username
   - 驼峰转换：user_name → userName

2. **建立字段映射表**
   - 获取所有Field
   - 支持两种命名方式
   - 不区分大小写

3. **类型自动转换**
   - 数字类型互转
   - 字符串转数字
   - 数字转布尔

4. **面向约定编程**
   - 遵循命名规范
   - 自动完成映射
   - 减少配置代码

---

**第二课完成！休息3分钟，准备第三课：复杂映射和实践！** ☕

