package com.mybatis.exceptions;

/**
 * MyBatis基础异常类
 * 
 * 所有MyBatis相关的异常都继承自这个类。
 * 这是一个运行时异常，不强制捕获。
 * 
 * @author 学习者
 */
public class MyBatisException extends RuntimeException {
    
    public MyBatisException() {
        super();
    }
    
    public MyBatisException(String message) {
        super(message);
    }
    
    public MyBatisException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public MyBatisException(Throwable cause) {
        super(cause);
    }
}

