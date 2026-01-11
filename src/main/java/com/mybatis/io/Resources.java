package com.mybatis.io;

import java.io.InputStream;

/**
 * 资源加载工具类
 * 
 * 用于加载类路径下的资源文件，如配置文件、Mapper文件等。
 * 
 * @author 学习者
 */
public class Resources {
    
    /**
     * 从类路径加载资源文件
     * 
     * @param resource 资源路径（相对于classpath）
     * @return 输入流
     */
    public static InputStream getResourceAsStream(String resource) {
        return getResourceAsStream(null, resource);
    }
    
    /**
     * 使用指定的ClassLoader加载资源
     * 
     * @param loader 类加载器
     * @param resource 资源路径
     * @return 输入流
     */
    public static InputStream getResourceAsStream(ClassLoader loader, String resource) {
        InputStream inputStream = null;
        
        // 尝试使用指定的ClassLoader加载
        if (loader != null) {
            inputStream = loader.getResourceAsStream(resource);
        }
        
        // 如果失败，使用当前类的ClassLoader
        if (inputStream == null) {
            inputStream = Resources.class.getClassLoader().getResourceAsStream(resource);
        }
        
        // 如果还是失败，使用系统ClassLoader
        if (inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(resource);
        }
        
        return inputStream;
    }
}

