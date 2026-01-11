package com.mybatis.session;

/**
 * SqlSessionFactory接口 - SqlSession工厂
 * 
 * SqlSessionFactory是MyBatis的核心组件，负责创建SqlSession对象。
 * 
 * 设计模式：工厂模式
 * - 将对象的创建逻辑封装起来
 * - 客户端不需要知道SqlSession的具体实现类
 * 
 * 生命周期：
 * - 创建：应用启动时通过SqlSessionFactoryBuilder创建
 * - 作用域：应用级别，全局唯一（单例）
 * - 销毁：随应用结束而销毁
 * 
 * 线程安全：SqlSessionFactory是线程安全的，可以被多个线程共享使用
 * 
 * 使用示例：
 * <pre>
 * // 1. 创建SqlSessionFactory（通常在应用启动时创建一次）
 * SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(inputStream);
 * 
 * // 2. 获取SqlSession
 * SqlSession session = factory.openSession();
 * 
 * // 3. 执行SQL
 * User user = session.selectOne("UserMapper.selectById", 1);
 * 
 * // 4. 关闭会话
 * session.close();
 * </pre>
 * 
 * @author 学习者
 */
public interface SqlSessionFactory {
    
    /**
     * 打开一个SqlSession
     * 
     * 默认行为：
     * - 不自动提交事务（autoCommit=false）
     * - 使用默认的执行器类型（SIMPLE）
     * - 从数据源获取一个数据库连接
     * 
     * @return SqlSession对象
     */
    SqlSession openSession();
    
    /**
     * 打开一个SqlSession，可以指定是否自动提交
     * 
     * @param autoCommit true表示自动提交事务，false表示需要手动提交
     * @return SqlSession对象
     */
    SqlSession openSession(boolean autoCommit);
    
    /**
     * 获取Configuration配置对象
     * 
     * @return Configuration对象
     */
    Configuration getConfiguration();
}

