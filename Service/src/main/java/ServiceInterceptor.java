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
public class ServiceInterceptor extends BaseServiceInterceptor{

    @Pointcut("execution(public * com.qunar.car.flash.marketing.service.impl.*.*(..))")
    @Override
    public void onRequest() {

    }
}
