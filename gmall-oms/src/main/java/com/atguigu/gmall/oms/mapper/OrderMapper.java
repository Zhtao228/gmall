package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author ZhTao
 * @email fopj10@163.com
 * @date 2022-08-20 19:21:26
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
