/**
 * @author qinbo.zuo
 * @create 2018-06-22 15:11
 **/
public class LockDemo {

    /**
     * @param driverId
     * @param userOrderId
     */
    private void acquireLock(Integer driverId, String userOrderId) {
        boolean lockDriverSuccess = lockerUtils.lock("assignDriver", "driverId", driverId + "");
        if (lockDriverSuccess) {
            boolean lockOrderSuccess = lockerUtils.lock("assignDriver", "orderId", userOrderId);
            if (lockOrderSuccess) {
                return;
            } else {
                lockerUtils.unlock("assignDriver", "driverId", driverId + "");
            }
        }

        throw new BusinessException(BizResponseCode.LOCK_FAIL);
    }
}