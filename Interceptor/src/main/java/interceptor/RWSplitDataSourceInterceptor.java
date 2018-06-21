package interceptor;

import com.qunar.mobile.car.common.datasource.DsLogConstant;
import com.qunar.mobile.car.common.datasource.DynamicDataSourceInterceptor;
import com.qunar.mobile.car.common.datasource.DynamicDataSourceKey;
import com.qunar.mobile.car.common.datasource.DynamicDataSourceKeyImpl;
import com.qunar.mobile.car.common.log.UnifyLogger;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;

/**
 * Created on 2018/4/2.
 *
 * @author jiawei
 */
public class RWSplitDataSourceInterceptor extends DynamicDataSourceInterceptor {

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(MethodInvocation invocation) throws Throwable{

        //如果数据源已经是写库, 则不在切换数据源, 避免写入后立刻读而未同步导致问题.
        DynamicDataSourceKey sourceKey = getDataSourceKey();
        Class dynamicDataSourceKeyClass = DynamicDataSourceKeyImpl.class;
        Field dbKeyField = dynamicDataSourceKeyClass.getDeclaredField("DB_KEY");
        dbKeyField.setAccessible(true);
        ThreadLocal<String> dbKey = (ThreadLocal<String>) dbKeyField.get(sourceKey);
        if(dbKey != null && StringUtils.equals(dbKey.get(), sourceKey.getWriteKey())){
            UnifyLogger.debug(DsLogConstant.LOGID_DS_INFO, false, "already in write mode, stay write!");
            return invocation.proceed();
        }

        return super.invoke(invocation);
    }
}
