package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuVo extends SpuEntity {
    // spu描述
    private List<String> spuImages;

    // spuAttr
    private List<SpuAttrValueVo> baseAttrs;

    // sku
    private List<SkuVo> skus;


}
