package com.mybatis.mapping;

import com.mybatis.session.Configuration;

/**
 * MappedStatement - SQL语句映射对象
 * 
 * MappedStatement是MyBatis中最重要的类之一，它代表了一条SQL语句的完整信息。
 * 每个<select>、<insert>、<update>、<delete>标签都会被解析成一个MappedStatement对象。
 * 
 * 包含的信息：
 * 1. SQL语句本身
 * 2. SQL类型（SELECT、INSERT、UPDATE、DELETE）
 * 3. 参数类型
 * 4. 返回值类型
 * 5. 缓存配置
 * 6. 结果映射配置
 * 
 * 生命周期：
 * - 创建：在配置文件解析阶段创建
 * - 存储：保存在Configuration的mappedStatements集合中
 * - 使用：SQL执行时根据statementId获取
 * 
 * @author 学习者
 */
public class MappedStatement {
    
    /**
     * 全局配置对象
     */
    private Configuration configuration;
    
    /**
     * SQL语句的唯一标识：namespace.id
     * 例如：com.mybatis.mapper.UserMapper.selectById
     */
    private String id;
    
    /**
     * SQL类型：SELECT、INSERT、UPDATE、DELETE
     */
    private SqlCommandType sqlCommandType;
    
    /**
     * SQL语句（可能包含#{}占位符）
     */
    private String sql;
    
    /**
     * 参数类型
     * 例如：java.lang.Long, com.mybatis.entity.User
     */
    private Class<?> parameterType;
    
    /**
     * 返回值类型
     * 例如：com.mybatis.entity.User, java.util.Map
     */
    private Class<?> resultType;
    
    /**
     * 是否使用缓存
     */
    private boolean useCache = true;
    
    /**
     * 私有构造方法，使用Builder模式创建对象
     */
    private MappedStatement() {
    }
    
    // ==================== Getter方法 ====================
    
    public Configuration getConfiguration() {
        return configuration;
    }
    
    public String getId() {
        return id;
    }
    
    public SqlCommandType getSqlCommandType() {
        return sqlCommandType;
    }
    
    public String getSql() {
        return sql;
    }
    
    public Class<?> getParameterType() {
        return parameterType;
    }
    
    public Class<?> getResultType() {
        return resultType;
    }
    
    public boolean isUseCache() {
        return useCache;
    }
    
    // ==================== Builder建造者模式 ====================
    
    /**
     * Builder建造者
     * 
     * 设计模式：建造者模式
     * - MappedStatement的属性很多，使用建造者模式可以灵活地构建对象
     * - 提高代码的可读性和可维护性
     */
    public static class Builder {
        private MappedStatement mappedStatement = new MappedStatement();
        
        public Builder(Configuration configuration, String id, SqlCommandType sqlCommandType) {
            mappedStatement.configuration = configuration;
            mappedStatement.id = id;
            mappedStatement.sqlCommandType = sqlCommandType;
        }
        
        public Builder sql(String sql) {
            mappedStatement.sql = sql;
            return this;
        }
        
        public Builder parameterType(Class<?> parameterType) {
            mappedStatement.parameterType = parameterType;
            return this;
        }
        
        public Builder resultType(Class<?> resultType) {
            mappedStatement.resultType = resultType;
            return this;
        }
        
        public Builder useCache(boolean useCache) {
            mappedStatement.useCache = useCache;
            return this;
        }
        
        public MappedStatement build() {
            return mappedStatement;
        }
    }
}

