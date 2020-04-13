package com.hawk.luaRedis.annotation;

import com.hawk.luaRedis.enums.LimitType;

import java.lang.annotation.*;

/**
 * @Author hawk9821
 * @Date 2020-04-12
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Limit {
    /**
     * 名字
     */
    String name() default "";

    /**
     * key
     */
    String key() default "";

    /**
     * Key的前缀
     */
    String prefix() default "";

    /**
     * 给定的时间范围 单位(秒)
     */
    int period();

    /**
     * 一定时间内最多访问次数
     */
    int count();

    /**
     * 限流的类型(用户自定义key，请求ip，接口方法名)
     */
    LimitType limitType() default LimitType.DEFAULT;
}
