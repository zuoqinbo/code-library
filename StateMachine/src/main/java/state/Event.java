package state;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.xxx.car.flash.tts.state.OptSystem.*;

/**
 * 状态变更事件
 * Created by zijian.zeng on 2016/7/8.
 */
@AllArgsConstructor
@Getter
public enum Event {

    ORDER_EVENT("用户下单", TTS),
    DISPATCH_EVENT("分配车辆", TTS),
    DRIVER_TAKEN_EVENT("司机接单", BIZ),
    DRIVER_DEPART_EVENT("司机已出发", BIZ),
    DRIVER_ARRIVE_EVENT("司机到达", BIZ),
    PASSENGER_ON_ABOARD_EVENT("乘客上车", BIZ),
    PASSENGER_ARRIVE_EVENT("乘客到达终点", BIZ),
    DRIVER_CONFIRM_BILL_EVENT("司机确认账单", BIZ),
    PAY_SUCCESS_EVENT("支付成功", TTS),

    CANCEL_BY_USER_EVENT("用户主动取消", TTS),
    CANCEL_BY_SYSTEM_EVENT("用户侧Boss取消", SYSTEM),
    CANCEL_BY_BOSS_EVENT("客服取消", CBOSS),
    DISPATCH_FAILED_EVENT("自动派单失败取消", DISPATCH),
    CANCEL_BY_DRIVER("司机取消用户单", BIZ),
    RE_DISPATCH_BY_DRIVER_CANCEL_EVENT("司机取消重新派发", BIZ),
    RE_DISPATCH_BY_BOSS_CANCEL_EVENT("司机侧Boss取消重新派发", BIZ),
    ;

    public final String desc;
    public final OptSystem system;
}
