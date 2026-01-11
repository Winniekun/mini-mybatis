package com.mybatis.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 方法调用信息封装
 * 
 * 封装了被拦截方法的所有信息，包括目标对象、方法、参数等。
 * 
 * 作用：
 * 1. 传递方法调用信息给拦截器
 * 2. 提供proceed()方法继续执行原方法
 * 3. 允许拦截器修改参数和返回值
 * 
 * @author 学习者
 */
public class Invocation {
    
    /**
     * 目标对象（被代理的对象）
     */
    private Object target;
    
    /**
     * 被拦截的方法
     */
    private Method method;
    
    /**
     * 方法参数
     */
    private Object[] args;
    
    public Invocation(Object target, Method method, Object[] args) {
        this.target = target;
        this.method = method;
        this.args = args;
    }
    
    /**
     * 执行原方法
     * 
     * 拦截器通过调用这个方法来继续执行被拦截的方法。
     * 
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    public Object proceed() throws InvocationTargetException, IllegalAccessException {
        return method.invoke(target, args);
    }
    
    public Object getTarget() {
        return target;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public Object[] getArgs() {
        return args;
    }
}

