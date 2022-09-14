package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "C:\\project\\rsa\\rsa.pub";
    private static final String priKeyPath = "C:\\project\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "2D34DSFGF3adsGFW4");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2NjI5MDkxMTB9.pZZ9czkfBMMRm0sKV6drTuG5yH7eyACWPe38qInoN9-Vf3IKbFlPPk1eNgkX3XeOG6jQ4d6SKMhYDw-qhBGA_VoOiVDR5uT48koAkeeOEgOqEnqczB4304NBkvWTjXitn0rPhyoyvUBVmr0cv9LXtVXQUOYMjygCaAoYFvCPfSwdQkI8292WmzmR2U3-5a6Bx0A4g7UcmuKzWiu7ljrGfAKpZJa70L2Ad7VIpXyh0Af_BjMOvzH_ApTNgpM3Md7rWwscnbVCQi4UInAkoPDK3p9xnAFQo0jNre4lJY199SsKJKeDQLA1xGB_PuCYawE4Uw__fDZRo9Z7i4tpJHFozA";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}