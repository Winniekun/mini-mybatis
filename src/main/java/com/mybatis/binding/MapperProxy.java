package com.mybatis.binding;

import com.mybatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Mapper接口代理类
 * 
 * MapperProxy是MyBatis的核心组件，实现了Mapper接口的"无实现类"调用。
 * 
 * 设计模式：代理模式 + 反射
 * - 使用JDK动态代理为Mapper接口创建代理对象
 * - 拦截接口方法调用，转换为SQL执行
 * 
 * 工作原理：
 * 1. 用户调用Mapper接口方法
 * 2. JDK动态代理拦截方法调用
 * 3. MapperProxy的invoke方法被触发
 * 4. 根据方法信息找到对应的SQL语句
 * 5. 通过SqlSession执行SQL
 * 6. 返回执行结果
 * 
 * 示例：
 * <pre>
 * // 接口定义
 * public interface UserMapper {
 *     User selectById(Long id);
 * }
 * 
 * // 获取代理对象
 * UserMapper mapper = sqlSession.getMapper(UserMapper.class);
 * 
 * // 调用方法（实际执行SQL）
 * User user = mapper.selectById(1L);  // 执行 com.xxx.UserMapper.selectById
 * </pre>
 * 
 * @author 学习者
 */
public class MapperProxy<T> implements InvocationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(MapperProxy.class);
    
    /**
     * SqlSession对象
     */
    private SqlSession sqlSession;
    
    /**
     * Mapper接口的Class对象
     */
    private Class<T> mapperInterface;
    
    public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
    }
    
    /**
     * 代理方法调用
     * 
     * 这是动态代理的核心方法，所有对Mapper接口的方法调用都会被路由到这里。
     * 
     * 核心流程：
     * 1. 构建statementId（namespace.methodName）
     * 2. 获取方法参数
     * 3. 根据返回值类型选择合适的SqlSession方法
     * 4. 执行SQL并返回结果
     * 
     * @param proxy 代理对象
     * @param method 被调用的方法
     * @param args 方法参数
     * @return 执行结果
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 如果调用的是Object的方法（toString、equals等），直接执行
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        
        // 构建statementId: 包名.类名.方法名
        String statementId = mapperInterface.getName() + "." + method.getName();
        
        logger.debug("Mapper方法调用: {}", statementId);
        
        // 获取方法参数（简化处理，只取第一个参数）
        Object parameter = null;
        if (args != null && args.length > 0) {
            parameter = args[0];
        }
        
        // 根据返回值类型判断执行哪种操作
        return executeMethod(statementId, parameter, method);
    }
    
    /**
     * 执行方法
     * 
     * 根据方法返回值类型，选择调用selectOne还是selectList
     * 
     * 判断依据：
     * - 返回Collection → 调用selectList
     * - 返回单个对象 → 调用selectOne
     * 
     * 真实的MyBatis会根据SQL类型（@Select、@Insert等注解或XML配置）来判断
     */
    private Object executeMethod(String statementId, Object parameter, Method method) {
        Class<?> returnType = method.getReturnType();
        
        // 判断是否为集合类型
        if (Collection.class.isAssignableFrom(returnType)) {
            logger.debug("执行selectList");
            return sqlSession.selectList(statementId, parameter);
        } 
        // 判断是否为void或int（可能是增删改操作）
        else if (returnType == void.class || returnType == int.class || returnType == Integer.class) {
            logger.debug("执行update");
            return sqlSession.update(statementId, parameter);
        }
        // 其他情况，执行selectOne
        else {
            logger.debug("执行selectOne");
            return sqlSession.selectOne(statementId, parameter);
        }
    }
}

