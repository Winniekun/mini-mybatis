# Day 4 学习笔记 - Part 1: ResultSetHandler详解

## 🎯 第一课：理解ResultSetHandler

### ResultSetHandler是什么？

ResultSetHandler是MyBatis**四大对象的最后一个**，负责将JDBC的ResultSet转换为Java对象。

### 类比理解

```
如果把MyBatis比作餐厅：

SqlSession       = 服务员（接待客人）
Executor         = 厨师长（协调做菜）
StatementHandler = 厨具（准备和执行）
ResultSetHandler = 装盘师（结果呈现）⭐ 我们在这里！

菜做好了（SQL执行完）
  ↓
装盘师（ResultSetHandler）
  ↓
把菜摆成好看的样子（ResultSet → Java对象）
  ↓
上桌（返回给用户）
```

---

## 📚 MyBatis四大核心对象回顾

```
        用户调用
          ↓
    ┌──────────────┐
    │  SqlSession  │ - 门面，对外接口
    └──────┬───────┘
           ↓
    ┌──────────────┐
    │   Executor   │ - 执行器，协调者
    └──────┬───────┘
           ↓
    ┌──────────────┐
    │StatementHandler│ - JDBC封装，执行SQL
    └──────┬───────┘
           ↓
    ┌──────────────┐
    │ResultSetHandler│ - 结果映射，ResultSet → Object ⭐
    └──────────────┘
```

---

## 🔍 ResultSetHandler的核心职责

### 1. 遍历ResultSet

```java
// ResultSet就像一个表格
// 需要逐行读取

ResultSet rs = ps.executeQuery();

// 当前位置：表头（还没有数据）
// ┌──────┬──────────┬──────────┐
// │  id  │ username │   email  │
// ├──────┼──────────┼──────────┤
// │  1   │  admin   │ a@xx.com │ ← rs.next() 移到第1行
// │  2   │  user1   │ b@xx.com │ ← rs.next() 移到第2行
// │  3   │  user2   │ c@xx.com │ ← rs.next() 移到第3行
// └──────┴──────────┴──────────┘

while (rs.next()) {  // 每次移动到下一行
    // 当前行的数据可以通过列名或列索引获取
    long id = rs.getLong("id");
    String username = rs.getString("username");
    String email = rs.getString("email");
}
```

---

### 2. 创建结果对象

```java
// 根据resultType创建对象实例
Object bean = resultType.newInstance();

// 例如：
// resultType = User.class
// 创建一个空的User对象
User user = new User();
```

---

### 3. 映射字段到属性

```java
// 从ResultSet读取字段值
String username = rs.getString("username");

// 设置到对象属性
user.setUsername(username);

// 或通过反射
Field field = User.class.getDeclaredField("username");
field.setAccessible(true);
field.set(user, username);
```

---

### 4. 处理类型转换

```java
// 数据库类型 → JDBC类型 → Java类型

MySQL BIGINT → java.lang.Long
MySQL VARCHAR → java.lang.String
MySQL DECIMAL → java.math.BigDecimal
MySQL DATETIME → java.sql.Timestamp → java.time.LocalDateTime
```

---

## 📖 ResultSetHandler的核心方法

### 方法1: handleResultSet() - 入口方法

```java
public <E> List<E> handleResultSet(ResultSet resultSet, Class<?> resultType) throws SQLException {
    List<E> resultList = new ArrayList<>();
    
    // 步骤1: 获取ResultSet元数据
    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = metaData.getColumnCount();
    // 元数据包含：
    // - 列数
    // - 每列的名称
    // - 每列的类型
    
    logger.debug("结果集包含{}列", columnCount);
    
    // 步骤2: 遍历结果集
    while (resultSet.next()) {
        // 步骤3: 创建单行对象
        E rowObject = createResultObject(resultSet, resultType, metaData, columnCount);
        resultList.add(rowObject);
    }
    
    logger.debug("结果集处理完成，共{}行", resultList.size());
    
    return resultList;
}
```

**执行流程**：
```
ResultSet (3行数据)
    ↓
遍历每一行
    ↓
┌─────────────────┐
│ 第1行           │ → createResultObject() → User对象1
├─────────────────┤
│ 第2行           │ → createResultObject() → User对象2
├─────────────────┤
│ 第3行           │ → createResultObject() → User对象3
└─────────────────┘
    ↓
List<User> (包含3个User对象)
```

---

### 方法2: createResultObject() - 创建对象

```java
private <E> E createResultObject(ResultSet resultSet, Class<?> resultType, 
                                  ResultSetMetaData metaData, int columnCount) throws SQLException {
    
    // 情况1: 简单类型（String、Integer、Long等）
    if (isSimpleType(resultType)) {
        // 直接返回第一列的值
        return (E) resultSet.getObject(1);
    }
    
    // 情况2: Map类型
    if (Map.class.isAssignableFrom(resultType)) {
        return (E) handleMapType(resultSet, metaData, columnCount);
    }
    
    // 情况3: JavaBean类型（User、Product等）
    return (E) handleBeanType(resultSet, resultType, metaData, columnCount);
}
```

**三种结果类型**：

#### 类型1：简单类型
```java
// SQL: SELECT COUNT(*) FROM user
// resultType: Long.class

// 处理：直接返回第一列
Long count = rs.getLong(1);  // 返回 5
```

#### 类型2：Map类型
```java
// SQL: SELECT id, username, email FROM user WHERE id = 1
// resultType: Map.class

// 处理：每列作为Map的一个entry
Map<String, Object> map = new HashMap<>();
map.put("id", 1L);
map.put("username", "admin");
map.put("email", "admin@example.com");
```

#### 类型3：JavaBean类型
```java
// SQL: SELECT id, username, email FROM user WHERE id = 1
// resultType: User.class

// 处理：映射到对象属性
User user = new User();
user.setId(1L);
user.setUsername("admin");
user.setEmail("admin@example.com");
```

---

### 方法3: handleBeanType() - JavaBean映射 ⭐核心

```java
private Object handleBeanType(ResultSet resultSet, Class<?> resultType, 
                               ResultSetMetaData metaData, int columnCount) throws SQLException {
    try {
        // 步骤1: 创建对象实例 ⭐
        Object bean = resultType.newInstance();
        // 内部调用：new User()
        
        // 步骤2: 获取所有字段，建立映射表 ⭐
        Field[] fields = resultType.getDeclaredFields();
        Map<String, Field> fieldMap = new HashMap<>();
        
        for (Field field : fields) {
            field.setAccessible(true);  // 允许访问private字段
            
            // 支持两种命名方式
            fieldMap.put(field.getName().toLowerCase(), field);
            // 例如：username → fieldMap["username"] = username字段
            
            fieldMap.put(camelToUnderscore(field.getName()).toLowerCase(), field);
            // 例如：userName → fieldMap["user_name"] = userName字段
        }
        
        // 步骤3: 遍历所有列，设置属性值 ⭐
        for (int i = 1; i <= columnCount; i++) {
            // 3.1 获取列名和列值
            String columnName = metaData.getColumnLabel(i);
            Object columnValue = resultSet.getObject(i);
            
            // 3.2 查找对应的字段
            Field field = fieldMap.get(columnName.toLowerCase());
            
            if (field != null && columnValue != null) {
                // 3.3 类型转换
                Object value = convertType(columnValue, field.getType());
                
                // 3.4 设置字段值
                field.set(bean, value);
                // 等价于：user.setUsername(value)
                
                logger.trace("设置属性: {} = {}", field.getName(), value);
            } else {
                logger.trace("未找到字段映射: {}", columnName);
            }
        }
        
        return bean;
        
    } catch (InstantiationException | IllegalAccessException e) {
        throw new MyBatisException("创建结果对象失败: " + resultType, e);
    }
}
```

**详细执行流程**：

```
假设查询：SELECT id, username, email FROM user WHERE id = 1

ResultSet:
┌──────┬──────────┬──────────────┐
│  id  │ username │    email     │
├──────┼──────────┼──────────────┤
│  1   │  admin   │ a@example.com│
└──────┴──────────┴──────────────┘

步骤1：创建对象
User user = new User();

步骤2：建立字段映射
fieldMap = {
    "id"       → id字段,
    "username" → username字段,
    "email"    → email字段
}

步骤3：遍历每列设置值

第1列：id
  columnName = "id"
  columnValue = 1L
  field = fieldMap.get("id")  // 找到id字段
  value = convertType(1L, Long.class)  // 类型转换
  field.set(user, value)  // user.id = 1L
  
第2列：username
  columnName = "username"
  columnValue = "admin"
  field = fieldMap.get("username")  // 找到username字段
  value = convertType("admin", String.class)  // 不需要转换
  field.set(user, value)  // user.username = "admin"
  
第3列：email
  columnName = "email"
  columnValue = "a@example.com"
  field = fieldMap.get("email")  // 找到email字段
  value = convertType("a@example.com", String.class)
  field.set(user, value)  // user.email = "a@example.com"

最终返回：
User {
    id = 1L,
    username = "admin",
    email = "a@example.com"
}
```

---

### 方法4: convertType() - 类型转换

```java
private Object convertType(Object value, Class<?> targetType) {
    if (value == null) {
        return null;
    }
    
    // 类型相同，直接返回
    if (targetType.isAssignableFrom(value.getClass())) {
        return value;
    }
    
    // 转换为String
    if (targetType == String.class) {
        return value.toString();
    }
    
    // 转换为Integer
    if (targetType == Integer.class || targetType == int.class) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
    
    // 转换为Long
    if (targetType == Long.class || targetType == long.class) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }
    
    // ... 其他类型转换
    
    return value;
}
```

**类型转换场景**：

```
场景1：数字类型转换
  数据库: BIGINT (Long)
  Java属性: int
  转换: ((Long)value).intValue()

场景2：字符串转数字
  数据库: VARCHAR ("123")
  Java属性: Integer
  转换: Integer.parseInt("123")

场景3：数字转布尔
  数据库: TINYINT (1)
  Java属性: Boolean
  转换: 1 != 0 → true
```

---

### 方法5: camelToUnderscore() - 命名转换

```java
private String camelToUnderscore(String camelCase) {
    // userName → user_name
    // createTime → create_time
    
    StringBuilder result = new StringBuilder();
    result.append(Character.toLowerCase(camelCase.charAt(0)));
    
    for (int i = 1; i < camelCase.length(); i++) {
        char ch = camelCase.charAt(i);
        if (Character.isUpperCase(ch)) {
            result.append('_');
            result.append(Character.toLowerCase(ch));
        } else {
            result.append(ch);
        }
    }
    
    return result.toString();
}
```

**命名转换示例**：

```
Java驼峰命名    →    数据库下划线命名
─────────────────────────────────────
id              →    id
username        →    username
userName        →    user_name
createTime      →    create_time
isActive        →    is_active
userId          →    user_id
```

**为什么需要命名转换？**

```
数据库规范：
  表名、字段名通常使用小写+下划线
  例如：user_name, create_time

Java规范：
  类名使用大驼峰（PascalCase）
  属性名使用小驼峰（camelCase）
  例如：userName, createTime

MyBatis自动转换：
  数据库字段 user_name ↔ Java属性 userName
```

---

## 🎯 完整执行流程

### 示例：查询用户列表

```java
// 1. 用户调用
List<User> users = mapper.selectAll();

// 2. 执行SQL
ResultSet rs = ps.executeQuery();
// rs包含3行数据：
// ┌────┬──────────┬────────────┐
// │ id │ username │   email    │
// ├────┼──────────┼────────────┤
// │ 1  │  admin   │ a@xx.com   │
// │ 2  │  user1   │ b@xx.com   │
// │ 3  │  user2   │ c@xx.com   │
// └────┴──────────┴────────────┘

// 3. ResultSetHandler处理
List<User> result = resultSetHandler.handleResultSet(rs, User.class);

// 内部流程：
// 3.1 获取元数据
ResultSetMetaData metaData = rs.getMetaData();
// columnCount = 3
// columns = ["id", "username", "email"]

// 3.2 遍历第1行
rs.next();  // 移到第1行
User user1 = new User();
user1.setId(rs.getLong("id"));         // 1L
user1.setUsername(rs.getString("username"));  // "admin"
user1.setEmail(rs.getString("email"));  // "a@xx.com"
result.add(user1);

// 3.3 遍历第2行
rs.next();  // 移到第2行
User user2 = new User();
user2.setId(rs.getLong("id"));         // 2L
user2.setUsername(rs.getString("username"));  // "user1"
user2.setEmail(rs.getString("email"));  // "b@xx.com"
result.add(user2);

// 3.4 遍历第3行
rs.next();  // 移到第3行
User user3 = new User();
user3.setId(rs.getLong("id"));         // 3L
user3.setUsername(rs.getString("username"));  // "user2"
user3.setEmail(rs.getString("email"));  // "c@xx.com"
result.add(user3);

// 3.5 没有更多行
rs.next();  // 返回false，退出循环

// 4. 返回结果
return result;  // List包含3个User对象
```

---

## 📊 ResultSet的本质

### ResultSet是什么？

```
ResultSet就是一个"游标"，指向查询结果的某一行

初始位置：
  ┌─────────┐
  │  游标   │ ← 在第一行之前
  ├─────────┤
  │  数据行1 │
  ├─────────┤
  │  数据行2 │
  ├─────────┤
  │  数据行3 │
  └─────────┘

调用next()后：
  ┌─────────┐
  │  数据行1 │ ← 游标移到这里
  ├─────────┤
  │  数据行2 │
  ├─────────┤
  │  数据行3 │
  └─────────┘
  
可以读取当前行的数据：
  rs.getLong("id")
  rs.getString("username")
  等等...
```

### ResultSet的常用方法

```java
// 移动游标
boolean next()         // 移到下一行，有数据返回true
boolean previous()     // 移到上一行
boolean first()        // 移到第一行
boolean last()         // 移到最后一行

// 获取数据
Object getObject(int columnIndex)       // 通过列索引
Object getObject(String columnLabel)    // 通过列名
String getString(String columnLabel)    // 获取String
Long getLong(String columnLabel)        // 获取Long
// ... 其他类型

// 获取元数据
ResultSetMetaData getMetaData()         // 获取结果集的元数据
```

---

## 🤔 思考题

### 1. 为什么需要ResultSetHandler？

<details>
<summary>点击查看答案</summary>

因为JDBC返回的是ResultSet，需要手动转换：

```java
// 原始JDBC方式（繁琐）
while (rs.next()) {
    User user = new User();
    user.setId(rs.getLong("id"));
    user.setUsername(rs.getString("username"));
    user.setEmail(rs.getString("email"));
    // 每个字段都要写一遍！
    users.add(user);
}

// ResultSetHandler方式（自动）
List<User> users = resultSetHandler.handleResultSet(rs, User.class);
// 一行搞定！
```

ResultSetHandler封装了：
- ✅ ResultSet遍历
- ✅ 对象创建
- ✅ 字段映射
- ✅ 类型转换

减少80%的重复代码！
</details>

### 2. 如何实现驼峰命名转换？

<details>
<summary>点击查看答案</summary>

通过两次映射：

```java
// 建立映射表
fieldMap.put("username", usernameField);      // 完全匹配
fieldMap.put("user_name", usernameField);     // 下划线匹配

// 无论数据库字段是username还是user_name
// 都能找到对应的username属性
```

这样既支持：
- username → username（完全匹配）
- user_name → userName（驼峰转换）
</details>

### 3. 类型转换的作用是什么？

<details>
<summary>点击查看答案</summary>

因为JDBC返回的类型可能与Java属性类型不完全匹配：

```
MySQL BIGINT      → JDBC Long     → Java int
MySQL DECIMAL     → JDBC BigDecimal → Java double
MySQL TINYINT(1)  → JDBC Integer  → Java boolean
```

convertType()负责：
- 判断类型是否匹配
- 如果不匹配，进行转换
- 处理常见的类型转换场景
</details>

---

## 🎯 核心要点

1. **ResultSetHandler的职责**
   - 遍历ResultSet
   - 创建对象
   - 映射字段
   - 转换类型

2. **支持三种结果类型**
   - 简单类型：直接返回第一列
   - Map类型：每列作为一个entry
   - JavaBean：自动映射属性

3. **自动命名转换**
   - 驼峰命名 ↔ 下划线命名
   - userName ↔ user_name

4. **自动类型转换**
   - 数据库类型 → Java类型
   - Long → int, String → int等

---

**第一课完成！休息3分钟，准备第二课：自动映射机制！** ☕

