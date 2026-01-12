# 🎊 Day 3 完整总结 - SQL执行器深度解析

## 今日成就 🏆

恭喜你完成了Day 3的学习！今天的内容非常重要，是MyBatis的核心执行机制！

---

## ✅ 今天学习的内容

### Part 1: Executor执行器 ⭐⭐⭐⭐⭐

**核心概念**：
```
Executor = SQL执行的协调者

三种类型：
1. SimpleExecutor（默认）
   - 每次创建新Statement
   - 简单直接
   - 适合99%场景

2. ReuseExecutor
   - 复用Statement
   - 提高性能
   - 适合重复SQL

3. BatchExecutor
   - 批量执行
   - 最高性能
   - 适合批量操作
```

**职责**：
- ✅ 获取MappedStatement
- ✅ 创建StatementHandler
- ✅ 协调SQL执行流程
- ✅ 不直接操作JDBC

---

### Part 2: StatementHandler ⭐⭐⭐⭐⭐

**核心概念**：
```
StatementHandler = JDBC的封装者

核心方法：
1. prepare()
   - 将#{param}替换为?
   - 创建PreparedStatement

2. parameterize()
   - 设置参数
   - ps.setObject()

3. query()/update()
   - 执行SQL
   - 处理结果
```

**关键理解**：
- ✅ 封装JDBC操作
- ✅ 使用PreparedStatement
- ✅ 预编译提升性能
- ✅ ?占位符防注入

---

### Part 3: #{} vs ${} ⭐⭐⭐⭐⭐ (面试高频)

**核心对比**：
```
#{}                     ${}
────────────────────────────────────
预编译                   字符串替换
PreparedStatement       Statement
?占位符                  直接拼接
防SQL注入 ✅             有注入风险 ⚠️
用于值                   用于表名列名
99%使用                 1%使用
```

**SQL注入**：
```
恶意输入改变SQL逻辑，获取敏感数据或破坏数据

防护方法：
✅ 优先使用#{}
✅ ${}必须验证输入
✅ 使用枚举限制值
✅ 永不直接使用用户输入
```

---

### Part 4: 实践案例 ⭐⭐⭐⭐

**安全实践**：
- ✅ 排序字段用枚举
- ✅ LIKE用CONCAT
- ✅ Service层验证
- ✅ 提供默认值

---

## 📖 核心流程回顾

### 完整的SQL执行流程

```
1. 用户调用
   mapper.selectById(1L)
      ↓
2. MapperProxy拦截
   构建statementId
      ↓
3. SqlSession.selectOne()
      ↓
4. Executor.query()
   ├─> 获取MappedStatement
   ├─> 创建StatementHandler
   └─> 协调执行
      ↓
5. StatementHandler.prepare()
   ├─> 将#{id}替换为?
   └─> connection.prepareStatement()
      ↓
6. StatementHandler.parameterize()
   └─> ps.setObject(1, 1L)
      ↓
7. StatementHandler.query()
   ├─> ps.executeQuery()
   ├─> 获取ResultSet
   └─> ResultSetHandler处理
      ↓
8. 返回结果
   List<User>
```

---

## 🎯 关键知识点

### 1. 为什么用PreparedStatement？

```
优势：
✅ 预编译，性能好
✅ 防SQL注入
✅ 类型安全
✅ 可以复用

劣势：
❌ 不能用于表名列名
❌ 占位符数量固定
```

### 2. #{} 的工作原理

```
步骤1: MyBatis解析
   SELECT * FROM user WHERE id = #{id}

步骤2: 替换为?
   SELECT * FROM user WHERE id = ?

步骤3: 预编译
   PreparedStatement ps = conn.prepareStatement(sql);

步骤4: 绑定参数
   ps.setLong(1, 1L);

步骤5: 执行
   ResultSet rs = ps.executeQuery();
```

### 3. ${} 的危险性

```
问题：直接字符串替换
   SELECT * FROM user WHERE id = ${id}
   如果 id = "1 OR 1=1"
   变成：SELECT * FROM user WHERE id = 1 OR 1=1
   返回所有数据！

解决：必须验证输入
   - 使用枚举
   - 白名单验证
   - 不直接使用用户输入
```

---

## 📊 对比表格

### Executor类型对比

| 特性 | Simple | Reuse | Batch |
|-----|--------|-------|-------|
| Statement管理 | 新建 | 复用 | 批量 |
| 性能 | 中 | 好 | 最好 |
| 适用场景 | 通用 | 重复SQL | 批量操作 |
| 默认 | ✅ | ❌ | ❌ |

### #{} vs ${} 对比

| 特性 | #{} | ${} |
|-----|-----|-----|
| 处理方式 | 预编译 | 字符串替换 |
| SQL注入 | 安全 ✅ | 危险 ⚠️ |
| 使用频率 | 99% | 1% |
| 适用场景 | 值 | 表名列名 |

---

## 🤔 面试题速查

### Q1: MyBatis的Executor有哪几种？

**答案**：
- SimpleExecutor（默认）：每次新建Statement
- ReuseExecutor：复用Statement，提高性能
- BatchExecutor：批量执行，最高性能

### Q2: #{} 和 ${} 的区别？

**答案**：
- #{}是预编译（PreparedStatement），${}是字符串替换
- #{}防SQL注入，${}有注入风险
- #{}用于参数值，${}用于表名列名
- 优先使用#{}，${}必须验证输入

### Q3: 如何防止SQL注入？

**答案**：
- 使用#{}而不是${}
- ${}时必须验证输入（枚举、白名单）
- LIKE查询用CONCAT函数
- 永不直接使用用户输入

### Q4: PreparedStatement的优势是什么？

**答案**：
- 预编译，提高性能
- 防止SQL注入
- 类型安全
- 可以复用

### Q5: 什么时候必须使用${}？

**答案**：
- 动态表名
- 动态列名
- ORDER BY字段
- 但必须验证输入！

---

## 💻 代码示例速查

### 安全的参数使用

```xml
<!-- ✅ 正确：使用#{} -->
<select id="selectById">
    SELECT * FROM user WHERE id = #{id}
</select>

<!-- ✅ 正确：LIKE查询 -->
<select id="search">
    SELECT * FROM user 
    WHERE name LIKE CONCAT('%', #{keyword}, '%')
</select>

<!-- ✅ 正确：价格区间 -->
<select id="selectByPriceRange">
    SELECT * FROM product 
    WHERE price BETWEEN #{minPrice} AND #{maxPrice}
</select>
```

### 安全的ORDER BY

```java
// 1. 定义枚举
public enum OrderColumn {
    ID("id"),
    NAME("name"),
    AGE("age");
    // ...
}

// 2. Service层验证
public List<User> getUsers(String orderBy) {
    OrderColumn column;
    try {
        column = OrderColumn.valueOf(orderBy.toUpperCase());
    } catch (Exception e) {
        column = OrderColumn.ID; // 默认值
    }
    return mapper.selectOrderBy(column.getColumn());
}
```

```xml
<!-- 3. XML中使用 -->
<select id="selectOrderBy">
    SELECT * FROM user ORDER BY ${orderColumn}
</select>
```

---

## 🔧 调试技巧

### 关键断点位置

```
1. MapperProxy.invoke() - 第80行
   观察：方法拦截

2. Executor.query() - 第48行
   观察：获取MappedStatement

3. StatementHandler.prepare() - 第72行
   观察：#{} 替换为 ?

4. StatementHandler.parameterize() - 第94行
   观察：参数设置

5. StatementHandler.query() - 第120行
   观察：SQL执行
```

### 观察要点

- ✅ SQL的转换过程
- ✅ 参数的绑定方式
- ✅ PreparedStatement的创建
- ✅ ResultSet的处理

---

## 📚 今日创建的文档

1. **学习笔记-Day3-Part1-Executor详解.md**
   - Executor的三种类型
   - 工作原理和职责

2. **学习笔记-Day3-Part2-StatementHandler.md**
   - JDBC封装
   - PreparedStatement详解
   - 核心方法分析

3. **学习笔记-Day3-Part3-参数处理与SQL注入.md**
   - #{} vs ${}
   - SQL注入原理
   - 防护最佳实践

4. **学习笔记-Day3-Part4-实践与总结.md**
   - 实践案例
   - 调试指导
   - 完整总结

---

## 🎯 掌握程度自测

### 基础知识（必须掌握）
- [x] 能说出Executor的三种类型
- [x] 能解释StatementHandler的作用
- [x] 能说出#{} 和 ${} 的区别
- [x] 理解PreparedStatement的优势

### 进阶理解（深入掌握）
- [ ] 能画出SQL执行的完整流程
- [ ] 能解释SQL注入的原理
- [ ] 能举例说明安全实践
- [ ] 能调试完整的执行过程

### 实战能力（融会贯通）
- [ ] 能实现安全的排序查询
- [ ] 能实现安全的搜索功能
- [ ] 能识别不安全的代码
- [ ] 能提出优化建议

---

## 🌟 学习收获

今天你学到了：

1. **Executor的设计**
   - 三种类型各有特点
   - SimpleExecutor是默认选择
   - 协调者模式的应用

2. **JDBC的封装**
   - StatementHandler简化JDBC
   - PreparedStatement的优势
   - 预编译提升性能

3. **安全编程**
   - #{} 防SQL注入
   - ${} 的危险性
   - 输入验证的重要性

4. **最佳实践**
   - 优先使用#{}
   - ${}必须验证
   - 使用枚举限制值

---

## 📅 明天预告：Day 4

明天我们将学习：

### 1. ResultSetHandler深入
- 结果集映射原理
- 自动映射机制
- 复杂对象映射

### 2. 高级特性
- 关联查询
- 懒加载
- 结果缓存

### 3. 性能优化
- 批量操作
- N+1问题
- 最佳实践

---

## 💪 今晚作业

### 必做：
1. ✅ 复习今天的4份笔记
2. ✅ 完整调试一次SQL执行
3. ✅ 理解#{} 和 ${} 的本质区别

### 选做：
1. 实现带分页的查询
2. 实现多条件组合查询
3. 研究MyBatis源码的ParameterHandler

---

## 🎊 激励时刻

今天的内容很充实，你坚持下来了！

你现在已经理解了：
- ✅ SQL是如何被执行的
- ✅ 参数是如何被处理的
- ✅ 如何编写安全的SQL
- ✅ MyBatis的核心设计

**这些都是高级工程师必备的知识！** 🔥

---

**Day 3圆满完成！好好休息，明天见！** 😴

**记住**：安全永远是第一位的！💡

