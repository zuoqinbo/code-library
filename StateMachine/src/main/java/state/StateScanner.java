package state;

import com.xxx.car.flash.tts.state.flash.AbstractFlashState;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import com.xxx.mobile.car.common.log.UnifyLogger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import static com.xxx.car.flash.common.constants.CommonLogId.LOG_COMM_INFO;

/**
 * 使用状态机<br>
 * 需要spring扫描到这个类<br>
 * @see FlashStateManager 即时单状态管理器
 *
 * Created by zijian.zeng on 2016/8/2.
 * mod by jianyu.lin
 */
@Component
public class StateScanner implements BeanPostProcessor {

    @Autowired
    private FlashStateManager flashStateManager;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        UnifyLogger.info(LOG_COMM_INFO.code, true,
                "StateScanner postProcessAfterInitialization." + bean);

        QFlashState qFlashState = AnnotationUtils.findAnnotation(bean.getClass(), QFlashState.class);
        if (qFlashState != null) {
            if (bean instanceof AbstractFlashState) {
                AbstractFlashState flashUpState = (AbstractFlashState) bean;
                flashUpState.setState(qFlashState.state());
                flashStateManager.registerHandleEvent(flashUpState.getState(), flashUpState);
                return bean;
            }
        }
        return bean;
    }
}
