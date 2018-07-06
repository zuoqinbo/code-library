/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.state.Event;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import org.springframework.stereotype.Component;

import static com.xxx.car.flash.tts.api.constant.FlashUserOrderStatus.*;

/**
 * 202 订单创建状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:49
 */
@SuppressWarnings("unused")
@Component
@QFlashState(state = CREATE_ORDER)
public class FlashCreateOrderState extends AbstractFlashState {

    @Override
    public int handleEvent(Event event) {
        switch (event) {
            case DISPATCH_EVENT:                    //  下单即派发 -> 401
                return ORDER_DISPATCHING;
            case CANCEL_BY_USER_EVENT:              //  用户取消
                return ORDER_CANCELED_BY_USER;
            case CANCEL_BY_BOSS_EVENT:              //  客服取消
                return ORDER_CANCELED_BY_BOSS;
            case CANCEL_BY_SYSTEM_EVENT:            //  系统取消
                return ORDER_CANCELED_BY_SYSTEM;
            default:
                return getState();
        }
    }
}
