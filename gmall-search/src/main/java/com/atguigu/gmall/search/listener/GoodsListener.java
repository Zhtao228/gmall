package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.service.SearchService;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import io.jsonwebtoken.lang.Collections;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private SearchService searchService;
    // 处理insert的消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_INSERT_QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE",
            ignoreDeclarationExceptions = "true",
            type = ExchangeTypes.TOPIC), key = {"item.insert"}))
    public void syncData(Long spuId, Channel channel, Message message) throws IOException {
        if (spuId ==null){
            return;
        }

        // 完成数据同步
        // 根据spiId 查询spu
        ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if (spuEntity == null) {
            return;
        }


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
                        wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
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
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }




}
