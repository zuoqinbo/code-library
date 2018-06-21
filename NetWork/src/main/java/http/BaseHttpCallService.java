package http;


import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 */
@SuppressWarnings("unused")
public abstract class BaseHttpCallService implements HttpService, IRetTimeOut {

    @Override
    public <T> T postEncodedForm(String url, Object reqObject, TypeReference<T> resTypeRef, boolean isCommonResponse) {
        //请求日志初始化
        LogUtils.initLogRequestIfNeeded();

        String jsonStr;
        int timeout = getTimeout();
        //截断链接里的qmq spanId
        String oldTraceId = cutOffSpanId();

        try {
            //拿到请求的参数列表
            OwlHttpParams httpParams = OwlHttpParamsUtil.buildPostParams(url,getBeanMap(reqObject),
                    timeout, timeout, CommonLogId.LOG_COMM_INFO.getCode(), "");

            //http post请求
            jsonStr = HttpSyncClient.post(httpParams);

            //请求回包日志
            UnifyLogger.info(true, "http call: url:{}, req:{}, res:{}", url, reqObject, jsonStr);
            if (isCommonResponse) {
                return getData(jsonStr, resTypeRef, url, reqObject);
            } else {
                //非通用返回值类型
                return JsonUtils.fromJson(jsonStr,resTypeRef);
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();
            handleTimeout(cause,url,reqObject);
            if( e instanceof BusinessException){
                throw (BusinessException)e;
            }
            throw new BusinessException(new BStatus(ResponseCode.UNKNOWN_ERROR),
                    "remote http call fail:"+url+" | req:"+ JsonUtils.toJson(reqObject), e);
        } finally {
            setSpanId(oldTraceId);
        }
    }

    @Override
    public <T> T postEncodedForm(String url, Object reqObject, TypeReference<T> resTypeRef) {
        return postEncodedForm(url,reqObject,resTypeRef,true);
    }


    @Override
    public <T> T postEncodedJson(String url, Object reqObject, TypeReference<T> resTypeRef, boolean isCommonResponse) {
        //请求日志初始化
        LogUtils.initLogRequestIfNeeded();

        String jsonStr;
        String reqJson = JsonUtils.toJson(reqObject);
        int timeout = getTimeout();
        //截断链接里的qmq spanId
        String oldTraceId = cutOffSpanId();

        try {
            //拿到请求的参数列表
            OwlHttpParams httpParams = OwlHttpParamsUtil.buildPostJosnParams(
                    url, reqJson, timeout, timeout, CommonLogId.LOG_COMM_INFO.getCode(), ""
            );

            //http post请求
            jsonStr = HttpSyncClient.post(httpParams);

            //请求回包日志
            UnifyLogger.info(true, "http call: url:{}, req:{}, res:{}", url, reqJson, jsonStr);
            if (isCommonResponse) {
                return getData(jsonStr, resTypeRef, url, reqObject);
            } else {
                //非通用返回值类型
                return JsonUtils.fromJson(jsonStr,resTypeRef);
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();
            handleTimeout(cause,url,reqObject);
            if( e instanceof BusinessException){
                throw (BusinessException)e;
            }
            throw new BusinessException(new BStatus(ResponseCode.UNKNOWN_ERROR),
                    "remote http call fail:" + url + " | req:" + reqJson, e);
        } finally {
            setSpanId(oldTraceId);
        }

    }

    private <T> T getData(String jsonStr, TypeReference<T> resTypeRef, String url, Object reqObject) throws IOException{
        //通用返回值类型
        Response<Object> commonRes = JsonUtils.fromJson(jsonStr, new TypeReference<Response<Object>>(){});
        if (commonRes == null || commonRes.getBStatus() == null
                || commonRes.getBStatus().getCode() != ResponseCode.SUCCESS) {
            BStatus bStatus;
            if(commonRes != null && commonRes.getBStatus() != null){
                bStatus = commonRes.getBStatus();
            }else{
                bStatus = new BStatus(ResponseCode.REMOTE_HTTP_CALL_ERROR);
            }
            //返回数据校验失败,抛出远程调用失败
            throw new BusinessException(bStatus,
                    "remote http call fail:"+url+" | req:"+ JsonUtils.toJson(reqObject)+" | res:" + JsonUtils.toJson(commonRes));
        }
        String dataString = "{}";
        if(commonRes.getData() != null){
            dataString = JsonUtils.toJson(commonRes.getData());
        }

        return JsonUtils.fromJson(dataString, resTypeRef);
    }

    private String cutOffSpanId(){
        //截断链接里的qmq spanId
        String traceId = ReqLogDataManager.getTraceId();
        if(traceId != null) {
            String[] tArray = traceId.split(",");
            if(tArray.length >= 3){
                String newTraceId = tArray[0] + "," + tArray[1];
                ReqLogDataManager.setTraceId(newTraceId);
            }
        }
        return traceId;
    }

    private void setSpanId(String traceId){
        ReqLogDataManager.setTraceId(traceId);
    }

    /**
     * 处理超时的方法
     * @param cause         异常原因
     * @param url           调用的url
     * @param reqObject     请求的对象
     */
    private void handleTimeout(Throwable cause,String url,Object reqObject) {
        if (cause instanceof SocketTimeoutException) {
            //超时错误,单独返回
            throw new BusinessException(new BStatus(ResponseCode.TIME_OUT_EXPECT),
                    "remote http call timeout:"+url+" | req:"+JsonUtils.toJson(reqObject), cause);
        }
    }

    /**
     * 获取bean对象属性和值的NameValuePair列表
     * @param bean  要处理的bean对象
     * @return  NameValuePair列表
     */
    private static HashMap<String,Object> getBeanMap(Object bean) {
        HashMap<String,Object> map = new HashMap<>();

        String json = JsonUtils.toJson(bean);
        Map<String,Object> fieldMap = JsonUtils.fromJsonToMap(json);

        if (fieldMap == null) {
            return map;
        }
        map.putAll(fieldMap);
        return map;
    }
}
