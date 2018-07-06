/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.state.Event;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import org.springframework.stereotype.Component;

import static com.xxx.car.flash.tts.api.constant.FlashUserOrderStatus.ORDER_FINISHED;
import static com.xxx.car.flash.tts.api.constant.FlashUserOrderStatus.ORDER_NEED_PAY;


/**
 * 501 司机出发状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:49
 */
@SuppressWarnings("unused")
@Component
@QFlashState(state = ORDER_NEED_PAY)
public class FlashOrderNeedPayState extends AbstractFlashState {

    @Override
    public int handleEvent(Event event) {
        switch (event) {
            case PAY_SUCCESS_EVENT:     //  支付成功
                return ORDER_FINISHED;
            default:
                return getState();
        }
    }
}
