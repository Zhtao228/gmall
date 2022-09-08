package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter bloomFilter;
    /**
     * joinPoint.getArgs(); 获取方法参数
     * joinPoint.getTarget().getClass(); 获取目标类
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        // 先拿到签名
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        // 通过签名获取目标对象
        Method method = signature.getMethod();
        // 获取方法上的注解对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        // 缓存前缀
        String prefix = gmallCache.prefix();
        // 目标方法的参数列表
        Object[] args = joinPoint.getArgs();
        String param = StringUtils.join(args, ",");
        // 缓存KEY
        String key = prefix + StringUtils.join(args, ",");
        // 使用布隆过滤器 防止缓存穿透
        if (!bloomFilter.contains(key)){
            return null;
        }
            // 先查询缓存，缓存直接命中则直接返回
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)){
            // 反序列化为目标方法的返回结果集类型
            return JSON.parseObject(json,method.getReturnType());
        }
        // 防止缓存穿透，添加分布式锁
        RLock lock = this.redissonClient.getLock(gmallCache.lock() + param);
        lock.lock();
        try {
            // 再次查询缓存，缓存命中存入则直接返回
            String json2 = this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(json2)){
                // 反序列化为目标方法的返回结果集类型
                return JSON.parseObject(json2,method.getReturnType());
            }
            // 执行目标方法
            Object result = joinPoint.proceed(args);
            // 把目标方法的返回结果集，放入缓存并释放分布式锁
            int timeout = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
            this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result),timeout, TimeUnit.MINUTES );

            return result;
        } finally {
            lock.unlock();
        }
    }

}
