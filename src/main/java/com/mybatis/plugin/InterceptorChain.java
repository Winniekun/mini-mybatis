package com.mybatis.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 拦截器链
 * 
 * 管理所有的拦截器，并负责将它们应用到目标对象上。
 * 
 * 设计模式：责任链模式
 * - 多个拦截器按顺序组成责任链
 * - 每个拦截器都可以处理请求或传递给下一个拦截器
 * 
 * 代理包装顺序：
 * 假设有三个拦截器：A、B、C
 * 包装顺序：target → A代理 → B代理 → C代理
 * 执行顺序：C → B → A → target
 * 
 * 这种设计让后添加的拦截器在外层，先执行。
 * 
 * @author 学习者
 */
public class InterceptorChain {
    
    private static final Logger logger = LoggerFactory.getLogger(InterceptorChain.class);
    
    /**
     * 拦截器列表
     */
    private List<Interceptor> interceptors = new ArrayList<>();
    
    /**
     * 为目标对象应用所有拦截器
     * 
     * 核心逻辑：
     * 1. 依次遍历所有拦截器
     * 2. 每个拦截器都为target创建一层代理
     * 3. 返回最终的代理对象
     * 
     * 例如：
     * target → pluginA(target) → pluginB(pluginA(target)) → pluginC(pluginB(pluginA(target)))
     * 
     * @param target 目标对象
     * @return 包装后的代理对象
     */
    public Object pluginAll(Object target) {
        for (Interceptor interceptor : interceptors) {
            target = interceptor.plugin(target);
        }
        return target;
    }
    
    /**
     * 添加拦截器
     * 
     * @param interceptor 拦截器
     */
    public void addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        logger.info("添加拦截器: {}", interceptor.getClass().getName());
    }
    
    /**
     * 获取所有拦截器
     * 
     * @return 拦截器列表
     */
    public List<Interceptor> getInterceptors() {
        return interceptors;
    }
    
    /**
     * 获取拦截器数量
     * 
     * @return 拦截器数量
     */
    public int size() {
        return interceptors.size();
    }
}

