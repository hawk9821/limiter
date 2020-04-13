package com.hawk.luaRedis.controller;

import com.hawk.luaRedis.annotation.Limit;
import com.hawk.luaRedis.enums.LimitType;
import lombok.ToString;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author hawk9821
 * @Date 2020-04-12
 */
@ToString
@RestController
public class LimiterController {
    private static final AtomicInteger ATOMIC_INTEGER_1 = new AtomicInteger();
    private static final AtomicInteger ATOMIC_INTEGER_2 = new AtomicInteger();
    private static final AtomicInteger ATOMIC_INTEGER_3 = new AtomicInteger();

    /**
     * @Author hawk9821
     * @Description 未指定 limitType 按照 方法名 限流，10秒内最多接收3次请求
     * @Date 2020/4/13 11:02
	 * @return int
    **/
    @Limit(key = "limitTest", period = 10, count = 3)
    @GetMapping("/limitTest1")
    public int testLimiter1() {
        return ATOMIC_INTEGER_1.incrementAndGet();
    }

    /**
     * @Author hawk9821
     * @Description 按照 key 限流，10秒内最多接收3次请求
     * @Date 2020/4/13 11:01
	 * @return int
    **/
    @Limit(key = "customer_limit_test", period = 10, count = 3, limitType = LimitType.CUSTOMER)
    @GetMapping("/limitTest2")
    public int testLimiter2() {
        return ATOMIC_INTEGER_2.incrementAndGet();
    }
    /**
     * @Author hawk9821
     * @Description 按照 IP 限流，10秒内最多接收3次请求
     * @Date 2020/4/13 11:00
	 * @return int
    **/
    @Limit(key = "ip_limit_test", period = 10, count = 3, limitType = LimitType.IP)
    @GetMapping("/limitTest3")
    public int testLimiter3() {
        return ATOMIC_INTEGER_3.incrementAndGet();
    }



}
