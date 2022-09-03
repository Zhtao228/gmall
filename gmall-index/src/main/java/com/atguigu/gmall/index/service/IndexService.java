package com.atguigu.gmall.index.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsFeign;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexService {

    @Autowired
    private GmallPmsFeign pmsFeign;

    public List<CategoryEntity> queryLv1Categories() {
        ResponseVo<List<CategoryEntity>> categoryResponseVo = this.pmsFeign.queryCategoriesByPid(0L);
        return categoryResponseVo.getData();
    }
}
