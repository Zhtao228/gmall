package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import jdk.nashorn.internal.objects.Global;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(JwtProperties.class)
class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties properties;

    public AuthGatewayFilterFactory(){
        super(PathConfig.class);
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                // 请求对象，类似于HttpServletRequest
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                // 1.判断当前请求在不在拦截名单之中，不在则直接放行
                // 获取拦截名单
                List<String> paths = config.paths;
                // 获取当前请求的请求路径
                String curPath = request.getURI().getPath();
                // 判断在不在拦截名单之中
                if (!CollectionUtils.isEmpty(paths) && !paths.stream().anyMatch(path -> StringUtils.startsWith(curPath, path))){
                    return chain.filter(exchange);
                }

                // 2.获取token。异步（后台管理前后端分离）-头信息  同步（门户端）-cookie
                String token = request.getHeaders().getFirst(properties.getToken());
                if (StringUtils.isBlank(token)){
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(properties.getCookieName())){
                        token = cookies.getFirst(properties.getCookieName()).getValue();
                    }
                }

                // 3.对token判空，如果为空则说明没有登录，应该重定向到登录页面，并拦截
                if (StringUtils.isBlank(token)){
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();
                }

                try {
                    // 4.解析token，如果出现异常则重定向到登录页面
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, properties.getPublicKey());

                    // 5.比较登录用户ip 和 当前请求用户的ip，如果不一致则重定向登录页面
                    String ip = map.get("ip").toString(); // 登录用户的ip地址
                    String curIp = IpUtils.getIpAddressAtGateway(request); // 当前用户的ip地址
                    if (!StringUtils.equals(ip, curIp)){
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                        return response.setComplete();
                    }

                    // 6.把登录信息直接传递给后续服务
                    request.mutate().header("id", map.get("id").toString())
                            .header("username", map.get("username").toString()).build();
                    exchange.mutate().request(request).build();

                    // 7.放行
                    return chain.filter(exchange);
                } catch (Exception e) {
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();
                }
            }
        };
    }
    @Data
    public static class PathConfig {

        private List<String> paths;
    }
}
