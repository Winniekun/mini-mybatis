package com.mybatis.session;

import java.util.List;

/**
 * SqlSession接口 - MyBatis的核心API
 * 
 * SqlSession是应用程序与MyBatis交互的主要接口，提供了执行SQL、获取Mapper、管理事务的方法。
 * 
 * 核心职责：
 * 1. 执行SQL语句（增删改查）
 * 2. 获取Mapper代理对象
 * 3. 事务管理（提交、回滚）
 * 4. 会话管理（关闭）
 * 
 * 设计模式：外观模式 - 为复杂的子系统提供简单的接口
 * 
 * 生命周期：
 * - 创建：通过SqlSessionFactory.openSession()创建
 * - 使用：执行SQL或获取Mapper
 * - 销毁：使用完毕后必须close()，释放数据库连接
 * 
 * 线程安全：SqlSession不是线程安全的，不能被共享，最佳作用域是请求或方法作用域
 * 
 * @author 学习者
 */
public interface SqlSession {
    
    /**
     * 查询单个对象
     * 
     * @param statementId SQL语句的唯一标识 (namespace.id)
     * @param parameter 参数对象
     * @param <T> 返回类型
     * @return 查询结果对象
     */
    <T> T selectOne(String statementId, Object parameter);
    
    /**
     * 查询列表
     * 
     * @param statementId SQL语句的唯一标识
     * @param parameter 参数对象
     * @param <E> 列表元素类型
     * @return 查询结果列表
     */
    <E> List<E> selectList(String statementId, Object parameter);
    
    /**
     * 插入操作
     * 
     * @param statementId SQL语句的唯一标识
     * @param parameter 参数对象
     * @return 影响的行数
     */
    int insert(String statementId, Object parameter);
    
    /**
     * 更新操作
     * 
     * @param statementId SQL语句的唯一标识
     * @param parameter 参数对象
     * @return 影响的行数
     */
    int update(String statementId, Object parameter);
    
    /**
     * 删除操作
     * 
     * @param statementId SQL语句的唯一标识
     * @param parameter 参数对象
     * @return 影响的行数
     */
    int delete(String statementId, Object parameter);
    
    /**
     * 提交事务
     */
    void commit();
    
    /**
     * 回滚事务
     */
    void rollback();
    
    /**
     * 关闭会话，释放资源
     * 注意：使用完SqlSession后必须调用此方法，建议使用try-with-resources
     */
    void close();
    
    /**
     * 获取Mapper代理对象
     * 
     * 这是MyBatis的核心功能之一，通过动态代理为Mapper接口创建实现类
     * 
     * 原理：
     * 1. 使用JDK动态代理创建Mapper接口的代理对象
     * 2. 代理对象拦截方法调用
     * 3. 根据方法签名找到对应的MappedStatement
     * 4. 执行SQL并返回结果
     * 
     * @param type Mapper接口的Class对象
     * @param <T> Mapper接口类型
     * @return Mapper代理对象
     */
    <T> T getMapper(Class<T> type);
    
    /**
     * 获取Configuration配置对象
     * 
     * @return Configuration对象
     */
    Configuration getConfiguration();
}

