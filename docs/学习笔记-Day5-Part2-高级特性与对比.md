# Day 5 学习笔记 - Part 2: 高级特性与真实MyBatis对比

## 🚀 第二课：高级特性探索

我们已经实现了MyBatis的核心功能，但真实的MyBatis还有很多高级特性。让我们了解一下！

---

## 📚 真实MyBatis的高级特性

### 1. 插件机制 (Interceptor) ⭐

**是什么？**
```
插件可以拦截MyBatis的核心方法调用，
在方法执行前后添加自定义逻辑。
```

**可以拦截的对象**：
```java
// 1. Executor - 拦截SQL执行
@Intercepts({
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
    )
})
public class MyExecutorPlugin implements Interceptor {
    public Object intercept(Invocation invocation) {
        // 前置处理
        System.out.println("SQL执行前...");
        
        // 执行原方法
        Object result = invocation.proceed();
        
        // 后置处理
        System.out.println("SQL执行后...");
        
        return result;
    }
}

// 2. StatementHandler - 拦截Statement准备
@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})

// 3. ParameterHandler - 拦截参数处理
@Intercepts({
    @Signature(
        type = ParameterHandler.class,
        method = "setParameters",
        args = {PreparedStatement.class}
    )
})

// 4. ResultSetHandler - 拦截结果处理
@Intercepts({
    @Signature(
        type = ResultSetHandler.class,
        method = "handleResultSets",
        args = {Statement.class}
    )
})
```

**典型应用场景**：

#### 场景1：SQL日志插件
```java
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {...})
})
public class SqlLogPlugin implements Interceptor {
    public Object intercept(Invocation invocation) {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        
        // 记录SQL
        System.out.println("SQL: " + ms.getSqlSource());
        System.out.println("参数: " + parameter);
        
        long start = System.currentTimeMillis();
        Object result = invocation.proceed();
        long end = System.currentTimeMillis();
        
        System.out.println("执行时间: " + (end - start) + "ms");
        
        return result;
    }
}
```

#### 场景2：分页插件
```java
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {...})
})
public class PagePlugin implements Interceptor {
    public Object intercept(Invocation invocation) {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();
        
        // 改写SQL，添加LIMIT
        String pageSql = sql + " LIMIT " + offset + ", " + pageSize;
        
        // 修改MappedStatement
        // ...
        
        return invocation.proceed();
    }
}
```

#### 场景3：性能监控插件
```java
public class PerformancePlugin implements Interceptor {
    public Object intercept(Invocation invocation) {
        long start = System.currentTimeMillis();
        
        try {
            return invocation.proceed();
        } finally {
            long time = System.currentTimeMillis() - start;
            if (time > 1000) {
                // 慢SQL告警
                logger.warn("慢SQL: 执行时间{}ms", time);
            }
        }
    }
}
```

**我们的实现**：
```java
// src/main/java/com/mybatis/plugin/SQLLogInterceptor.java
public class SQLLogInterceptor implements Interceptor {
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Method method = invocation.getMethod();
        Object[] args = invocation.getArgs();
        
        logger.info("拦截方法: {}.{}", 
            target.getClass().getSimpleName(), 
            method.getName());
        
        return invocation.proceed();
    }
}
```

---

### 2. 缓存机制 (Cache) ⭐⭐

**MyBatis的两级缓存**：

#### 一级缓存（默认开启）
```
作用域：SqlSession级别
生命周期：SqlSession的生命周期

工作原理：
同一个SqlSession内：
  第1次查询 → 查数据库 → 放入缓存
  第2次查询 → 直接从缓存取

不同SqlSession：
  各自独立的缓存，互不影响
```

**代码示例**：
```java
SqlSession session = factory.openSession();
UserMapper mapper = session.getMapper(UserMapper.class);

// 第1次查询：查数据库
User user1 = mapper.selectById(1L);

// 第2次查询：从缓存取
User user2 = mapper.selectById(1L);

// user1 == user2 返回true（同一个对象）

session.close();
```

**何时清空一级缓存？**
```
1. SqlSession.close() - 关闭会话
2. SqlSession.clearCache() - 手动清除
3. 执行update/insert/delete - 自动清除
4. SqlSession.commit() - 提交事务
5. SqlSession.rollback() - 回滚事务
```

#### 二级缓存（需要配置）
```
作用域：Mapper（namespace）级别
生命周期：应用程序的生命周期

工作原理：
同一个Mapper下：
  SqlSession1查询 → 关闭 → 数据放入二级缓存
  SqlSession2查询 → 从二级缓存取

跨SqlSession共享数据
```

**如何开启**：
```xml
<!-- mybatis-config.xml -->
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>

<!-- UserMapper.xml -->
<mapper namespace="com.example.UserMapper">
    <!-- 开启二级缓存 -->
    <cache/>
    
    <select id="selectById" resultType="User" useCache="true">
        SELECT * FROM user WHERE id = #{id}
    </select>
</mapper>
```

**实体类需要序列化**：
```java
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

**缓存配置**：
```xml
<cache
    eviction="LRU"           <!-- 缓存回收策略 -->
    flushInterval="60000"    <!-- 刷新间隔(ms) -->
    size="512"               <!-- 缓存大小 -->
    readOnly="false"/>       <!-- 只读 -->
```

**缓存回收策略**：
```
LRU    - 最近最少使用（默认）
FIFO   - 先进先出
SOFT   - 软引用
WEAK   - 弱引用
```

---

### 3. 动态SQL ⭐⭐⭐

**是什么？**
```
根据条件动态生成SQL语句
```

#### <if> 标签
```xml
<select id="selectByCondition" resultType="User">
    SELECT * FROM user
    WHERE 1=1
    <if test="username != null">
        AND username = #{username}
    </if>
    <if test="age != null">
        AND age > #{age}
    </if>
</select>
```

#### <where> 标签
```xml
<select id="selectByCondition" resultType="User">
    SELECT * FROM user
    <where>
        <if test="username != null">
            AND username = #{username}
        </if>
        <if test="age != null">
            AND age > #{age}
        </if>
    </where>
</select>

<!-- <where>会自动：
     1. 添加WHERE关键字
     2. 去掉第一个AND或OR
-->
```

#### <foreach> 标签
```xml
<!-- 批量查询 -->
<select id="selectByIds" resultType="User">
    SELECT * FROM user
    WHERE id IN
    <foreach collection="ids" item="id" open="(" close=")" separator=",">
        #{id}
    </foreach>
</select>

<!-- 
输入: ids = [1, 2, 3]
生成: WHERE id IN (1, 2, 3)
-->

<!-- 批量插入 -->
<insert id="batchInsert">
    INSERT INTO user (username, email) VALUES
    <foreach collection="users" item="user" separator=",">
        (#{user.username}, #{user.email})
    </foreach>
</insert>

<!--
输入: users = [user1, user2, user3]
生成: VALUES ('name1', 'email1'), ('name2', 'email2'), (...)
-->
```

#### <choose><when><otherwise> 标签
```xml
<select id="selectByCondition" resultType="User">
    SELECT * FROM user
    WHERE 1=1
    <choose>
        <when test="username != null">
            AND username = #{username}
        </when>
        <when test="email != null">
            AND email = #{email}
        </when>
        <otherwise>
            AND status = 1
        </otherwise>
    </choose>
</select>

<!-- 类似Java的if-else if-else -->
```

#### <set> 标签
```xml
<update id="updateSelective">
    UPDATE user
    <set>
        <if test="username != null">
            username = #{username},
        </if>
        <if test="email != null">
            email = #{email},
        </if>
    </set>
    WHERE id = #{id}
</update>

<!-- <set>会自动：
     1. 添加SET关键字
     2. 去掉最后一个逗号
-->
```

---

### 4. ResultMap（高级映射）⭐⭐

**为什么需要ResultMap？**
```
自动映射无法满足：
1. 字段名完全不同
2. 一对一关联
3. 一对多关联
4. 复杂对象映射
```

#### 基本映射
```xml
<resultMap id="userMap" type="User">
    <id column="user_id" property="id"/>
    <result column="user_name" property="name"/>
    <result column="user_email" property="email"/>
    <result column="create_time" property="createTime"/>
</resultMap>

<select id="selectById" resultMap="userMap">
    SELECT user_id, user_name, user_email, create_time
    FROM t_user
    WHERE user_id = #{id}
</select>
```

#### 一对一映射 (<association>)
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

<select id="selectWithDetail" resultMap="userWithDetail">
    SELECT 
        u.id, u.username,
        d.address, d.phone
    FROM user u
    LEFT JOIN user_detail d ON u.id = d.user_id
    WHERE u.id = #{id}
</select>
```

#### 一对多映射 (<collection>)
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

<select id="selectWithOrders" resultMap="userWithOrders">
    SELECT 
        u.id AS user_id, u.username,
        o.id AS order_id, o.amount
    FROM user u
    LEFT JOIN `order` o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

---

### 5. 懒加载 (Lazy Loading)

**是什么？**
```
延迟加载关联对象，
只在访问时才查询数据库。
```

**配置**：
```xml
<!-- mybatis-config.xml -->
<settings>
    <setting name="lazyLoadingEnabled" value="true"/>
    <setting name="aggressiveLazyLoading" value="false"/>
</settings>
```

**示例**：
```xml
<resultMap id="userWithOrders" type="User">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    
    <!-- 懒加载：fetchType="lazy" -->
    <collection 
        property="orders" 
        ofType="Order"
        select="selectOrdersByUserId"
        column="id"
        fetchType="lazy"/>
</resultMap>

<select id="selectById" resultMap="userWithOrders">
    SELECT id, username FROM user WHERE id = #{id}
</select>

<select id="selectOrdersByUserId" resultType="Order">
    SELECT * FROM `order` WHERE user_id = #{userId}
</select>
```

**工作原理**：
```java
User user = mapper.selectById(1L);
// 此时只执行了: SELECT id, username FROM user

System.out.println(user.getUsername());
// 不触发orders查询

System.out.println(user.getOrders().size());
// 此时才执行: SELECT * FROM `order` WHERE user_id = 1
```

---

## 📊 我们的实现 vs 真实MyBatis

### 功能对比表

| 功能 | 我们的实现 | 真实MyBatis | 说明 |
|-----|-----------|------------|------|
| **基础功能** | | | |
| 动态代理 | ✅ 完整 | ✅ | MapperProxy |
| XML配置解析 | ✅ 简化 | ✅ 完整 | 只支持基本标签 |
| SQL执行 | ✅ 完整 | ✅ | SimpleExecutor |
| 参数处理 | ✅ 简化 | ✅ 完整 | 只支持简单类型 |
| 结果映射 | ✅ 自动映射 | ✅ 完整 | 不支持<resultMap> |
| 驼峰转换 | ✅ 完整 | ✅ | 完全实现 |
| 类型转换 | ✅ 基础 | ✅ 完整 | 常见类型 |
| **执行器** | | | |
| SimpleExecutor | ✅ 完整 | ✅ | 默认执行器 |
| ReuseExecutor | ❌ 未实现 | ✅ | 复用Statement |
| BatchExecutor | ❌ 未实现 | ✅ | 批量执行 |
| **高级特性** | | | |
| 动态SQL | ❌ 未实现 | ✅ | <if>、<foreach>等 |
| ResultMap | ❌ 未实现 | ✅ | 手动映射 |
| 一对一 | ❌ 未实现 | ✅ | <association> |
| 一对多 | ❌ 未实现 | ✅ | <collection> |
| 懒加载 | ❌ 未实现 | ✅ | 延迟加载 |
| 一级缓存 | ❌ 未实现 | ✅ | SqlSession级别 |
| 二级缓存 | ❌ 未实现 | ✅ | Mapper级别 |
| 插件机制 | ✅ 框架 | ✅ 完整 | 基本结构 |
| TypeHandler | ❌ 未实现 | ✅ | 自定义类型处理 |
| **其他** | | | |
| 注解支持 | ❌ 未实现 | ✅ | @Select等 |
| 存储过程 | ❌ 未实现 | ✅ | CallableStatement |
| 主键生成 | ❌ 未实现 | ✅ | useGeneratedKeys |
| 多数据源 | ❌ 未实现 | ✅ | 数据源切换 |

---

## 🎯 我们实现了核心的20%

### 已实现（核心功能）✅

```
1. 动态代理机制 ⭐⭐⭐⭐⭐
   - MapperProxy
   - 接口自动实现
   
2. 配置管理 ⭐⭐⭐⭐⭐
   - Configuration
   - XML解析
   
3. SQL执行 ⭐⭐⭐⭐⭐
   - Executor
   - StatementHandler
   - PreparedStatement
   
4. 结果映射 ⭐⭐⭐⭐⭐
   - ResultSetHandler
   - 自动字段映射
   - 驼峰转换
   
5. 基础设施 ⭐⭐⭐⭐
   - 连接管理
   - 事务管理
```

### 未实现（高级特性）

```
1. 动态SQL ⭐⭐⭐
   - <if>、<foreach>等
   - SQL片段拼接
   
2. 复杂映射 ⭐⭐⭐
   - <resultMap>
   - 一对一、一对多
   - 懒加载
   
3. 缓存机制 ⭐⭐
   - 一级缓存
   - 二级缓存
   
4. 其他执行器 ⭐⭐
   - ReuseExecutor
   - BatchExecutor
   
5. 注解支持 ⭐
   - @Select
   - @Insert等
```

---

## 💡 为什么只实现核心20%？

### 1. 二八定律

```
80%的功能需求
只需要20%的核心功能

核心功能：
✅ 动态代理
✅ SQL执行
✅ 结果映射

这些是MyBatis的本质！
```

### 2. 学习效率

```
深入理解核心原理
比浅尝辄止所有功能更重要

我们focus on：
- 为什么需要动态代理？
- SQL如何执行？
- 结果如何映射？

而不是：
- 记住所有标签
- 记住所有配置
- 记住所有API
```

### 3. 可扩展性

```
有了核心框架
高级特性可以逐步添加

例如：
- 添加<if>标签？在XML解析时处理
- 添加缓存？在Executor层添加
- 添加插件？已有InterceptorChain

核心稳定，功能可扩展
```

---

## 🎯 如何继续学习？

### 路径1：阅读真实MyBatis源码

**推荐阅读顺序**：
```
1. SqlSessionFactoryBuilder
   - 入口类
   - 构建流程
   
2. XMLConfigBuilder
   - 配置解析
   - 对比我们的实现
   
3. Executor接口及其实现
   - SimpleExecutor
   - ReuseExecutor
   - BatchExecutor
   
4. StatementHandler
   - PreparedStatementHandler
   - 参数处理
   
5. ResultSetHandler
   - DefaultResultSetHandler
   - 结果映射
   
6. Plugin机制
   - InterceptorChain
   - Plugin.wrap()
```

### 路径2：实现缺失的功能

**练习项目**：
```
1. 实现一级缓存
   - 在SqlSession中维护Map
   - query前先查缓存
   - update后清除缓存
   
2. 实现动态SQL的<if>标签
   - 解析<if>标签
   - 判断test条件
   - 动态拼接SQL
   
3. 实现ResultMap
   - 解析<resultMap>标签
   - 手动指定映射关系
   - 支持一对一
```

### 路径3：使用MyBatis解决实际问题

**实战项目**：
```
1. 电商系统
   - 用户管理
   - 商品管理
   - 订单管理
   - 复杂查询
   
2. 博客系统
   - 文章CRUD
   - 分类管理
   - 标签管理
   - 评论系统
   
3. 权限系统
   - 用户角色
   - 角色权限
   - 多对多关系
   - 动态权限
```

---

**第二课完成！我们了解了MyBatis的高级特性！** 🎉

**休息3分钟，准备最后一课：面试准备与学习建议！** ☕

