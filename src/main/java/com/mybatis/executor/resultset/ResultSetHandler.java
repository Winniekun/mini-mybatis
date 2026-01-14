package com.mybatis.executor.resultset;

import com.mybatis.exceptions.MyBatisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 结果集处理器
 * 
 * ResultSetHandler是MyBatis四大对象之一，负责将JDBC的ResultSet转换为Java对象。
 * 
 * 核心职责：
 * 1. 遍历ResultSet
 * 2. 创建结果对象
 * 3. 将数据库字段映射到对象属性
 * 4. 处理类型转换
 * 
 * 映射策略：
 * 1. 自动映射：根据字段名自动匹配属性（支持下划线转驼峰）
 * 2. 手动映射：通过<resultMap>标签指定映射关系（后续实现）
 * 
 * 类型转换：
 * - 数据库类型 → JDBC类型 → Java类型
 * - 例如: MySQL的VARCHAR → String
 * 
 * @author 学习者
 */
public class ResultSetHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ResultSetHandler.class);
    
    /**
     * 处理结果集
     * 
     * @param resultSet 结果集
     * @param resultType 返回类型
     * @param <E> 结果元素类型
     * @return 结果列表
     */
    public <E> List<E> handleResultSet(ResultSet resultSet, Class<?> resultType) throws SQLException {
        List<E> resultList = new ArrayList<>();
        
        if (resultType == null) {
            logger.warn("resultType为空，返回空列表");
            return resultList;
        }
        
        logger.debug("开始处理结果集，目标类型: {}", resultType.getName());
        
        // 获取结果集元数据
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        logger.debug("结果集包含{}列", columnCount);
        
        // 遍历结果集
        while (resultSet.next()) {
            // 创建结果对象
            E rowObject = createResultObject(resultSet, resultType, metaData, columnCount);
            resultList.add(rowObject);
        }
        
        logger.debug("结果集处理完成，共{}行", resultList.size());
        
        return resultList;
    }
    
    /**
     * 创建结果对象
     * 
     * @param resultSet 结果集
     * @param resultType 结果类型
     * @param metaData 元数据
     * @param columnCount 列数
     * @param <E> 结果类型
     * @return 结果对象
     */
    @SuppressWarnings("unchecked")
    private <E> E createResultObject(ResultSet resultSet, Class<?> resultType, 
                                      ResultSetMetaData metaData, int columnCount) throws SQLException {
        
        // 处理简单类型（String、Integer、Long等）
        if (isSimpleType(resultType)) {
            return (E) resultSet.getObject(1);
        }
        
        // 处理Map类型
        if (Map.class.isAssignableFrom(resultType)) {
            return (E) handleMapType(resultSet, metaData, columnCount);
        }
        
        // 处理JavaBean类型
        return (E) handleBeanType(resultSet, resultType, metaData, columnCount);
    }
    
    /**
     * 处理Map类型
     * 
     * 将每一行数据转换为Map，key为列名，value为列值
     */
    private Map<String, Object> handleMapType(ResultSet resultSet, ResultSetMetaData metaData, 
                                               int columnCount) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            Object columnValue = resultSet.getObject(i);
            map.put(columnName, columnValue);
        }
        
        return map;
    }
    
    /**
     * 处理JavaBean类型
     * 
     * 核心流程：
     * 1. 创建对象实例
     * 2. 遍历所有列
     * 3. 根据列名找到对应的属性
     * 4. 设置属性值
     * 
     * 命名转换：
     * - 数据库字段：user_name (下划线分隔)
     * - Java属性：userName (驼峰命名)
     */
    private Object handleBeanType(ResultSet resultSet, Class<?> resultType, 
                                   ResultSetMetaData metaData, int columnCount) throws SQLException {
        try {
            // 1. 创建对象实例
            Object bean = resultType.newInstance();
            
            // 2. 获取所有字段
            Field[] fields = resultType.getDeclaredFields();
            Map<String, Field> fieldMap = new HashMap<>();
            
            for (Field field : fields) {
                field.setAccessible(true);
                // 支持驼峰命名和下划线命名
                fieldMap.put(field.getName().toLowerCase(), field);
                fieldMap.put(camelToUnderscore(field.getName()).toLowerCase(), field);
            }
            
            // 3. 遍历所有列，设置属性值
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object columnValue = resultSet.getObject(i);
                
                // 查找对应的字段
                Field field = fieldMap.get(columnName.toLowerCase());
                
                if (field != null && columnValue != null) {
                    // 类型转换
                    Object value = convertType(columnValue, field.getType());
                    field.set(bean, value);
                    
                    logger.trace("设置属性: {} = {}", field.getName(), value);
                } else {
                    logger.trace("未找到字段映射: {}", columnName);
                }
            }
            
            return bean;
            
        } catch (InstantiationException | IllegalAccessException e) {
            throw new MyBatisException("创建结果对象失败: " + resultType, e);
        }
    }
    
    /**
     * 类型转换
     * 
     * 将数据库返回的类型转换为Java属性的类型
     * 
     * 常见转换：
     * - BIGINT → Long
     * - VARCHAR → String
     * - DECIMAL → BigDecimal
     * - TIMESTAMP → Date
     */
    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // 类型相同，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // 转换为String
        if (targetType == String.class) {
            return value.toString();
        }
        
        // 转换为Integer
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }
        
        // 转换为Long
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        }
        
        // 转换为Double
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        }
        
        // 转换为Boolean
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            return Boolean.parseBoolean(value.toString());
        }
        
        // 其他类型暂不支持，直接返回
        logger.warn("不支持的类型转换: {} → {}", value.getClass(), targetType);
        return value;
    }
    
    /**
     * 驼峰命名转下划线命名
     * 
     * 例如: userName → user_name
     */
    private String camelToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        
        for (int i = 1; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               Number.class.isAssignableFrom(clazz) ||
               java.util.Date.class.isAssignableFrom(clazz) ||
               clazz == Boolean.class ||
               clazz == Character.class;
    }
}

