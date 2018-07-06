/*
 * Copyright (c) 2015 xxx.com. All Rights Reserved.
 */
package interceptor;


import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.validation.BindingResult;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controller的统一AOP处理
 * 各应用的aop应继承此类,以实现统一逻辑
 * @author jiawei.wang
 */
@SuppressWarnings("unused")
public abstract class BaseControllerInterceptor {
    /**
     * Controller收到所有请求的切点
     */
    public abstract void onRequest();

    @PostConstruct
    private void init() {

    }

    @Around("onRequest()")
    public Object handleRequest(ProceedingJoinPoint joinPoint) {
    	String methodName = getMethodName(joinPoint);
        long startTs = System.currentTimeMillis();
        String key = "httpfacade." + methodName;
        Object ret = null;
        Throwable ept = null;
        Object[] args = joinPoint.getArgs();
        try {
            preHandle(joinPoint);
            return (ret = joinPoint.proceed());
        }catch (Throwable t){
        	ept = t;
        	if (t instanceof BusinessException) {
        		BusinessException expt = (BusinessException) t;
        		return new Response.Builder()
                 	.bStatus(expt.getBStatus())
                 	.build();
			}
            return new Response.Builder()
                    .code(ResponseCode.UNKNOWN_ERROR)
                    .build();
        }finally{
			handleRet(key, args, ret, startTs, ept);
		}
    }

    private void preHandle(ProceedingJoinPoint joinPoint) {

        // 处理参数校验结果
        Object[] args = joinPoint.getArgs();
        if (ArrayUtils.isNotEmpty(args)) {
            for (Object arg : args) {
                if (arg instanceof BindingResult && ((BindingResult) arg).hasErrors()) {
                    String error = ((BindingResult) arg).getFieldError().getField() + ":"
                            + ((BindingResult) arg).getFieldError().getDefaultMessage();
                    throw new BusinessException(new BStatus(ResponseCode.PARAM_ERROR, error));
                }
            }
        }
    }

    private void handleRet(String key, Object[] args, Object ret, long startTs, Throwable ept) {
		long timeCost = System.currentTimeMillis() - startTs;
        UnifyLogger.info("[" + (timeCost)+"ms]key:" + key + ".httpReq:" +
                JsonUtils.toJson(filterMethodArgs(args)) + ",httpRes:" + JsonUtils.toJson(ret));
		if (ept == null) {
			ZtcMetrics.recordTimeAndResult(key, timeCost, TimeUnit.MILLISECONDS, ret);
		}else{
			UnifyLogger.error("FLASH-HTTP-ERR", "FLASH-HTTP-ERR" ,true,
                    "[" + (timeCost)+"ms]key:" + key + ".httpReq:" + JsonUtils.toJson(filterMethodArgs(args)) + ",httpRes:" + JsonUtils.toJson(ret), ept);
			String errCode = ErrorCodeUtil.parseErrCode(ept);
			ZtcMetrics.recordTimeAndCode(key, timeCost, TimeUnit.MILLISECONDS, errCode);
		}
	}

    private static String getMethodName(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        if (signature instanceof MethodSignature) {
            return ((MethodSignature)signature).getMethod().getName();
        } else {
            return signature.getName();
        }
    }

    private List<Object> filterMethodArgs(Object[] objects) {
        if (objects == null) {
            return null;
        }
        List<Object> objList = Lists.newArrayList();
        for (Object object : objects) {
            if (object instanceof HttpServletRequest
                    || object instanceof HttpServletResponse
                    || object instanceof BindingResult) {
                continue;
            }
            objList.add(object);
        }
        return objList;
    }
}
