package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author ZhTao
 * @email fopj10@163.com
 * @date 2022-08-20 19:23:17
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
