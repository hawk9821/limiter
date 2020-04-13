### Redis实现接口限流

`spring aop` 对需要限流的接口进行拦截，通过 `redis lua` 处理具体限流逻辑。

具体使用如下： 对需要限流的接口添加 `@Limit` 注解

```java
@Limit(key = "limitTest", period = 10, count = 3)
@GetMapping("/limitTest1")
public int testLimiter1() {
    return ATOMIC_INTEGER_1.incrementAndGet();
}
```

`@Limit` 注解类：

```java
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
```

Aop切面类：

```java
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
        HttpServletRequest request = ((ServletRequestAttributes) 	RequestContextHolder.getRequestAttributes()).getRequest();
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
```





