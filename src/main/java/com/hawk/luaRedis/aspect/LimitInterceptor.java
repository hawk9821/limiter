package com.hawk.luaRedis.aspect;

import com.google.common.collect.ImmutableList;
import com.hawk.luaRedis.annotation.Limit;
import com.hawk.luaRedis.enums.LimitType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @Author hawk9821
 * @Date 2020-04-12
 * @Description 限流切面实现
 */
@Aspect
@Configuration
@Slf4j
public class LimitInterceptor {

    @Autowired
    @Qualifier("limitRedisTemplate")
    private RedisTemplate<String, Serializable> limitRedisTemplate;

    private static final String UNKNOWN = "unknown";


    /**
     * @Author hawk9821
     * @Description 切面
     * @Date 2020/4/13 11:10
	 * @param pjp
	 * @return java.lang.Object
    **/
    @Around("execution(public * *(..)) && @annotation(com.hawk.luaRedis.annotation.Limit)")
    public Object interceptor(ProceedingJoinPoint pjp){
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Limit limitAnnotation = method.getAnnotation(Limit.class);
        LimitType limitType = limitAnnotation.limitType();
        String name = limitAnnotation.name();
        String key;
        int limitPeriod = limitAnnotation.period();
        int limitCount = limitAnnotation.count();
        /**
         * 根据限流类型获取不同的key ,如果不传我们会以方法名作为key
         */
        switch (limitType) {
            case IP:
                key = getIpAddress();
                break;
            case CUSTOMER:
                key = limitAnnotation.key();
                break;
            case DEFAULT:
            default:
                key = method.getDeclaringClass()+"."+method.getName();
        }

        ImmutableList<String> keys = ImmutableList.of(StringUtils.join(limitAnnotation.prefix(), key));
        try {
            String luaScript = buildLuaScript();
            RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
            Number count = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);
            log.info("Access try count is {} for name={} and key = {}", count, name, key);
            if (count != null && count.intValue() <= limitCount) {
                return pjp.proceed();
            } else {
                throw new RuntimeException("You have been dragged into the blacklist");
            }
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw new RuntimeException(e.getLocalizedMessage());
            }
            throw new RuntimeException("server exception");
        }
    }

    private String buildLuaScript() {
        StringBuilder lua = new StringBuilder();
        lua.append("local c\n");
        lua.append("c = redis.call('get',KEYS[1])\n");
        // 调用不超过最大值，则直接返回
        lua.append("if c and tonumber(c) > tonumber(ARGV[1]) then\n");
        lua.append("return c;\n");
        lua.append("end\n");
        // 执行计算器自加
        lua.append("c = redis.call('incr',KEYS[1])\n");
        lua.append("if tonumber(c) == 1 then\n");
        // 从第一次调用开始限流，设置对应键值的过期
        lua.append("redis.call('expire',KEYS[1],ARGV[2])\n");
        lua.append("end\n");
        lua.append("return c;");
        return lua.toString();
    }

    private String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * @Author hawk9821
     * @Description
     * @Date 2020/4/12 23:29 
     * @Param [xxx, b]
     * @Return java.lang.String
    **/
    public String test(String xxx,int b){

        return xxx;
    }
}
