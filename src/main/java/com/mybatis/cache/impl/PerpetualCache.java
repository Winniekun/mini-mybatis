package com.mybatis.cache.impl;

import com.mybatis.cache.Cache;

import java.util.HashMap;
import java.util.Map;

/**
 * 永久缓存实现
 * 
 * PerpetualCache是MyBatis默认的缓存实现，基于HashMap。
 * 
 * 特点：
 * 1. 简单直接，基于HashMap
 * 2. 没有大小限制
 * 3. 没有过期时间
 * 4. 线程不安全（需要外部同步）
 * 
 * 使用场景：
 * - 一级缓存（SqlSession级别）
 * - 二级缓存的基础实现
 * 
 * @author 学习者
 */
public class PerpetualCache implements Cache {
    
    /**
     * 缓存ID
     */
    private final String id;
    
    /**
     * 缓存容器：简单的HashMap
     */
    private final Map<Object, Object> cache = new HashMap<>();
    
    public PerpetualCache(String id) {
        this.id = id;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void putObject(Object key, Object value) {
        cache.put(key, value);
    }
    
    @Override
    public Object getObject(Object key) {
        return cache.get(key);
    }
    
    @Override
    public Object removeObject(Object key) {
        return cache.remove(key);
    }
    
    @Override
    public void clear() {
        cache.clear();
    }
    
    @Override
    public int getSize() {
        return cache.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (getId() == null) {
            throw new IllegalStateException("Cache instances require an ID.");
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cache)) {
            return false;
        }
        
        Cache otherCache = (Cache) o;
        return getId().equals(otherCache.getId());
    }
    
    @Override
    public int hashCode() {
        if (getId() == null) {
            throw new IllegalStateException("Cache instances require an ID.");
        }
        return getId().hashCode();
    }
}
