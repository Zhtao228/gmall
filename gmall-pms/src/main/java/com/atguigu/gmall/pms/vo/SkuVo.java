package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuVo extends SkuEntity {

    private List<SkuAttrValueEntity> saleAttrs;

    private List<String> images;
    // sku_bounds_table
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    private List<Integer> work;


    // sku_full_reduction_table
    private BigDecimal reducePrice;
    private Integer fulladdOther;
    private BigDecimal fullPrice;


    // sku_ladder_table
    private Integer ladderAddOther;
    private BigDecimal discount;
    private Integer fullCount;
}
