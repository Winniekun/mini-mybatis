package com.mybatis.binding;

import com.mybatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

/**
 * Mapper代理工厂
 * 
 * 专门用于创建Mapper接口的代理对象。
 * 
 * 设计模式：工厂模式
 * - 封装代理对象的创建过程
 * - 为每个Mapper接口提供统一的创建方式
 * 
 * 为什么需要工厂？
 * 1. 创建代理对象的代码比较复杂（JDK动态代理的API）
 * 2. 需要为不同的SqlSession创建不同的代理对象
 * 3. 工厂模式让代码更清晰、更易维护
 * 
 * @author 学习者
 */
public class MapperProxyFactory<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(MapperProxyFactory.class);
    
    /**
     * Mapper接口的Class对象
     */
    private Class<T> mapperInterface;
    
    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
    
    /**
     * 创建Mapper代理对象
     * 
     * 使用JDK动态代理创建接口的代理实现。
     * 
     * JDK动态代理三要素：
     * 1. ClassLoader：类加载器
     * 2. Interfaces：要代理的接口列表
     * 3. InvocationHandler：方法调用处理器
     * 
     * @param sqlSession SqlSession对象
     * @return Mapper代理对象
     */
    @SuppressWarnings("unchecked")
    public T newInstance(SqlSession sqlSession) {
        logger.debug("创建Mapper代理对象: {}", mapperInterface.getName());
        
        // 创建MapperProxy实例
        MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface);
        
        // 使用JDK动态代理创建代理对象
        return (T) Proxy.newProxyInstance(
            mapperInterface.getClassLoader(),    // 类加载器
            new Class[]{mapperInterface},         // 接口列表
            mapperProxy                           // InvocationHandler
        );
    }
    
    public Class<T> getMapperInterface() {
        return mapperInterface;
    }
}

