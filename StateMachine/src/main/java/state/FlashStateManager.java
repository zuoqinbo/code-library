package state;

import com.google.common.collect.Maps;
import com.xxx.car.flash.common.exceptions.BusinessException;
import com.xxx.car.flash.common.pojo.BStatus;
import com.xxx.car.flash.tts.model.po.ChangeStateParam;
import com.xxx.car.flash.tts.policy.HookPolicy;
import com.xxx.car.flash.tts.policy.StateChangePolicy;
import com.xxx.car.flash.tts.state.flash.AbstractFlashState;
import com.xxx.car.flash.tts.util.annotation.QFlashState;
import com.xxx.mobile.carpool.ztc.log.UnifyLogger;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.xxx.car.flash.common.constants.CommonLogId.*;
import static com.xxx.car.flash.tts.constants.LogIdEnum.ALARM_ORDER_COLUMN_UPDATE;
import static com.xxx.car.flash.tts.constants.LogIdEnum.ALARM_ORDER_STATUS_MACHINE;
import static com.xxx.car.flash.tts.constants.ResponseCode.*;

/**
 * 工程中spring扫描或xml配置
 * <br><br>
 * Author: jianyu.lin <br>
 * Date: 2018/3/27 Time: 下午2:04
 */
@Service
public class FlashStateManager {

    private Map<Integer, AbstractFlashState> stateMap = Maps.newHashMap();

    void registerHandleEvent(Integer status, AbstractFlashState pickUpState){
        UnifyLogger.info(LOG_COMM_INFO.code, true,
                "FlashStateManager registerHandleEvent:" + status);
        stateMap.put(status, pickUpState);
    }

    /**
     * 状态流转事件处理
     * @return Pair[updateRet, 流转后状态]
     */
    public int handleEvent(String orderId, int status, Event event,
                           StateChangePolicy stateChangePolicy, HookPolicy hookPolicy, Object... others) {
        AbstractFlashState state = stateMap.get(status);
        if (state == null) {
            UnifyLogger.error(LOG_COMM_ERROR.code,
                    ALARM_STATE_NOT_FOUND.code, true,
                    "get state  failed! status:" + status);
            throw new BusinessException(UNKNOWN_ORDER_STATUS);
        }

        QFlashState qPickUpState = AnnotationUtils.findAnnotation(state.getClass(), QFlashState.class);
        if (qPickUpState != null) {
            if (qPickUpState.isFinalState()) {
                throw new BusinessException(new BStatus(FINAL_STATE_NO_NEED_HANDLE), "订单已是终态, orderId:{}" + orderId);
            }
        }

        int targetStatus = state.handleEvent(event);

        ChangeStateParam param = ChangeStateParam.builder()
                .orderId(orderId)
                .originStatus(status)
                .targetStatus(targetStatus)
                .build();

        //  更新订单状态操作
        boolean ret = targetStatus != status && stateChangePolicy.updateOrderStatus(param, others);
        UnifyLogger.info(true, ALARM_ORDER_COLUMN_UPDATE,
                "order status change, ret:{}, param:{}", ret, param);

        if (!ret) {
            throw new BusinessException(STATUS_MACHINE_FLOW_ERROR, "订单状态流转失败, change:" + param);
        }

        hookPolicy.hookup(event, orderId, status, targetStatus);
        UnifyLogger.info(true, ALARM_ORDER_STATUS_MACHINE, "handle event, change:{}", param);
        return targetStatus;
    }

    /**
     * 是否是终态
     * @param status 状态code
     * @return flag
     */
    @SuppressWarnings("unused")
    public boolean isFinalState(int status) {
        AbstractFlashState state = stateMap.get(status);
        if (state == null) {
            return false;
        }
        QFlashState qFlashState = AnnotationUtils.findAnnotation(state.getClass(), QFlashState.class);
        return qFlashState != null && qFlashState.isFinalState();
    }
}
