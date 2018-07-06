/*
 * Copyright (c) 2018 xxx.com. All Rights Reserved.
 */
package state.flash;

import com.xxx.car.flash.tts.state.Event;
import lombok.Data;

/**
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:04
 */
@Data
public abstract class AbstractFlashState {

    private int state;

    /**
     * @param event 事件
     * @return 状态流转事件处理
     */
    public abstract int handleEvent(Event event);
}
