package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public String addCart(Cart cart){
        if (cart == null || cart.getSkuId() == null){
            throw new RuntimeException("没有选择添加到购物车的商品信息！");
        }
        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId="
                + cart.getSkuId() + "&count=" + cart.getCount();
    }

    @GetMapping("addCart.html")
    public String queryCart(Model model, Cart cart){
        BigDecimal count = cart.getCount();
        cart = this.cartService.queryCart(cart.getSkuId());
        cart.setCount(count);
        model.addAttribute("cart",cart);
        return "addCart";
    }

    @GetMapping("updateNum")
    @ResponseBody
    public ResponseVo<Object> updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo<Object> deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }



    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> cartList = this.cartService.queryCarts();
        model.addAttribute("carts",cartList);
        return "cart";
    }

    @GetMapping("test")
    @ResponseBody
    public String test() throws ExecutionException, InterruptedException {
        long now = System.currentTimeMillis();
        System.out.println("controller.test方法开始执行！");

        cartService.executor2();

//        this.cartService.executor1().addCallback(new SuccessCallback<String>() {
//            @Override
//            public void onSuccess(String s) {
//                System.out.println("future1  的正常执行结果 ：" + s);
//            }
//        }, new FailureCallback() {
//            @Override
//            public void onFailure(Throwable throwable) {
//                System.out.println("future1  的cuowu执行结果 ：" + throwable.getMessage());
//            }
//        });
//        this.cartService.executor2().addCallback(new SuccessCallback<String>() {
//            @Override
//            public void onSuccess(String s) {
//                System.out.println("future2  的正常执行结果 ：" + s);
//            }
//        }, new FailureCallback() {
//            @Override
//            public void onFailure(Throwable throwable) {
//                System.out.println("future2  的cuowu执行结果 ：" + throwable.getMessage());
//            }
//        });

        System.out.println("controller.test方法结束执行！！！" + (System.currentTimeMillis() - now));

//        UserInfo userInfo = LoginInterceptor.getUserInfo();
//        System.out.printf(userInfo.toString());
        return "hello!";
    }
}
