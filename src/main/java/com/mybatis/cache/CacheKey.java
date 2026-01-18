package com.mybatis.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 缓存Key
 * 
 * CacheKey用于标识一次查询操作，判断两次查询是否相同。
 * 
 * 缓存Key的组成：
 * 1. MappedStatement的ID
 * 2. 分页参数（offset、limit）
 * 3. SQL语句
 * 4. 参数值
 * 5. Environment ID
 * 
 * 判断相等的条件：
 * - hashCode相同
 * - checksum相同
 * - count相同
 * - updateList中的每个元素都相同
 * 
 * 示例：
 * <pre>
 * CacheKey key = new CacheKey();
 * key.update(statementId);    // "UserMapper.selectById"
 * key.update(offset);          // 0
 * key.update(limit);           // Integer.MAX_VALUE
 * key.update(sql);             // "SELECT * FROM user WHERE id = ?"
 * key.update(parameterValue);  // 1L
 * </pre>
 * 
 * @author 学习者
 */
public class CacheKey implements Cloneable, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 默认哈希码
     */
    public static final CacheKey NULL_CACHE_KEY = new CacheKey() {
        @Override
        public void update(Object object) {
            throw new UnsupportedOperationException("Not allowed to update NULL_CACHE_KEY");
        }
        
        @Override
        public void updateAll(Object[] objects) {
            throw new UnsupportedOperationException("Not allowed to update NULL_CACHE_KEY");
        }
    };
    
    private static final int DEFAULT_MULTIPLIER = 37;
    private static final int DEFAULT_HASHCODE = 17;
    
    /**
     * 倍数（用于计算hashCode）
     */
    private final int multiplier;
    
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
     * 更新列表（存储所有影响缓存的因素）
     */
    private List<Object> updateList;
    
    public CacheKey() {
        this.hashcode = DEFAULT_HASHCODE;
        this.multiplier = DEFAULT_MULTIPLIER;
        this.count = 0;
        this.updateList = new ArrayList<>();
    }
    
    public CacheKey(Object[] objects) {
        this();
        updateAll(objects);
    }
    
    public int getUpdateCount() {
        return count;
    }
    
    /**
     * 更新缓存Key
     * 
     * 将影响缓存的因素添加到updateList中，
     * 并更新hashcode和checksum。
     * 
     * @param object 影响缓存的因素
     */
    public void update(Object object) {
        int baseHashCode = object == null ? 1 : getHashCode(object);
        
        count++;
        checksum += baseHashCode;
        baseHashCode *= count;
        
        hashcode = multiplier * hashcode + baseHashCode;
        
        updateList.add(object);
    }
    
    public void updateAll(Object[] objects) {
        for (Object o : objects) {
            update(o);
        }
    }
    
    /**
     * 判断两个CacheKey是否相等
     * 
     * 相等条件：
     * 1. hashcode相同
     * 2. checksum相同
     * 3. count相同
     * 4. updateList中的每个元素都相同
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CacheKey)) {
            return false;
        }
        
        final CacheKey cacheKey = (CacheKey) object;
        
        if (hashcode != cacheKey.hashcode) {
            return false;
        }
        if (checksum != cacheKey.checksum) {
            return false;
        }
        if (count != cacheKey.count) {
            return false;
        }
        
        for (int i = 0; i < updateList.size(); i++) {
            Object thisObject = updateList.get(i);
            Object thatObject = cacheKey.updateList.get(i);
            if (!equals(thisObject, thatObject)) {
                return false;
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
        StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
        for (Object object : updateList) {
            returnValue.append(':').append(object);
        }
        return returnValue.toString();
    }
    
    @Override
    public CacheKey clone() throws CloneNotSupportedException {
        CacheKey clonedCacheKey = (CacheKey) super.clone();
        clonedCacheKey.updateList = new ArrayList<>(updateList);
        return clonedCacheKey;
    }
    
    private int getHashCode(Object object) {
        if (object == null) {
            return 0;
        }
        
        if (object.getClass().isArray()) {
            // 数组类型
            if (object instanceof Object[]) {
                int hash = 0;
                for (Object o : (Object[]) object) {
                    hash += getHashCode(o);
                }
                return hash;
            }
            // 基本类型数组
            if (object instanceof int[]) {
                int hash = 0;
                for (int o : (int[]) object) {
                    hash += o;
                }
                return hash;
            }
            // 其他类型数组，简化处理
            return object.hashCode();
        }
        
        return object.hashCode();
    }
    
    private boolean equals(Object thisObject, Object thatObject) {
        if (thisObject == null) {
            return thatObject == null;
        } else if (thatObject == null) {
            return false;
        }
        
        if (thisObject.getClass().isArray()) {
            // 数组类型的比较
            if (thisObject instanceof Object[] && thatObject instanceof Object[]) {
                Object[] thisArray = (Object[]) thisObject;
                Object[] thatArray = (Object[]) thatObject;
                
                if (thisArray.length != thatArray.length) {
                    return false;
                }
                
                for (int i = 0; i < thisArray.length; i++) {
                    if (!equals(thisArray[i], thatArray[i])) {
                        return false;
                    }
                }
                
                return true;
            }
            
            // 基本类型数组，直接用equals
            return thisObject.equals(thatObject);
        }
        
        return thisObject.equals(thatObject);
    }
}
