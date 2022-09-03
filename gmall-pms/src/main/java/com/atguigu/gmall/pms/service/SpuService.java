package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SpuEntity;

import java.util.List;
import java.util.Map;

/**
 * spu信息
 *
 * @author ZhTao
 * @email fopj10@163.com
 * @date 2022-08-20 18:57:25
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    PageResultVo querySpuByPageAndCid(PageParamVo paramVo, Long categoryId);

    void bigSave(SpuVo spu);
}

