/*
 * Copyright (c) 2017 Qunar.com. All Rights Reserved.
 */
package filters;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.qunar.car.flash.common.constants.CommonLogId;
import com.qunar.car.flash.common.constants.ResponseCode;
import com.qunar.car.flash.common.exceptions.BusinessException;
import com.qunar.car.flash.common.pojo.BStatus;
import com.qunar.car.flash.common.pojo.Response;
import com.qunar.mobile.carpool.ztc.log.UnifyLogger;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import qunar.tc.qconfig.client.Configuration;
import qunar.tc.qconfig.client.MapConfig;
import qunar.tc.qmq.service.ConsumerMessageHandler;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * ExceptionInvokerFilter
 * <p>
 * @author william.liangf
 * @author ding.lid
 * @author liang.deng
 * @author jiawei.wang
 */
@Activate(group = Constants.PROVIDER, before = {"providerWatcherFilter", "consumerWatcherFilter"})
public class CustomExceptionFilter implements Filter {
    private final Logger logger;

    private Map<String, String> configMap;

    private static final String CODE_DESC_PREFIX = "flash.bStatus.code.desc.";

    public CustomExceptionFilter() {
        this(LoggerFactory.getLogger(CustomExceptionFilter.class));
        final MapConfig config = MapConfig.get("code_desc.properties");
        this.configMap = config.asMap();
        config.addListener(new Configuration.ConfigListener<Map<String, String>>() {
            @Override
            public void onLoad(Map<String, String> stringStringMap) {
                configMap = stringStringMap;
            }
        });
    }

    public CustomExceptionFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            String methodName = invocation.getMethodName();
            Result result = invoker.invoke(invocation);
            // 无异常或特殊接口直接返回结果
            if (!result.hasException()
                    || ConsumerMessageHandler.class == invoker.getInterface()
                    || GenericService.class == invoker.getInterface()) {
                return result;
            }

            // 待处理异常
            Throwable exception = result.getException();

            // 是Dubbo本身的异常，直接抛出
            if (exception instanceof RpcException) {
                return result;
            }

            // 业务异常, 包装成response返回
            if (exception instanceof BusinessException) {
                BusinessException businessException = (BusinessException) exception;
                if(businessException.getAlarmId() == null){
                    businessException.setAlarmId(CommonLogId.FLASH_DUBBO_ERROR);
                }

                BStatus bStatus = businessException.getBStatus();
                if(null != bStatus && StringUtils.isBlank(bStatus.getDes())){
                    bStatus.setDes(MapUtils.getString(configMap, CODE_DESC_PREFIX + bStatus.getCode()));
                }

                UnifyLogger.error(businessException.getAlarmId(),"{} dubbo调用返回业务异常: {}", methodName, businessException);
                return new RpcResult(
                        new Response.Builder<>()
                                .bStatus(businessException.getBStatus())
                                .build()
                );
            }

            // 在方法签名上有声明，直接抛出
            try {
                Method method = invoker.getInterface().getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                Class<?>[] exceptionClasses = method.getExceptionTypes();
                for (Class<?> exceptionClass : exceptionClasses) {
                    if (exception.getClass().equals(exceptionClass)) {
                        return result;
                    }
                }
            } catch (NoSuchMethodException e) {
                logger.error("no such method:" + invocation.getMethodName(), e);
            }

            logger.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()
                    + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()
                    + ", exception: " + exception.getClass().getName() + ": " + exception.getMessage(), exception);

            return new RpcResult(Response.builder().failBStatus(ResponseCode.UNKNOWN_ERROR, "未知错误").build());
        } catch (RuntimeException e) {

            logger.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()
                    + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()
                    + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);

            return new RpcResult(Response.builder().failBStatus(ResponseCode.UNKNOWN_ERROR, "未知错误").build());
        }
    }
}
