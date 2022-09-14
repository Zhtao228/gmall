package com.atguigu.gmall.ums.service.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.omg.CORBA.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch(type){
            case 1:wrapper.eq("username",data);break;
            case 2:wrapper.eq("phone",data);break;
            case 3:wrapper.eq("email",data);break;
            default:
                return null;
        }
        return this.count(wrapper) == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        String salt = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
        userEntity.setSalt(salt);

        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword()+salt));

        userEntity.setCreateTime(new Date());
        userEntity.setLevelId(1l);
        userEntity.setStatus(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setSourceType(1);
        userEntity.setNickname(userEntity.getUsername());

        this.save(userEntity);


    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        // 1.根据登录名查询用户
        UserEntity userEntity = this.getOne(new QueryWrapper<UserEntity>().eq("username", loginName)
                .or().eq("phone", loginName)
                .or().eq("email", loginName));

        // 2.判断用户是否为空
        if (userEntity == null) {
            return null;
        }

        // 3.对用户输入的明文密码加盐加密， 和 数据库中的密文密码比较，一致说明密码正确
        password = DigestUtils.md5Hex(password + userEntity.getSalt());
        if (StringUtils.equals(password, userEntity.getPassword())){
            return userEntity;
        }

        return null;
    }

}