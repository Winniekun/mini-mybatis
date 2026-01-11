# Day 2 学习笔记 - Part 3: 实践练习

## 🎯 第三课：动手实践 - 创建自己的Mapper

现在让我们把学到的知识付诸实践！我们要创建一个**ProductMapper**来管理商品信息。

---

## 📝 任务目标

创建一个完整的Mapper，包括：
1. ✅ 数据库表
2. ✅ 实体类 (Product.java)
3. ✅ Mapper接口 (ProductMapper.java)
4. ✅ Mapper配置文件 (ProductMapper.xml)
5. ✅ 测试代码

---

## Step 1: 创建数据库表

```sql
-- 1. 创建商品表
CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category VARCHAR(50) COMMENT '商品分类',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存',
    description TEXT COMMENT '商品描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 2. 插入测试数据
INSERT INTO product (product_name, category, price, stock, description) VALUES
('iPhone 15 Pro', '手机', 7999.00, 50, '苹果最新款手机'),
('MacBook Pro', '电脑', 12999.00, 30, '苹果笔记本电脑'),
('AirPods Pro', '耳机', 1999.00, 100, '苹果无线耳机');

-- 3. 查询验证
SELECT * FROM product;
```

---

## Step 2: 创建实体类

**位置**: `src/test/java/com/mybatis/test/entity/Product.java`

**要点**：
- 字段名使用驼峰命名（Java规范）
- 数据库字段名用下划线（数据库规范）
- MyBatis会自动转换

**字段对应关系**：
```
数据库字段          Java字段
────────────────────────────────
id                  id
product_name        productName  ← 自动转换
category            category
price               price
stock               stock
description         description
create_time         createTime   ← 自动转换
```

---

## Step 3: 创建Mapper接口

**位置**: `src/test/java/com/mybatis/test/mapper/ProductMapper.java`

```java
package com.mybatis.test.mapper;

import com.mybatis.test.entity.Product;
import java.util.List;

/**
 * 商品Mapper接口
 * 
 * 注意：
 * 1. 这是接口，不需要实现类
 * 2. 方法名要和XML中的id对应
 * 3. 参数类型要和parameterType对应
 * 4. 返回类型要和resultType对应
 */
public interface ProductMapper {
    
    /**
     * 根据ID查询商品
     */
    Product selectById(Long id);
    
    /**
     * 查询所有商品
     */
    List<Product> selectAll();
    
    /**
     * 根据分类查询
     */
    List<Product> selectByCategory(String category);
    
    /**
     * 插入商品
     */
    int insert(Product product);
    
    /**
     * 更新商品
     */
    int update(Product product);
    
    /**
     * 删除商品
     */
    int deleteById(Long id);
}
```

**设计要点**：
- 方法名清晰表达意图
- 参数简单（单个参数或实体对象）
- 返回类型明确（单个对象、列表、影响行数）

---

## Step 4: 创建Mapper配置文件

**位置**: `src/main/resources/mapper/ProductMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper>
<mapper namespace="com.mybatis.test.mapper.ProductMapper">
    
    <!-- 
        重要！namespace必须是接口的完整类名
        这样才能关联接口和XML
    -->
    
    <!-- 1. 根据ID查询 -->
    <select id="selectById" 
            parameterType="java.lang.Long" 
            resultType="com.mybatis.test.entity.Product">
        SELECT 
            id, 
            product_name,
            category,
            price,
            stock,
            description,
            create_time
        FROM product
        WHERE id = #{id}
    </select>
    
    <!-- 2. 查询所有商品 -->
    <select id="selectAll" 
            resultType="com.mybatis.test.entity.Product">
        SELECT 
            id, 
            product_name,
            category,
            price,
            stock,
            description,
            create_time
        FROM product
        ORDER BY id DESC
    </select>
    
    <!-- 3. 根据分类查询 -->
    <select id="selectByCategory" 
            parameterType="java.lang.String" 
            resultType="com.mybatis.test.entity.Product">
        SELECT 
            id, 
            product_name,
            category,
            price,
            stock,
            description,
            create_time
        FROM product
        WHERE category = #{category}
    </select>
    
    <!-- 4. 插入商品 -->
    <insert id="insert" 
            parameterType="com.mybatis.test.entity.Product">
        INSERT INTO product (
            product_name, 
            category, 
            price, 
            stock, 
            description, 
            create_time
        ) VALUES (
            #{productName}, 
            #{category}, 
            #{price}, 
            #{stock}, 
            #{description}, 
            #{createTime}
        )
    </insert>
    
    <!-- 5. 更新商品 -->
    <update id="update" 
            parameterType="com.mybatis.test.entity.Product">
        UPDATE product SET
            product_name = #{productName},
            category = #{category},
            price = #{price},
            stock = #{stock},
            description = #{description}
        WHERE id = #{id}
    </update>
    
    <!-- 6. 删除商品 -->
    <delete id="deleteById" 
            parameterType="java.lang.Long">
        DELETE FROM product WHERE id = #{id}
    </delete>
    
</mapper>
```

**配置要点**：

1. **namespace**：必须是接口全限定名
2. **id**：必须和接口方法名一致
3. **parameterType**：参数类型（可选，MyBatis能自动推断）
4. **resultType**：返回类型（必须）
5. **#{参数名}**：参数占位符

---

## Step 5: 注册Mapper

修改 `mybatis-config.xml`，添加新的Mapper：

```xml
<mappers>
    <mapper resource="mapper/UserMapper.xml"/>
    <!-- 添加这一行 -->
    <mapper resource="mapper/ProductMapper.xml"/>
</mappers>
```

---

## Step 6: 测试代码

在 `MybatisTest.java` 中添加测试方法：

```java
/**
 * 测试ProductMapper
 */
@Test
public void testProductMapper() {
    System.out.println("\n========== 测试：ProductMapper ==========");
    
    SqlSession session = sqlSessionFactory.openSession();
    
    try {
        // 获取Mapper
        ProductMapper mapper = session.getMapper(ProductMapper.class);
        
        // 1. 查询所有商品
        System.out.println("\n--- 查询所有商品 ---");
        List<Product> products = mapper.selectAll();
        for (Product product : products) {
            System.out.println(product);
        }
        
        // 2. 根据ID查询
        System.out.println("\n--- 根据ID查询 ---");
        Product product = mapper.selectById(1L);
        System.out.println(product);
        
        // 3. 根据分类查询
        System.out.println("\n--- 根据分类查询 ---");
        List<Product> phones = mapper.selectByCategory("手机");
        for (Product p : phones) {
            System.out.println(p);
        }
        
    } finally {
        session.close();
    }
}
```

---

## 🎯 执行流程分析

当你调用 `mapper.selectById(1L)` 时，发生了什么？

```
1. JDK动态代理拦截
   MapperProxy.invoke()
   
2. 构建statementId
   "com.mybatis.test.mapper.ProductMapper.selectById"
   
3. 从Configuration获取MappedStatement
   configuration.getMappedStatement(statementId)
   
4. 获取SQL
   ms.getSql() = "SELECT * FROM product WHERE id = #{id}"
   
5. 替换占位符
   "SELECT * FROM product WHERE id = ?"
   
6. 设置参数
   ps.setLong(1, 1L)
   
7. 执行查询
   ResultSet rs = ps.executeQuery()
   
8. 映射结果
   Product product = new Product();
   product.setId(rs.getLong("id"));
   product.setProductName(rs.getString("product_name"));
   // ... 其他字段
   
9. 返回结果
   return product;
```

---

## 💡 常见问题和解决方案

### 问题1：找不到Mapper

**错误信息**:
```
MyBatisException: Mapper未注册: ProductMapper
```

**原因**：
- namespace写错了
- ProductMapper.xml没有在mybatis-config.xml中配置
- 类路径不对

**解决**：
1. 检查namespace是否是完整类名
2. 检查是否在`<mappers>`中配置了
3. 检查文件路径是否正确

### 问题2：找不到SQL语句

**错误信息**:
```
未找到SQL语句: com.mybatis.test.mapper.ProductMapper.selectById
```

**原因**：
- XML中的id和接口方法名不一致
- namespace不匹配

**解决**：
确保：namespace.id = 完整的statementId

### 问题3：字段映射失败

**现象**：某些字段值为null

**原因**：
- 数据库字段名和Java字段名不匹配
- 例如：product_name vs productName

**解决方案**：

方案1：MyBatis自动转换（我们用的）
```
product_name → productName （自动）
create_time → createTime （自动）
```

方案2：SQL中使用别名
```sql
SELECT 
    product_name AS productName,
    create_time AS createTime
FROM product
```

方案3：使用resultMap（高级功能）
```xml
<resultMap id="productMap" type="Product">
    <result column="product_name" property="productName"/>
    <result column="create_time" property="createTime"/>
</resultMap>
```

---

## 🎓 学习要点

### 1. 三个文件必须对应

```
Product.java (实体)
    ↕ 字段对应
ProductMapper.java (接口)
    ↕ 方法对应
ProductMapper.xml (配置)
```

### 2. namespace的重要性

```
namespace作用：
1. 关联接口和XML
2. 作为statementId前缀
3. 避免SQL id冲突
```

### 3. 参数映射

```
简单类型：
#{id} → 直接取值

对象类型：
#{productName} → 调用 product.getProductName()
#{price} → 调用 product.getPrice()
```

---

## 📊 完成检查清单

- [ ] 数据库表创建成功
- [ ] Product实体类编写完成
- [ ] ProductMapper接口定义清晰
- [ ] ProductMapper.xml配置正确
- [ ] mybatis-config.xml中已注册
- [ ] 测试代码能够运行
- [ ] 所有CRUD操作都测试通过

---

## 🚀 进阶挑战

如果你完成了基本任务，可以尝试：

### 挑战1：添加复杂查询

```java
// 价格区间查询
List<Product> selectByPriceRange(Double minPrice, Double maxPrice);

// 模糊搜索
List<Product> searchByName(String keyword);

// 分页查询
List<Product> selectByPage(int offset, int limit);
```

### 挑战2：添加批量操作

```java
// 批量插入
int batchInsert(List<Product> products);

// 批量删除
int batchDelete(List<Long> ids);
```

### 挑战3：添加统计查询

```java
// 统计商品总数
int count();

// 按分类统计
Map<String, Integer> countByCategory();
```

---

**第三课完成！你已经会创建自己的Mapper了！** 🎉

---

## 📝 Day 2 作业

### 必做：
1. ✅ 完成Product相关的所有代码
2. ✅ 运行并验证所有功能
3. ✅ 理解三个文件的对应关系

### 选做：
1. 尝试添加一个进阶挑战的功能
2. 创建一个Order实体和Mapper
3. 思考：如何实现关联查询？

---

**今天到此结束！明天见！** 💪

