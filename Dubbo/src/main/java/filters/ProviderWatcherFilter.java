package filters;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.google.common.base.Strings;
import com.qunar.car.flash.common.exceptions.BusinessException;
import com.qunar.car.flash.common.metrics.ZtcMetrics;
import com.qunar.car.flash.common.metrics.ZtcMetrics.Context;
import com.qunar.car.flash.common.monitor.util.ErrorCodeUtil;
import com.qunar.car.flash.common.utils.JsonUtils;
import com.qunar.car.trace.util.QTraceUtil;
import com.qunar.mobile.car.common.log.ReqLogDataManager;
import com.qunar.mobile.car.common.log.UnifyLogger;
import com.qunar.mobile.car.common.log.consts.CommReqParamKeyConst;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import qunar.tc.qmq.base.BaseMessage;
import qunar.tc.qmq.service.ConsumerMessageHandler;

import java.util.UUID;


@Activate(group = Constants.PROVIDER)
public class ProviderWatcherFilter implements Filter{

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation)
			throws RpcException {
		 Result result = null;
		 Throwable expt = null;
		 Context context = null;
		 try {
			Class<?> clazz = invoker.getInterface();
			if (ConsumerMessageHandler.class.isAssignableFrom(clazz)) {
				// qmq 消费者
				try {
					Object param = invocation.getArguments()[0];
					if (param instanceof BaseMessage) {
						BaseMessage message = (BaseMessage) param;
						String prefix = message.getStringProperty(BaseMessage.keys.qmq_prefix);
						prefix = StringUtils.replace(prefix, ".", "_");
						
						if (isQschedule(message)) {
							context = ZtcMetrics.timerContext("qschdule." +  prefix);
						}else {
							setCarTrace(message);//设置专车traceId
							context = ZtcMetrics.timerContext("qmq.consumer." +  prefix);
						}
					}
					
				} catch (Exception e) {
					UnifyLogger.error("qmq-interceptor", "qmq拦截错误", false, "expt:" + ExceptionUtils.getFullStackTrace(e));
				}
			}else {
				context = ZtcMetrics.timerContext("dubbo.provider." + invoker.getInterface().getSimpleName() + "." + invocation.getMethodName());
			}
            result = invoker.invoke(invocation);
            return result;
         } catch (Exception e) {
        	expt = e;
        	UnifyLogger.error("COMMON", "", false, "异常:" + invocation.getMethodName() + ",PARAMS:" + JsonUtils.toJson(invocation.getArguments()), e);
            throw new RuntimeException(e);
         } finally{
        	 try {
             	doMonitor(result, invoker, context, expt);
             } catch (Throwable e) {
            	 UnifyLogger.error("COMMON-ERR", "", false, "Fail to watch when called by " + RpcContext.getContext().getRemoteHost()
             			+ ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()
             			+ ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
             }
         }
	}

	private void setCarTrace(BaseMessage message) {
		ReqLogDataManager.reqInit();
		String traceId = message
				.getStringProperty(CommReqParamKeyConst.PARAM_TRACEID);
		if (Strings.isNullOrEmpty(traceId)) {
			traceId = UUID.randomUUID().toString();
		}
		ReqLogDataManager.setTraceId(QTraceUtil.boundQTracerID(traceId));
	}

	private boolean isQschedule(BaseMessage message) {
		Boolean isQs = (Boolean) message.getProperty("qschedule");
		return BooleanUtils.isTrue(isQs);
	}

	private void doMonitor(Result result, Invoker<?> invoker, Context context, Throwable expt) {
		String errCode = null;
		if ((expt != null || result.hasException()) && GenericService.class != invoker.getInterface()) {
			//如果有异常
			if (expt == null) {
				expt = result.getException();
			}
			errCode = ErrorCodeUtil.parseErrCode(expt);
			if(! (expt instanceof BusinessException)) {
				UnifyLogger.error("dubbo.provider", "dubbo.provider.exception", true, "dubbo执行异常,invoker:" + invoker.getInterface() + ",expt:" + ExceptionUtils.getFullStackTrace(expt));
			}
		}
		//记录次数，时间，错误码
		if (context != null) {
			context.stopWithErrCode(errCode);
		}
	}

}
