package http;



/**
 * Created on 2018/3/30.
 *
 * @author jiawei
 */
public interface HttpService {

    /**
     * 发送http post表单请求
     * @param url               请求的url
     * @param reqObject         请求的bean参数对象
     * @param resTypeRef         返回的数据类型
     * @param isCommonResponse  是否是通用返回值类型
     * @return      按指定的数据类型进行json解析并返回
     */
    <T> T postEncodedForm(String url, Object reqObject, TypeReference<T> resTypeRef, boolean isCommonResponse);

    /**
     * 发送http post表单请求,默认返回通用返回值(CommonResponse)类型
     * @param url               请求的url
     * @param reqObject         请求的bean参数对象
     * @param resTypeRef           返回的数据类型
     * @return      按指定的数据类型进行json解析并返回
     */
    <T> T postEncodedForm(String url, Object reqObject, TypeReference<T> resTypeRef);
    /**
     * 发送http post json请求
     * @param url               请求的url
     * @param reqObject         请求的bean参数对象
     * @param resTypeRef         返回的数据类型
     * @param isCommonResponse  是否是通用返回值类型
     * @return      按指定的数据类型进行json解析并返回
     */
    <T> T postEncodedJson(String url, Object reqObject, TypeReference<T> resTypeRef, boolean isCommonResponse);
}
