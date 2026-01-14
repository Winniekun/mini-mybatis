# Day 5 学习笔记 - Part 3: 面试准备与学习建议

## 🎯 第三课：面试高频题精讲

### 面试准备策略

```
初级工程师（1-3年）：
  重点：基础原理
  - MyBatis的执行流程
  - #{} 和 ${} 的区别
  - 一级缓存和二级缓存
  - 能说清楚基本原理即可

中级工程师（3-5年）：
  重点：深入理解
  - 四大核心对象
  - 动态代理原理
  - 插件机制
  - 源码级别的理解

高级工程师（5年+）：
  重点：设计思想
  - 为什么这样设计？
  - 如何优化性能？
  - 如何扩展功能？
  - 架构级别的思考
```

---

## 📚 面试题库（按频率排序）

### ⭐⭐⭐⭐⭐ 必问题（100%会问）

#### Q1: #{} 和 ${} 的区别是什么？

**标准答案**：
```
1. 本质区别：
   #{}是预编译，${}是字符串替换

2. 安全性：
   #{}防SQL注入，${}有注入风险

3. 使用场景：
   #{}用于参数值，${}用于表名列名

4. 底层实现：
   #{}使用PreparedStatement（?占位符）
   ${}直接拼接SQL字符串

5. 性能：
   #{}可以预编译缓存，性能更好
   ${}每次都要编译，性能较差
```

**加分回答**：
```java
// #{}的处理流程
原始SQL: SELECT * FROM user WHERE id = #{id}
    ↓
替换为: SELECT * FROM user WHERE id = ?
    ↓
PreparedStatement ps = conn.prepareStatement(sql)
    ↓
ps.setLong(1, id)  // 参数会被转义
    ↓
ps.executeQuery()

// ${}的处理流程
原始SQL: SELECT * FROM ${tableName}
    ↓
直接替换: SELECT * FROM user
    ↓
Statement stmt = conn.createStatement()
    ↓
stmt.executeQuery(sql)  // SQL已经是最终形式
```

**防注入示例**：
```java
// 错误示例（SQL注入）
String username = "admin' OR '1'='1";
// SQL: SELECT * FROM user WHERE username = '${username}'
// 变成: SELECT * FROM user WHERE username = 'admin' OR '1'='1'
// 返回所有用户！

// 正确示例（安全）
String username = "admin' OR '1'='1";
// SQL: SELECT * FROM user WHERE username = #{username}
// PreparedStatement会转义: 'admin\' OR \'1\'=\'1\''
// 单引号被转义，作为普通字符串处理
```

---

#### Q2: MyBatis的执行流程是什么？

**标准答案**：
```
1. 加载配置
   - 读取mybatis-config.xml
   - 解析Mapper XML
   - 构建Configuration对象

2. 创建SqlSession
   - SqlSessionFactory.openSession()
   - 获取数据库连接
   - 创建Executor

3. 获取Mapper代理
   - session.getMapper(UserMapper.class)
   - MapperProxy动态代理

4. 调用方法
   - mapper.selectById(1L)
   - MapperProxy拦截

5. 构建statementId
   - namespace + methodName
   - 例如: com.example.UserMapper.selectById

6. 获取MappedStatement
   - 包含SQL、参数类型、返回类型

7. Executor执行
   - 创建StatementHandler
   - 准备Statement
   - 设置参数
   - 执行SQL

8. StatementHandler处理
   - prepare(): #{} → ?
   - parameterize(): 设置参数
   - query(): 执行SQL

9. ResultSetHandler映射
   - 遍历ResultSet
   - 创建对象
   - 映射字段
   - 类型转换

10. 返回结果
```

**流程图回答**：
```
用户调用
  ↓
MapperProxy拦截
  ↓
SqlSession
  ↓
Executor
  ↓
StatementHandler
  ├─> prepare()
  ├─> parameterize()
  └─> query()
  ↓
ResultSetHandler
  ↓
返回结果
```

---

#### Q3: MyBatis的一级缓存和二级缓存的区别？

**标准答案**：
```
一级缓存（Session级别）：
1. 作用域：SqlSession
2. 默认：开启
3. 生命周期：SqlSession的生命周期
4. 何时清空：
   - close()
   - update/insert/delete
   - commit()
   - clearCache()
5. 线程安全：是（每个线程独立SqlSession）

二级缓存（Mapper级别）：
1. 作用域：Mapper（namespace）
2. 默认：关闭
3. 生命周期：应用程序的生命周期
4. 何时清空：
   - 执行update/insert/delete
   - 配置的flushInterval到期
5. 线程安全：是（需要实体类实现Serializable）
6. 跨SqlSession共享
```

**代码示例**：
```java
// 一级缓存示例
SqlSession session = factory.openSession();
UserMapper mapper = session.getMapper(UserMapper.class);

User user1 = mapper.selectById(1L);  // 查数据库
User user2 = mapper.selectById(1L);  // 从缓存取

System.out.println(user1 == user2);  // true（同一个对象）

session.close();

// 二级缓存示例
// Session1
SqlSession session1 = factory.openSession();
UserMapper mapper1 = session1.getMapper(UserMapper.class);
User user1 = mapper1.selectById(1L);  // 查数据库
session1.close();  // 数据进入二级缓存

// Session2
SqlSession session2 = factory.openSession();
UserMapper mapper2 = session2.getMapper(UserMapper.class);
User user2 = mapper2.selectById(1L);  // 从二级缓存取

System.out.println(user1 == user2);  // false（不是同一个对象，但数据相同）
```

**何时使用**：
```
使用一级缓存（默认）：
✅ 所有场景

使用二级缓存（谨慎）：
✅ 读多写少
✅ 数据一致性要求不高
✅ 单表查询

不使用二级缓存：
❌ 读写频繁
❌ 数据一致性要求高
❌ 多表关联查询
❌ 分布式环境（建议用Redis）
```

---

### ⭐⭐⭐⭐ 高频题（80%会问）

#### Q4: MyBatis的动态代理是如何实现的？

**标准答案**：
```
1. JDK动态代理
   - 基于接口
   - InvocationHandler

2. MapperProxyFactory
   - 为每个Mapper接口创建代理工厂
   - 工厂方法创建MapperProxy

3. MapperProxy
   - 实现InvocationHandler
   - invoke方法拦截所有方法调用
   - 构建statementId
   - 委托给SqlSession

4. MapperRegistry
   - 注册所有Mapper接口
   - 维护接口→工厂的映射
```

**代码示例**：
```java
// 1. 注册Mapper
configuration.addMapper(UserMapper.class);
// 内部：mapperRegistry.addMapper(UserMapper.class)
// 内部：knownMappers.put(UserMapper.class, new MapperProxyFactory(UserMapper.class))

// 2. 获取Mapper
UserMapper mapper = session.getMapper(UserMapper.class);
// 内部：mapperRegistry.getMapper(UserMapper.class, session)
// 内部：factory.newInstance(session)
// 内部：Proxy.newProxyInstance(..., new MapperProxy(session, mapperInterface))

// 3. 调用方法
User user = mapper.selectById(1L);
// 被MapperProxy.invoke()拦截
// statementId = "com.example.UserMapper.selectById"
// sqlSession.selectOne(statementId, 1L)
```

**为什么用动态代理**：
```
优势：
✅ 用户只需定义接口
✅ 不需要写实现类
✅ 自动生成实现
✅ 灵活可扩展

劣势：
❌ 反射调用，性能略低（可忽略）
❌ 调试稍困难
```

---

#### Q5: MyBatis如何防止SQL注入？

**标准答案**：
```
1. 使用#{}而不是${}
   - #{}使用PreparedStatement
   - 参数通过?占位符绑定
   - 参数会被自动转义

2. PreparedStatement的防注入原理
   - SQL结构在编译时确定
   - 参数不参与SQL编译
   - 参数只是数据，不是代码

3. ${}的安全使用
   - 只用于表名、列名
   - 必须验证输入（白名单）
   - 使用枚举限制值
```

**技术细节**：
```sql
-- 使用#{}
SELECT * FROM user WHERE username = #{username}
-- 编译后：
SELECT * FROM user WHERE username = ?
-- 参数绑定：
ps.setString(1, username)

-- 参数值：admin' OR '1'='1
-- 实际效果：
SELECT * FROM user WHERE username = 'admin\' OR \'1\'=\'1\''
-- 单引号被转义，作为普通字符
```

---

#### Q6: MyBatis的四大核心对象是什么？

**标准答案**：
```
1. Executor（执行器）
   - 作用：协调SQL执行
   - 类型：Simple、Reuse、Batch
   - 生命周期：SqlSession级别

2. StatementHandler（语句处理器）
   - 作用：封装JDBC操作
   - 方法：prepare、parameterize、query/update
   - 职责：创建Statement、设置参数、执行SQL

3. ParameterHandler（参数处理器）
   - 作用：设置SQL参数
   - 职责：将Java对象转为JDBC参数

4. ResultSetHandler（结果集处理器）
   - 作用：映射结果集
   - 职责：ResultSet → Java对象
   - 能力：自动映射、类型转换、驼峰转换
```

**它们的关系**：
```
SqlSession
    ↓ 创建
Executor
    ↓ 创建和使用
StatementHandler + ParameterHandler + ResultSetHandler
    ↓
JDBC操作
```

---

### ⭐⭐⭐ 进阶题（50%会问）

#### Q7: MyBatis的插件机制是如何实现的？

**标准答案**：
```
1. 可拦截的对象
   - Executor
   - StatementHandler
   - ParameterHandler
   - ResultSetHandler

2. 实现方式
   - 实现Interceptor接口
   - @Intercepts注解指定拦截点
   - intercept()方法处理逻辑

3. 工作原理
   - 责任链模式
   - 层层包装
   - 类似Spring AOP

4. 典型应用
   - 分页插件（PageHelper）
   - SQL日志插件
   - 性能监控插件
```

**代码示例**：
```java
@Intercepts({
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
    )
})
public class PagePlugin implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 前置处理
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        
        // 获取原始SQL
        BoundSql boundSql = ms.getBoundSql(args[1]);
        String sql = boundSql.getSql();
        
        // 改写SQL（添加LIMIT）
        String pageSql = sql + " LIMIT ?, ?";
        
        // 执行原方法
        Object result = invocation.proceed();
        
        // 后置处理
        return result;
    }
}
```

---

#### Q8: MyBatis的延迟加载是如何实现的？

**标准答案**：
```
1. 配置
   <setting name="lazyLoadingEnabled" value="true"/>

2. 使用场景
   - 一对一关联
   - 一对多关联
   - 减少不必要的查询

3. 实现原理
   - CGLIB或Javassist生成代理
   - 代理拦截getter方法
   - 首次访问时才执行SQL

4. 触发加载
   - 访问懒加载属性
   - 调用相关方法
```

**示例**：
```xml
<resultMap id="userWithOrders" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>
    <collection 
        property="orders" 
        select="selectOrdersByUserId"
        column="id"
        fetchType="lazy"/>  <!-- 懒加载 -->
</resultMap>
```

```java
User user = mapper.selectById(1L);
// 只执行：SELECT id, username FROM user WHERE id = 1

System.out.println(user.getUsername());
// 不触发orders查询

System.out.println(user.getOrders().size());
// 此时才执行：SELECT * FROM orders WHERE user_id = 1
```

---

#### Q9: MyBatis如何处理N+1查询问题？

**什么是N+1问题**：
```
查询N个对象，每个对象又查询关联对象
总共执行：1 + N 次查询

例如：
SELECT * FROM user           -- 查询10个用户（1次）
SELECT * FROM order WHERE user_id = 1   -- 第1个用户的订单
SELECT * FROM order WHERE user_id = 2   -- 第2个用户的订单
...
SELECT * FROM order WHERE user_id = 10  -- 第10个用户的订单
总共11次查询！
```

**解决方案**：

方案1：使用JOIN查询
```xml
<select id="selectUsersWithOrders" resultMap="userWithOrders">
    SELECT 
        u.id AS user_id,
        u.username,
        o.id AS order_id,
        o.amount
    FROM user u
    LEFT JOIN `order` o ON u.id = o.user_id
</select>
<!-- 只需1次查询 -->
```

方案2：使用批量查询
```java
// 1. 查询所有用户
List<User> users = userMapper.selectAll();

// 2. 收集所有用户ID
List<Long> userIds = users.stream()
    .map(User::getId)
    .collect(Collectors.toList());

// 3. 批量查询所有订单
List<Order> orders = orderMapper.selectByUserIds(userIds);
// SELECT * FROM order WHERE user_id IN (1,2,3,...,10)

// 4. 手动组装数据
// ...
```

方案3：关闭懒加载
```xml
<setting name="lazyLoadingEnabled" value="false"/>
<!-- 一次性加载所有数据 -->
```

---

### ⭐⭐ 深入题（20%会问）

#### Q10: MyBatis和Hibernate的区别？

**标准答案**：
```
MyBatis（半自动ORM）：
✅ SQL可控，灵活
✅ 学习成本低
✅ 适合复杂查询
✅ 性能优化容易
❌ 需要写SQL
❌ 切换数据库麻烦

Hibernate（全自动ORM）：
✅ 完全面向对象
✅ 不用写SQL
✅ 数据库无关
✅ 对象缓存强大
❌ 学习成本高
❌ 复杂查询困难
❌ 性能优化困难

选择建议：
- 互联网项目：MyBatis（灵活、可控）
- 企业项目：Hibernate（规范、标准）
- 简单CRUD：Hibernate
- 复杂查询：MyBatis
```

---

## 🎯 项目经验怎么说？

### 模板1：基础使用

```
"在XX项目中使用MyBatis作为持久层框架：

1. 项目背景
   - XX系统的后端开发
   - 使用Spring Boot + MyBatis
   - MySQL数据库

2. 我的工作
   - 设计数据库表结构
   - 编写Mapper接口和XML
   - 实现CRUD功能
   - 编写复杂查询SQL

3. 遇到的问题
   - N+1查询问题
   - 使用JOIN优化
   - 性能提升XX%

4. 技术亮点
   - 使用动态SQL优化查询
   - 合理使用缓存
   - 批量操作提升性能
"
```

### 模板2：深入理解

```
"深入学习了MyBatis的源码和原理：

1. 学习目标
   - 理解ORM框架的设计思想
   - 掌握动态代理的应用
   - 学习责任链模式

2. 学习过程
   - 从零实现了简化版MyBatis
   - 包括动态代理、配置解析、SQL执行、结果映射
   - 理解了四大核心对象的作用

3. 收获
   - 深入理解了MyBatis的执行流程
   - 能够解决实际开发中的问题
   - 对设计模式有了更深的理解

4. 应用
   - 能够编写自定义插件
   - 能够排查性能问题
   - 能够指导团队使用
"
```

---

## 📚 学习建议

### 1. 初学者（0-1年）

**学习路径**：
```
Week 1-2: 基础使用
  - 搭建环境
  - 简单CRUD
  - 基本配置

Week 3-4: 进阶使用
  - 动态SQL
  - 结果映射
  - 关联查询

Week 5-6: 实战项目
  - 完整项目
  - 最佳实践
  - 问题排查
```

**推荐资源**：
- 官方文档（中文版）
- 《MyBatis从入门到精通》
- 视频教程（B站、慕课网）

---

### 2. 中级工程师（1-3年）

**学习路径**：
```
Month 1: 深入原理
  - 四大核心对象
  - 执行流程
  - 设计模式

Month 2: 源码阅读
  - SqlSessionFactory构建
  - Executor执行
  - ResultSetHandler映射

Month 3: 实践应用
  - 自定义插件
  - 性能优化
  - 二次开发
```

**推荐资源**：
- MyBatis源码（GitHub）
- 《MyBatis技术内幕》
- 技术博客（美团、阿里）

---

### 3. 高级工程师（3年+）

**学习路径**：
```
研究方向：
1. 框架设计
   - 为什么这样设计？
   - 有什么优缺点？
   - 如何改进？

2. 性能优化
   - 慢SQL排查
   - 缓存策略
   - 批量操作

3. 扩展开发
   - 自定义TypeHandler
   - 自定义插件
   - 二次开发
```

**推荐资源**：
- MyBatis源码深度剖析
- 设计模式实战
- 数据库性能优化

---

## 🚀 下一步怎么走？

### 方向1：实战为主

```
找一个实际项目练手：
1. 电商系统
2. 博客系统
3. 权限系统

重点：
- 完整的CRUD
- 复杂查询
- 性能优化
- 问题排查
```

### 方向2：原理为主

```
深入研究MyBatis原理：
1. 阅读源码
2. 写技术博客
3. 分享给他人

重点：
- 设计思想
- 实现细节
- 优缺点分析
```

### 方向3：扩展为主

```
基于MyBatis做扩展：
1. 自定义插件
2. 通用Mapper
3. 代码生成器

重点：
- 解决实际问题
- 提高开发效率
- 开源贡献
```

---

## 🎊 总结

### 你现在掌握了什么？

```
✅ MyBatis的完整执行流程
✅ 四大核心对象的作用
✅ 动态代理的实现原理
✅ SQL执行和结果映射
✅ 面试高频题的回答
✅ 实际问题的解决能力
```

### 你能回答什么问题？

```
✅ MyBatis是如何工作的？
✅ #{} 和 ${} 有什么区别？
✅ 如何防止SQL注入？
✅ 缓存是如何实现的？
✅ 如何优化MyBatis性能？
✅ 如何排查MyBatis问题？
```

### 你具备什么能力？

```
✅ 独立使用MyBatis开发
✅ 解决实际开发问题
✅ 阅读MyBatis源码
✅ 指导团队使用
✅ 通过MyBatis面试
✅ 设计类似的框架
```

---

**第三课完成！你已经完成了所有学习！** 🎉

**恭喜你！5天的学习圆满结束！** 🎊

