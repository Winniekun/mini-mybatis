package com.mybatis.executor;

import com.mybatis.cache.Cache;
import com.mybatis.cache.CacheKey;
import com.mybatis.cache.impl.PerpetualCache;
import com.mybatis.exceptions.MyBatisException;
import com.mybatis.mapping.MappedStatement;
import com.mybatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Executor的基础实现（抽象类）
 * 
 * BaseExecutor使用模板方法模式，定义了SQL执行的基本流程，
 * 具体的Statement处理由子类实现。
 * 
 * 核心功能：
 * 1. 一级缓存管理
 * 2. 事务管理
 * 3. SQL执行流程控制
 * 4. CacheKey的创建
 * 
 * 设计模式：
 * - 模板方法模式：query()和update()定义流程，doQuery()和doUpdate()由子类实现
 * - 策略模式：不同的子类实现不同的Statement处理策略
 * 
 * 子类：
 * - SimpleExecutor：每次新建Statement
 * - ReuseExecutor：复用Statement
 * - BatchExecutor：批量执行
 * 
 * @author 学习者
 */
public abstract class BaseExecutor implements Executor {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseExecutor.class);
    
    /**
     * 全局配置
     */
    protected Configuration configuration;
    
    /**
     * 数据库连接
     */
    protected Connection connection;
    
    /**
     * 一级缓存（本地缓存）⭐⭐⭐⭐⭐
     * 
     * 作用域：SqlSession级别
     * 生命周期：SqlSession的生命周期
     * 实现：PerpetualCache（基于HashMap）
     */
    protected Cache localCache;
    
    /**
     * 查询栈深度（防止循环引用）
     */
    protected int queryStack;
    
    /**
     * 是否已关闭
     */
    private boolean closed;
    
    protected BaseExecutor(Configuration configuration, Connection connection) {
        this.configuration = configuration;
        this.connection = connection;
        this.localCache = new PerpetualCache("LocalCache");
        this.queryStack = 0;
        this.closed = false;
    }
    
    // ========================================
    // 模板方法：query() ⭐⭐⭐⭐⭐
    // ========================================
    
    /**
     * 查询操作（模板方法）
     * 
     * 执行流程：
     * 1. 检查是否已关闭
     * 2. 创建CacheKey
     * 3. 从一级缓存获取
     * 4. 缓存命中：直接返回
     * 5. 缓存未命中：查询数据库（调用子类的doQuery方法）
     * 6. 将结果放入缓存
     * 7. 返回结果
     */
    @Override
    public <E> List<E> query(String statementId, Object parameter) throws SQLException {
        // 检查是否已关闭
        if (closed) {
            throw new MyBatisException("Executor已关闭");
        }
        
        logger.debug("执行查询: {}", statementId);
        
        // 1. 获取MappedStatement
        MappedStatement ms = configuration.getMappedStatement(statementId);
        if (ms == null) {
            throw new MyBatisException("未找到SQL语句: " + statementId);
        }
        
        // 2. 创建缓存Key ⭐⭐⭐⭐⭐
        CacheKey key = createCacheKey(ms, parameter);
        
        // 3. 调用重载方法
        return query(ms, parameter, key);
    }
    
    /**
     * 查询操作（带CacheKey）
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> query(MappedStatement ms, Object parameter, CacheKey key) throws SQLException {
        List<E> list;
        
        try {
            queryStack++;
            
            // 从一级缓存获取 ⭐⭐⭐⭐⭐
            list = (List<E>) localCache.getObject(key);
            
            if (list != null) {
                logger.debug("缓存命中: {} [cacheKey={}]", ms.getId(), key);
                return list;
            } else {
                logger.debug("缓存未命中，查询数据库: {}", ms.getId());
                
                // 从数据库查询 ⭐⭐⭐⭐⭐
                list = queryFromDatabase(ms, parameter, key);
            }
            
        } finally {
            queryStack--;
        }
        
        // 如果查询栈为0，可以在这里处理延迟加载等逻辑
        if (queryStack == 0) {
            // 预留：处理延迟加载
        }
        
        return list;
    }
    
    /**
     * 从数据库查询
     */
    private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, CacheKey key) 
            throws SQLException {
        
        List<E> list;
        
        // 先在缓存中放一个占位符（防止循环引用）
        localCache.putObject(key, null);
        
        try {
            // 执行查询（抽象方法，由子类实现）⭐⭐⭐⭐⭐
            list = doQuery(ms, parameter);
            
        } finally {
            // 移除占位符
            localCache.removeObject(key);
        }
        
        // 放入缓存 ⭐⭐⭐⭐⭐
        localCache.putObject(key, list);
        
        logger.debug("查询完成，结果已缓存: {} [size={}, cacheKey={}]", 
            ms.getId(), list.size(), key);
        
        return list;
    }
    
    // ========================================
    // 模板方法：update() ⭐⭐⭐⭐⭐
    // ========================================
    
    /**
     * 更新操作（模板方法）
     * 
     * 执行流程：
     * 1. 检查是否已关闭
     * 2. 清空一级缓存（重要！）
     * 3. 执行更新（调用子类的doUpdate方法）
     * 4. 返回影响行数
     */
    @Override
    public int update(String statementId, Object parameter) throws SQLException {
        // 检查是否已关闭
        if (closed) {
            throw new MyBatisException("Executor已关闭");
        }
        
        logger.debug("执行更新: {}", statementId);
        
        // 1. 获取MappedStatement
        MappedStatement ms = configuration.getMappedStatement(statementId);
        if (ms == null) {
            throw new MyBatisException("未找到SQL语句: " + statementId);
        }
        
        // 2. 清空一级缓存 ⭐⭐⭐⭐⭐
        // 因为更新操作会改变数据，缓存的数据可能已经过期
        clearLocalCache();
        
        // 3. 执行更新（抽象方法，由子类实现）⭐⭐⭐⭐⭐
        int rows = doUpdate(ms, parameter);
        
        logger.debug("更新完成: {} [rows={}]", ms.getId(), rows);
        
        return rows;
    }
    
    // ========================================
    // 缓存相关方法 ⭐⭐⭐⭐⭐
    // ========================================
    
    /**
     * 创建缓存Key
     * 
     * CacheKey的组成：
     * 1. MappedStatement的ID
     * 2. SQL语句
     * 3. 参数值
     * 
     * @param ms MappedStatement
     * @param parameter 参数
     * @return CacheKey
     */
    public CacheKey createCacheKey(MappedStatement ms, Object parameter) {
        if (closed) {
            throw new MyBatisException("Executor已关闭");
        }
        
        CacheKey cacheKey = new CacheKey();
        
        // 1. MappedStatement的ID
        cacheKey.update(ms.getId());
        
        // 2. SQL语句
        cacheKey.update(ms.getSql());
        
        // 3. 参数值
        cacheKey.update(parameter);
        
        // 4. Environment（如果有的话）
        // 这里简化了，真实MyBatis会加上Environment ID
        
        return cacheKey;
    }
    
    /**
     * 判断缓存中是否存在
     */
    public boolean isCached(CacheKey key) {
        return localCache.getObject(key) != null;
    }
    
    /**
     * 清空本地缓存
     */
    public void clearLocalCache() {
        if (!closed) {
            localCache.clear();
            logger.debug("一级缓存已清空");
        }
    }
    
    // ========================================
    // 事务管理
    // ========================================
    
    @Override
    public void commit() throws SQLException {
        if (closed) {
            throw new MyBatisException("Executor已关闭");
        }
        
        // 清空缓存
        clearLocalCache();
        
        // 提交事务
        if (connection != null && !connection.getAutoCommit()) {
            connection.commit();
            logger.debug("事务已提交");
        }
    }
    
    @Override
    public void rollback() throws SQLException {
        if (closed) {
            throw new MyBatisException("Executor已关闭");
        }
        
        // 清空缓存
        clearLocalCache();
        
        // 回滚事务
        if (connection != null && !connection.getAutoCommit()) {
            connection.rollback();
            logger.debug("事务已回滚");
        }
    }
    
    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        
        // 清空缓存
        clearLocalCache();
        
        // 执行子类的清理工作
        doClose();
        
        closed = true;
        logger.debug("Executor已关闭");
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    // ========================================
    // 抽象方法：由子类实现 ⭐⭐⭐⭐⭐
    // ========================================
    
    /**
     * 执行查询（由子类实现）
     * 
     * @param ms MappedStatement
     * @param parameter 参数
     * @param <E> 结果类型
     * @return 查询结果
     * @throws SQLException SQL异常
     */
    protected abstract <E> List<E> doQuery(MappedStatement ms, Object parameter) 
            throws SQLException;
    
    /**
     * 执行更新（由子类实现）
     * 
     * @param ms MappedStatement
     * @param parameter 参数
     * @return 影响行数
     * @throws SQLException SQL异常
     */
    protected abstract int doUpdate(MappedStatement ms, Object parameter) 
            throws SQLException;
    
    /**
     * 关闭时的清理工作（由子类实现）
     * 
     * @throws SQLException SQL异常
     */
    protected abstract void doClose() throws SQLException;
}

