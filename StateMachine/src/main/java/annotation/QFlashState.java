/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package annotation;

import java.lang.annotation.*;

/**
 * 即时单订单状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午1:30
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface QFlashState {

    /**
     * @return 订单状态
     */
    int state();

    /**
     * @return 是否是终态
     */
    boolean isFinalState() default false;
}
