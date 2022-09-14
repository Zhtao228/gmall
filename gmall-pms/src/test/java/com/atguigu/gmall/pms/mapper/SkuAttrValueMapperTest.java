package com.atguigu.gmall.pms.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class SkuAttrValueMapperTest {

    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Test
    void queryMappingBySkuId() {
        List<Long> longs = Arrays.asList(73l, 74l);
        System.out.println(this.attrValueMapper.queryMappingBySkuId(longs));
    }
}