package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("/**")
    public String toIndex(Model model, @RequestHeader(value = "id",required = false)Long id,
                          @RequestHeader(value = "username",required = false)String username){
        List<CategoryEntity> categoryEntities = this.indexService.queryLv1Categories();
        model.addAttribute("categories",categoryEntities);
        // TODO 加载其他数据
        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLv2CategoriesById(@PathVariable("pid") Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryLv2CategoriesById(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/testlock")
    @ResponseBody
    public ResponseVo<Object> testLock(){
        indexService.testLock1();
        return ResponseVo.ok();
    }
}
