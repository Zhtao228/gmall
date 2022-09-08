package com.atguigu.gmall.index.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GmallCache {

    String prefix() default "";

    int timeout() default 5;

    int random() default 5;

    String lock() default "lock";
}