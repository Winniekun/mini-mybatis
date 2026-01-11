package com.mybatis.session;

import com.mybatis.builder.xml.XMLConfigBuilder;

import java.io.InputStream;

/**
 * SqlSessionFactory建造者
 * 
 * 这是MyBatis的入口类，负责创建SqlSessionFactory对象。
 * 
 * 设计模式：建造者模式
 * - 封装复杂的构建过程
 * - 提供简洁的API给用户使用
 * 
 * 使用示例：
 * <pre>
 * InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
 * SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(inputStream);
 * </pre>
 * 
 * 核心流程：
 * 1. 读取配置文件（mybatis-config.xml）
 * 2. 解析配置文件，构建Configuration对象
 * 3. 使用Configuration创建SqlSessionFactory
 * 
 * @author 学习者
 */
public class SqlSessionFactoryBuilder {
    
    /**
     * 根据配置文件输入流构建SqlSessionFactory
     * 
     * 这个方法是MyBatis启动的入口，完成了以下工作：
     * 1. 创建XMLConfigBuilder解析器
     * 2. 解析配置文件，生成Configuration对象
     * 3. 使用Configuration创建DefaultSqlSessionFactory
     * 
     * @param inputStream 配置文件输入流
     * @return SqlSessionFactory对象
     */
    public SqlSessionFactory build(InputStream inputStream) {
        // 1. 创建配置文件解析器
        XMLConfigBuilder parser = new XMLConfigBuilder(inputStream);
        
        // 2. 解析配置文件，得到Configuration对象
        Configuration configuration = parser.getConfiguration();
        
        // 3. 创建并返回SqlSessionFactory
        return new DefaultSqlSessionFactory(configuration);
    }
}

