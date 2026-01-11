package com.mybatis.plugin;

import java.util.Properties;

/**
 * 拦截器接口
 * 
 * MyBatis插件的核心接口，用于拦截MyBatis的四大对象的方法调用。
 * 
 * 可拦截的四大对象：
 * 1. Executor - SQL执行器
 * 2. StatementHandler - JDBC语句处理器
 * 3. ParameterHandler - 参数处理器
 * 4. ResultSetHandler - 结果集处理器
 * 
 * 常见应用场景：
 * 1. 分页插件 - 拦截Executor的query方法，添加分页逻辑
 * 2. SQL日志 - 拦截StatementHandler，打印完整SQL
 * 3. 性能监控 - 拦截方法执行时间
 * 4. 数据权限 - 拦截SQL，添加权限过滤条件
 * 5. 敏感数据加密 - 拦截结果集处理
 * 
 * 设计模式：责任链模式 + 代理模式
 * - 多个拦截器组成责任链
 * - 使用动态代理实现方法拦截
 * 
 * 使用示例：
 * <pre>
 * @Intercepts({
 *     @Signature(type = Executor.class, method = "query", 
 *                args = {String.class, Object.class})
 * })
 * public class PageInterceptor implements Interceptor {
 *     public Object intercept(Invocation invocation) throws Throwable {
 *         // 添加分页逻辑
 *         return invocation.proceed();
 *     }
 * }
 * </pre>
 * 
 * @author 学习者
 */
public interface Interceptor {
    
    /**
     * 拦截方法
     * 
     * 这是插件的核心方法，所有被拦截的方法调用都会经过这里。
     * 
     * 核心流程：
     * 1. 前置处理（修改参数、记录日志等）
     * 2. 调用invocation.proceed()执行原方法
     * 3. 后置处理（修改返回值、记录结果等）
     * 
     * @param invocation 方法调用信息（目标对象、方法、参数等）
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    Object intercept(Invocation invocation) throws Throwable;
    
    /**
     * 为目标对象创建代理
     * 
     * 这个方法由MyBatis框架调用，用于决定是否对目标对象进行代理。
     * 
     * 默认实现：使用Plugin.wrap()方法创建代理
     * 自定义实现：可以不创建代理，直接返回target
     * 
     * @param target 被拦截的目标对象（Executor、StatementHandler等）
     * @return 代理对象或原对象
     */
    Object plugin(Object target);
    
    /**
     * 设置插件属性
     * 
     * 在MyBatis初始化时调用，用于传递插件的配置参数。
     * 
     * 配置示例：
     * <pre>
     * <plugins>
     *   <plugin interceptor="com.mybatis.plugin.PageInterceptor">
     *     <property name="pageSize" value="10"/>
     *   </plugin>
     * </plugins>
     * </pre>
     * 
     * @param properties 插件配置属性
     */
    void setProperties(Properties properties);
}

