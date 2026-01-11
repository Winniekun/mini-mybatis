package com.mybatis.mapping;

/**
 * SQL命令类型枚举
 * 
 * 定义了MyBatis支持的所有SQL命令类型。
 * 这个枚举在SQL执行时用于判断SQL的类型，从而采取不同的执行策略。
 * 
 * @author 学习者
 */
public enum SqlCommandType {
    
    /**
     * 查询语句
     * 特点：返回结果集，不修改数据
     */
    SELECT,
    
    /**
     * 插入语句
     * 特点：返回影响行数，可能返回自增主键
     */
    INSERT,
    
    /**
     * 更新语句
     * 特点：返回影响行数
     */
    UPDATE,
    
    /**
     * 删除语句
     * 特点：返回影响行数
     */
    DELETE,
    
    /**
     * 未知类型
     */
    UNKNOWN
}

