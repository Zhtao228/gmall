package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CategoryMapperTest {


    @Autowired
    private CategoryMapper categoryMapper;



    @Test
    public void query(Long pid){
        List<CategoryEntity> categoryEntities = categoryMapper.queryCateLevel1AndLevel2(2L);
        System.out.println(categoryEntities);
    }
}