# Executor深度剖析 - 设计思想、模式与缓存

## 📚 目录

1. Executor的设计思想
2. Executor的类层次结构
3. 设计模式详解
4. 三种Executor的实现原理
5. 一级缓存的设计与实现
6. 二级缓存的设计与实现
7. 缓存失效机制
8. 源码级别的理解

---

## 🎯 Part 1: Executor的设计思想

### 为什么需要Executor？

**问题背景：**
```
用户调用：mapper.selectById(1L)
需要做什么？
1. 获取SQL配置
2. 创建Statement
3. 设置参数
4. 执行SQL
5. 处理结果
6. 管理资源
7. 处理缓存
8. 管理事务

这些逻辑写在哪里？
- 写在SqlSession？太重了
- 写在StatementHandler？职责不清
- 写在Mapper？违背单一职责

解决方案：抽取Executor层！
```

---

### Executor的核心职责

```
┌─────────────────────────────────────┐
│         Executor的职责边界           │
└─────────────────────────────────────┘

【应该做的】✅
1. 协调SQL执行流程
2. 管理一级缓存
3. 管理事务（延迟提交）
4. 创建StatementHandler
5. 批量操作管理
6. 延迟加载管理

【不应该做的】❌
1. JDBC操作 → 交给StatementHandler
2. 参数设置 → 交给ParameterHandler
3. 结果映射 → 交给ResultSetHandler
4. 配置管理 → 交给Configuration

核心原则：协调者，不是执行者
```

---

### 设计目标

```
1. 单一职责
   - Executor只负责协调
   - 具体工作委托给Handler

2. 开闭原则
   - 对扩展开放（可以添加新Executor）
   - 对修改关闭（核心接口稳定）

3. 依赖倒置
   - 依赖抽象（Executor接口）
   - 不依赖具体实现

4. 策略模式
   - 不同Executor = 不同策略
   - 运行时可切换

5. 模板方法
   - BaseExecutor定义模板
   - 子类实现细节
```

---

## 🏗️ Part 2: Executor的类层次结构

### 完整的类图

```
                   Executor (接口)
                        │
        ┌───────────────┼───────────────┐
        │               │               │
    BaseExecutor    CachingExecutor  自定义Executor
    (抽象类)         (缓存装饰器)
        │
        ├─────────────┬─────────────┬─────────────┐
        │             │             │             │
  SimpleExecutor  ReuseExecutor BatchExecutor  ClosedExecutor
   (简单执行)      (复用)        (批量)        (已关闭)
```

---

### Executor接口定义

```java
public interface Executor {
    
    /**
     * 查询操作
     */
    <E> List<E> query(
        MappedStatement ms,           // SQL配置
        Object parameter,             // 参数
        RowBounds rowBounds,          // 分页
        ResultHandler resultHandler   // 结果处理器
    ) throws SQLException;
    
    /**
     * 查询操作（带缓存key）
     */
    <E> List<E> query(
        MappedStatement ms,
        Object parameter,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        CacheKey key,                 // 缓存键
        BoundSql boundSql             // SQL绑定
    ) throws SQLException;
    
    /**
     * 更新操作（INSERT/UPDATE/DELETE）
     */
    int update(
        MappedStatement ms,
        Object parameter
    ) throws SQLException;
    
    /**
     * 刷新批量语句
     */
    List<BatchResult> flushStatements() throws SQLException;
    
    /**
     * 提交事务
     */
    void commit(boolean required) throws SQLException;
    
    /**
     * 回滚事务
     */
    void rollback(boolean required) throws SQLException;
    
    /**
     * 创建缓存Key
     */
    CacheKey createCacheKey(
        MappedStatement ms,
        Object parameterObject,
        RowBounds rowBounds,
        BoundSql boundSql
    );
    
    /**
     * 是否缓存
     */
    boolean isCached(MappedStatement ms, CacheKey key);
    
    /**
     * 清除本地缓存
     */
    void clearLocalCache();
    
    /**
     * 延迟加载
     */
    void deferLoad(
        MappedStatement ms,
        MetaObject resultObject,
        String property,
        CacheKey key,
        Class<?> targetType
    );
    
    /**
     * 获取事务
     */
    Transaction getTransaction();
    
    /**
     * 关闭Executor
     */
    void close(boolean forceRollback);
    
    /**
     * 是否已关闭
     */
    boolean isClosed();
    
    /**
     * 设置包装的Executor
     */
    void setExecutorWrapper(Executor executor);
}
```

---

### BaseExecutor抽象类（模板方法模式）⭐⭐⭐⭐⭐

```java
public abstract class BaseExecutor implements Executor {
    
    // 事务对象
    protected Transaction transaction;
    // 包装的Executor（装饰器模式）
    protected Executor wrapper;
    
    // 延迟加载队列
    protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;
    // 本地缓存（一级缓存）⭐⭐⭐⭐⭐
    protected PerpetualCache localCache;
    // 本地输出参数缓存
    protected PerpetualCache localOutputParameterCache;
    
    // 配置对象
    protected Configuration configuration;
    
    // 查询栈层级（防止循环引用）
    protected int queryStack;
    // 是否已关闭
    private boolean closed;
    
    
    // =====================================
    // 模板方法：query() ⭐⭐⭐⭐⭐
    // =====================================
    @Override
    public <E> List<E> query(
        MappedStatement ms, 
        Object parameter, 
        RowBounds rowBounds, 
        ResultHandler resultHandler
    ) throws SQLException {
        
        // 1. 获取BoundSql
        BoundSql boundSql = ms.getBoundSql(parameter);
        
        // 2. 创建缓存Key
        CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
        
        // 3. 调用重载方法
        return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
    }
    
    @Override
    public <E> List<E> query(
        MappedStatement ms,
        Object parameter,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        CacheKey key,
        BoundSql boundSql
    ) throws SQLException {
        
        ErrorContext.instance()
            .resource(ms.getResource())
            .activity("executing a query")
            .object(ms.getId());
        
        // 检查是否已关闭
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        
        // 如果是第一层查询，且配置了刷新缓存，则清空缓存
        if (queryStack == 0 && ms.isFlushCacheRequired()) {
            clearLocalCache();
        }
        
        List<E> list;
        try {
            // 查询层级+1（防止循环引用）
            queryStack++;
            
            // 从缓存中获取 ⭐⭐⭐⭐⭐
            list = resultHandler == null 
                ? (List<E>) localCache.getObject(key) 
                : null;
            
            if (list != null) {
                // 缓存命中：处理存储过程的输出参数
                handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
            } else {
                // 缓存未命中：从数据库查询 ⭐⭐⭐⭐⭐
                list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
            }
            
        } finally {
            // 查询层级-1
            queryStack--;
        }
        
        // 如果查询栈为0
        if (queryStack == 0) {
            // 处理延迟加载
            for (DeferredLoad deferredLoad : deferredLoads) {
                deferredLoad.load();
            }
            deferredLoads.clear();
            
            // 如果缓存级别是STATEMENT，清空缓存
            if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
                clearLocalCache();
            }
        }
        
        return list;
    }
    
    
    // =====================================
    // 从数据库查询（核心方法）⭐⭐⭐⭐⭐
    // =====================================
    private <E> List<E> queryFromDatabase(
        MappedStatement ms,
        Object parameter,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        CacheKey key,
        BoundSql boundSql
    ) throws SQLException {
        
        List<E> list;
        
        // 1. 先在缓存中放一个占位符（防止循环引用）
        localCache.putObject(key, EXECUTION_PLACEHOLDER);
        
        try {
            // 2. 执行查询（抽象方法，由子类实现）⭐⭐⭐⭐⭐
            list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
            
        } finally {
            // 3. 移除占位符
            localCache.removeObject(key);
        }
        
        // 4. 放入缓存 ⭐⭐⭐⭐⭐
        localCache.putObject(key, list);
        
        // 5. 如果是存储过程，缓存输出参数
        if (ms.getStatementType() == StatementType.CALLABLE) {
            localOutputParameterCache.putObject(key, parameter);
        }
        
        return list;
    }
    
    
    // =====================================
    // 抽象方法：子类必须实现 ⭐⭐⭐⭐⭐
    // =====================================
    protected abstract <E> List<E> doQuery(
        MappedStatement ms,
        Object parameter,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        BoundSql boundSql
    ) throws SQLException;
    
    protected abstract int doUpdate(
        MappedStatement ms,
        Object parameter
    ) throws SQLException;
    
    protected abstract List<BatchResult> doFlushStatements(boolean isRollback)
        throws SQLException;
    
    
    // =====================================
    // 更新操作 ⭐⭐⭐⭐⭐
    // =====================================
    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        
        ErrorContext.instance()
            .resource(ms.getResource())
            .activity("executing an update")
            .object(ms.getId());
        
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        
        // 更新前清空缓存 ⭐⭐⭐⭐⭐
        clearLocalCache();
        
        // 执行更新（调用子类实现）
        return doUpdate(ms, parameter);
    }
    
    
    // =====================================
    // 创建缓存Key ⭐⭐⭐⭐⭐
    // =====================================
    @Override
    public CacheKey createCacheKey(
        MappedStatement ms,
        Object parameterObject,
        RowBounds rowBounds,
        BoundSql boundSql
    ) {
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        
        CacheKey cacheKey = new CacheKey();
        
        // 1. MappedStatement的ID
        cacheKey.update(ms.getId());
        
        // 2. 分页参数
        cacheKey.update(rowBounds.getOffset());
        cacheKey.update(rowBounds.getLimit());
        
        // 3. SQL语句
        cacheKey.update(boundSql.getSql());
        
        // 4. 参数值
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
        
        for (ParameterMapping parameterMapping : parameterMappings) {
            Object value;
            String propertyName = parameterMapping.getProperty();
            
            if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                value = metaObject.getValue(propertyName);
            }
            cacheKey.update(value);
        }
        
        // 5. Environment ID
        if (configuration.getEnvironment() != null) {
            cacheKey.update(configuration.getEnvironment().getId());
        }
        
        return cacheKey;
    }
    
    
    // =====================================
    // 清空本地缓存 ⭐⭐⭐⭐⭐
    // =====================================
    @Override
    public void clearLocalCache() {
        if (!closed) {
            localCache.clear();
            localOutputParameterCache.clear();
        }
    }
    
    
    // =====================================
    // 事务管理
    // =====================================
    @Override
    public void commit(boolean required) throws SQLException {
        if (closed) {
            throw new ExecutorException("Cannot commit, transaction is already closed");
        }
        clearLocalCache();
        flushStatements();
        if (required) {
            transaction.commit();
        }
    }
    
    @Override
    public void rollback(boolean required) throws SQLException {
        if (!closed) {
            try {
                clearLocalCache();
                flushStatements(true);
            } finally {
                if (required) {
                    transaction.rollback();
                }
            }
        }
    }
    
    
    // =====================================
    // 关闭Executor
    // =====================================
    @Override
    public void close(boolean forceRollback) {
        try {
            try {
                rollback(forceRollback);
            } finally {
                if (transaction != null) {
                    transaction.close();
                }
            }
        } catch (SQLException e) {
            log.warn("Unexpected exception on closing transaction.", e);
        } finally {
            transaction = null;
            deferredLoads = null;
            localCache = null;
            localOutputParameterCache = null;
            closed = true;
        }
    }
}
```

---

## 🎨 Part 3: 设计模式详解

### 1. 模板方法模式 ⭐⭐⭐⭐⭐

**定义：**
```
在父类中定义算法的骨架，
将某些步骤延迟到子类中实现。
```

**在Executor中的应用：**

```java
// BaseExecutor（父类）定义模板
public abstract class BaseExecutor implements Executor {
    
    // 模板方法：定义算法骨架 ⭐⭐⭐⭐⭐
    @Override
    public <E> List<E> query(...) {
        // 步骤1: 创建缓存Key
        CacheKey key = createCacheKey(...);
        
        // 步骤2: 从缓存获取
        List<E> list = localCache.getObject(key);
        
        if (list != null) {
            // 步骤3: 缓存命中
            return list;
        } else {
            // 步骤4: 缓存未命中，从数据库查询
            return queryFromDatabase(...);
        }
    }
    
    private <E> List<E> queryFromDatabase(...) {
        // 步骤5: 放入占位符
        localCache.putObject(key, PLACEHOLDER);
        
        // 步骤6: 执行查询（抽象方法，子类实现）⭐⭐⭐⭐⭐
        list = doQuery(...);  // 这里调用子类实现
        
        // 步骤7: 放入缓存
        localCache.putObject(key, list);
        
        return list;
    }
    
    // 抽象方法：子类必须实现具体逻辑
    protected abstract <E> List<E> doQuery(...);
}


// SimpleExecutor（子类）实现具体步骤
public class SimpleExecutor extends BaseExecutor {
    
    @Override
    protected <E> List<E> doQuery(...) {
        // 实现具体的查询逻辑
        Statement stmt = null;
        try {
            Configuration configuration = ms.getConfiguration();
            StatementHandler handler = configuration.newStatementHandler(...);
            stmt = prepareStatement(handler, ...);
            return handler.query(stmt, resultHandler);
        } finally {
            closeStatement(stmt);  // SimpleExecutor：用完就关闭
        }
    }
}


// ReuseExecutor（子类）不同的实现
public class ReuseExecutor extends BaseExecutor {
    
    // Statement缓存
    private final Map<String, Statement> statementMap = new HashMap<>();
    
    @Override
    protected <E> List<E> doQuery(...) {
        String sql = boundSql.getSql();
        Statement stmt;
        
        // 从缓存获取Statement
        if (statementMap.containsKey(sql)) {
            stmt = statementMap.get(sql);  // 复用
        } else {
            stmt = prepareStatement(handler, ...);
            statementMap.put(sql, stmt);  // 缓存
        }
        
        return handler.query(stmt, resultHandler);
    }
}
```

**模板方法的优势：**
```
1. 代码复用
   - 公共逻辑在父类（缓存、事务）
   - 差异逻辑在子类（Statement管理）

2. 扩展性好
   - 添加新Executor只需继承BaseExecutor
   - 实现doQuery/doUpdate/doFlushStatements

3. 符合开闭原则
   - 对扩展开放（新Executor）
   - 对修改关闭（BaseExecutor稳定）
```

---

### 2. 装饰器模式 ⭐⭐⭐⭐⭐

**定义：**
```
动态地给对象添加额外的职责，
不改变原对象的结构。
```

**在Executor中的应用：CachingExecutor**

```java
// CachingExecutor装饰SimpleExecutor，添加二级缓存功能
public class CachingExecutor implements Executor {
    
    // 被装饰的Executor（一般是SimpleExecutor）
    private final Executor delegate;
    
    // 事务缓存管理器（二级缓存）
    private final TransactionalCacheManager tcm = new TransactionalCacheManager();
    
    public CachingExecutor(Executor delegate) {
        this.delegate = delegate;
        delegate.setExecutorWrapper(this);
    }
    
    @Override
    public <E> List<E> query(...) {
        BoundSql boundSql = ms.getBoundSql(parameter);
        CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
        return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
    }
    
    @Override
    public <E> List<E> query(
        MappedStatement ms,
        Object parameter,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        CacheKey key,
        BoundSql boundSql
    ) throws SQLException {
        
        // 获取MappedStatement的二级缓存
        Cache cache = ms.getCache();
        
        if (cache != null) {
            // 如果配置了二级缓存
            flushCacheIfRequired(ms);
            
            if (ms.isUseCache() && resultHandler == null) {
                // 从二级缓存获取 ⭐⭐⭐⭐⭐
                List<E> list = (List<E>) tcm.getObject(cache, key);
                
                if (list == null) {
                    // 二级缓存未命中，委托给delegate查询（会使用一级缓存）
                    list = delegate.query(ms, parameter, rowBounds, resultHandler, key, boundSql);
                    
                    // 放入二级缓存 ⭐⭐⭐⭐⭐
                    tcm.putObject(cache, key, list);
                }
                
                return list;
            }
        }
        
        // 没有配置二级缓存，直接委托
        return delegate.query(ms, parameter, rowBounds, resultHandler, key, boundSql);
    }
    
    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        // 更新操作会清空二级缓存
        flushCacheIfRequired(ms);
        return delegate.update(ms, parameter);
    }
    
    @Override
    public void commit(boolean required) throws SQLException {
        // 提交事务时，将二级缓存真正写入 ⭐⭐⭐⭐⭐
        delegate.commit(required);
        tcm.commit();
    }
    
    @Override
    public void rollback(boolean required) throws SQLException {
        try {
            delegate.rollback(required);
        } finally {
            if (required) {
                // 回滚时清空二级缓存
                tcm.rollback();
            }
        }
    }
    
    // 其他方法都委托给delegate
    @Override
    public void clearLocalCache() {
        delegate.clearLocalCache();
    }
    
    @Override
    public CacheKey createCacheKey(...) {
        return delegate.createCacheKey(...);
    }
    
    // ...
}
```

**装饰器的层次：**
```
用户代码
    ↓
CachingExecutor（装饰器：二级缓存）
    ↓
SimpleExecutor（被装饰者：一级缓存 + SQL执行）
    ↓
StatementHandler
    ↓
JDBC
```

**装饰器的优势：**
```
1. 职责分离
   - SimpleExecutor：SQL执行 + 一级缓存
   - CachingExecutor：二级缓存

2. 灵活组合
   - 可以选择是否启用二级缓存
   - 不修改SimpleExecutor代码

3. 符合单一职责
   - 每个类只做一件事
```

---

### 3. 策略模式 ⭐⭐⭐⭐

**定义：**
```
定义一系列算法，
将每个算法封装起来，
并使它们可以互换。
```

**在Executor中的应用：**

```java
// 策略接口
public interface Executor {
    <E> List<E> query(...);
    int update(...);
}

// 策略1：简单执行
public class SimpleExecutor extends BaseExecutor {
    // 每次新建Statement
}

// 策略2：复用执行
public class ReuseExecutor extends BaseExecutor {
    // 复用Statement
}

// 策略3：批量执行
public class BatchExecutor extends BaseExecutor {
    // 批量执行
}

// 策略选择（Configuration）
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    
    Executor executor;
    
    // 根据类型选择策略 ⭐⭐⭐⭐⭐
    if (ExecutorType.BATCH == executorType) {
        executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
        executor = new ReuseExecutor(this, transaction);
    } else {
        executor = new SimpleExecutor(this, transaction);
    }
    
    // 如果开启了二级缓存，用装饰器包装
    if (cacheEnabled) {
        executor = new CachingExecutor(executor);
    }
    
    // 插件拦截
    executor = (Executor) interceptorChain.pluginAll(executor);
    
    return executor;
}
```

**使用策略：**
```java
// 默认策略
SqlSession session = factory.openSession();
// 内部：new SimpleExecutor()

// 批量策略
SqlSession session = factory.openSession(ExecutorType.BATCH);
// 内部：new BatchExecutor()

// 复用策略
SqlSession session = factory.openSession(ExecutorType.REUSE);
// 内部：new ReuseExecutor()
```

---

## 🔍 Part 4: 三种Executor的实现原理

### 1. SimpleExecutor（简单执行器）⭐⭐⭐⭐⭐

**特点：**
```
每次执行SQL都创建新的Statement
用完立即关闭
```

**源码实现：**
```java
public class SimpleExecutor extends BaseExecutor {
    
    @Override
    public <E> List<E> doQuery(
        MappedStatement ms,
        Object parameter,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        BoundSql boundSql
    ) throws SQLException {
        
        Statement stmt = null;
        try {
            Configuration configuration = ms.getConfiguration();
            
            // 1. 创建StatementHandler
            StatementHandler handler = configuration.newStatementHandler(
                wrapper, ms, parameter, rowBounds, resultHandler, boundSql
            );
            
            // 2. 准备Statement（每次都新建）⭐⭐⭐⭐⭐
            stmt = prepareStatement(handler, ms.getStatementLog());
            
            // 3. 执行查询
            return handler.query(stmt, resultHandler);
            
        } finally {
            // 4. 立即关闭Statement ⭐⭐⭐⭐⭐
            closeStatement(stmt);
        }
    }
    
    private Statement prepareStatement(
        StatementHandler handler,
        Log statementLog
    ) throws SQLException {
        
        Statement stmt;
        Connection connection = getConnection(statementLog);
        
        // 创建Statement
        stmt = handler.prepare(connection, transaction.getTimeout());
        
        // 设置参数
        handler.parameterize(stmt);
        
        return stmt;
    }
    
    @Override
    protected int doUpdate(MappedStatement ms, Object parameter) 
        throws SQLException {
        
        Statement stmt = null;
        try {
            Configuration configuration = ms.getConfiguration();
            StatementHandler handler = configuration.newStatementHandler(...);
            
            // 准备Statement
            stmt = prepareStatement(handler, ms.getStatementLog());
            
            // 执行更新
            return handler.update(stmt);
            
        } finally {
            // 关闭Statement
            closeStatement(stmt);
        }
    }
}
```

**执行流程：**
```
query()
  ↓
创建StatementHandler
  ↓
prepareStatement()
  ├─> connection.prepareStatement(sql)  // 新建Statement
  └─> handler.parameterize(stmt)
  ↓
handler.query(stmt)
  ↓
closeStatement(stmt)  // 立即关闭
```

**优缺点：**
```
优点：
✅ 简单直观
✅ 不会有Statement泄露
✅ 不会有状态问题

缺点：
❌ 每次都创建Statement（有开销）
❌ 不能复用预编译
```

---

### 2. ReuseExecutor（复用执行器）⭐⭐⭐⭐

**特点：**
```
复用相同SQL的Statement
减少编译次数
```

**源码实现：**
```java
public class ReuseExecutor extends BaseExecutor {
    
    // Statement缓存 ⭐⭐⭐⭐⭐
    private final Map<String, Statement> statementMap = new HashMap<>();
    
    @Override
    public <E> List<E> doQuery(
        MappedStatement ms,
        Object parameter,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        BoundSql boundSql
    ) throws SQLException {
        
        Configuration configuration = ms.getConfiguration();
        StatementHandler handler = configuration.newStatementHandler(...);
        
        // 准备Statement（可能复用）⭐⭐⭐⭐⭐
        Statement stmt = prepareStatement(handler, ms.getStatementLog());
        
        // 执行查询
        return handler.query(stmt, resultHandler);
    }
    
    private Statement prepareStatement(
        StatementHandler handler,
        Log statementLog
    ) throws SQLException {
        
        Statement stmt;
        BoundSql boundSql = handler.getBoundSql();
        String sql = boundSql.getSql();
        
        // 检查缓存 ⭐⭐⭐⭐⭐
        if (hasStatementFor(sql)) {
            // 从缓存获取
            stmt = getStatement(sql);
            // 重新设置超时时间
            applyTransactionTimeout(stmt);
        } else {
            // 创建新的Statement
            Connection connection = getConnection(statementLog);
            stmt = handler.prepare(connection, transaction.getTimeout());
            // 放入缓存 ⭐⭐⭐⭐⭐
            putStatement(sql, stmt);
        }
        
        // 设置参数（每次都要设置）
        handler.parameterize(stmt);
        
        return stmt;
    }
    
    private boolean hasStatementFor(String sql) {
        try {
            Statement statement = statementMap.get(sql);
            return statement != null && !statement.getConnection().isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    private Statement getStatement(String s) {
        return statementMap.get(s);
    }
    
    private void putStatement(String sql, Statement stmt) {
        statementMap.put(sql, stmt);
    }
    
    @Override
    public List<BatchResult> doFlushStatements(boolean isRollback) {
        // 关闭所有缓存的Statement ⭐⭐⭐⭐⭐
        for (Statement stmt : statementMap.values()) {
            closeStatement(stmt);
        }
        statementMap.clear();
        return Collections.emptyList();
    }
}
```

**执行流程：**
```
第1次查询（SQL: SELECT * FROM user WHERE id = ?）
  ↓
prepareStatement()
  ├─> hasStatementFor(sql)  // false
  ├─> connection.prepareStatement(sql)  // 新建
  ├─> putStatement(sql, stmt)  // 缓存
  └─> parameterize(stmt)
  ↓
query()

第2次查询（相同SQL）
  ↓
prepareStatement()
  ├─> hasStatementFor(sql)  // true
  ├─> getStatement(sql)  // 从缓存取 ⭐⭐⭐⭐⭐
  └─> parameterize(stmt)  // 重新设置参数
  ↓
query()

close时：
  ↓
doFlushStatements()
  ├─> 遍历statementMap
  └─> 关闭所有Statement
```

**优缺点：**
```
优点：
✅ 复用Statement，减少编译
✅ 提升性能（特别是复杂SQL）

缺点：
❌ 占用更多内存
❌ 需要管理Statement生命周期
❌ Connection关闭会导致Statement失效
```

**适用场景：**
```
✅ 同一SQL多次执行
✅ SQL比较复杂
✅ 内存充足

示例：
for (int i = 0; i < 1000; i++) {
    User user = mapper.selectById(i);
    // Statement只编译一次，复用999次
}
```

---

### 3. BatchExecutor（批量执行器）⭐⭐⭐⭐

**特点：**
```
批量执行SQL
减少网络往返
提升吞吐量
```

**源码实现：**
```java
public class BatchExecutor extends BaseExecutor {
    
    // 批量结果列表
    private final List<Statement> statementList = new ArrayList<>();
    private final List<BatchResult> batchResultList = new ArrayList<>();
    
    // 当前SQL
    private String currentSql;
    // 当前MappedStatement
    private MappedStatement currentStatement;
    
    @Override
    public int doUpdate(MappedStatement ms, Object parameterObject) 
        throws SQLException {
        
        final Configuration configuration = ms.getConfiguration();
        final StatementHandler handler = configuration.newStatementHandler(...);
        final BoundSql boundSql = handler.getBoundSql();
        final String sql = boundSql.getSql();
        
        final Statement stmt;
        
        // 检查SQL是否相同 ⭐⭐⭐⭐⭐
        if (sql.equals(currentSql) && ms.equals(currentStatement)) {
            // 相同SQL：取最后一个Statement
            int last = statementList.size() - 1;
            stmt = statementList.get(last);
            
            // 重新设置超时
            applyTransactionTimeout(stmt);
            
            // 设置参数
            handler.parameterize(stmt);
            
            // 添加到当前BatchResult
            BatchResult batchResult = batchResultList.get(last);
            batchResult.addParameterObject(parameterObject);
            
        } else {
            // 不同SQL：创建新Statement
            Connection connection = getConnection(ms.getStatementLog());
            stmt = handler.prepare(connection, transaction.getTimeout());
            handler.parameterize(stmt);
            
            // 更新当前SQL
            currentSql = sql;
            currentStatement = ms;
            
            // 添加到列表
            statementList.add(stmt);
            batchResultList.add(new BatchResult(ms, sql, parameterObject));
        }
        
        // 添加到批次（不执行）⭐⭐⭐⭐⭐
        handler.batch(stmt);
        
        return BATCH_UPDATE_RETURN_VALUE;
    }
    
    @Override
    public List<BatchResult> doFlushStatements(boolean isRollback) 
        throws SQLException {
        
        try {
            List<BatchResult> results = new ArrayList<>();
            
            if (isRollback) {
                return Collections.emptyList();
            }
            
            // 执行所有批次 ⭐⭐⭐⭐⭐
            for (int i = 0, n = statementList.size(); i < n; i++) {
                Statement stmt = statementList.get(i);
                applyTransactionTimeout(stmt);
                
                BatchResult batchResult = batchResultList.get(i);
                try {
                    // 执行批量操作 ⭐⭐⭐⭐⭐
                    batchResult.setUpdateCounts(stmt.executeBatch());
                    
                    MappedStatement ms = batchResult.getMappedStatement();
                    List<Object> parameterObjects = batchResult.getParameterObjects();
                    
                    // 处理主键生成
                    KeyGenerator keyGenerator = ms.getKeyGenerator();
                    if (Jdbc3KeyGenerator.class.equals(keyGenerator.getClass())) {
                        Jdbc3KeyGenerator jdbc3KeyGenerator = (Jdbc3KeyGenerator) keyGenerator;
                        jdbc3KeyGenerator.processBatch(ms, stmt, parameterObjects);
                    } else if (!NoKeyGenerator.class.equals(keyGenerator.getClass())) {
                        for (Object parameter : parameterObjects) {
                            keyGenerator.processAfter(this, ms, stmt, parameter);
                        }
                    }
                    
                    // 关闭Statement
                    closeStatement(stmt);
                    
                } catch (BatchUpdateException e) {
                    throw new BatchExecutorException(
                        "Error flushing statements.  Cause: " + e, e,
                        batchResult, results
                    );
                }
                
                results.add(batchResult);
            }
            
            return results;
            
        } finally {
            // 清空列表
            for (Statement stmt : statementList) {
                closeStatement(stmt);
            }
            currentSql = null;
            statementList.clear();
            batchResultList.clear();
        }
    }
    
    @Override
    public <E> List<E> doQuery(
        MappedStatement ms,
        Object parameterObject,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        BoundSql boundSql
    ) throws SQLException {
        
        Statement stmt = null;
        try {
            // 查询前先刷新批量操作 ⭐⭐⭐⭐⭐
            flushStatements();
            
            Configuration configuration = ms.getConfiguration();
            StatementHandler handler = configuration.newStatementHandler(...);
            Connection connection = getConnection(ms.getStatementLog());
            
            stmt = handler.prepare(connection, transaction.getTimeout());
            handler.parameterize(stmt);
            
            return handler.query(stmt, resultHandler);
            
        } finally {
            closeStatement(stmt);
        }
    }
}
```

**执行流程：**
```
插入操作1：
  mapper.insert(user1)
    ↓
  doUpdate()
    ├─> 创建Statement
    ├─> parameterize()
    └─> stmt.addBatch()  // 只是添加到批次

插入操作2：
  mapper.insert(user2)
    ↓
  doUpdate()
    ├─> 检查SQL是否相同
    ├─> 复用Statement
    ├─> parameterize()
    └─> stmt.addBatch()  // 继续添加

...

commit时：
  session.commit()
    ↓
  flushStatements()
    ├─> stmt.executeBatch()  // 一次性执行所有
    ├─> 处理主键生成
    └─> 返回BatchResult
```

**优缺点：**
```
优点：
✅ 减少网络往返（N次 → 1次）
✅ 大幅提升批量操作性能
✅ 适合批量插入/更新

缺点：
❌ 不能立即获取影响行数
❌ 不能立即获取自增主键
❌ 调试困难
❌ 出错时难以定位具体哪条SQL
```

**适用场景：**
```
✅ 批量插入数据
✅ 批量更新数据
✅ 数据导入

示例：
SqlSession session = factory.openSession(ExecutorType.BATCH);
try {
    UserMapper mapper = session.getMapper(UserMapper.class);
    
    for (int i = 0; i < 10000; i++) {
        User user = new User();
        user.setUsername("user" + i);
        mapper.insert(user);  // 只是addBatch
    }
    
    session.commit();  // 真正执行
} finally {
    session.close();
}
```

---

## 💾 Part 5: 一级缓存的设计与实现

### 一级缓存概述

**定义：**
```
一级缓存 = SqlSession级别的缓存
生命周期 = SqlSession的生命周期
默认开启，无法关闭（可以设置为STATEMENT级别）
```

**作用：**
```
同一个SqlSession内：
  第1次查询 → 查数据库 → 放入缓存
  第2次查询 → 从缓存取

不同SqlSession：
  各自独立的缓存，互不影响
```

---

### 缓存实现：PerpetualCache

```java
public class PerpetualCache implements Cache {
    
    private final String id;
    
    // 缓存容器：简单的HashMap ⭐⭐⭐⭐⭐
    private final Map<Object, Object> cache = new HashMap<>();
    
    public PerpetualCache(String id) {
        this.id = id;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void putObject(Object key, Object value) {
        cache.put(key, value);
    }
    
    @Override
    public Object getObject(Object key) {
        return cache.get(key);
    }
    
    @Override
    public Object removeObject(Object key) {
        return cache.remove(key);
    }
    
    @Override
    public void clear() {
        cache.clear();
    }
    
    @Override
    public int getSize() {
        return cache.size();
    }
    
    // ...
}
```

---

### 缓存Key的设计：CacheKey ⭐⭐⭐⭐⭐

**为什么需要CacheKey？**
```
问题：如何判断两次查询是否相同？

错误方案：只用SQL判断
  SELECT * FROM user WHERE id = #{id}
  
  第1次：id = 1
  第2次：id = 2
  
  SQL相同，但参数不同！
  不能返回相同结果！

正确方案：综合判断
  - SQL语句
  - 参数值
  - 分页参数
  - MappedStatement ID
  - Environment ID
```

**CacheKey实现：**
```java
public class CacheKey implements Cloneable, Serializable {
    
    private static final int DEFAULT_MULTIPLIER = 37;
    private static final int DEFAULT_HASHCODE = 17;
    
    // 倍数
    private final int multiplier;
    // 哈希码
    private int hashcode;
    // 校验和
    private long checksum;
    // 更新次数
    private int count;
    
    // 更新列表（存储所有影响缓存的因素）⭐⭐⭐⭐⭐
    private List<Object> updateList;
    
    public CacheKey() {
        this.hashcode = DEFAULT_HASHCODE;
        this.multiplier = DEFAULT_MULTIPLIER;
        this.count = 0;
        this.updateList = new ArrayList<>();
    }
    
    // 更新缓存Key ⭐⭐⭐⭐⭐
    public void update(Object object) {
        int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);
        
        count++;
        checksum += baseHashCode;
        baseHashCode *= count;
        
        hashcode = multiplier * hashcode + baseHashCode;
        
        updateList.add(object);
    }
    
    // 判断相等 ⭐⭐⭐⭐⭐
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CacheKey)) {
            return false;
        }
        
        final CacheKey cacheKey = (CacheKey) object;
        
        // 1. hashcode必须相同
        if (hashcode != cacheKey.hashcode) {
            return false;
        }
        // 2. checksum必须相同
        if (checksum != cacheKey.checksum) {
            return false;
        }
        // 3. count必须相同
        if (count != cacheKey.count) {
            return false;
        }
        
        // 4. updateList中的每个元素都必须相同 ⭐⭐⭐⭐⭐
        for (int i = 0; i < updateList.size(); i++) {
            Object thisObject = updateList.get(i);
            Object thatObject = cacheKey.updateList.get(i);
            
            if (!ArrayUtil.equals(thisObject, thatObject)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        return hashcode;
    }
}
```

**创建CacheKey的过程：**
```java
// BaseExecutor.createCacheKey()
public CacheKey createCacheKey(...) {
    CacheKey cacheKey = new CacheKey();
    
    // 1. MappedStatement的ID
    cacheKey.update(ms.getId());
    // 例如：com.example.UserMapper.selectById
    
    // 2. 分页参数
    cacheKey.update(rowBounds.getOffset());  // 0
    cacheKey.update(rowBounds.getLimit());   // Integer.MAX_VALUE
    
    // 3. SQL语句
    cacheKey.update(boundSql.getSql());
    // 例如：SELECT * FROM user WHERE id = ?
    
    // 4. 参数值 ⭐⭐⭐⭐⭐
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    for (ParameterMapping pm : parameterMappings) {
        Object value = // 获取参数值
        cacheKey.update(value);  // 例如：1L
    }
    
    // 5. Environment ID
    if (configuration.getEnvironment() != null) {
        cacheKey.update(configuration.getEnvironment().getId());
    }
    
    return cacheKey;
}
```

**示例：**
```java
// 查询1
User user1 = mapper.selectById(1L);
// CacheKey: [selectById, 0, MAX, "SELECT...", 1L, "development"]

// 查询2（相同参数）
User user2 = mapper.selectById(1L);
// CacheKey: [selectById, 0, MAX, "SELECT...", 1L, "development"]
// equals() = true → 缓存命中 ⭐⭐⭐⭐⭐

// 查询3（不同参数）
User user3 = mapper.selectById(2L);
// CacheKey: [selectById, 0, MAX, "SELECT...", 2L, "development"]
// equals() = false → 缓存未命中，查数据库
```

---

### 缓存的生命周期

```java
// BaseExecutor中的缓存管理
public abstract class BaseExecutor implements Executor {
    
    // 一级缓存 ⭐⭐⭐⭐⭐
    protected PerpetualCache localCache;
    
    protected BaseExecutor(Configuration configuration, Transaction transaction) {
        // 创建缓存
        this.localCache = new PerpetualCache("LocalCache");
        // ...
    }
    
    // 查询时使用缓存
    public <E> List<E> query(...) {
        CacheKey key = createCacheKey(...);
        
        // 从缓存获取 ⭐⭐⭐⭐⭐
        List<E> list = (List<E>) localCache.getObject(key);
        
        if (list != null) {
            // 缓存命中
            return list;
        } else {
            // 缓存未命中，查数据库
            list = queryFromDatabase(...);
            // 放入缓存 ⭐⭐⭐⭐⭐
            localCache.putObject(key, list);
            return list;
        }
    }
    
    // 更新时清空缓存 ⭐⭐⭐⭐⭐
    public int update(MappedStatement ms, Object parameter) {
        clearLocalCache();  // 清空缓存
        return doUpdate(ms, parameter);
    }
    
    // 提交时清空缓存
    public void commit(boolean required) {
        clearLocalCache();
        // ...
    }
    
    // 回滚时清空缓存
    public void rollback(boolean required) {
        clearLocalCache();
        // ...
    }
    
    // 关闭时清空缓存
    public void close(boolean forceRollback) {
        try {
            rollback(forceRollback);
        } finally {
            localCache = null;  // 销毁缓存
        }
    }
    
    // 清空缓存
    public void clearLocalCache() {
        if (!closed) {
            localCache.clear();
        }
    }
}
```

---

### 缓存级别配置

```xml
<settings>
    <!-- 
        LOCAL_CACHE_SCOPE: 缓存级别
        - SESSION: SqlSession级别（默认）
        - STATEMENT: Statement级别（查询后立即清空）
    -->
    <setting name="localCacheScope" value="SESSION"/>
</settings>
```

**STATEMENT级别的实现：**
```java
public <E> List<E> query(...) {
    // ...
    List<E> list = queryFromDatabase(...);
    
    // 如果是STATEMENT级别，查询后立即清空 ⭐⭐⭐⭐⭐
    if (queryStack == 0 
        && configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
        clearLocalCache();
    }
    
    return list;
}
```

---

### 一级缓存的总结

```
特点：
✅ SqlSession级别
✅ 默认开启
✅ 基于HashMap
✅ 使用CacheKey作为键

生命周期：
- 创建：SqlSession创建时
- 使用：query时先查缓存
- 清空：update/commit/rollback时
- 销毁：SqlSession关闭时

优势：
✅ 减少数据库访问
✅ 提升性能
✅ 自动管理

注意事项：
⚠️ SqlSession不能跨线程共享
⚠️ 长时间持有SqlSession可能导致脏读
⚠️ 分布式环境不适用
```

---

## 🌐 Part 6: 二级缓存的设计与实现

### 二级缓存概述

**定义：**
```
二级缓存 = Mapper（namespace）级别的缓存
生命周期 = 应用程序的生命周期
默认关闭，需要配置开启
跨SqlSession共享
```

**架构：**
```
SqlSession1 → CachingExecutor → 二级缓存(UserMapper)
                                      ↓
SqlSession2 → CachingExecutor → 二级缓存(UserMapper)
                                      ↓
                                  一级缓存
                                      ↓
                                   数据库
```

---

### CachingExecutor实现

```java
public class CachingExecutor implements Executor {
    
    // 被装饰的Executor
    private final Executor delegate;
    
    // 事务缓存管理器 ⭐⭐⭐⭐⭐
    private final TransactionalCacheManager tcm = new TransactionalCacheManager();
    
    public CachingExecutor(Executor delegate) {
        this.delegate = delegate;
        delegate.setExecutorWrapper(this);
    }
    
    @Override
    public <E> List<E> query(
        MappedStatement ms,
        Object parameter,
        RowBounds rowBounds,
        ResultHandler resultHandler,
        CacheKey key,
        BoundSql boundSql
    ) throws SQLException {
        
        // 获取MappedStatement配置的缓存 ⭐⭐⭐⭐⭐
        Cache cache = ms.getCache();
        
        if (cache != null) {
            // 如果配置了二级缓存
            
            // 检查是否需要刷新缓存
            flushCacheIfRequired(ms);
            
            if (ms.isUseCache() && resultHandler == null) {
                // 确保不是存储过程
                ensureNoOutParams(ms, boundSql);
                
                // 从二级缓存获取 ⭐⭐⭐⭐⭐
                @SuppressWarnings("unchecked")
                List<E> list = (List<E>) tcm.getObject(cache, key);
                
                if (list == null) {
                    // 二级缓存未命中
                    // 委托给delegate查询（会使用一级缓存）
                    list = delegate.query(ms, parameter, rowBounds, resultHandler, key, boundSql);
                    
                    // 放入二级缓存（事务缓存）⭐⭐⭐⭐⭐
                    tcm.putObject(cache, key, list);
                }
                
                return list;
            }
        }
        
        // 没有配置二级缓存，直接委托
        return delegate.query(ms, parameter, rowBounds, resultHandler, key, boundSql);
    }
    
    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        // 更新操作：刷新缓存 ⭐⭐⭐⭐⭐
        flushCacheIfRequired(ms);
        return delegate.update(ms, parameter);
    }
    
    @Override
    public void commit(boolean required) throws SQLException {
        // 先提交delegate
        delegate.commit(required);
        
        // 再提交事务缓存（真正写入二级缓存）⭐⭐⭐⭐⭐
        tcm.commit();
    }
    
    @Override
    public void rollback(boolean required) throws SQLException {
        try {
            delegate.rollback(required);
        } finally {
            if (required) {
                // 回滚事务缓存 ⭐⭐⭐⭐⭐
                tcm.rollback();
            }
        }
    }
    
    private void flushCacheIfRequired(MappedStatement ms) {
        Cache cache = ms.getCache();
        if (cache != null && ms.isFlushCacheRequired()) {
            tcm.clear(cache);
        }
    }
}
```

---

### TransactionalCacheManager（事务缓存管理器）⭐⭐⭐⭐⭐

**为什么需要事务缓存？**
```
问题：如果直接写入二级缓存会怎样？

场景1：
  Session1: 查询user(id=1) → 放入二级缓存
  Session1: 修改user(id=1) → 还没提交
  Session2: 查询user(id=1) → 从二级缓存取到旧数据！

脏读问题！

解决：使用事务缓存
  Session1: 查询 → 放入临时缓存（TransactionalCache）
  Session1: 修改 → 清空临时缓存
  Session1: commit → 临时缓存 → 二级缓存 ⭐⭐⭐⭐⭐
  Session2: 查询 → 从二级缓存取到新数据
```

**实现：**
```java
public class TransactionalCacheManager {
    
    // 事务缓存映射表 ⭐⭐⭐⭐⭐
    // Key: 二级缓存对象
    // Value: 事务缓存包装器
    private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();
    
    // 清空缓存
    public void clear(Cache cache) {
        getTransactionalCache(cache).clear();
    }
    
    // 获取对象
    public Object getObject(Cache cache, CacheKey key) {
        return getTransactionalCache(cache).getObject(key);
    }
    
    // 放入对象（暂时放入临时缓存）⭐⭐⭐⭐⭐
    public void putObject(Cache cache, CacheKey key, Object value) {
        getTransactionalCache(cache).putObject(key, value);
    }
    
    // 提交（写入真正的二级缓存）⭐⭐⭐⭐⭐
    public void commit() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.commit();
        }
    }
    
    // 回滚（清空临时缓存）
    public void rollback() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.rollback();
        }
    }
    
    // 获取事务缓存包装器
    private TransactionalCache getTransactionalCache(Cache cache) {
        return transactionalCaches.computeIfAbsent(cache, TransactionalCache::new);
    }
}
```

---

### TransactionalCache（事务缓存包装器）

```java
public class TransactionalCache implements Cache {
    
    // 真正的二级缓存
    private final Cache delegate;
    
    // 提交时是否清空
    private boolean clearOnCommit;
    
    // 待提交的数据（临时缓存）⭐⭐⭐⭐⭐
    private final Map<Object, Object> entriesToAddOnCommit;
    
    // 未命中的key集合
    private final Set<Object> entriesMissedInCache;
    
    public TransactionalCache(Cache delegate) {
        this.delegate = delegate;
        this.clearOnCommit = false;
        this.entriesToAddOnCommit = new HashMap<>();
        this.entriesMissedInCache = new HashSet<>();
    }
    
    @Override
    public Object getObject(Object key) {
        // 先从真正的缓存获取
        Object object = delegate.getObject(key);
        
        if (object == null) {
            // 记录未命中
            entriesMissedInCache.add(key);
        }
        
        // 如果clearOnCommit=true，返回null
        if (clearOnCommit) {
            return null;
        } else {
            return object;
        }
    }
    
    @Override
    public void putObject(Object key, Object object) {
        // 放入临时缓存，不直接写入真正的缓存 ⭐⭐⭐⭐⭐
        entriesToAddOnCommit.put(key, object);
    }
    
    @Override
    public Object removeObject(Object key) {
        return null;
    }
    
    @Override
    public void clear() {
        clearOnCommit = true;
        entriesToAddOnCommit.clear();
    }
    
    // 提交：将临时缓存写入真正的缓存 ⭐⭐⭐⭐⭐
    public void commit() {
        if (clearOnCommit) {
            delegate.clear();
        }
        flushPendingEntries();
        reset();
    }
    
    // 回滚：清空临时缓存
    public void rollback() {
        unlockMissedEntries();
        reset();
    }
    
    private void reset() {
        clearOnCommit = false;
        entriesToAddOnCommit.clear();
        entriesMissedInCache.clear();
    }
    
    // 刷新待提交的数据到真正的缓存 ⭐⭐⭐⭐⭐
    private void flushPendingEntries() {
        for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
            // 写入真正的二级缓存
            delegate.putObject(entry.getKey(), entry.getValue());
        }
        
        // 对于未命中的key，放入null值
        for (Object entry : entriesMissedInCache) {
            if (!entriesToAddOnCommit.containsKey(entry)) {
                delegate.putObject(entry, null);
            }
        }
    }
    
    private void unlockMissedEntries() {
        for (Object entry : entriesMissedInCache) {
            try {
                delegate.removeObject(entry);
            } catch (Exception e) {
                log.warn("Unexpected exception while removing uncommitted cache entry", e);
            }
        }
    }
}
```

---

### 二级缓存的配置

**1. 开启二级缓存总开关：**
```xml
<!-- mybatis-config.xml -->
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>
```

**2. Mapper中配置缓存：**
```xml
<!-- UserMapper.xml -->
<mapper namespace="com.example.UserMapper">
    
    <!-- 开启二级缓存 -->
    <cache
        eviction="LRU"           <!-- 回收策略 -->
        flushInterval="60000"    <!-- 刷新间隔(ms) -->
        size="512"               <!-- 缓存大小 -->
        readOnly="false"/>       <!-- 只读 -->
    
    <!-- 使用缓存的查询 -->
    <select id="selectById" resultType="User" useCache="true">
        SELECT * FROM user WHERE id = #{id}
    </select>
    
    <!-- 不使用缓存的查询 -->
    <select id="selectAll" resultType="User" useCache="false">
        SELECT * FROM user
    </select>
    
</mapper>
```

**3. 实体类实现Serializable：**
```java
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

---

### 缓存装饰器链 ⭐⭐⭐⭐

**MyBatis提供了多个缓存装饰器：**

```
PerpetualCache（基础缓存）
    ↓ 装饰
LruCache（LRU回收策略）
    ↓ 装饰
SerializedCache（序列化）
    ↓ 装饰
LoggingCache（日志）
    ↓ 装饰
SynchronizedCache（同步锁）
    ↓ 装饰
TransactionalCache（事务）
```

**装饰器示例：**

```java
// LruCache（最近最少使用）
public class LruCache implements Cache {
    
    private final Cache delegate;
    private Map<Object, Object> keyMap;
    private Object eldestKey;
    
    public LruCache(Cache delegate) {
        this.delegate = delegate;
        setSize(1024);
    }
    
    @Override
    public void putObject(Object key, Object value) {
        delegate.putObject(key, value);
        cycleKeyList(key);
    }
    
    @Override
    public Object getObject(Object key) {
        keyMap.get(key); // 更新访问顺序
        return delegate.getObject(key);
    }
    
    private void setSize(final int size) {
        keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                boolean tooBig = size() > size;
                if (tooBig) {
                    eldestKey = eldest.getKey();
                }
                return tooBig;
            }
        };
    }
    
    private void cycleKeyList(Object key) {
        keyMap.put(key, key);
        if (eldestKey != null) {
            delegate.removeObject(eldestKey);
            eldestKey = null;
        }
    }
}


// SerializedCache（序列化缓存）
public class SerializedCache implements Cache {
    
    private final Cache delegate;
    
    public SerializedCache(Cache delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void putObject(Object key, Object object) {
        if (object == null || object instanceof Serializable) {
            // 序列化后存储 ⭐⭐⭐⭐⭐
            delegate.putObject(key, serialize((Serializable) object));
        } else {
            throw new CacheException("Object must be serializable");
        }
    }
    
    @Override
    public Object getObject(Object key) {
        Object object = delegate.getObject(key);
        // 反序列化 ⭐⭐⭐⭐⭐
        return object == null ? null : deserialize((byte[]) object);
    }
    
    private byte[] serialize(Serializable value) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            oos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new CacheException("Error serializing object.", e);
        }
    }
    
    private Serializable deserialize(byte[] value) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(value);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Serializable) ois.readObject();
        } catch (Exception e) {
            throw new CacheException("Error deserializing object.", e);
        }
    }
}
```

---

### 二级缓存的总结

```
特点：
✅ Mapper（namespace）级别
✅ 跨SqlSession共享
✅ 需要配置开启
✅ 基于装饰器模式

生命周期：
- 创建：第一次使用Mapper时
- 使用：query时先查二级缓存，再查一级缓存
- 清空：update/commit时
- 销毁：应用程序关闭时

事务性：
✅ 使用TransactionalCache
✅ commit时才真正写入
✅ rollback时丢弃

优势：
✅ 跨SqlSession共享
✅ 提升性能
✅ 减少数据库访问

注意事项：
⚠️ 只适合读多写少的场景
⚠️ 分布式环境慎用
⚠️ 实体类必须实现Serializable
⚠️ 多表关联查询可能导致脏读
```

---

## 📊 Part 7: 缓存对比总结

### 一级缓存 vs 二级缓存

| 特性 | 一级缓存 | 二级缓存 |
|-----|---------|---------|
| **作用域** | SqlSession | Mapper(namespace) |
| **生命周期** | SqlSession | 应用程序 |
| **默认状态** | 开启 | 关闭 |
| **是否共享** | 否 | 是 |
| **实现类** | PerpetualCache | 可配置（装饰器链） |
| **失效时机** | update/commit/close | update/commit |
| **配置** | localCacheScope | cache标签 |
| **线程安全** | 否（一个线程一个SqlSession） | 是（SynchronizedCache） |
| **序列化** | 不需要 | 需要（SerializedCache） |
| **适用场景** | 所有场景 | 读多写少 |
| **分布式** | 不适用 | 不适用（建议用Redis） |

---

### 缓存查询流程

```
用户查询
    ↓
CachingExecutor（如果开启二级缓存）
    ├─> 检查二级缓存
    │   ├─> 命中 → 直接返回
    │   └─> 未命中 ↓
    │
    └─> BaseExecutor（一级缓存）
        ├─> 检查一级缓存
        │   ├─> 命中 → 直接返回 → 放入二级缓存（commit时）
        │   └─> 未命中 ↓
        │
        └─> 查询数据库
            └─> 放入一级缓存 → 放入二级缓存（commit时）
```

---

## 🎯 总结

### Executor的设计精髓

```
1. 模板方法模式
   - BaseExecutor定义骨架
   - 子类实现细节
   - 代码复用 + 灵活扩展

2. 装饰器模式
   - CachingExecutor装饰SimpleExecutor
   - 添加二级缓存功能
   - 不修改原有代码

3. 策略模式
   - 三种Executor = 三种策略
   - 运行时可切换
   - 适应不同场景

4. 职责分离
   - Executor：协调流程
   - Handler：具体执行
   - 清晰的层次结构

5. 缓存设计
   - 一级缓存：SqlSession级别
   - 二级缓存：Mapper级别
   - 事务性保证

6. 性能优化
   - ReuseExecutor：复用Statement
   - BatchExecutor：批量执行
   - 缓存机制：减少数据库访问
```

---

**这份文档涵盖了Executor的所有核心内容，建议多读几遍！** 📖

有任何问题随时问我！😊

