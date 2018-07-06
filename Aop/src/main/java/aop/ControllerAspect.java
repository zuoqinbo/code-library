package aop;

import com.xxx.car.flash.common.interceptor.BaseControllerInterceptor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ControllerAspect extends BaseControllerInterceptor {

    @Override
    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void onRequest() {

    }
}
