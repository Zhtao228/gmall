package com.atguigu.gmall.index.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.StringUtils;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsFeign;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.google.common.collect.Lists;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsFeign pmsFeign;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate redisTemplate;


    public static final String KEY_PREFIX = "index:category:";


    public List<CategoryEntity> queryLv1Categories() {
        String catrgories = this.redisTemplate.opsForValue().get(KEY_PREFIX + "index");
        if (StringUtils.isNotBlank(catrgories)){
            List<CategoryEntity> categories = JSON.parseArray(catrgories, CategoryEntity.class);
            return categories;
        }
        ResponseVo<List<CategoryEntity>> categoryResponseVo = this.pmsFeign.queryCategoriesByPid(0L);
        List<CategoryEntity> categoryResponseVoData = categoryResponseVo.getData();
        this.redisTemplate.opsForValue().set(KEY_PREFIX + "index", JSONObject.toJSONString(categoryResponseVoData),30, TimeUnit.DAYS);
        return categoryResponseVoData;
    }

    @GmallCache(prefix = "index:cates",timeout = 14400,random = 3600,lock = "lock")
    public List<CategoryEntity> queryLv2CategoriesById(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsFeign.queryLv2CategoriesWithSubsById(pid);
        return listResponseVo.getData();
    }

    public List<CategoryEntity> queryLv2CategoriesById2(Long pid) {

        String cacheCatrgories = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(cacheCatrgories)){
            List<CategoryEntity> categoryEntities = JSON.parseArray(cacheCatrgories, CategoryEntity.class);
            return categoryEntities;
        }
        ResponseVo<List<CategoryEntity>> subcategoryEntities = pmsFeign.queryLv2CategoriesWithSubsById(pid);
        List<CategoryEntity> subcategoryEntitiesData = subcategoryEntities.getData();
        this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSONObject.toJSONString(subcategoryEntitiesData),30, TimeUnit.DAYS);
       return subcategoryEntitiesData;
    }


    public void testLock() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.tryLock("lock", uuid,300l);

        if (lock){
            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)){
                return;
            }
            // 有值就转成成int
            Integer num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
           this.testSubLock(uuid);
           this.unlock("lock",uuid);
        }
    }

    public void testLock1(){
        RLock lock = redissonClient.getLock("lock");
        lock.lock(10, TimeUnit.SECONDS);
        try {
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                this.redisTemplate.opsForValue().set("num", "1");
                return;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
        } finally {
            lock.unlock();
        }

    }


    private void testSubLock(String uuid) {
        Boolean lock = this.tryLock("lock", uuid, 300l);
        if (lock){
            System.out.println("分布式锁可重入锁");
            this.unlock("lock",uuid);
        }
    }


    public Boolean tryLock(String lockName,String uuid,Long expire){
        String script = "if (redis.call('exists',KEYS[1]) == 0 or redis.call('hexists',KEYS[1],ARGV[1]) == 1) " +
                "then" +
                "   redis.call('hincrby',KEYS[1],ARGV[1],1)" +
                "   redis.call('expire',KEYS[1],ARGV[2])" +
                "   return 1 " +
                "else" +
                "   return 0 " +
                "end";
        if (!this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList(lockName),uuid,expire.toString())){
            try {
                Thread.sleep(2000);
                tryLock(lockName, uuid, expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.renewTime(lockName, uuid, expire);
        return true;
    }

    /**
     * 锁延期
     * 线程等待超时时间的2/3时间后,执行锁延时代码,直到业务逻辑执行完毕,因此在此过程中,其他线程无法获取到锁,保证了线程安全性
     * @param lockName
     * @param expire 单位：毫秒
     */
    private void renewTime(String lockName, String uuid, Long expire){
        String script = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 1) " +
                "then " +
                " return redis.call('expire', KEYS[1], ARGV[2]) " +
                "else " +
                "return 0 " +
                "end";
        new Timer().schedule(new TimerTask(){
            @Override
            public void run() {
                Boolean execute = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
                if (execute){
                    renewTime(lockName, uuid, expire);
                }
            }
        },expire *1000 /3);
    }

    public void unlock(String lockName,String uuid){
        String script = "if redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1 " +
                "then " +
                "   redis.call('hincrby', KEYS[1], ARGV[1], 1) " +
                "   redis.call('expire', KEYS[1], ARGV[2]) " +
                "   return 1 " +
                "else " +
                "   return 0 " +
                "end";
        // 这里之所以没有跟加锁一样使用 Boolean ,这是因为解锁 lua 脚本中，三个返回值含义如下：
        // 1 代表解锁成功，锁被释放
        // 0 代表可重入次数被减 1
        // null 代表其他线程尝试解锁，解锁失败
        Long result = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Lists.newArrayList(lockName), uuid);
        // 如果未返回值，代表尝试解其他线程的锁
        if (result == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by lockName: "
                    + lockName + " with request: "  + uuid);
        }
    }


}
