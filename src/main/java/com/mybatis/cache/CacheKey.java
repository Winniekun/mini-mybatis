package com.mybatis.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存键
 * 
 * CacheKey是MyBatis缓存系统的核心类，用于生成缓存的唯一标识。
 * 
 * 缓存键的组成：
 * 1. statementId - SQL语句ID
 * 2. offset - 分页偏移量
 * 3. limit - 分页大小
 * 4. sql - SQL语句
 * 5. parameterValues - 参数值
 * 6. environment - 环境ID
 * 
 * 为什么不直接用String？
 * - 参数可能是复杂对象，需要计算hashCode
 * - 需要支持动态添加更新因子
 * - 需要高效的equals和hashCode实现
 * 
 * 设计模式：值对象模式
 * - 通过多个属性的组合生成唯一标识
 * 
 * @author 学习者
 */
public class CacheKey {
    
    /**
     * 默认哈希值
     */
    private static final int DEFAULT_HASHCODE = 17;
    
    /**
     * 乘数因子
     */
    private static final int DEFAULT_MULTIPLIER = 37;
    
    /**
     * 哈希码
     */
    private int hashcode;
    
    /**
     * 校验和
     */
    private long checksum;
    
    /**
     * 更新次数
     */
    private int count;
    
    /**
     * 更新因子列表
     */
    private List<Object> updateList;
    
    public CacheKey() {
        this.hashcode = DEFAULT_HASHCODE;
        this.checksum = 0;
        this.count = 0;
        this.updateList = new ArrayList<>();
    }
    
    /**
     * 添加更新因子
     * 
     * 每添加一个因子，都会更新hashcode和checksum
     * 
     * @param object 更新因子
     */
    public void update(Object object) {
        if (object == null) {
            object = "null";
        }
        
        int baseHashCode = object.hashCode();
        
        // 更新计数
        count++;
        
        // 更新校验和
        checksum += baseHashCode;
        
        // 更新哈希码
        baseHashCode *= count;
        hashcode = DEFAULT_MULTIPLIER * hashcode + baseHashCode;
        
        // 添加到更新列表
        updateList.add(object);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CacheKey)) {
            return false;
        }
        
        CacheKey other = (CacheKey) obj;
        
        // 快速比较
        if (hashcode != other.hashcode) {
            return false;
        }
        if (checksum != other.checksum) {
            return false;
        }
        if (count != other.count) {
            return false;
        }
        
        // 详细比较每个更新因子
        for (int i = 0; i < updateList.size(); i++) {
            Object thisObject = updateList.get(i);
            Object otherObject = other.updateList.get(i);
            
            if (thisObject == null) {
                if (otherObject != null) {
                    return false;
                }
            } else {
                if (!thisObject.equals(otherObject)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        return hashcode;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CacheKey{");
        builder.append("hashcode=").append(hashcode);
        builder.append(", checksum=").append(checksum);
        builder.append(", count=").append(count);
        builder.append(", updateList=").append(updateList);
        builder.append('}');
        return builder.toString();
    }
}

