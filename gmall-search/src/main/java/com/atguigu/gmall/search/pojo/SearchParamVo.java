package com.atguigu.gmall.search.pojo;


import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {

    // https://search.jd.com/search?
    // keyword=%E6%89%8B%E6%9C%BA&p
    // sort=2&wq=%E6%89%8B%E6%9C%BA&psort=2&
    // ev=exbrand_%E5%8D%8E%E4%B8%BA%EF%BC%88HUAWEI%EF%BC%89%7C%7C%E8%8D%A3%E8%80%80%EF%BC%88HONOR%EF%BC%89%5E3753_90279%7C%7C76033%5E3751_88228%5
    // Eexprice_3000-6000%5E

    // 检索字段
    private String keyword;
    // 品牌ID
    private List<Long> brandId;
    // 种类ID
    private List<Long> categoryId;
    // 过滤的检索参数
    private List<String> props;

    //是否有货
    private Boolean store;

    // 排序
    private Integer sort = 0;

    // 页码
    private Integer pageNum = 1;
    private final Integer pageSize = 20;

    // 价格区间
    private Double priceFrom;
    private Double priceTo;

}
