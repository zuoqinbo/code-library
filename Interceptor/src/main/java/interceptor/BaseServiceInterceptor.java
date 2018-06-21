package interceptor;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2018/3/28.
 *
 * @author jiawei
 */
public abstract class BaseServiceInterceptor {

    private Map<String, String> configMap;

    private static final String codeDescKey = "flash.bStatus.code.desc.";

    /**
     * Controller收到所有请求的切点
     */
    public abstract void onRequest();

    public BaseServiceInterceptor(){
        final MapConfig config = MapConfig.get("code_desc.properties");
        this.configMap = config.asMap();
        config.addListener(new Configuration.ConfigListener<Map<String, String>>() {
            @Override
            public void onLoad(Map<String, String> stringStringMap) {
                configMap = stringStringMap;
            }
        });
    }

    @Around("onRequest()")
    public Object handleRequest(ProceedingJoinPoint joinPoint) {
        String methodName = getMethodName(joinPoint);
        long startTime = System.currentTimeMillis();
        String key = "service." + methodName;
        Object ret = null;
        Throwable ept = null;
        try {
            return (ret = joinPoint.proceed());
        }catch (Throwable t){
            ept = t;
            if (t instanceof BusinessException) {
                BusinessException expt = (BusinessException) t;
                BStatus bStatus = expt.getBStatus();
                if(bStatus != null){
                    if(StringUtils.isBlank(bStatus.getDes())){
                        bStatus.setDes(configMap.get(codeDescKey + bStatus.getCode()));
                    }
                }
                throw new BusinessException(bStatus, t);
            }
            throw new BusinessException(new BStatus(ResponseCode.UNKNOWN_ERROR), t);
        }finally{
            long endTime = System.currentTimeMillis();
            if(ept == null) {
                ZtcMetrics.recordTimeAndResult(key, endTime - startTime, TimeUnit.MILLISECONDS, ret);
            }else{
                String errCode = ErrorCodeUtil.parseErrCode(ept);
                ZtcMetrics.recordTimeAndCode(key, endTime - startTime, TimeUnit.MILLISECONDS, errCode);
            }
        }
    }

    private static String getMethodName(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        if (signature instanceof MethodSignature) {
            return ((MethodSignature)signature).getMethod().getName();
        } else {
            return signature.getName();
        }
    }
}
