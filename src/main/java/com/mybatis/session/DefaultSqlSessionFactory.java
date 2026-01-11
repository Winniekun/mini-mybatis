package com.mybatis.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SqlSessionFactory的默认实现
 * 
 * 负责创建SqlSession对象，管理数据库连接。
 * 
 * 设计特点：
 * 1. 线程安全：可以被多个线程共享
 * 2. 全局唯一：通常在应用中只创建一次
 * 3. 重量级对象：包含了所有配置信息和数据库连接池
 * 
 * 生命周期：
 * - 应用启动时创建
 * - 应用运行期间一直存在
 * - 应用关闭时销毁
 * 
 * @author 学习者
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultSqlSessionFactory.class);
    
    /**
     * 全局配置对象
     */
    private Configuration configuration;
    
    public DefaultSqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
        logger.info("SqlSessionFactory创建成功");
    }
    
    /**
     * 打开一个SqlSession（默认不自动提交）
     * 
     * @return SqlSession对象
     */
    @Override
    public SqlSession openSession() {
        return openSession(false);
    }
    
    /**
     * 打开一个SqlSession
     * 
     * 每次调用都会：
     * 1. 创建一个新的SqlSession对象
     * 2. 从数据源获取一个数据库连接
     * 3. 设置事务的自动提交模式
     * 
     * @param autoCommit 是否自动提交事务
     * @return SqlSession对象
     */
    @Override
    public SqlSession openSession(boolean autoCommit) {
        logger.debug("打开SqlSession [autoCommit={}]", autoCommit);
        return new DefaultSqlSession(configuration, autoCommit);
    }
    
    /**
     * 获取Configuration配置对象
     * 
     * @return Configuration对象
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
}

