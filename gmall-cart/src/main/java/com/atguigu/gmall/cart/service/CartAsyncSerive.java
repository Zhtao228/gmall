package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncSerive {

    @Autowired
    private CartMapper cartMapper;

    @Async
    public void updateCart(String userId, String skuId, Cart cart) {
        this.cartMapper.update(cart,new UpdateWrapper<Cart>()
                .eq("userId",userId)
                .eq("skuId",skuId));
    }

    @Async
    public void insertCart(Cart cart) {
        cartMapper.insert(cart);
    }

    @Async
    public void deleteByUserId(String userKey) {
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("userId",userKey));
    }

    public void deleteCartByUserIdAndSkuId(String userId, Long skuId) {
        this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuId));
    }
}
