/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package com.xxx.car.flash.tts.spring.aop;

import com.xxx.car.flash.common.exceptions.BusinessException;
import com.xxx.car.flash.common.spring.SpringAppContext;
import com.xxx.car.flash.tts.qmq.consumer.delegate.AbstractQmqDelegator;
import com.xxx.car.flash.tts.util.annotation.QmqDelegate;
import org.apache.commons.lang.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import xxx.tc.qmq.Message;

import java.lang.reflect.Method;

import static com.xxx.car.flash.tts.constants.ResponseCode.SYSTEM_INNER_ERROR;

/**
 * qmq消息委托处理
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/4/2 Time: 下午7:28
 */
@Component
@Aspect
public class QmqConsumerDelegateAspect {

    @Before("@annotation(com.xxx.car.flash.tts.util.annotation.QmqDelegate)")
    public void onReceiveQmq(JoinPoint point) throws Exception {
        //  check first param
        if (ArrayUtils.isEmpty(point.getArgs()) || !(point.getArgs()[0] instanceof Message)) {
            throw new BusinessException(SYSTEM_INNER_ERROR, "@QmqDelegate注释方法首参数必须是 xxx.tc.qmq.Message类型");
        }
        //  qmq message
        Message message = (Message) point.getArgs()[0];
        //  invoke delegate
        AbstractQmqDelegator delegator = getDelegator(getQmqDelegate(point));
        if (delegator == null) {
            throw new BusinessException(SYSTEM_INNER_ERROR, "@QmqDelegate委托类未被spring容器管理，请添加 @Service| @Component");
        }
        delegator.invokeDelegate(message);
    }

    /**
     * parse @QmqDelegate
     * @param point joint point
     * @return @QmqDelegate
     */
    private QmqDelegate getQmqDelegate(JoinPoint point) throws Exception {
        Class<?> clazz = point.getTarget().getClass();
        MethodSignature signature = ((MethodSignature) point.getSignature());
        Method method = clazz.getDeclaredMethod(signature.getName(), signature.getParameterTypes());
        return method.getDeclaredAnnotation(QmqDelegate.class);
    }

    /**
     * 从spring容器获取委托类实例<br>
     * 注意：委托实例线程不安全请添加 @Scope("prototype")
     * @param qmqDelegate 委托注解
     * @return 委托类实例
     */
    private AbstractQmqDelegator getDelegator(QmqDelegate qmqDelegate) throws Exception {
        Class<? extends AbstractQmqDelegator> delegateClass = qmqDelegate.value();
        return SpringAppContext.AppContext.getBean(delegateClass);
    }
}
