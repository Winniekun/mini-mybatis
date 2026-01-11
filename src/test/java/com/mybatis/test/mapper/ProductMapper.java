package com.mybatis.test.mapper;

import com.mybatis.test.entity.Product;

import java.util.List;

/**
 * 商品Mapper接口
 * 
 * 这是一个接口，没有实现类！MyBatis会通过动态代理自动创建实现。
 * 
 * 命名规范：
 * - select开头：查询操作
 * - insert开头：插入操作
 * - update开头：更新操作
 * - delete开头：删除操作
 * 
 * 方法命名要求：
 * - 必须和XML中的id一致
 * - 见名知意
 * 
 * @author 学习者
 */
public interface ProductMapper {
    
    /**
     * 根据ID查询商品
     * 
     * @param id 商品ID
     * @return 商品对象，不存在返回null
     */
    Product selectById(Long id);
    
    /**
     * 查询所有商品
     * 
     * @return 商品列表
     */
    List<Product> selectAll();
    
    /**
     * 根据分类查询商品
     * 
     * @param category 商品分类
     * @return 该分类下的所有商品
     */
    List<Product> selectByCategory(String category);
    
    /**
     * 插入商品
     * 
     * @param product 商品对象
     * @return 影响的行数
     */
    int insert(Product product);
    
    /**
     * 更新商品信息
     * 
     * @param product 商品对象（必须包含id）
     * @return 影响的行数
     */
    int update(Product product);
    
    /**
     * 根据ID删除商品
     * 
     * @param id 商品ID
     * @return 影响的行数
     */
    int deleteById(Long id);
}

