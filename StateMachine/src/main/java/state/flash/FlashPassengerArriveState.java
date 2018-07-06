/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.state.Event;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import org.springframework.stereotype.Component;

import static com.xxx.car.flash.tts.api.constant.FlashUserOrderStatus.ORDER_NEED_PAY;
import static com.xxx.car.flash.tts.api.constant.FlashUserOrderStatus.ORDER_PASSENGER_ARRIVE;


/**
 * 406 司机出发状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:49
 */
@SuppressWarnings("unused")
@Component
@QFlashState(state = ORDER_PASSENGER_ARRIVE)
public class FlashPassengerArriveState extends AbstractFlashState {

    @Override
    public int handleEvent(Event event) {
        switch (event) {
            case DRIVER_CONFIRM_BILL_EVENT:  //  乘客到达目的地
                return ORDER_NEED_PAY;
            default:
                return getState();
        }
    }
}
