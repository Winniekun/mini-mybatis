package com.mybatis.session;

import com.mybatis.exceptions.MyBatisException;
import com.mybatis.executor.Executor;
import com.mybatis.executor.SimpleExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * SqlSession的默认实现
 * 
 * SqlSession是MyBatis的核心接口，提供了执行SQL、获取Mapper、管理事务的功能。
 * 
 * 核心组件：
 * 1. Configuration - 全局配置
 * 2. Connection - 数据库连接
 * 3. Executor - SQL执行器
 * 
 * 设计特点：
 * 1. 非线程安全：每个线程都应该有自己的SqlSession实例
 * 2. 轻量级对象：创建和销毁的代价很小
 * 3. 必须关闭：使用完毕后必须调用close()方法释放资源
 * 
 * 最佳实践：
 * <pre>
 * try (SqlSession session = sqlSessionFactory.openSession()) {
 *     // 执行数据库操作
 *     User user = session.selectOne("UserMapper.selectById", 1);
 *     // 提交事务
 *     session.commit();
 * } // 自动关闭
 * </pre>
 * 
 * @author 学习者
 */
public class DefaultSqlSession implements SqlSession {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultSqlSession.class);
    
    /**
     * 全局配置对象
     */
    private Configuration configuration;
    
    /**
     * 数据库连接
     */
    private Connection connection;
    
    /**
     * SQL执行器
     */
    private Executor executor;
    
    /**
     * 是否自动提交事务
     */
    private boolean autoCommit;
    
    public DefaultSqlSession(Configuration configuration, boolean autoCommit) {
        this.configuration = configuration;
        this.autoCommit = autoCommit;
        
        // 获取数据库连接
        this.connection = getConnection();
        
        // 创建SQL执行器
        this.executor = new SimpleExecutor(configuration, connection);
        
        logger.debug("SqlSession创建成功 [autoCommit={}]", autoCommit);
    }
    
    /**
     * 获取数据库连接
     */
    private Connection getConnection() {
        try {
            // 加载数据库驱动
            Class.forName(configuration.getJdbcDriver());
            
            // 建立连接
            Connection conn = DriverManager.getConnection(
                configuration.getJdbcUrl(),
                configuration.getJdbcUsername(),
                configuration.getJdbcPassword()
            );
            
            // 设置事务提交模式
            conn.setAutoCommit(autoCommit);
            
            logger.debug("数据库连接建立成功");
            return conn;
            
        } catch (ClassNotFoundException e) {
            throw new MyBatisException("数据库驱动未找到: " + configuration.getJdbcDriver(), e);
        } catch (SQLException e) {
            throw new MyBatisException("获取数据库连接失败", e);
        }
    }
    
    /**
     * 查询单个对象
     */
    @Override
    public <T> T selectOne(String statementId, Object parameter) {
        logger.debug("执行selectOne: {}", statementId);
        List<T> list = selectList(statementId, parameter);
        
        if (list == null || list.isEmpty()) {
            return null;
        }
        
        if (list.size() > 1) {
            throw new MyBatisException("期望查询一条记录，但实际返回了" + list.size() + "条");
        }
        
        return list.get(0);
    }
    
    /**
     * 查询列表
     */
    @Override
    public <E> List<E> selectList(String statementId, Object parameter) {
        logger.debug("执行selectList: {}", statementId);
        try {
            return executor.query(statementId, parameter);
        } catch (SQLException e) {
            throw new MyBatisException("查询失败: " + statementId, e);
        }
    }
    
    /**
     * 插入操作
     */
    @Override
    public int insert(String statementId, Object parameter) {
        logger.debug("执行insert: {}", statementId);
        try {
            return executor.update(statementId, parameter);
        } catch (SQLException e) {
            throw new MyBatisException("插入失败: " + statementId, e);
        }
    }
    
    /**
     * 更新操作
     */
    @Override
    public int update(String statementId, Object parameter) {
        logger.debug("执行update: {}", statementId);
        try {
            return executor.update(statementId, parameter);
        } catch (SQLException e) {
            throw new MyBatisException("更新失败: " + statementId, e);
        }
    }
    
    /**
     * 删除操作
     */
    @Override
    public int delete(String statementId, Object parameter) {
        logger.debug("执行delete: {}", statementId);
        try {
            return executor.update(statementId, parameter);
        } catch (SQLException e) {
            throw new MyBatisException("删除失败: " + statementId, e);
        }
    }
    
    /**
     * 提交事务
     */
    @Override
    public void commit() {
        logger.debug("提交事务");
        try {
            if (!autoCommit && connection != null) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new MyBatisException("事务提交失败", e);
        }
    }
    
    /**
     * 回滚事务
     */
    @Override
    public void rollback() {
        logger.debug("回滚事务");
        try {
            if (!autoCommit && connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
            throw new MyBatisException("事务回滚失败", e);
        }
    }
    
    /**
     * 关闭会话，释放资源
     */
    @Override
    public void close() {
        logger.debug("关闭SqlSession");
        
        // 关闭执行器
        if (executor != null) {
            try {
                executor.close();
            } catch (SQLException e) {
                logger.error("关闭Executor失败", e);
            }
        }
        
        // 关闭数据库连接
        if (connection != null) {
            try {
                connection.close();
                logger.debug("数据库连接已关闭");
            } catch (SQLException e) {
                logger.error("关闭数据库连接失败", e);
            }
        }
    }
    
    /**
     * 获取Mapper代理对象
     * 
     * 这是MyBatis最核心的功能之一！
     * 通过动态代理，让Mapper接口"自动"拥有实现。
     * 
     * 工作原理：
     * 1. 从Configuration获取MapperRegistry
     * 2. MapperRegistry根据接口类型找到对应的MapperProxyFactory
     * 3. MapperProxyFactory使用JDK动态代理创建代理对象
     * 4. 返回代理对象给调用者
     * 
     * @param type Mapper接口类型
     * @param <T> Mapper类型
     * @return Mapper代理对象
     */
    @Override
    public <T> T getMapper(Class<T> type) {
        return configuration.getMapper(type, this);
    }
    
    /**
     * 获取Configuration配置对象
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
}

