package filters;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.qunar.car.flash.common.metrics.ZtcMetrics;
import com.qunar.car.flash.common.metrics.ZtcMetrics.Context;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.List;


@Activate(group = Constants.CONSUMER)
public class ConsumerWatcherFilter implements Filter{

	private final Logger logger;

	public ConsumerWatcherFilter() {
		this(LoggerFactory.getLogger(ConsumerWatcherFilter.class));
	}

	public ConsumerWatcherFilter(Logger logger) {
		this.logger = logger;
	}

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation)
			throws RpcException {
		 Result result = null;
		 Throwable expt = null;
		 Context context = null;
		 try {
			 Class<?> clazz = invoker.getInterface();
			if (BrokerMessageService.class.isAssignableFrom(clazz)) {
				// qmq producer
				try {
					Object firstArg = invocation.getArguments()[0];
					if (firstArg instanceof List) {
						firstArg = ((List<?>) firstArg).get(0);
					}
					if (firstArg instanceof BaseMessage) {
						BaseMessage message = (BaseMessage) firstArg;
						String prefix = message.getSubject();
						prefix = StringUtils.replace(prefix, ".", "_");
						context = ZtcMetrics.timerContext("qmq.producer." + prefix);
					}
				} catch (Exception e) {
					UnifyLogger.error("qmq-interceptor", "qmq拦截错误", false,
							"expt:" + ExceptionUtils.getFullStackTrace(e));
				}
			} else {
				context = ZtcMetrics.timerContext("dubbo.consumer." + invoker.getInterface().getSimpleName() + "." + invocation.getMethodName());
			}
            result = invoker.invoke(invocation);
            return result;
         } catch (Exception e) {
        	expt = e;
            throw new RuntimeException(e);
         } finally{
        	 try {
             	doMonitor(result, invoker, context, expt);
             } catch (Throwable e) {
             	logger.warn("Fail to watch when called by " + RpcContext.getContext().getRemoteHost()
             			+ ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()
             			+ ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
             }
         }
	}

	private void doMonitor(Result result, Invoker<?> invoker, Context context, Throwable expt) {
		String errCode = null;
		if ((expt != null || result.hasException()) && GenericService.class != invoker.getInterface()) {
			//如果有异常
			if (expt == null) {
				expt = result.getException();
			}
			errCode = ErrorCodeUtil.parseErrCode(expt);
			UnifyLogger.error("dubbo.provider", "dubbo.provider.exception", true, "dubbo执行异常,invoker:" + invoker.getInterface() + ",expt:" + ExceptionUtils.getFullStackTrace(expt));
		}
		//记录次数，时间，错误码
		context.stopWithErrCode(errCode);
	}


}
