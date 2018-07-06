/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */

import com.xxx.car.flash.tts.qmq.consumer.delegate.AbstractQmqDelegator;

import java.lang.annotation.*;

/**
 * qmq委托注解
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/4/2 Time: 下午7:17
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QmqDelegate {

    /**
     * @return 委托类class
     */
    Class<? extends AbstractQmqDelegator> value();
}
