import com.qunar.car.flash.common.interceptor.BaseControllerInterceptor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Created on 2018/4/26.
 *
 * @author jiawei
 */
@Component
@Aspect
public class ControllerInterceptor extends BaseControllerInterceptor {

    @Pointcut("(execution(public * com.qunar.car.flash.marketing.controller.*.*(..)) )" +
            "|| (execution(public * com.qunar.car.flash.common.spring.dubbo.*.*(..)))")
    @Override
    public void onRequest() {
    }

}
