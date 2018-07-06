package aop;

import com.google.common.base.Joiner;
import com.xxx.car.flash.common.pojo.Response;
import com.xxx.car.flash.dispatch.aop.annotation.IdempotentCheck;
import com.xxx.car.flash.dispatch.api.model.req.UniqueRequest;
import com.xxx.car.flash.dispatch.constant.DspResponseCode;
import com.xxx.car.flash.dispatch.qconfig.DispatchCommonConfig;
import com.xxx.car.flash.dispatch.utils.CacheKeyUtil;
import com.xxx.mobile.carpool.ztc.log.UnifyLogger;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import xxx.tc.qmq.Message;
import xxx.tc.qmq.NeedRetryException;
import redis.clients.jedis.JedisCommands;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * 幂等控制切面
 * 1.支持QMQ消息幂等
 * 2.支持Dubbo请求幂等(第一个请求参数包含reqKey)
 *
 * Created by ljjing.luo on 2018/4/13.
 */
@Aspect
@Component
public class IdempotentCheckAspect {

    private static final String EXIST_VALUE = "1";
    private static final String FINISH_VALUE = "2";

    @Resource
    private JedisCommands sedis;

    @Resource
    private DispatchCommonConfig dispatchCommonConfig;

    @Pointcut("@annotation(com.xxx.car.flash.dispatch.aop.annotation.IdempotentCheck)")
    private void idempotentPointcut() {
    }

    @Around("idempotentPointcut()")
    public Object execution(ProceedingJoinPoint joinPoint) throws Throwable {

        if (ArrayUtils.isEmpty(joinPoint.getArgs())) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 取实现类的Method
        if (method.getDeclaringClass().isInterface()) {
            method = MethodUtils.getAccessibleMethod(joinPoint.getTarget().getClass(), method.getName(), signature.getParameterTypes());
        }

        IdempotentCheck idempotentCheck = method.getAnnotation(IdempotentCheck.class);
        if (null == idempotentCheck) {
            return joinPoint.proceed();
        }

        Object arg = joinPoint.getArgs()[0];
        if (arg instanceof Message) {
            return checkQmq(joinPoint, (Message) arg);
        } else if (arg instanceof UniqueRequest) {
            return checkUniqueRequest(joinPoint, (UniqueRequest) arg);
        }

        return joinPoint.proceed();
    }

    private Object checkQmq(ProceedingJoinPoint joinPoint, Message message) throws Throwable {

        String uniqueKey = CacheKeyUtil.genQmqMessageKey(message.getMessageId());
        String ok = sedis.set(uniqueKey, EXIST_VALUE, "NX", "EX",
                dispatchCommonConfig.getQmqIdempotentCacheTime());
        if (!StringUtils.equalsIgnoreCase(ok, "OK")) { // key已存在

            String result = sedis.get(uniqueKey);
            if (StringUtils.equalsIgnoreCase(result, FINISH_VALUE)) {
                // 上一次处理已完成
                UnifyLogger.info("消息命中幂等:{}", message);
                return null;
            }

            // 目前成功状态未知,等一会儿在重投一次
            Long ttl = sedis.ttl(uniqueKey);
            throw new NeedRetryException(System.currentTimeMillis() + ttl, "消息已在处理中");
        }

        Object ret = joinPoint.proceed();

        // 消息处理成功,设置幂等缓存
        sedis.setex(uniqueKey, dispatchCommonConfig.getQmqIdempotentCacheTime(), FINISH_VALUE);
        return ret;
    }


    private Object checkUniqueRequest(ProceedingJoinPoint joinPoint, UniqueRequest req) throws Throwable {

        String key = Joiner.on(":").join("ur", req.getSource(), req.getReqKey());
        String ok = sedis.set(CacheKeyUtil.genReqUniqueKey(key), EXIST_VALUE, "NX", "EX",
                dispatchCommonConfig.getDubboIdempotentCacheTime());

        if (!StringUtils.equalsIgnoreCase(ok, "OK")) {
            UnifyLogger.info("UniqueRequest请求命中幂等:{},req:{}", joinPoint.getSignature(), key);
            return Response.builder().failBStatus(DspResponseCode.DUPLICATE_REQUEST, "重复的请求:" + key).build();
        }

        return joinPoint.proceed();
    }

}
