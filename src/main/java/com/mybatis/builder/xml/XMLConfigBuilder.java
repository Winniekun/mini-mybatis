package com.mybatis.builder.xml;

import com.mybatis.exceptions.MyBatisException;
import com.mybatis.io.Resources;
import com.mybatis.mapping.MappedStatement;
import com.mybatis.mapping.SqlCommandType;
import com.mybatis.session.Configuration;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * XML配置文件解析器
 * 
 * 负责解析mybatis-config.xml主配置文件，构建Configuration对象。
 * 
 * 解析内容包括：
 * 1. <settings> - 全局配置
 * 2. <environments> - 环境配置（数据源、事务管理器）
 * 3. <mappers> - Mapper映射文件位置
 * 
 * 设计模式：建造者模式
 * - 通过解析配置文件，逐步构建复杂的Configuration对象
 * 
 * @author 学习者
 */
public class XMLConfigBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(XMLConfigBuilder.class);
    
    /**
     * 全局配置对象
     */
    private Configuration configuration;
    
    public XMLConfigBuilder(InputStream inputStream) {
        this.configuration = new Configuration();
        // 解析配置文件
        parse(inputStream);
    }
    
    /**
     * 解析配置文件
     */
    private void parse(InputStream inputStream) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(inputStream);
            Element root = document.getRootElement();
            
            logger.info("开始解析MyBatis配置文件...");
            
            // 解析<environments>标签
            parseEnvironments(root.element("environments"));
            
            // 解析<mappers>标签
            parseMappers(root.element("mappers"));
            
            logger.info("MyBatis配置文件解析完成！");
            
        } catch (DocumentException e) {
            throw new MyBatisException("解析配置文件失败", e);
        }
    }
    
    /**
     * 解析<environments>标签
     * 
     * 示例：
     * <environments default="development">
     *   <environment id="development">
     *     <transactionManager type="JDBC"/>
     *     <dataSource type="POOLED">
     *       <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
     *       <property name="url" value="jdbc:mysql://localhost:3306/test"/>
     *       <property name="username" value="root"/>
     *       <property name="password" value="123456"/>
     *     </dataSource>
     *   </environment>
     * </environments>
     */
    private void parseEnvironments(Element environments) {
        if (environments == null) {
            logger.warn("未找到<environments>配置");
            return;
        }
        
        // 获取默认环境id
        String defaultEnv = environments.attributeValue("default");
        logger.debug("默认环境: {}", defaultEnv);
        
        // 获取所有<environment>标签
        List<Element> environmentList = environments.elements("environment");
        for (Element environment : environmentList) {
            String id = environment.attributeValue("id");
            
            // 只解析默认环境
            if (id.equals(defaultEnv)) {
                // 解析数据源配置
                Element dataSource = environment.element("dataSource");
                if (dataSource != null) {
                    parseDataSource(dataSource);
                }
                break;
            }
        }
    }
    
    /**
     * 解析<dataSource>标签
     */
    private void parseDataSource(Element dataSource) {
        List<Element> properties = dataSource.elements("property");
        for (Element property : properties) {
            String name = property.attributeValue("name");
            String value = property.attributeValue("value");
            
            switch (name) {
                case "driver":
                    configuration.setJdbcDriver(value);
                    logger.debug("数据库驱动: {}", value);
                    break;
                case "url":
                    configuration.setJdbcUrl(value);
                    logger.debug("数据库URL: {}", value);
                    break;
                case "username":
                    configuration.setJdbcUsername(value);
                    logger.debug("数据库用户名: {}", value);
                    break;
                case "password":
                    configuration.setJdbcPassword(value);
                    logger.debug("数据库密码: ******");
                    break;
            }
        }
    }
    
    /**
     * 解析<mappers>标签
     * 
     * 示例：
     * <mappers>
     *   <mapper resource="mapper/UserMapper.xml"/>
     * </mappers>
     */
    private void parseMappers(Element mappers) {
        if (mappers == null) {
            logger.warn("未找到<mappers>配置");
            return;
        }
        
        List<Element> mapperList = mappers.elements("mapper");
        for (Element mapper : mapperList) {
            String resource = mapper.attributeValue("resource");
            logger.debug("解析Mapper文件: {}", resource);
            
            // 加载并解析Mapper文件
            InputStream inputStream = Resources.getResourceAsStream(resource);
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(inputStream, configuration);
            mapperBuilder.parse();
        }
    }
    
    /**
     * 获取构建好的Configuration对象
     */
    public Configuration getConfiguration() {
        return configuration;
    }
}

