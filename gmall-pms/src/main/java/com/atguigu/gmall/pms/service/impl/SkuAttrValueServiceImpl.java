package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.seata.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.web.bind.annotation.PathVariable;
import springfox.documentation.spring.web.json.Json;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchAttrValueByCIdAndSkuId(Long cid, Long skuId) {

        List<AttrEntity> attrEntities =
                attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid).eq("search_type", 1));
        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id",skuId).in("attr_id",attrIds));
    }

    @Override
    public List<SaleAttrValueVo> querySkuAttrValueBySpuId(Long spuId) {
        List<SkuEntity> skuEntities = this.skuMapper.selectList(
                new QueryWrapper<SkuEntity>().eq("spu_id",spuId));
        if (CollectionUtils.isEmpty(skuEntities)){
            return  null;
        }
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());

        List<SkuAttrValueEntity> skuAttrValueEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds));
        if (CollectionUtils.isEmpty(skuAttrValueEntities)){
            return null;
        }
        Map<Long, List<SkuAttrValueEntity>> map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
         map.forEach((attrId,attrValueVos) ->{
             SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
             saleAttrValueVo.setAttrId(attrId);
             saleAttrValueVo.setAttrName(attrValueVos.get(0).getAttrName());
             saleAttrValueVo.setAttrValues(attrValueVos.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
             saleAttrValueVos.add(saleAttrValueVo);
         });
        return saleAttrValueVos;
    }

    @Override
    public String queryMappingBySpuId(Long spuId) {
        List<SkuEntity> skuEntities = this.skuMapper.selectList(
                new QueryWrapper<SkuEntity>().eq("spu_id",spuId));
        if (CollectionUtils.isEmpty(skuEntities)){
            return  null;
        }
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());

        List<Map<String ,Object>> maps =this.attrValueMapper.queryMappingBySkuId(skuIds);
        if (CollectionUtils.isEmpty(maps)){
            return null;
        }
        Map<String, Long> mappingMap = maps.stream().collect(Collectors.toMap(map -> map.get("attrValues").toString(), map -> (Long) map.get("sku_id")));
        return JSON.toJSONString(mappingMap);

    }

}