package com.atguigu.gmall.pms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * sku销售属性&值
 *
 * @author ZhTao
 * @email fopj10@163.com
 * @date 2022-08-20 18:57:25
 */
@Api(tags = "sku销售属性&值 管理")
@RestController
@RequestMapping("pms/skuattrvalue")
public class SkuAttrValueController {

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @GetMapping("mapping/{spuId}")
    public ResponseVo<String> queryMappingBySpuId(@PathVariable("spuId") Long spuId){
        String json =  skuAttrValueService.queryMappingBySpuId(spuId);
        return ResponseVo.ok(json);
    }
    @GetMapping("sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValueBySkuId(@PathVariable("skuId")Long skuId){
        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueService.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id",skuId));
        return ResponseVo.ok(skuAttrValueEntities);
    }

    @GetMapping("spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValueBySpuId(@PathVariable("spuId") Long spuId){
        List<SaleAttrValueVo> saleAttrValueVos = this.skuAttrValueService.querySkuAttrValueBySpuId(spuId);
        return ResponseVo.ok(saleAttrValueVos);
    }


    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySkuAttrValueByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = skuAttrValueService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }

    @GetMapping("search/attr/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueByCIdAndSkuId(
            @PathVariable("cid") Long cid,
            @RequestParam("skuId") Long skuId){
        List<SkuAttrValueEntity> attrValueEntities =
                skuAttrValueService.querySearchAttrValueByCIdAndSkuId(cid,skuId);
        return ResponseVo.ok(attrValueEntities);
    }

    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SkuAttrValueEntity> querySkuAttrValueById(@PathVariable("id") Long id){
		SkuAttrValueEntity skuAttrValue = skuAttrValueService.getById(id);

        return ResponseVo.ok(skuAttrValue);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SkuAttrValueEntity skuAttrValue){
		skuAttrValueService.save(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SkuAttrValueEntity skuAttrValue){
		skuAttrValueService.updateById(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		skuAttrValueService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
