# Day 3 学习笔记 - Part 4: 实践案例与总结

## 🎯 第四课：动手实践

让我们通过实际案例来巩固今天学到的知识！

---

## 📝 实践任务1：实现带排序的商品查询

### 需求

实现一个商品查询接口，支持按不同字段排序：
- 按价格排序
- 按库存排序
- 按创建时间排序

### 错误示例（不安全）❌

```java
// ProductMapper.java
public interface ProductMapper {
    // ❌ 危险：直接使用用户输入
    List<Product> selectAllOrderBy(@Param("orderColumn") String orderColumn);
}
```

```xml
<!-- ProductMapper.xml -->
<!-- ❌ 危险：SQL注入风险 -->
<select id="selectAllOrderBy" resultType="Product">
    SELECT * FROM product 
    ORDER BY ${orderColumn}
</select>
```

**攻击演示**：
```java
// 恶意输入
String orderColumn = "price; DROP TABLE product; --";

// 生成的SQL
SELECT * FROM product ORDER BY price; DROP TABLE product; --

// 结果：表被删除！
```

---

### 正确示例（安全）✅

**方案1：使用枚举**

```java
// 1. 定义排序枚举
public enum ProductOrderColumn {
    PRICE("price", "价格"),
    STOCK("stock", "库存"),
    CREATE_TIME("create_time", "创建时间");
    
    private String column;
    private String desc;
    
    ProductOrderColumn(String column, String desc) {
        this.column = column;
        this.desc = desc;
    }
    
    public String getColumn() {
        return column;
    }
}

// 2. Mapper接口
public interface ProductMapper {
    List<Product> selectAllOrderBy(
        @Param("orderColumn") String orderColumn,
        @Param("orderDirection") String orderDirection
    );
}
```

```xml
<!-- 3. Mapper XML -->
<select id="selectAllOrderBy" resultType="Product">
    SELECT * FROM product 
    ORDER BY ${orderColumn} ${orderDirection}
</select>
```

```java
// 4. Service层验证
public List<Product> getProducts(String orderBy, String direction) {
    // 验证排序字段
    ProductOrderColumn column;
    try {
        column = ProductOrderColumn.valueOf(orderBy.toUpperCase());
    } catch (IllegalArgumentException e) {
        // 默认按价格排序
        column = ProductOrderColumn.PRICE;
    }
    
    // 验证排序方向
    if (!"ASC".equalsIgnoreCase(direction) && 
        !"DESC".equalsIgnoreCase(direction)) {
        direction = "ASC";
    }
    
    // 安全地调用
    return productMapper.selectAllOrderBy(
        column.getColumn(), 
        direction.toUpperCase()
    );
}
```

**关键点**：
- ✅ 使用枚举限制可选值
- ✅ Service层验证输入
- ✅ 提供默认值
- ✅ 永远不直接使用用户输入

---

## 📝 实践任务2：安全的搜索功能

### 需求

实现商品名称的模糊搜索。

### 错误示例（不安全）❌

```xml
<!-- ❌ 危险：SQL注入 -->
<select id="searchByName" resultType="Product">
    SELECT * FROM product 
    WHERE product_name LIKE '%${keyword}%'
</select>
```

**攻击演示**：
```java
// 恶意输入
String keyword = "%' OR '1'='1";

// 生成的SQL
SELECT * FROM product WHERE product_name LIKE '%%' OR '1'='1%'

// 结果：返回所有商品
```

---

### 正确示例（安全）✅

**方案1：使用CONCAT函数**

```xml
<!-- ✅ 安全：使用#{}和CONCAT -->
<select id="searchByName" resultType="Product">
    SELECT * FROM product 
    WHERE product_name LIKE CONCAT('%', #{keyword}, '%')
</select>
```

**方案2：在Java代码中处理**

```java
// Java代码
public List<Product> searchProducts(String keyword) {
    // 在Java中添加%
    String pattern = "%" + keyword + "%";
    return productMapper.searchByNameSafe(pattern);
}
```

```xml
<!-- XML -->
<select id="searchByNameSafe" resultType="Product">
    SELECT * FROM product 
    WHERE product_name LIKE #{pattern}
</select>
```

---

## 📝 实践任务3：复杂参数处理

### 需求

实现一个高级搜索：按分类、价格区间、关键词搜索。

```java
// 搜索条件对象
public class ProductSearchParam {
    private String category;      // 分类
    private BigDecimal minPrice;  // 最低价格
    private BigDecimal maxPrice;  // 最高价格
    private String keyword;       // 关键词
    
    // getter/setter...
}
```

```java
// Mapper接口
public interface ProductMapper {
    List<Product> searchProducts(ProductSearchParam param);
}
```

```xml
<!-- Mapper XML -->
<select id="searchProducts" resultType="Product">
    SELECT * FROM product 
    WHERE 1=1
    <if test="category != null and category != ''">
        AND category = #{category}
    </if>
    <if test="minPrice != null">
        AND price >= #{minPrice}
    </if>
    <if test="maxPrice != null">
        AND price <= #{maxPrice}
    </if>
    <if test="keyword != null and keyword != ''">
        AND product_name LIKE CONCAT('%', #{keyword}, '%')
    </if>
    ORDER BY price ASC
</select>
```

**关键点**：
- ✅ 所有值都用 #{}
- ✅ 动态SQL（<if>标签）
- ✅ 安全可靠

---

## 🔬 调试实践

让我们通过调试来理解SQL执行过程：

### 断点设置

1. **StatementHandler.prepare()** - 第72行
   - 观察SQL的转换：#{} → ?

2. **StatementHandler.parameterize()** - 第94行
   - 观察参数的设置

3. **StatementHandler.query()** - 第120行
   - 观察SQL的执行

### 调试步骤

```java
// 1. 运行测试
@Test
public void testDebug() {
    SqlSession session = sqlSessionFactory.openSession();
    ProductMapper mapper = session.getMapper(ProductMapper.class);
    
    // 在这里打断点
    Product product = mapper.selectById(1L);
    
    session.close();
}

// 2. F7进入方法，观察执行流程：
//    MapperProxy.invoke()
//      → SqlSession.selectOne()
//        → Executor.query()
//          → StatementHandler.prepare()    ← 断点1
//            → StatementHandler.parameterize()  ← 断点2
//              → StatementHandler.query()      ← 断点3
```

### 观察内容

**断点1（prepare）**：
```java
sql = "SELECT * FROM product WHERE id = #{id}"
preparedSql = "SELECT * FROM product WHERE id = ?"
// 观察：#{id}被替换为?
```

**断点2（parameterize）**：
```java
parameter = 1L
// 观察：调用ps.setObject(1, 1L)
```

**断点3（query）**：
```java
// 观察：ps.executeQuery()执行
// 观察：ResultSet包含查询结果
```

---

## 🎯 Day 3 完整总结

### 今天学到的核心内容

#### 1. Executor执行器

```
SimpleExecutor    - 每次新建Statement（默认）
ReuseExecutor     - 复用Statement
BatchExecutor     - 批量执行

核心职责：
✅ 协调SQL执行流程
✅ 不直接操作JDBC
✅ 委托给StatementHandler
```

#### 2. StatementHandler

```
核心职责：
✅ prepare() - 创建PreparedStatement
✅ parameterize() - 设置参数
✅ query()/update() - 执行SQL

关键：
✅ 封装JDBC操作
✅ 将#{}转换为?
✅ 使用PreparedStatement
```

#### 3. #{} vs ${}

```
#{}:
✅ 预编译
✅ 防SQL注入
✅ 用于值
✅ 99%的情况使用

${}:
⚠️ 字符串替换
⚠️ 有注入风险
⚠️ 用于表名、列名
⚠️ 必须验证输入
```

#### 4. SQL注入防护

```
最佳实践：
✅ 优先使用#{}
✅ ${}必须验证
✅ 使用枚举限制值
✅ LIKE用CONCAT
✅ 永不直接用户输入
```

---

## 📊 知识图谱

```
              Executor
                 │
        ┌────────┼────────┐
        ↓        ↓        ↓
    Simple    Reuse    Batch
        │
        └─> StatementHandler
                 │
        ┌────────┼────────┐
        ↓        ↓        ↓
    prepare  parameterize  query
        │        │         │
        ↓        ↓         ↓
    #{} → ?  setObject  executeQuery
        │        │         │
        ↓        ↓         ↓
    PreparedStatement → ResultSet
```

---

## 🤔 面试题精选

### 1. MyBatis中#{} 和 ${} 的区别？

**答案要点**：
- #{}是预编译，${}是字符串替换
- #{}防SQL注入，${}有风险
- #{}用于值，${}用于表名列名
- #{}生成PreparedStatement，${}生成Statement

### 2. MyBatis如何防止SQL注入？

**答案要点**：
- 使用PreparedStatement预编译
- 参数通过?占位符绑定
- 参数会被自动转义
- #{}是安全的

### 3. 什么时候必须使用${}？

**答案要点**：
- 动态表名
- 动态列名
- ORDER BY字段
- 但必须验证输入

### 4. Executor有几种类型？各有什么特点？

**答案要点**：
- SimpleExecutor：简单，每次新建Statement
- ReuseExecutor：复用Statement
- BatchExecutor：批量执行，提高性能

---

## 📝 作业

### 必做：
1. ✅ 实现安全的商品排序查询
2. ✅ 实现安全的搜索功能
3. ✅ 理解#{} 和 ${} 的区别
4. ✅ 调试一遍完整流程

### 选做：
1. 实现价格区间查询
2. 实现多条件组合查询
3. 研究真实MyBatis的ParameterHandler

---

## 🎓 学习进度

```
Week 1: 基础入门
  Day 1: ✅ 基本使用和执行流程
  Day 2: ✅ Configuration和配置解析
  Day 3: ✅ Executor和SQL执行
  Day 4: ⏳ ResultSetHandler和高级特性
  Day 5: ⏳ 完整项目实战
```

---

## 💪 你现在掌握了

- ✅ Executor的工作原理
- ✅ StatementHandler的职责
- ✅ SQL预编译的过程
- ✅ #{} 和 ${} 的本质区别
- ✅ SQL注入的原理和防护
- ✅ 参数处理的最佳实践

**这些都是面试高频考点！** 🔥

---

## 🌙 今晚任务

1. **复习今天的笔记**
   - 重点：Executor的三种类型
   - 重点：#{} vs ${}
   - 重点：SQL注入防护

2. **运行调试**
   - 完整跟踪一次SQL执行
   - 观察参数设置过程
   - 理解预编译的效果

3. **思考问题**
   - 如果要实现动态SQL（<if>、<foreach>），应该在哪个环节处理？
   - ResultSetHandler是如何把ResultSet转换为Java对象的？

---

**Day 3 圆满完成！今天的内容很重要，好好消化！** 🎉

**明天见！** 💪

