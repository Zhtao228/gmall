package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import io.jsonwebtoken.lang.Collections;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private AttrGroupMapper attrGroupMapper;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<AttrGroupEntity> queryAttrGroupWithAttrsByCid(Long cid) {

        List<AttrGroupEntity> AttrGroupEntities = list(
                new QueryWrapper<AttrGroupEntity>().eq("category_id",cid));
        if (Collections.isEmpty(AttrGroupEntities)){
            return AttrGroupEntities;
        }
        AttrGroupEntities.forEach(attrGroupEntity -> {
            List<AttrEntity> AttrEntities =
                    attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            attrGroupEntity.setAttrEntities(AttrEntities);
        });
        return AttrGroupEntities;
    }

    @Override
    public List<ItemGroupVo> queryGroupsWithAttrValuesByCidAndSpuAndSkuId(Long cid, Long spuId, Long skuId) {
        // 1.根据分类id查询分组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(groupEntities)){
            return null;
        }
        // 遍历分组，查询每个分组下的规则参数
        return groupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();

            itemGroupVo.setId(attrGroupEntity.getId());
            itemGroupVo.setName(attrGroupEntity.getName());

            List<AttrEntity> attrEntities = this.attrMapper.selectList(
                    new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));

            if (Collections.isEmpty(attrEntities)){
                return itemGroupVo;
            }
            List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());


            // 查询规则参数及值
            List<AttrValueVo> attrs = new ArrayList<>();

            List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(
                    new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
            if (!Collections.isEmpty(spuAttrValueEntities)){
                attrs.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();
                    BeanUtils.copyProperties(spuAttrValueEntity,attrValueVo);
                    return attrValueVo;
                }).collect(Collectors.toList()));
            }
            List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(
                    new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
            if (!Collections.isEmpty(skuAttrValueEntities)){
                attrs.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();
                    BeanUtils.copyProperties(skuAttrValueEntity,attrValueVo);
                    return attrValueVo;
                }).collect(Collectors.toList()));
            }
            itemGroupVo.setAttrs(attrs);
            return itemGroupVo;
        }).collect(Collectors.toList());
    }

}