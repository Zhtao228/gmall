package com.atguigu.gmall.gateway.config;


import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    private String pubKeyPath;
    private String cookieName;
    private String token;

    private PublicKey publicKey;

    @PostConstruct
    public void init(){
        try {
            // 读取公私钥对象
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
