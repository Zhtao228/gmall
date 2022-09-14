package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private ExecutorService executorService;


    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            //        1.根据skuId查询sku  V
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new RuntimeException("你要访问的商品不存在");
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setDefaltImage(skuEntity.getDefaultImage());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            return skuEntity;
        }, executorService);

        CompletableFuture<Void> categoryFuture = skuFuture.thenAcceptAsync(skuEntity -> {
//        2.根据三级分类Id查询一二三级分类 V
            ResponseVo<List<CategoryEntity>> categoriesResponseVo = pmsClient.queryCategoriesByCid3(skuEntity.getCategoryId());
            List<CategoryEntity> categoryEntities = categoriesResponseVo.getData();
            itemVo.setCategories(categoryEntities);
        }, executorService);

        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuEntity -> {
//        3.根据品牌id查询品牌 V
            ResponseVo<BrandEntity> brandEntityResponseVo = pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, executorService);

        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {
//        4.根据spuId查询spu V
            ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, executorService);

        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
//        5.根据skuId查询营销信息 V
            ResponseVo<List<ItemSaleVo>> salesResponseVo = smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            itemVo.setSales(itemSaleVos);
        }, executorService);

        CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(() -> {
//        6.根据skuId查询库存信息  V
            ResponseVo<List<WareSkuEntity>> wareSkuEntityResponseVo = wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuEntityResponseVo.getData();
            if (!Collections.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().
                        anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, executorService);

        CompletableFuture<Void> skuImagesFuture = CompletableFuture.runAsync(() -> {
//        7.根据skuid查询sku图片列表 V
            ResponseVo<List<SkuImagesEntity>> skuImagesResponseVo = pmsClient.querySkuImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = skuImagesResponseVo.getData();
            itemVo.setImages(skuImagesEntities);
        }, executorService);

        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
//        8.根据spuId查询spu下所有sku的销售属性 V
            ResponseVo<List<SaleAttrValueVo>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValueBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrsResponseVo.getData();
            itemVo.setSaleAttrs(saleAttrValueVos);
        }, executorService);

        CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
//        9.根据skuId查询当前sku的销售属性 V
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                Map<Long, String> map = skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));
                itemVo.setSaleAttr(map);
            }
        }, executorService);

        CompletableFuture<Void> mappingFuture = skuFuture.thenAcceptAsync(skuEntity -> {
//        10.根据spuId查询spu下所有销售属性组合与skuId的映射关系 V
            ResponseVo<String> stringResponseVo = this.pmsClient.queryMappingBySpuId(skuEntity.getSpuId());
            String mapping = stringResponseVo.getData();
            itemVo.setSkuJsons(mapping);
        }, executorService);

        CompletableFuture<Void> spuDescFuture = skuFuture.thenAcceptAsync(skuEntity -> {
//        11.根据spuId查询spu的海报信息
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, executorService);

        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {
//        12.根据分类id、spuId、skuId查询规格参数分组及组下的规格参数和值
            ResponseVo<List<ItemGroupVo>> ItemGroupVoResponseVo = pmsClient.queryGroupsWithAttrValuesByCidAndSpuAndSkuId(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> itemGroupVos = ItemGroupVoResponseVo.getData();
            itemVo.setGroups(itemGroupVos);
        }, executorService);

        CompletableFuture.allOf(groupFuture,spuDescFuture,mappingFuture,saleAttrFuture,saleAttrsFuture,
                skuImagesFuture,wareFuture,salesFuture,spuFuture,brandFuture,categoryFuture).join();

        executorService.execute( () -> {
            this.creatHtml(itemVo);
        });

        return itemVo;
    }

    private void creatHtml(ItemVo itemVo){
        try(PrintWriter writer = new PrintWriter(new File("C:\\project\\html\\" + itemVo.getSkuId() + ".html"));) {
            Context context = new Context();
            context.setVariable("itemVo",itemVo);
            this.templateEngine.process("item",context,writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
