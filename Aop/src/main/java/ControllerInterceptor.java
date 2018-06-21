import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 */
@Component
@Aspect
public class ControllerInterceptor extends BaseControllerInterceptor {

    @Pointcut("execution(public *.marketing.controller.*.*(..))")
    @Override
    public void onRequest() {
    }
    @Around("onRequest()")
    public Object handleRequest(ProceedingJoinPoint joinPoint) {
        if (ReqLogDataManager.TRACEID_DEFAULT.equals(ReqLogDataManager.getTraceId())) {
            ReqLogDataManager.setTraceId(
                    QTraceUtil.boundQTracerID(UUID.randomUUID().toString()));
        }

        return super.handleRequest(joinPoint);
    }
}
