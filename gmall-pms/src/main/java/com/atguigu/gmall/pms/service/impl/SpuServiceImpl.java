package com.atguigu.gmall.pms.service.impl;


import com.alibaba.nacos.common.utils.StringUtils;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.service.SpuService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.jsonwebtoken.lang.Collections;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.logging.log4j.util.Strings;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {


    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByPageAndCid(PageParamVo paramVo, Long categoryId) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        if (categoryId != 0){
            wrapper.eq("category_id",categoryId);
        }

        String key = paramVo.getKey();
        if (StringUtils.isNotBlank(key)){
            wrapper.and(t -> t.eq("id",key).or(u -> u.like("name",key)));
        }

        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @GlobalTransactional(rollbackFor = Exception.class)
    @Override
    public void bigSave(SpuVo spu) {

        // 保存Spu_table
        Long spuId = this.savaSpuInfo(spu);

        // 保存Spu_desc
        this.savaSpuDesc(spu, spuId);

        //保存Spu_Attr_value_table
        this.saveSpuAttr(spu, spuId);
        // 保存Sku
        this.saveSku(spu, spuId);

        this.rabbitTemplate.convertAndSend("PMS_SPU_EXCHANGE","item.insert",spuId);

    }

    private void saveSku(SpuVo spu, Long spuId) {
        List<SkuVo> skus = spu.getSkus();
        if (Collections.isEmpty(skus)){
            return;
        }
        skus.forEach(skuVo -> {
            // 保存 sku_table
            skuVo.setSpuId(spuId);
            skuVo.setCategoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());

            List<String> skuImages = skuVo.getImages();
            if (!Collections.isEmpty(skuImages)){
                skuVo.setDefaultImage(Strings.isBlank(skuVo.getDefaultImage()) ? skuImages.get(0) : skuVo.getDefaultImage());
            }
            skuMapper.insert(skuVo);

            Long skuId = skuVo.getId();

            // 保存 Sku_Attr_value_table
//            List<SkuAttrValueEntity> skuAttrs = skuVo.getSkuAttrs();
//            if (!Collections.isEmpty(skuAttrs)){
//                skuAttrs.forEach(skuAttrValueEntity -> {
//                    skuAttrValueEntity.setSkuId(spuId);
//                });
//                skuAttrValueService.saveBatch(skuAttrs);
//            }
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!Collections.isEmpty(saleAttrs)){
                saleAttrs.forEach(skuAttrValueEntity -> {
                    skuAttrValueEntity.setSkuId(skuId);
                });
                this.skuAttrValueService.saveBatch(saleAttrs);
            }

            // 保存 Sku_images_table
            if (!Collections.isEmpty(skuImages)){
                skuImagesService.saveBatch(skuImages.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(image,skuVo.getDefaultImage()) ? 1 :0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }

            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            gmallSmsClient.saveSales(skuSaleVo);
        });
    }

    private void saveSpuAttr(SpuVo spu, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        spuAttrValueService.saveBatch(baseAttrs.stream().map(spuAttrValueVo -> {
            SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
            BeanUtils.copyProperties(spuAttrValueVo,spuAttrValueEntity);
            spuAttrValueEntity.setSpuId(spuId);
            return spuAttrValueEntity;
        }).collect(Collectors.toList()));
    }

    private void savaSpuDesc(SpuVo spu, Long spuId) {
        List<String> spuDesc = spu.getSpuImages();
        if (!Collections.isEmpty(spuDesc)){
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuDesc,","));
            spuDescMapper.insert(spuDescEntity);
        }
    }

    private Long savaSpuInfo(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        save(spu);
        return spu.getId();
    }

    private void sendMessage(Long id , String type){
        try {
            this.rabbitTemplate.convertAndSend("item.exchange","item."+type,id);
        } catch (Exception e) {
            log.error(type+"商品消息发送异常，商品id："+id,e);
        }
    }

}