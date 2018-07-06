/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.util.annotation.QFlashState;
import org.springframework.stereotype.Component;

import static com.xxx.car.flash.tts.api.constant.FlashUserOrderStatus.ORDER_RE_DISPATCHING;


/**
 * 4011 重新派单中状态
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:49
 */
@SuppressWarnings("unused")
@Component
@QFlashState(state = ORDER_RE_DISPATCHING)
public class FlashReDispatchingState extends FlashDispatchingState {
}
