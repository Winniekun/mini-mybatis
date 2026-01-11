package com.mybatis.test;

import com.mybatis.io.Resources;
import com.mybatis.session.SqlSession;
import com.mybatis.session.SqlSessionFactory;
import com.mybatis.session.SqlSessionFactoryBuilder;
import com.mybatis.test.entity.Product;
import com.mybatis.test.entity.User;
import com.mybatis.test.mapper.ProductMapper;
import com.mybatis.test.mapper.UserMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis测试类
 * 
 * 通过这个测试类，你可以：
 * 1. 验证MyBatis的基本功能
 * 2. 理解MyBatis的使用流程
 * 3. 对比真实MyBatis的API
 * 
 * @author 学习者
 */
public class MybatisTest {
    
    private SqlSessionFactory sqlSessionFactory;
    
    /**
     * 初始化SqlSessionFactory
     * 
     * 这个方法在所有测试方法之前执行一次。
     * SqlSessionFactory是重量级对象，应该全局创建一次。
     */
    @Before
    public void init() throws Exception {
        // 1. 加载配置文件
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        
        // 2. 创建SqlSessionFactory
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        
        System.out.println("========================================");
        System.out.println("SqlSessionFactory初始化完成！");
        System.out.println("========================================\n");
    }
    
    /**
     * 测试：使用SqlSession直接执行SQL
     * 
     * 这是MyBatis最原始的使用方式。
     */
    @Test
    public void testSqlSession() {
        System.out.println("\n========== 测试：SqlSession直接执行SQL ==========");
        
        // 1. 获取SqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();
        
        try {
            // 2. 执行查询
            User user = sqlSession.selectOne("com.mybatis.test.mapper.UserMapper.selectById", 1L);
            
            // 3. 打印结果
            System.out.println("查询结果: " + user);
            
        } finally {
            // 4. 关闭SqlSession
            sqlSession.close();
        }
    }
    
    /**
     * 测试：使用Mapper接口（推荐方式）
     * 
     * 这是MyBatis的核心功能，也是最常用的方式。
     * 通过动态代理，让Mapper接口"自动"拥有实现。
     */
    @Test
    public void testMapperProxy() {
        System.out.println("\n========== 测试：Mapper接口代理 ==========");
        
        SqlSession sqlSession = sqlSessionFactory.openSession();
        
        try {
            // 1. 获取Mapper代理对象
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            System.out.println("Mapper代理对象: " + userMapper.getClass().getName());
            
            // 2. 调用方法（就像调用普通的接口方法一样！）
            User user = userMapper.selectById(1L);
            
            // 3. 打印结果
            System.out.println("查询结果: " + user);
            
        } finally {
            sqlSession.close();
        }
    }
    
    /**
     * 测试：查询列表
     */
    @Test
    public void testSelectList() {
        System.out.println("\n========== 测试：查询列表 ==========");
        
        SqlSession sqlSession = sqlSessionFactory.openSession();
        
        try {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            
            // 查询所有用户
            List<User> users = userMapper.selectAll();
            
            System.out.println("查询到 " + users.size() + " 个用户：");
            for (User user : users) {
                System.out.println("  " + user);
            }
            
        } finally {
            sqlSession.close();
        }
    }
    
    /**
     * 测试：插入数据
     */
    @Test
    public void testInsert() {
        System.out.println("\n========== 测试：插入数据 ==========");
        
        SqlSession sqlSession = sqlSessionFactory.openSession();
        
        try {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            
            // 创建新用户
            User user = new User();
            user.setUsername("testuser");
            user.setPassword("123456");
            user.setEmail("test@example.com");
            user.setCreateTime(LocalDateTime.now());
            
            // 插入数据
            int rows = userMapper.insert(user);
            System.out.println("插入成功，影响 " + rows + " 行");
            
            // 提交事务
            sqlSession.commit();
            
        } finally {
            sqlSession.close();
        }
    }
    
    /**
     * 测试：更新数据
     */
    @Test
    public void testUpdate() {
        System.out.println("\n========== 测试：更新数据 ==========");
        
        SqlSession sqlSession = sqlSessionFactory.openSession();
        
        try {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            
            // 先查询
            User user = userMapper.selectById(1L);
            System.out.println("更新前: " + user);
            
            // 修改数据
            user.setUsername("updated_user");
            user.setEmail("updated@example.com");
            
            // 执行更新
            int rows = userMapper.update(user);
            System.out.println("更新成功，影响 " + rows + " 行");
            
            // 提交事务
            sqlSession.commit();
            
            // 再次查询验证
            User updatedUser = userMapper.selectById(1L);
            System.out.println("更新后: " + updatedUser);
            
        } finally {
            sqlSession.close();
        }
    }
    
    /**
     * 测试：删除数据
     */
    @Test
    public void testDelete() {
        System.out.println("\n========== 测试：删除数据 ==========");
        
        SqlSession sqlSession = sqlSessionFactory.openSession();
        
        try {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            
            // 删除数据
            int rows = userMapper.deleteById(999L);
            System.out.println("删除成功，影响 " + rows + " 行");
            
            // 提交事务
            sqlSession.commit();
            
        } finally {
            sqlSession.close();
        }
    }
    
    /**
     * 测试：ProductMapper - 完整的CRUD操作
     * 
     * 这个测试展示了如何使用自己创建的Mapper
     */
    @Test
    public void testProductMapper() {
        System.out.println("\n========== 测试：ProductMapper CRUD操作 ==========");
        
        SqlSession sqlSession = sqlSessionFactory.openSession();
        
        try {
            // 1. 获取Mapper
            ProductMapper mapper = sqlSession.getMapper(ProductMapper.class);
            System.out.println("ProductMapper代理对象: " + mapper.getClass().getName());
            
            // 2. 查询所有商品
            System.out.println("\n--- 1. 查询所有商品 ---");
            List<Product> allProducts = mapper.selectAll();
            System.out.println("共查询到 " + allProducts.size() + " 个商品：");
            for (Product p : allProducts) {
                System.out.println("  " + p);
            }
            
            // 3. 根据ID查询
            System.out.println("\n--- 2. 根据ID查询商品 ---");
            Product product = mapper.selectById(1L);
            if (product != null) {
                System.out.println("查询结果: " + product);
            } else {
                System.out.println("未找到ID为1的商品");
            }
            
            // 4. 根据分类查询
            System.out.println("\n--- 3. 根据分类查询 ---");
            List<Product> phones = mapper.selectByCategory("手机");
            System.out.println("手机类商品数量: " + phones.size());
            for (Product p : phones) {
                System.out.println("  " + p.getProductName() + " - ¥" + p.getPrice());
            }
            
            // 5. 插入新商品（可选，会修改数据库）
            System.out.println("\n--- 4. 插入新商品 ---");
            Product newProduct = new Product();
            newProduct.setProductName("iPad Pro");
            newProduct.setCategory("平板");
            newProduct.setPrice(new BigDecimal("6999.00"));
            newProduct.setStock(20);
            newProduct.setDescription("苹果平板电脑");
            newProduct.setCreateTime(LocalDateTime.now());
            
            int rows = mapper.insert(newProduct);
            System.out.println("插入成功，影响 " + rows + " 行");
            
            // 提交事务
            sqlSession.commit();
            System.out.println("事务已提交");
            
        } finally {
            sqlSession.close();
        }
    }
}

