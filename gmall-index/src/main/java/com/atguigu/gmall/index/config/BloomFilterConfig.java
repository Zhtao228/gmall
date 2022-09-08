package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsFeign;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX="INDEX:CATES:";
    
    @Autowired
    private GmallPmsFeign pmsFeign;

    @Bean
    public RBloomFilter rbloomFilter(){
        // 初始化布隆过滤器
        RBloomFilter<String> bloomfilter = this.redissonClient.getBloomFilter("index:bf");
        bloomfilter.tryInit(2000, 0.03);
        
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsFeign.queryCategoriesByPid(0l);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        if (!CollectionUtils.isEmpty(categoryEntities)){
            categoryEntities.forEach(categoryEntity -> {
                bloomfilter.add(KEY_PREFIX + categoryEntity.getId());
            });
        }
        return bloomfilter;
    }
}