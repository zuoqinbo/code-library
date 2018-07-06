package aop.annotation;

import java.lang.annotation.*;

/**
 * 基于redis的分布式锁
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DistributedLock {

    /** redis bean name */
    String redisBeanName() default "";

    /** 过期时间(秒) */
    int expire() default 60;

    /** 锁标识前缀,可用于区分业务场景 */
    String prefix() default "";

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    @interface Key {

        /** 是否可省略,至少要有一个不可省略的key */
        boolean optional() default false;

        /** 字段名称,复杂对象时须指定 */
        String field() default "";
    }
}
