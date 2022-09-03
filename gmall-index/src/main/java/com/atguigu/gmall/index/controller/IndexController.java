package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping
    public String toIndex(Model model){
        List<CategoryEntity> categoryEntities = this.indexService.queryLv1Categories();
        model.addAttribute("cates",categoryEntities);
        return "index";
    }
}
