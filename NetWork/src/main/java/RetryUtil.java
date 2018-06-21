import com.qunar.car.flash.common.constants.ResponseCode;
import com.qunar.car.flash.common.exceptions.BusinessException;
import com.qunar.car.flash.common.pojo.BStatus;
import com.qunar.mobile.carpool.ztc.log.UnifyLogger;

import java.util.concurrent.Callable;

/**
 * 代码重试辅助工具
 */
public class RetryUtil {


    /** 最大重试间隔 */
    private static final long MAX_DELAY_MILLISECOND = 120 * 1000;

    /**
     * 同步重试执行业务代码
     *
     * @param callable 待执行的代码，业务逻辑需要支持幂等
     * @param taskDesc 任务描述
     * @param maxRetry 最大重试次数，小于0不会重试
     * @param delayMills 重试间隔毫秒，非正数则立即重试
     * @param <T> 数据类型
     * @return 执行结果
     */
    public static <T> T executeWithRetry(Callable<T> callable, String taskDesc, int maxRetry, long delayMills) {
        return executeWithRetry(callable, taskDesc, maxRetry, delayMills, false, Exception.class);
    }


    /**
     * 同步重试执行业务代码
     *
     * @param callable 待执行的代码，业务逻辑需要支持幂等
     * @param taskDesc 任务描述
     * @param maxRetry 最大重试次数，小于0不会重试
     * @param delayMills 重试间隔毫秒，非正数则立即重试
     * @param linearDelay 重试间隔是否线性递增
     * @param retryExceptionClasses 需要重试的异常类型
     * @param <T> 数据类型
     * @return 执行结果
     */
    public static <T> T executeWithRetry(Callable<T> callable,
                                         String taskDesc,
                                         int maxRetry,
                                         long delayMills,
                                         boolean linearDelay,
                                         Class<? extends Exception>... retryExceptionClasses) throws BusinessException {

        // 1. 参数检查
        if (null == callable) {
            return null;
        }

        if (maxRetry < 0) {
            maxRetry = 0;
        }

        Exception saveException = null;

        // 1. 开始处理
        for (int i = 0; i <= maxRetry; i++) {
            try {

                // 3. 执行业务代码
                return callable.call();

            } catch (Exception e) {

                // 4. 异常处理
                saveException = e;
                boolean needRetry = false;

                // 4.1 判断当前异常是否需要重试
                if (retryExceptionClasses.length > 0) {
                    for (Class<?> clazz : retryExceptionClasses) {
                        if (clazz.isInstance(saveException)) {
                            needRetry = true;
                            break;
                        }
                    }
                }

                if (!needRetry) {
                    throw new BusinessException(new BStatus(ResponseCode.UNKNOWN_ERROR), saveException);
                }

                if (i < maxRetry) {

                    // 4.2 判断是否需要sleep
                    long time2Sleep = 0;
                    long startTime = System.currentTimeMillis();

                    if (delayMills > 0) {
                        time2Sleep = linearDelay ? delayMills * (i + 1) : delayMills;
                        if (time2Sleep > MAX_DELAY_MILLISECOND) {
                            time2Sleep = MAX_DELAY_MILLISECOND;
                        }

                        try {
                            Thread.sleep(time2Sleep);
                        } catch (InterruptedException ignored) {
                            // 中断不再重新sleep
                        }
                    }

                    UnifyLogger.info("{}-执行异常,即将尝试执行第{}次重试.本次重试计划等待{}ms,实际等待{}ms  ", taskDesc, i + 1, time2Sleep, System.currentTimeMillis() - startTime, saveException);
                }
            }
        }

        // 5. 重试次数结束仍然失败
        if (null != saveException) {
            // 理论上不会为空
            throw new BusinessException(new BStatus(ResponseCode.UNKNOWN_ERROR), saveException);
        }

        throw new IllegalStateException("RetryUtil未知异常," + taskDesc);
    }

}
