package spring;

import java.lang.annotation.*;

/**
 * User: zhaohuiyu
 * Date: 7/5/13
 * Time: 7:28 PM
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface QSchedule {
    /**
     * job的名称，全局唯一
     *
     * @return
     */
    String value() default "";

    /**
     * 共享线程池bean的name
     *
     * @return
     */
    String executor() default "";
}

