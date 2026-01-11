package com.mybatis.executor;

import com.mybatis.exceptions.MyBatisException;
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
 * 1. 每次执行都创建新的Statement
 * 2. 执行完毕后立即关闭Statement
 * 3. 不复用Statement对象
 * 4. 实现简单，适合学习
 * 
 * 执行流程：
 * 1. 根据statementId获取MappedStatement
 * 2. 创建StatementHandler
 * 3. 准备Statement对象
 * 4. 设置参数
 * 5. 执行SQL
 * 6. 处理结果
 * 7. 关闭Statement
 * 
 * @author 学习者
 */
public class SimpleExecutor implements Executor {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleExecutor.class);
    
    /**
     * 全局配置对象
     */
    private Configuration configuration;
    
    /**
     * 数据库连接
     */
    private Connection connection;
    
    public SimpleExecutor(Configuration configuration, Connection connection) {
        this.configuration = configuration;
        this.connection = connection;
    }
    
    /**
     * 执行查询操作
     */
    @Override
    public <E> List<E> query(String statementId, Object parameter) throws SQLException {
        logger.debug("SimpleExecutor执行查询: {}", statementId);
        
        // 1. 获取MappedStatement
        MappedStatement ms = configuration.getMappedStatement(statementId);
        if (ms == null) {
            throw new MyBatisException("未找到SQL语句: " + statementId);
        }
        
        // 2. 创建StatementHandler
        StatementHandler handler = new StatementHandler(configuration);
        
        // 3. 准备Statement
        Statement statement = handler.prepare(connection, ms.getSql());
        
        // 4. 设置参数
        handler.parameterize(statement, parameter);
        
        // 5. 执行查询
        List<E> result = handler.query(statement, ms.getResultType());
        
        // 6. 关闭Statement
        statement.close();
        
        logger.debug("查询完成，返回{}条记录", result.size());
        return result;
    }
    
    /**
     * 执行更新操作
     */
    @Override
    public int update(String statementId, Object parameter) throws SQLException {
        logger.debug("SimpleExecutor执行更新: {}", statementId);
        
        // 1. 获取MappedStatement
        MappedStatement ms = configuration.getMappedStatement(statementId);
        if (ms == null) {
            throw new MyBatisException("未找到SQL语句: " + statementId);
        }
        
        // 2. 创建StatementHandler
        StatementHandler handler = new StatementHandler(configuration);
        
        // 3. 准备Statement
        Statement statement = handler.prepare(connection, ms.getSql());
        
        // 4. 设置参数
        handler.parameterize(statement, parameter);
        
        // 5. 执行更新
        int rows = handler.update(statement);
        
        // 6. 关闭Statement
        statement.close();
        
        logger.debug("更新完成，影响{}行", rows);
        return rows;
    }
    
    /**
     * 提交事务
     */
    @Override
    public void commit() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            connection.commit();
            logger.debug("事务已提交");
        }
    }
    
    /**
     * 回滚事务
     */
    @Override
    public void rollback() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            connection.rollback();
            logger.debug("事务已回滚");
        }
    }
    
    /**
     * 关闭执行器
     */
    @Override
    public void close() throws SQLException {
        // SimpleExecutor不需要特殊的清理工作
        // 数据库连接由SqlSession管理
        logger.debug("SimpleExecutor已关闭");
    }
}

