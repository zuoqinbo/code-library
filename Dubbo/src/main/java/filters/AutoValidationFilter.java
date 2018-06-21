package filters;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.validation.Validation;
import com.alibaba.dubbo.validation.Validator;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.ArrayUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动参数校验Filter
 *
 * Created by ljjing.luo on 2018/4/17.
 */
@Activate(group = {Constants.CONSUMER, Constants.PROVIDER}, order = -1000)
public class AutoValidationFilter implements Filter {

    private final Logger logger;

    private Validation validation;

    public AutoValidationFilter() {
        this(LoggerFactory.getLogger(AutoValidationFilter.class));
    }

    public AutoValidationFilter(Logger logger) {
        this.logger = logger;
        logger.info("Dubbo接口参数自动校验功能已启用");
    }

    public void setValidation(Validation validation) {
        this.validation = validation;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        if (validation == null || invocation.getMethodName().startsWith("$") || ArrayUtils.isEmpty(invocation.getArguments())) {
            return invoker.invoke(invocation);
        }

        try {

            Validator validator = validation.getValidator(invoker.getUrl());
            if (null != validator) {

                try {
                    validator.validate(invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments());
                } catch (ConstraintViolationException e) {
                    logger.error("dubbo请求参数自动校验不通过,service:" + invoker.getInterface().getName() + ",method:" + invocation.getMethodName() + ",detail:" + e.getConstraintViolations());
                    List<String> messageList = new ArrayList<>();
                    for(ConstraintViolation constraintViolation : e.getConstraintViolations()){
                        messageList.add(constraintViolation.getMessage());
                    }
                    return new RpcResult(Response.builder().failBStatus(ResponseCode.PARAM_ERROR, "参数校验不通过:" + Joiner.on(",").join(messageList)).build());
                }

                for (Object arg : invocation.getArguments()) {
                    if (arg instanceof ValidatableRequest && !((ValidatableRequest) arg).isValid()) {
                        logger.error("dubbo请求参数自动校验自定义规则不通过,service:" + invoker.getInterface().getName() + ",method:" + invocation.getMethodName() + ",argumentType:" + arg.getClass().getSimpleName());
                        return new RpcResult(Response.builder().failBStatus(ResponseCode.PARAM_ERROR, "参数" + arg.getClass().getSimpleName() + "自定义校验不通过").build());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("dubbo请求参数自动校验过程抛出未捕获异常,service:" + invoker.getInterface().getName() + ",method:" + invocation.getMethodName(), e);
            return new RpcResult(Response.builder().failBStatus(ResponseCode.UNKNOWN_ERROR, "未知错误").build());
        }

        return invoker.invoke(invocation);
    }
}
