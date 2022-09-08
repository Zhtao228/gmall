package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import io.jsonwebtoken.lang.Collections;
import io.swagger.models.auth.In;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    ElasticsearchRestTemplate restTemplate;

    @Autowired
    GmallWmsClient wmsClient;

    @Autowired
    GmallPmsClient pmsClient;

    @Test
    void contextLoads() {
        IndexOperations indexOps = restTemplate.indexOps(Goods.class);
        if (!indexOps.exists()){
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
        }

        Integer pageNum = 1;
        Integer pageSize = 100;

        do {
            ResponseVo<List<SpuEntity>> spuresponseVo = pmsClient.querySpuByPageJson(new PageParamVo(1, 100, null));
            List<SpuEntity> spuEntities = spuresponseVo.getData();
            if (Collections.isEmpty(spuEntities)){
                return;
            }
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = pmsClient.querySkuBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                if (Collections.isEmpty(skuEntities)){
                    return;
                }

                ResponseVo<BrandEntity> brandEntityResponseVo = pmsClient.queryBrandById(spuEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResponseVo.getData();

                ResponseVo<CategoryEntity> categoryEntityResponseVo = pmsClient.queryCategoryById(spuEntity.getCategoryId());
                CategoryEntity categoryEntity = categoryEntityResponseVo.getData();

                ResponseVo<List<SpuAttrValueEntity>> baseAttrValueResponseVo = pmsClient.querySearchAttrValueByCIdAndSpuId(spuEntity.getCategoryId(), spuEntity.getId());
                List<SpuAttrValueEntity> spuAttrEntities = baseAttrValueResponseVo.getData();

                restTemplate.save( skuEntities.stream().map(skuEntity -> {
                    Goods goods = new Goods();
                    BeanUtils.copyProperties(skuEntity,goods);
                    goods.setSkuId(skuEntity.getId());
                    goods.setCreateTime(spuEntity.getCreateTime());
                    goods.setPrice(skuEntity.getPrice().doubleValue());

                    ResponseVo<List<WareSkuEntity>> wareResponseVo = wmsClient.queryWareSkuBySkuId(skuEntity.getId());
                    List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                    if (!Collections.isEmpty(wareSkuEntities)){
                        goods.setStore(
                                wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                                        wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0)
                        );
                        goods.setSales(
                                wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b) -> a + b).get());
                    }

                    if (brandEntity != null){
                        goods.setBrandId(brandEntity.getId());
                        goods.setBrandName(brandEntity.getName());
                        goods.setLogo(brandEntity.getLogo());
                    }

                    if (categoryEntity != null){
                        goods.setCategoryId(categoryEntity.getId());
                        goods.setCategoryName(categoryEntity.getName());
                    }

                    ResponseVo<List<SkuAttrValueEntity>> skuAttrValueEntityResponseVo = this.pmsClient.querySearchAttrValueByCIdAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueEntityResponseVo.getData();

                    List<SearchAttrValueVo> searchAttrs =new ArrayList<>();

                    if (!Collections.isEmpty(spuAttrEntities)){
                        searchAttrs.addAll(spuAttrEntities.stream().map(spuAttrValueEntity -> {
                            SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                            BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValueVo);
                            return searchAttrValueVo;
                        }).collect(Collectors.toList()));
                    }

                    if (!Collections.isEmpty(skuAttrValueEntities)){
                        searchAttrs.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                            SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                            BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                            return searchAttrValueVo;
                        }).collect(Collectors.toList()));
                    }
                    goods.setSearchAttrs(searchAttrs);
                    return goods;
                }).collect(Collectors.toList()));
            });

            //如果当前页的记录数是100,进入下一页
            pageSize = spuEntities.size();

            pageNum++;

        }while (pageSize == 100);



    }

}
