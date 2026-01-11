package com.mybatis.session;

import com.mybatis.binding.MapperRegistry;
import com.mybatis.mapping.MappedStatement;
import com.mybatis.plugin.InterceptorChain;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置类 - MyBatis的核心配置中心
 * 
 * 这个类是MyBatis的心脏，保存了所有的配置信息：
 * 1. 数据源配置
 * 2. Mapper映射配置
 * 3. SQL语句映射
 * 4. 类型处理器
 * 5. 插件配置
 * 
 * 设计模式：单例模式 - 全局唯一的配置对象
 * 
 * @author 学习者
 */
public class Configuration {
    
    /**
     * Mapper注册中心
     */
    private MapperRegistry mapperRegistry = new MapperRegistry(this);
    
    /**
     * 拦截器链
     */
    private InterceptorChain interceptorChain = new InterceptorChain();
    
    /**
     * MappedStatement映射
     * key: namespace.id (例如: com.mybatis.mapper.UserMapper.selectById)
     * value: MappedStatement对象(包含SQL、参数类型、返回类型等)
     */
    private Map<String, MappedStatement> mappedStatements = new HashMap<>();
    
    /**
     * 数据库连接信息
     */
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    
    /**
     * 是否开启二级缓存
     */
    private boolean cacheEnabled = true;
    
    /**
     * 是否开启延迟加载
     */
    private boolean lazyLoadingEnabled = false;
    
    /**
     * 默认执行器类型: SIMPLE, REUSE, BATCH
     */
    private String defaultExecutorType = "SIMPLE";
    
    // ==================== MappedStatement相关 ====================
    
    public void addMappedStatement(String key, MappedStatement statement) {
        mappedStatements.put(key, statement);
    }
    
    public MappedStatement getMappedStatement(String key) {
        return mappedStatements.get(key);
    }
    
    public boolean hasMappedStatement(String key) {
        return mappedStatements.containsKey(key);
    }
    
    // ==================== 数据库配置相关 ====================
    
    public String getJdbcDriver() {
        return jdbcDriver;
    }
    
    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }
    
    public String getJdbcUrl() {
        return jdbcUrl;
    }
    
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }
    
    public String getJdbcUsername() {
        return jdbcUsername;
    }
    
    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }
    
    public String getJdbcPassword() {
        return jdbcPassword;
    }
    
    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }
    
    // ==================== 其他配置 ====================
    
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
    
    public boolean isLazyLoadingEnabled() {
        return lazyLoadingEnabled;
    }
    
    public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
        this.lazyLoadingEnabled = lazyLoadingEnabled;
    }
    
    public String getDefaultExecutorType() {
        return defaultExecutorType;
    }
    
    public void setDefaultExecutorType(String defaultExecutorType) {
        this.defaultExecutorType = defaultExecutorType;
    }
    
    // ==================== Mapper注册相关 ====================
    
    /**
     * 添加Mapper接口
     */
    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }
    
    /**
     * 获取Mapper代理对象
     */
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        return mapperRegistry.getMapper(type, sqlSession);
    }
    
    /**
     * 判断Mapper是否已注册
     */
    public boolean hasMapper(Class<?> type) {
        return mapperRegistry.hasMapper(type);
    }
    
    // ==================== 拦截器相关 ====================
    
    /**
     * 获取拦截器链
     */
    public InterceptorChain getInterceptorChain() {
        return interceptorChain;
    }
}

