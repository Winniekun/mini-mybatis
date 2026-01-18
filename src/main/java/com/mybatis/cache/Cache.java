package com.mybatis.cache;

/**
 * 缓存接口
 * 
 * MyBatis的缓存体系基于此接口。
 * 
 * 缓存层次：
 * 1. 一级缓存（Session级别）：默认开启，使用PerpetualCache实现
 * 2. 二级缓存（Mapper级别）：需要配置，可以有多种实现
 * 
 * 设计模式：装饰器模式
 * - 基础实现：PerpetualCache
 * - 装饰器：LruCache、FifoCache、SoftCache等
 * 
 * @author 学习者
 */
public interface Cache {
    
    /**
     * 获取缓存ID
     */
    String getId();
    
    /**
     * 存入缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    void putObject(Object key, Object value);
    
    /**
     * 从缓存获取
     * 
     * @param key 缓存键
     * @return 缓存值，不存在返回null
     */
    Object getObject(Object key);
    
    /**
     * 移除缓存
     * 
     * @param key 缓存键
     * @return 被移除的值
     */
    Object removeObject(Object key);
    
    /**
     * 清空缓存
     */
    void clear();
    
    /**
     * 获取缓存大小
     */
    int getSize();
}
