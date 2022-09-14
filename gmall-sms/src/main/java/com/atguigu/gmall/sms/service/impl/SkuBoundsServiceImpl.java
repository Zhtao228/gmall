package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuFullReductionMapper skuFullReductionMapper;

    @Autowired
    private SkuLadderMapper skuLadderMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public void saveSale(SkuSaleVo skuSaleVo) {

        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(skuSaleVo,skuBoundsEntity);
        List<Integer> works = skuSaleVo.getWork();
        if (works != null && works.size() == 4){
            skuBoundsEntity.setWork(works.get(0) + works.get(1)*2 +works.get(2)*2 + works.get(3)*2);
        }
        save(skuBoundsEntity);

        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuSaleVo,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuSaleVo.getFulladdOther());
        skuFullReductionMapper.insert(skuFullReductionEntity);

        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuSaleVo,skuLadderEntity);
        skuLadderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        skuLadderMapper.insert(skuLadderEntity);
    }

    @Override
    public List<ItemSaleVo> querySalesBySkuId(Long skuId) {
        List<ItemSaleVo> itemSaleVos = new ArrayList<>();
        // 查询积分优惠
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id",skuId));
        if (skuBoundsEntity != null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setSaleId(skuBoundsEntity.getId());
            itemSaleVo.setDesc("送"+ skuBoundsEntity.getGrowBounds() + "成长积分，" +
                                "送"+ skuBoundsEntity.getBuyBounds()+"购物积分");
            itemSaleVo.setType("积分");
            itemSaleVos.add(itemSaleVo);
        }

        // 查询满减信息
        SkuFullReductionEntity skuFullReductionEntity =  skuFullReductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id",skuId));
        if (skuFullReductionEntity != null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setSaleId(skuFullReductionEntity.getId());
            itemSaleVo.setDesc("满"+ skuFullReductionEntity.getFullPrice() + "，" +
                    "减"+ skuFullReductionEntity.getReducePrice()+"");
            itemSaleVo.setType("满减");
            itemSaleVos.add(itemSaleVo);
        }

        // 查询打折信息
        SkuLadderEntity skuLadderEntity =  skuLadderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id",skuId));
        if (skuLadderEntity != null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setSaleId(skuLadderEntity.getId());
            itemSaleVo.setDesc("满"+ skuLadderEntity.getFullCount() + "件，" +
                    "打"+ skuLadderEntity.getDiscount().divide(new BigDecimal(10))+"折");
            itemSaleVo.setType("打折");
            itemSaleVos.add(itemSaleVo);
        }
        return itemSaleVos;
    }

}