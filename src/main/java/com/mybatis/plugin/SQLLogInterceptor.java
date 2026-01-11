package com.mybatis.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * SQL日志拦截器（示例插件）
 * 
 * 这是一个实际可用的插件示例，用于演示如何编写MyBatis插件。
 * 
 * 功能：
 * 1. 记录SQL执行时间
 * 2. 打印方法调用信息
 * 3. 监控慢SQL
 * 
 * 使用场景：
 * - 开发阶段：打印详细的SQL执行日志
 * - 生产环境：监控慢SQL，发现性能问题
 * 
 * @author 学习者
 */
public class SQLLogInterceptor implements Interceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLLogInterceptor.class);
    
    /**
     * 慢SQL阈值（毫秒）
     */
    private long slowSqlThreshold = 1000;
    
    /**
     * 拦截方法执行
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 记录方法调用
        String methodName = invocation.getMethod().getName();
        String className = invocation.getTarget().getClass().getSimpleName();
        
        logger.debug(">>>>> {}.{} 开始执行", className, methodName);
        
        // 执行原方法
        Object result = invocation.proceed();
        
        // 计算执行时间
        long endTime = System.currentTimeMillis();
        long cost = endTime - startTime;
        
        // 记录执行结果
        if (cost > slowSqlThreshold) {
            logger.warn("<<<<< {}.{} 执行完成 [耗时: {}ms] ⚠️ 慢SQL警告", className, methodName, cost);
        } else {
            logger.debug("<<<<< {}.{} 执行完成 [耗时: {}ms]", className, methodName, cost);
        }
        
        return result;
    }
    
    /**
     * 创建代理对象
     */
    @Override
    public Object plugin(Object target) {
        // 使用Plugin工具类创建代理
        return Plugin.wrap(target, this);
    }
    
    /**
     * 设置插件属性
     */
    @Override
    public void setProperties(Properties properties) {
        if (properties != null) {
            String threshold = properties.getProperty("slowSqlThreshold");
            if (threshold != null) {
                this.slowSqlThreshold = Long.parseLong(threshold);
                logger.info("慢SQL阈值设置为: {}ms", slowSqlThreshold);
            }
        }
    }
}

