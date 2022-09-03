package com.atguigu.gmall.wms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.wms.entity.WareEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface GmallWmsApi  {

    @GetMapping("wms/waresku/sku/{id}")
    ResponseVo<List<WareSkuEntity>> queryWareSkuBySkuId(@PathVariable("id")Long id);
}
