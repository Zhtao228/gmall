package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    // 三级分类
    private List<CategoryEntity> categories;

    // 品牌ID
    private Long brandId;
    // 品牌名称
    private String brandName;

    // spu ID
    private Long spuId;
    // spu 名称
    private String spuName;

    // sku
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Long weight;
    private String defaltImage;

    // sku 的图片
    private List<SkuImagesEntity> images;

    // 优惠信息
    private List<ItemSaleVo> sales;

    // 是否有货
    private Boolean store = false;

    // 销售属性，购买时的可选参数
    // [{attrId: 3, attrName: 机身颜色, attrValues: ['暗夜黑', '白天白']},
    // {attrId: 4, attrName: 运行内存, attrValues: ['8G', '12G']},
    // {attrId: 5, attrName: 机身存储, attrValues: ['128G', '256G', '512G']}]
    private List<SaleAttrValueVo> saleAttrs;

    // 单条销售属性   {3: '白天白', 4: '12G', 5: '256G'}  V O
    private Map<Long,String> saleAttr;

    // 销售属性组合和skuId映射关系：{'暗夜黑,8G,128G': 100, '白天白,12G,128G': 101} V O
    private String skuJson;

    // spu的海报信息  V O
    private List<String> spuImages;

    // 规格参数分组 V O
    private List<ItemGroupVo> groups;






}
