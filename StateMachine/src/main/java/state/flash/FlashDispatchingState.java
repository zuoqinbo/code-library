/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.state.Event;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import org.springframework.stereotype.Component;

import static com.xxx.car.flash.tts.api.constant.FlashUserOrderStatus.*;


/**
 * 401 派单中状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:49
 */
@SuppressWarnings("unused")
@Component
@QFlashState(state = ORDER_DISPATCHING)
public class FlashDispatchingState extends AbstractFlashState {

    @Override
    public int handleEvent(Event event) {
        switch (event) {
            case DRIVER_TAKEN_EVENT:                //  司机接单
                return ORDER_DRIVER_TAKEN;
            case CANCEL_BY_BOSS_EVENT:              //  客服取消
                return ORDER_CANCELED_BY_BOSS;
            case DISPATCH_FAILED_EVENT:             //  派单失败取消
                return ORDER_CANCELED_BY_DISPATCH_FAILED;
            case CANCEL_BY_USER_EVENT:              //  用户取消
                return ORDER_CANCELED_BY_USER;
            case CANCEL_BY_SYSTEM_EVENT:            //  系统取消
                return ORDER_CANCELED_BY_SYSTEM;
            default:
                return getState();
        }
    }
}
