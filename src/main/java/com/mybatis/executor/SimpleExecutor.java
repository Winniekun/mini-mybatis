package com.mybatis.executor;

import com.mybatis.executor.statement.StatementHandler;
import com.mybatis.mapping.MappedStatement;
import com.mybatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 简单执行器
 * 
 * SimpleExecutor是最基本的执行器实现，每次执行SQL都会创建新的Statement对象。
 * 
 * 特点：
 * 1. 每次执行都创建新的Statement ⭐
 * 2. 执行完毕后立即关闭Statement ⭐
 * 3. 不复用Statement对象
 * 4. 实现简单，适合大多数场景
 * 5. 继承BaseExecutor，拥有一级缓存功能
 * 
 * 与ReuseExecutor的区别：
 * - SimpleExecutor：每次新建Statement，用完关闭
 * - ReuseExecutor：缓存Statement，多次复用
 * 
 * 执行流程：
 * 1. BaseExecutor处理缓存逻辑
 * 2. 缓存未命中时调用doQuery/doUpdate
 * 3. doQuery/doUpdate创建Statement
 * 4. 执行SQL
 * 5. 立即关闭Statement ⭐
 * 
 * @author 学习者
 */
public class SimpleExecutor extends BaseExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleExecutor.class);
    
    public SimpleExecutor(Configuration configuration, Connection connection) {
        super(configuration, connection);
    }
    
    /**
     * 执行查询（子类实现）
     * 
     * SimpleExecutor的策略：
     * - 每次都创建新的Statement
     * - 执行完立即关闭
     */
    @Override
    protected <E> List<E> doQuery(MappedStatement ms, Object parameter) throws SQLException {
        Statement stmt = null;
        try {
            logger.debug("SimpleExecutor.doQuery: {}", ms.getId());
            
            // 1. 创建StatementHandler
            StatementHandler handler = new StatementHandler(configuration);
            
            // 2. 准备Statement（每次都新建）⭐
            stmt = handler.prepare(connection, ms.getSql());
            
            // 3. 设置参数
            handler.parameterize(stmt, parameter);
            
            // 4. 执行查询
            List<E> result = handler.query(stmt, ms.getResultType());
            
            logger.debug("doQuery完成: {} [rows={}]", ms.getId(), result.size());
            
            return result;
            
        } finally {
            // 5. 立即关闭Statement ⭐
            closeStatement(stmt);
        }
    }
    
    /**
     * 执行更新（子类实现）
     * 
     * SimpleExecutor的策略：
     * - 每次都创建新的Statement
     * - 执行完立即关闭
     */
    @Override
    protected int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
        Statement stmt = null;
        try {
            logger.debug("SimpleExecutor.doUpdate: {}", ms.getId());
            
            // 1. 创建StatementHandler
            StatementHandler handler = new StatementHandler(configuration);
            
            // 2. 准备Statement（每次都新建）⭐
            stmt = handler.prepare(connection, ms.getSql());
            
            // 3. 设置参数
            handler.parameterize(stmt, parameter);
            
            // 4. 执行更新
            int rows = handler.update(stmt);
            
            logger.debug("doUpdate完成: {} [rows={}]", ms.getId(), rows);
            
            return rows;
            
        } finally {
            // 5. 立即关闭Statement ⭐
            closeStatement(stmt);
        }
    }
    
    /**
     * 关闭时的清理工作
     */
    @Override
    protected void doClose() throws SQLException {
        // SimpleExecutor不需要特殊的清理工作
        // Statement都是临时创建的，用完就关闭了
        logger.debug("SimpleExecutor.doClose");
    }
    
    /**
     * 关闭Statement
     */
    private void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.warn("关闭Statement失败", e);
            }
        }
    }
}

