package com.mybatis.executor;

import java.sql.SQLException;
import java.util.List;

/**
 * SQL执行器接口
 * 
 * Executor是MyBatis的核心组件，负责执行SQL语句。
 * 
 * 设计模式：模板方法模式
 * - 定义了SQL执行的基本流程
 * - 具体实现由子类完成
 * 
 * MyBatis中的三种执行器：
 * 1. SimpleExecutor（简单执行器）- 每次执行都创建新的Statement
 * 2. ReuseExecutor（复用执行器）- 复用Statement对象
 * 3. BatchExecutor（批量执行器）- 批量执行SQL，提高性能
 * 
 * 核心职责：
 * 1. 获取MappedStatement
 * 2. 处理SQL参数
 * 3. 执行SQL语句
 * 4. 处理结果集
 * 5. 管理缓存
 * 
 * 执行流程：
 * SqlSession → Executor → StatementHandler → JDBC
 * 
 * @author 学习者
 */
public interface Executor {
    
    /**
     * 执行查询操作
     * 
     * @param statementId SQL语句ID
     * @param parameter 参数对象
     * @param <E> 结果类型
     * @return 查询结果列表
     * @throws SQLException SQL异常
     */
    <E> List<E> query(String statementId, Object parameter) throws SQLException;
    
    /**
     * 执行更新操作（INSERT、UPDATE、DELETE）
     * 
     * @param statementId SQL语句ID
     * @param parameter 参数对象
     * @return 影响的行数
     * @throws SQLException SQL异常
     */
    int update(String statementId, Object parameter) throws SQLException;
    
    /**
     * 提交事务
     * 
     * @throws SQLException SQL异常
     */
    void commit() throws SQLException;
    
    /**
     * 回滚事务
     * 
     * @throws SQLException SQL异常
     */
    void rollback() throws SQLException;
    
    /**
     * 关闭执行器，释放资源
     * 
     * @throws SQLException SQL异常
     */
    void close() throws SQLException;
}

