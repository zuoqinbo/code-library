/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.state.Event;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import org.springframework.stereotype.Component;

import static com.xxx.car.flash.tts.api.constant.FlashUserOrderStatus.*;

/**
 * 402 司机接单状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:49
 */
@SuppressWarnings("unused")
@Component
@QFlashState(state = ORDER_DRIVER_TAKEN)
public class FlashDriverTakenState extends AbstractFlashState {

    @Override
    public int handleEvent(Event event) {
        switch (event) {
            case DRIVER_DEPART_EVENT:                   //  司机出发
                return ORDER_DRIVER_DEPARTED;
            case CANCEL_BY_USER_EVENT:                  //  用户取消
                return ORDER_CANCELED_BY_USER;
            case CANCEL_BY_BOSS_EVENT:                  //  客服取消
                return ORDER_CANCELED_BY_BOSS;
            case RE_DISPATCH_BY_DRIVER_CANCEL_EVENT:    //  司机取消重派发
            case RE_DISPATCH_BY_BOSS_CANCEL_EVENT:      //  Boss取消重派
                return ORDER_RE_DISPATCHING;
            default:
                return getState();
        }
    }
}
