package aop;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.xxx.car.flash.common.constants.ResponseCode;
import com.xxx.car.flash.common.exceptions.BusinessException;
import com.xxx.car.flash.common.pojo.BStatus;
import com.xxx.car.flash.common.spring.SpringAppContext;
import com.xxx.car.flash.common.utils.JsonUtils;
import com.xxx.car.flash.dispatch.aop.annotation.DistributedLock;
import com.xxx.car.flash.dispatch.constant.DspResponseCode;
import com.xxx.car.flash.dispatch.utils.AssertUtil;
import com.xxx.mobile.carpool.ztc.log.UnifyLogger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import redis.clients.jedis.JedisCommands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 基于redis的分布式锁切面
 * 1. 支持在方法上添加注解的方式实现分布式锁的自动锁定和释放
 * 2. 也可以自定义一个DistributedLockTemplate的bean进行编程式控制
 *
 * @see DistributedLock
 * @see DistributedLock.Key
 * Created by ljjing.luo on 2018/4/16.
 */
@Aspect
@Component
public class DistributedLockAspect {

    /** 默认的redis,指定之后大部分场景不需要在DistributedLock注解中指定redisBeanName */
    @Value("${distributed.lock.default.redis.bean.name:#{null}}")
    private String defaultRedisBeanName;

    /** template缓存 */
    private Map<String, DistributedLockTemplate> templateMap = Maps.newHashMap();

    @Pointcut("@annotation(com.xxx.car.flash.dispatch.aop.annotation.DistributedLock)")
    public void distributedLockPointcut() {
    }

    @Around("distributedLockPointcut()")
    public Object execution(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 取实现类的Method
        if (method.getDeclaringClass().isInterface()) {
            method = MethodUtils.getAccessibleMethod(joinPoint.getTarget().getClass(), method.getName(), signature.getParameterTypes());
        }

        final DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        if (null == distributedLock) {
            return null;
        }

        String lockKey = parseDistributedLockKey(method, joinPoint.getArgs());
        AssertUtil.assertTure(StringUtils.isNotEmpty(lockKey), "lockKey为空:" + signature);
        if (StringUtils.isNotEmpty(distributedLock.prefix())) {
            lockKey = distributedLock.prefix() + "_" + lockKey;
        }

        UnifyLogger.info("准备获取分布式锁并执行,method:{}, lockKey={}, args={}", method.getName(), lockKey, JsonUtils.toJson(joinPoint.getArgs()));

        return getDistributedLockTemplate(distributedLock.redisBeanName())
                .executeWithLock(lockKey, distributedLock.expire(), () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (BusinessException e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new BusinessException(new BStatus(ResponseCode.UNKNOWN_ERROR, "未知错误"), e);
                    }
                });
    }

    private String parseDistributedLockKey(Method method, Object[] args) {
        if (null == args || args.length == 0) {
            throw new IllegalStateException("DistributedLock注解标注的方法必须带参数");
        }

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (null == parameterAnnotations || parameterAnnotations.length == 0) {
            throw new IllegalStateException("方法参数中缺少DistributedLock.key");
        }

        List<String> valueList = Lists.newArrayList();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof DistributedLock.Key) {
                    DistributedLock.Key key = (DistributedLock.Key) parameterAnnotations[i][j];

                    Object value = getKeyItemValue(key, args[i]);

                    if (null == value && key.optional()) {
                        continue;
                    }
                    AssertUtil.assertNotNull(value, "key注解的参数值不能为null,method:" + method.getName() + ",index:" + i);

                    if (value.getClass().isArray()) {
                        throw new IllegalArgumentException("Key注解的参数不支持数组类型,method:" + method.getName() + ",index:" + i);
                    }
                    valueList.add(value.toString());
                }
            }
        }

        return Joiner.on("-").skipNulls().join(valueList);
    }

    private Object getKeyItemValue(DistributedLock.Key key, Object parameterValue) {

        if (null == parameterValue) {
            return null;
        }
        if (BeanUtils.isSimpleValueType(parameterValue.getClass())) {
            return parameterValue;
        }

        if (StringUtils.isEmpty(key.field())) {
            throw new IllegalArgumentException("key注解在复杂对象上,但field参数未指定");
        }

        Field field = FieldUtils.getField(parameterValue.getClass(), key.field(), true);
        if (null == field) {
            if (key.optional()) {
                return null;
            }
            throw new IllegalStateException("key注解标注的字段未找到:" + key.field() + ",class:" + parameterValue.getClass().getSimpleName());
        }

        return ReflectionUtils.getField(field, parameterValue);
    }

    private DistributedLockTemplate getDistributedLockTemplate(String redisBeanName) {

        redisBeanName = StringUtils.defaultIfEmpty(redisBeanName, defaultRedisBeanName);
        AssertUtil.assertNotNull(redisBeanName, "redisBeanName未指定,默认值也未指定");

        DistributedLockTemplate template = templateMap.get(redisBeanName);
        if (null == template) { // 暂不考虑并发,不影响使用
            JedisCommands jedis = SpringAppContext.AppContext.getBean(JedisCommands.class, defaultRedisBeanName);

            template = new DistributedLockTemplate();
            template.setJedis(jedis);
            templateMap.put(redisBeanName, template);
        }

        return template;
    }

    /**
     * 分布式锁处理模版
     * 如果需要编程式进行分布式锁控制，可以单独定义一个DistributedLockTemplate的bean
     */
    public class DistributedLockTemplate {

        private static final String LOCKED = "LOCKED";

        private JedisCommands jedis;

        public <V> V executeWithLock(String lockKey, long expire, Callable<V> callback) {

            try {
                if ("OK".equalsIgnoreCase(jedis.set(lockKey, LOCKED, "NX", "EX", expire))) {
                    UnifyLogger.info("分布式锁获取成功,lockKey={}", lockKey);
                    return callback.call();
                }

                UnifyLogger.info("分布式锁已被抢占,本次获取失败,lockKey={}", lockKey);
                throw new BusinessException(new BStatus(DspResponseCode.DATA_LOCK_FAIL, "锁竞争失败"));
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(new BStatus(ResponseCode.UNKNOWN_ERROR, "未知错误"), e);
            } finally {
                try {
                    jedis.del(lockKey);
                } catch (Exception e) {
                    UnifyLogger.error("DistributedLockTemplate", "DistributedLockTemplate_releaseFail", false, "删除redis缓存数据失败", e);
                }
            }
        }

        public void setJedis(JedisCommands jedis) {
            this.jedis = jedis;
        }
    }
}
