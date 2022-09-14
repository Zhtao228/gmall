package com.atguigu.gmall.item.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService executorService(
            @Value("${ThreadPool.corePoolSize}") Integer corePoolSize,
            @Value("${ThreadPool.maxmumPoolSize}") Integer maxmumPoolSize,
            @Value("${ThreadPool.keepAliveTime}") Integer keepAliveTime,
            @Value("${ThreadPool.blockingQueue}") Integer blockingQueue){

        return new ThreadPoolExecutor(corePoolSize, maxmumPoolSize, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<>(blockingQueue));

    }
}
