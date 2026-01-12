package com.mybatis.builder.xml;

import com.mybatis.exceptions.MyBatisException;
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
 * Mapper XML文件解析器
 * 
 * 负责解析Mapper.xml映射文件，将SQL语句解析成MappedStatement对象。
 * 
 * 解析内容包括：
 * 1. <select> - 查询语句
 * 2. <insert> - 插入语句
 * 3. <update> - 更新语句
 * 4. <delete> - 删除语句
 * 
 * 每个SQL标签都会被解析成一个MappedStatement对象，存储在Configuration中。
 * 
 * @author 学习者
 */
public class XMLMapperBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(XMLMapperBuilder.class);
    
    private InputStream inputStream;
    private Configuration configuration;
    
    public XMLMapperBuilder(InputStream inputStream, Configuration configuration) {
        this.inputStream = inputStream;
        this.configuration = configuration;
    }
    
    /**
     * 解析Mapper文件
     */
    public void parse() {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(inputStream);
            Element root = document.getRootElement();
            
            // 获取namespace
            String namespace = root.attributeValue("namespace");
            logger.info("解析Mapper: {}", namespace);
            
            // 注册Mapper接口
            registerMapper(namespace);
            
            // 解析<select>标签
            parseStatements(root.elements("select"), namespace, SqlCommandType.SELECT);
            
            // 解析<insert>标签
            parseStatements(root.elements("insert"), namespace, SqlCommandType.INSERT);
            
            // 解析<update>标签
            parseStatements(root.elements("update"), namespace, SqlCommandType.UPDATE);
            
            // 解析<delete>标签
            parseStatements(root.elements("delete"), namespace, SqlCommandType.DELETE);
            
            logger.info("Mapper解析完成: {} (共{}条SQL语句)", namespace, 
                root.elements("select").size() + root.elements("insert").size() + 
                root.elements("update").size() + root.elements("delete").size());
            
        } catch (DocumentException e) {
            throw new MyBatisException("解析Mapper文件失败", e);
        }
    }
    
    /**
     * 注册Mapper接口
     */
    private void registerMapper(String namespace) {
        try {
            Class<?> mapperClass = Class.forName(namespace);
            if (!configuration.hasMapper(mapperClass)) {
                 configuration.addMapper(mapperClass);
            }
        } catch (ClassNotFoundException e) {
            logger.warn("未找到Mapper接口: {}", namespace);
        }
    }
    
    /**
     * 解析SQL语句标签
     * 
     * @param elements SQL标签列表
     * @param namespace 命名空间
     * @param sqlCommandType SQL类型
     */
    private void parseStatements(List<Element> elements, String namespace, SqlCommandType sqlCommandType) {
        for (Element element : elements) {
            String id = element.attributeValue("id");
            String parameterType = element.attributeValue("parameterType");
            String resultType = element.attributeValue("resultType");
            String sql = element.getTextTrim();
            
            // 构建statementId: namespace.id
            String statementId = namespace + "." + id;
            
            logger.debug("解析SQL语句: {} [{}]", statementId, sqlCommandType);
            logger.debug("SQL: {}", sql);
            
            // 创建MappedStatement对象
            MappedStatement.Builder builder = new MappedStatement.Builder(
                configuration, statementId, sqlCommandType);
            
            builder.sql(sql);
            
            // 设置参数类型
            if (parameterType != null && !parameterType.isEmpty()) {
                try {
                    Class<?> parameterClass = Class.forName(parameterType);
                    builder.parameterType(parameterClass);
                } catch (ClassNotFoundException e) {
                    logger.warn("参数类型不存在: {}", parameterType);
                }
            }
            
            // 设置返回类型
            if (resultType != null && !resultType.isEmpty()) {
                try {
                    Class<?> resultClass = Class.forName(resultType);
                    builder.resultType(resultClass);
                } catch (ClassNotFoundException e) {
                    logger.warn("返回类型不存在: {}", resultType);
                }
            }
            
            // 构建MappedStatement并添加到Configuration
            MappedStatement mappedStatement = builder.build();
            configuration.addMappedStatement(statementId, mappedStatement);
        }
    }
}

