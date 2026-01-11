package com.mybatis.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 插件代理类
 * 
 * 使用JDK动态代理为目标对象创建代理，实现方法拦截。
 * 
 * 设计模式：代理模式
 * - 为四大对象创建代理
 * - 拦截目标方法的调用
 * 
 * 工作原理：
 * 1. 使用JDK动态代理包装目标对象
 * 2. 当目标方法被调用时，先调用拦截器的intercept方法
 * 3. 拦截器可以决定是否继续执行原方法
 * 
 * @author 学习者
 */
public class Plugin implements InvocationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(Plugin.class);
    
    /**
     * 目标对象
     */
    private Object target;
    
    /**
     * 拦截器
     */
    private Interceptor interceptor;
    
    private Plugin(Object target, Interceptor interceptor) {
        this.target = target;
        this.interceptor = interceptor;
    }
    
    /**
     * 为目标对象创建代理
     * 
     * 这是一个工具方法，用于简化代理对象的创建。
     * 
     * @param target 目标对象
     * @param interceptor 拦截器
     * @return 代理对象
     */
    public static Object wrap(Object target, Interceptor interceptor) {
        if (target == null) {
            return null;
        }
        
        logger.debug("为{}创建插件代理", target.getClass().getSimpleName());
        
        // 创建代理对象
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            new Plugin(target, interceptor)
        );
    }
    
    /**
     * 代理方法调用
     * 
     * 当代理对象的方法被调用时，这个方法会被触发。
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 创建Invocation对象
        Invocation invocation = new Invocation(target, method, args);
        
        // 调用拦截器的intercept方法
        return interceptor.intercept(invocation);
    }
}

