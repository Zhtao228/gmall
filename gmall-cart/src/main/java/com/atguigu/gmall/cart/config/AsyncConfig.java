package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.cart.exception.handler.AsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {
    @Autowired
    private AsyncExceptionHandler asyncExceptionHandler;

    // 配置线程池，控制线程数
    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

    // 配置统一的异常处理器
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncExceptionHandler;
    }
}
