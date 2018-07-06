/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.state.Event;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import org.springframework.stereotype.Component;



/**
 * 606 派单失败状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:49
 */
@SuppressWarnings("unused")
@Component
@QFlashState(state = ORDER_CANCELED_BY_DISPATCH_FAILED, isFinalState = true)
public class FlashDispatchCancelState extends AbstractFlashState {

    @Override
    public int handleEvent(Event event) {
        return getState();
    }
}
