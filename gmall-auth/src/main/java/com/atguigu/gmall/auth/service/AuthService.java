package com.atguigu.gmall.auth.service;


import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.AuthException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableConfigurationProperties({JwtProperties.class})
public class AuthService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private JwtProperties jwtProperties;

    public void login(String loginName, String password,
                      HttpServletRequest request, HttpServletResponse response) {
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUser(loginName, password);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity == null){
            throw  new AuthException("用户名密码错误！！");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("userName",userEntity.getUsername());
        map.put("id", IpUtils.getIpAddressAtService(request));

        try {
            String token = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());

            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,jwtProperties.getExpire()*60);

            CookieUtils.setCookie(request,response,this.jwtProperties.getUnick(),userEntity.getNickname(),jwtProperties.getExpire()*60);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
