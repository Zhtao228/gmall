package com.atguigu.gmall.cart.service;

import brave.internal.collect.UnsafeArrayMap;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.controller.UserInfo;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import io.jsonwebtoken.lang.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartAsyncSerive asyncSerive;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:price:";

    public void addCart(Cart cart) {
        String userId = getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String,Object,Object> hashOps = this.redisTemplate.boundHashOps(key);

        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)){
            String json = hashOps.get(skuId).toString();
            cart = JSON.parseObject(json,Cart.class);
            cart.setCount(cart.getCount().add(count));
            this.asyncSerive.updateCart(userId,skuId,cart);
        }else {
            cart.setUserId(userId);
            cart.setCheck(true);

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null){
                throw new CartException("您新增的商品不存在！");
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setDefaultImage(skuEntity.getDefaultImage());
            cart.setPrice(skuEntity.getPrice());

            ResponseVo<List<WareSkuEntity>> wareResponseVo = wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!Collections.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                    wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0
                ));
            }

            ResponseVo<List<SkuAttrValueEntity>> saleAttrsResponseVo = pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrsResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));

            ResponseVo<List<ItemSaleVo>> ItemSaleResponseVo = smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = ItemSaleResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));

            asyncSerive.insertCart(cart);

            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId,skuEntity.getPrice().toString());
        }
        hashOps.put(skuId,JSON.toJSONString(cart));
    }

    private String getUserId(){
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null ) {
            return userInfo.getUserId().toString();
        }
        return userInfo.getUserKey();
    }

    public Cart queryCart(Long skuId) {
        String userId = getUserId();
        BoundHashOperations<String , Object , Object> boundHashOperations = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (boundHashOperations.hasKey(skuId.toString())){
            String cartJson = boundHashOperations.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson,Cart.class);
        }
        throw  new CartException("当前用户的购物车没有该记录");
    }

    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unloginKey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(unloginKey);
        List<Object> cartJsons = hashOps.values();
        List<Cart> unLoginCarts = null;
        if (!Collections.isEmpty(cartJsons)){
            unLoginCarts = cartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        Long userId = userInfo.getUserId();
        if (userId == null){
            return unLoginCarts;
        }

        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!Collections.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if (loginHashOps.hasKey(skuId)){
                    String cartJson = loginHashOps.get(skuId).toString();
                    cart = JSON.parseObject(cartJson,Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    this.asyncSerive.updateCart(userId.toString(),skuId,cart);
                }else {
                    cart.setId(null);
                    cart.setUserId(userId.toString());
                    this.asyncSerive.insertCart(cart);
                }
                loginHashOps.put(skuId,JSON.toJSONString(cart));
            });
            this.redisTemplate.delete(KEY_PREFIX + userKey);
            this.asyncSerive.deleteByUserId(userKey);
        }
        List<Object> loginCartJsons = loginHashOps.values();
        if (Collections.isEmpty(loginCartJsons)){
            return null;
        }
        return loginCartJsons.stream().map(cartJson -> {
            Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
            cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
            return  cart;
        }).collect(Collectors.toList());
    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            BigDecimal count = cart.getCount();
            cart = JSON.parseObject(cartJson,Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            this.asyncSerive.updateCart(userId,cart.getSkuId().toString(),cart);
            return;
        }
        throw new CartException("您操作的购物车记录不存在！！");
    }

    @Async
    public ListenableFuture<String> executor1() {
        try {
            System.out.println("executor1方法开始束执行！");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor1方法结束执行！！！！");
            return AsyncResult.forValue("executor1一切正常" );
        } catch (InterruptedException e) {
            e.printStackTrace();
        return AsyncResult.forValue("executor1失误");
        }
    }

    @Async
    public String executor2() {
        try {
            System.out.println("executor2方法开始束执行！");
            TimeUnit.SECONDS.sleep(6);
            System.out.println("executor2方法结束执行！！！！");
            int i = 1/ 0;
            return "executor2";
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

    }

    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(key)){
           this.asyncSerive.deleteCartByUserIdAndSkuId(userId,skuId);
           hashOps.delete(skuId.toString());
        }
    }
}
