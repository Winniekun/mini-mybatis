package com.mybatis.binding;

import com.mybatis.exceptions.MyBatisException;
import com.mybatis.session.Configuration;
import com.mybatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper注册中心
 * 
 * 管理所有Mapper接口的代理工厂，提供Mapper注册和获取服务。
 * 
 * 设计模式：注册表模式
 * - 维护一个注册表（Map），存储Mapper接口和对应的代理工厂
 * - 提供注册和查询功能
 * 
 * 核心功能：
 * 1. 注册Mapper接口
 * 2. 创建Mapper代理对象
 * 3. 管理所有Mapper的生命周期
 * 
 * 工作流程：
 * 1. 配置解析阶段：将所有Mapper接口注册到Registry
 * 2. 运行阶段：根据接口类型从Registry获取代理对象
 * 
 * @author 学习者
 */
public class MapperRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(MapperRegistry.class);
    
    /**
     * Configuration对象
     */
    private Configuration configuration;
    
    /**
     * Mapper注册表
     * key: Mapper接口的Class对象
     * value: Mapper代理工厂
     */
    private Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();
    
    public MapperRegistry(Configuration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * 获取Mapper代理对象
     * 
     * @param type Mapper接口类型
     * @param sqlSession SqlSession对象
     * @param <T> Mapper类型
     * @return Mapper代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        // 从注册表中获取对应的工厂
        MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
        
        if (mapperProxyFactory == null) {
            throw new MyBatisException("Mapper未注册: " + type.getName() + 
                "。请检查是否在配置文件中配置了对应的Mapper.xml");
        }
        
        // 使用工厂创建代理对象
        return mapperProxyFactory.newInstance(sqlSession);
    }
    
    /**
     * 注册Mapper接口
     * 
     * @param type Mapper接口类型
     * @param <T> Mapper类型
     */
    public <T> void addMapper(Class<T> type) {
        // 只能注册接口
        if (!type.isInterface()) {
            throw new MyBatisException("只能注册接口类型: " + type.getName());
        }
        
        // 检查是否已经注册
        if (knownMappers.containsKey(type)) {
            logger.warn("Mapper已经注册: {}", type.getName());
            return;
        }
        
        // 创建代理工厂并注册
        MapperProxyFactory<T> factory = new MapperProxyFactory<>(type);
        knownMappers.put(type, factory);
        
        logger.info("注册Mapper: {}", type.getName());
    }
    
    /**
     * 判断Mapper是否已注册
     * 
     * @param type Mapper接口类型
     * @return 是否已注册
     */
    public boolean hasMapper(Class<?> type) {
        return knownMappers.containsKey(type);
    }
    
    /**
     * 获取已注册的Mapper数量
     * 
     * @return Mapper数量
     */
    public int getMapperCount() {
        return knownMappers.size();
    }
}

