package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    @PostMapping("pms/spu/json")
    @ApiOperation("spu分页查询")
    ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    @ApiOperation("sku查询")
    ResponseVo<List<SkuEntity>> querySkuBySkuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/spuattrvalue/search/attr/{cid}")
    @ApiOperation("商品属性查询")
    ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValueByCIdAndSpuId(
            @PathVariable("cid") Long cid,
            @RequestParam("spuId") Long spuId);

    @GetMapping("pms/brand/{id}")
    @ApiOperation("品牌查询")
    ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    @ApiOperation("分类详情查询")
    ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/skuattrvalue/search/attr/{cid}")
    ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueByCIdAndSkuId(
            @PathVariable("cid") Long cid,
            @RequestParam("skuId") Long skuId);

    @GetMapping("parent/{parentId}")
    ResponseVo<List<CategoryEntity>> queryCategoriesByPid(@PathVariable("parentId")Long parentId);


}
