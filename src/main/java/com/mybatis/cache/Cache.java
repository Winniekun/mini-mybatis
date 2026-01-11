package com.mybatis.cache;

/**
 * 缓存接口
 * 
 * MyBatis的缓存系统基于这个接口设计。
 * 
 * 缓存层级：
 * 1. 一级缓存（Local Cache）- SqlSession级别，默认开启
 * 2. 二级缓存（Second Level Cache）- Mapper级别，需要手动开启
 * 
 * 缓存策略：
 * - LRU (Least Recently Used) - 最近最少使用
 * - FIFO (First In First Out) - 先进先出
 * - SOFT (Soft Reference) - 软引用
 * - WEAK (Weak Reference) - 弱引用
 * 
 * 设计模式：装饰器模式
 * - PerpetualCache（基础缓存） → LruCache（LRU装饰） → 其他装饰
 * 
 * @author 学习者
 */
public interface Cache {
    
    /**
     * 获取缓存ID
     * 
     * @return 缓存ID（通常是namespace）
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
     * 获取缓存
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
     * 
     * @return 缓存项数量
     */
    int getSize();
}

