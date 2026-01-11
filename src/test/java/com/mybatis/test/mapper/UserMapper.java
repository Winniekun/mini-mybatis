package com.mybatis.test.mapper;

import com.mybatis.test.entity.User;

import java.util.List;

/**
 * 用户Mapper接口
 * 
 * 注意：这是一个接口，没有实现类！
 * MyBatis会通过动态代理自动为这个接口创建实现。
 * 
 * @author 学习者
 */
public interface UserMapper {
    
    /**
     * 根据ID查询用户
     * 
     * @param id 用户ID
     * @return 用户对象，不存在返回null
     */
    User selectById(Long id);
    
    /**
     * 查询所有用户
     * 
     * @return 用户列表
     */
    List<User> selectAll();
    
    /**
     * 插入用户
     * 
     * @param user 用户对象
     * @return 影响的行数
     */
    int insert(User user);
    
    /**
     * 更新用户
     * 
     * @param user 用户对象
     * @return 影响的行数
     */
    int update(User user);
    
    /**
     * 根据ID删除用户
     * 
     * @param id 用户ID
     * @return 影响的行数
     */
    int deleteById(Long id);
}

