package com.mybatis.executor.statement;

import com.mybatis.executor.resultset.ResultSetHandler;
import com.mybatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDBC Statement处理器
 * 
 * StatementHandler是MyBatis四大对象之一，负责处理JDBC的Statement对象。
 * 
 * 核心职责：
 * 1. 创建Statement对象
 * 2. 设置SQL参数
 * 3. 执行SQL语句
 * 4. 调用ResultSetHandler处理结果集
 * 
 * MyBatis中的Statement类型：
 * 1. SimpleStatementHandler - 处理不带参数的SQL
 * 2. PreparedStatementHandler - 处理带参数的SQL（最常用）
 * 3. CallableStatementHandler - 处理存储过程调用
 * 
 * 我们这里实现最常用的PreparedStatement处理逻辑。
 * 
 * @author 学习者
 */
public class StatementHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StatementHandler.class);
    
    /**
     * 全局配置对象
     */
    private Configuration configuration;
    
    /**
     * 结果集处理器
     */
    private ResultSetHandler resultSetHandler;
    
    /**
     * #{} 占位符的正则表达式
     * 匹配 #{id}、#{name}、#{user.id} 等格式
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{([^}]+)\\}");
    
    public StatementHandler(Configuration configuration) {
        this.configuration = configuration;
        this.resultSetHandler = new ResultSetHandler();
    }
    
    /**
     * 准备Statement对象
     * 
     * 将SQL中的#{param}占位符替换为?，创建PreparedStatement
     * 
     * 例如：
     * 输入: SELECT * FROM user WHERE id = #{id}
     * 输出: SELECT * FROM user WHERE id = ?
     * 
     * @param connection 数据库连接
     * @param sql 原始SQL（包含#{param}占位符）
     * @return PreparedStatement对象
     */
    public Statement prepare(Connection connection, String sql) throws SQLException {
        // 将#{param}替换为?
        String preparedSql = sql.replaceAll("#\\{[^}]+\\}", "?");
        
        logger.debug("原始SQL: {}", sql);
        logger.debug("预编译SQL: {}", preparedSql);
        
        // 创建PreparedStatement
        return connection.prepareStatement(preparedSql);
    }
    
    /**
     * 设置SQL参数
     * 
     * 支持两种参数类型：
     * 1. 简单类型（String、Integer、Long等）- 直接设置到第一个占位符
     * 2. 复杂类型（JavaBean、Map）- 根据属性名匹配占位符
     * 
     * 参数处理是MyBatis的核心功能之一，真实的MyBatis会有更复杂的参数处理逻辑。
     * 
     * @param statement PreparedStatement对象
     * @param parameter 参数对象
     */
    public void parameterize(Statement statement, Object parameter) throws SQLException {
        if (parameter == null) {
            return;
        }
        
        PreparedStatement ps = (PreparedStatement) statement;
        
        // 简单类型直接设置
        if (isSimpleType(parameter)) {
            logger.debug("设置参数: [1] = {}", parameter);
            ps.setObject(1, parameter);
        } else {
            // TODO: 复杂类型的参数处理将在后续优化
            // 需要解析SQL中的#{param}，然后通过反射获取对象的属性值
            logger.warn("暂不支持复杂类型参数: {}", parameter.getClass());
        }
    }
    
    /**
     * 执行查询
     * 
     * @param statement Statement对象
     * @param resultType 返回类型
     * @param <E> 结果元素类型
     * @return 查询结果列表
     */
    public <E> List<E> query(Statement statement, Class<?> resultType) throws SQLException {
        // 执行查询
        PreparedStatement ps = (PreparedStatement) statement;
        ResultSet resultSet = ps.executeQuery();
        
        logger.debug("SQL执行成功，开始处理结果集");
        
        // 处理结果集
        List<E> result = resultSetHandler.handleResultSet(resultSet, resultType);
        
        // 关闭结果集
        resultSet.close();
        
        return result;
    }
    
    /**
     * 执行更新（INSERT、UPDATE、DELETE）
     * 
     * @param statement Statement对象
     * @return 影响的行数
     */
    public int update(Statement statement) throws SQLException {
        PreparedStatement ps = (PreparedStatement) statement;
        int rows = ps.executeUpdate();
        
        logger.debug("SQL执行成功，影响{}行", rows);
        
        return rows;
    }
    
    /**
     * 判断是否为简单类型
     * 
     * 简单类型包括：
     * - 基本数据类型及其包装类
     * - String
     * - Date
     * - 等等
     */
    private boolean isSimpleType(Object obj) {
        if (obj == null) {
            return true;
        }
        
        Class<?> clazz = obj.getClass();
        
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Boolean.class ||
               clazz == Byte.class ||
               clazz == Short.class ||
               clazz == Character.class ||
               Number.class.isAssignableFrom(clazz) ||
               java.util.Date.class.isAssignableFrom(clazz);
    }
}

