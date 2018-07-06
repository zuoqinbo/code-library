/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.state.Event;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import org.springframework.stereotype.Component;


/**
 * 405 司机出发状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:49
 */
@SuppressWarnings("unused")
@Component
@QFlashState(state = ORDER_PASSENGER_TAKEN)
public class FlashPassengerTakenState extends AbstractFlashState {

    @Override
    public int handleEvent(Event event) {
        switch (event) {
            case PASSENGER_ARRIVE_EVENT:  //  乘客到达目的地
                return ORDER_PASSENGER_ARRIVE;
            default:
                return getState();
        }
    }
}
