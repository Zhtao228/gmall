package com.atguigu.gmall.cart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CartController {

    @GetMapping("test")
    @ResponseBody
    public String test(){
        return "hello!";
    }
}
