import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created on 2018-05-31
 *
 * @author qinbo.zuo
 */
@Component
public class LockerUtils {

    @Autowired
    private Sedis3 redis;

    public boolean lock(String methodName, String target, String value) {
        String key = this.getLockerKey(methodName, target, value);
        boolean r = redis.setnx(key, "0") > 0;
        redis.expire(key, (int) TimeUnit.SECONDS.toSeconds(60));
        return r;
    }

    public void unlock(String methodName, String target, String value) {
        String key = this.getLockerKey(methodName, target, value);
        redis.del(key);
    }


    public String getLockerKey(String methodName, String target, String value) {
        return String.format(LOCKER_KEY, methodName, target, value);
    }

}


