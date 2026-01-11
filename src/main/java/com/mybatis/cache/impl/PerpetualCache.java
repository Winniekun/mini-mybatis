package com.mybatis.cache.impl;

import com.mybatis.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 永久缓存（基础实现）
 * 
 * 这是MyBatis缓存的基础实现，使用HashMap存储数据。
 * 
 * 特点：
 * 1. 简单高效
 * 2. 没有过期策略
 * 3. 没有容量限制
 * 4. 可以作为装饰器模式的基础
 * 
 * 命名由来：Perpetual = 永久的，表示缓存永不过期
 * 
 * 应用场景：
 * - 一级缓存的底层实现
 * - 作为其他缓存装饰器的基础
 * 
 * @author 学习者
 */
public class PerpetualCache implements Cache {
    
    private static final Logger logger = LoggerFactory.getLogger(PerpetualCache.class);
    
    /**
     * 缓存ID
     */
    private String id;
    
    /**
     * 缓存存储
     */
    private Map<Object, Object> cache = new HashMap<>();
    
    public PerpetualCache(String id) {
        this.id = id;
        logger.debug("创建缓存: {}", id);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void putObject(Object key, Object value) {
        cache.put(key, value);
        logger.trace("缓存存入: {} = {}", key, value);
    }
    
    @Override
    public Object getObject(Object key) {
        Object value = cache.get(key);
        if (value != null) {
            logger.trace("缓存命中: {} = {}", key, value);
        } else {
            logger.trace("缓存未命中: {}", key);
        }
        return value;
    }
    
    @Override
    public Object removeObject(Object key) {
        Object value = cache.remove(key);
        logger.trace("缓存移除: {} = {}", key, value);
        return value;
    }
    
    @Override
    public void clear() {
        cache.clear();
        logger.debug("清空缓存: {}", id);
    }
    
    @Override
    public int getSize() {
        return cache.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerpetualCache that = (PerpetualCache) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

