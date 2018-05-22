/*
 * Copyright (c) 2016 Qunar.com. All Rights Reserved.
 */
package threadLocal;

import java.io.IOException;

/**
 * 日志过滤器
 */
@SuppressWarnings("unused")
public class LogFilter implements Filter {

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        ReqLogDataManager.reqInit();
        //初始化traceId
        String traceId = request.getParameter(CommReqParamKeyConst.PARAM_TRACEID);
        if(!StringUtils.isBlank(traceId)){
            ReqLogDataManager.setTraceId(traceId);
        }
        StringBuilder reqInfo = new StringBuilder();
        doFetchCommReqInfo(reqInfo,request);
        ReqLogDataManager.setReqInfo(reqInfo.toString());

        filterChain.doFilter(servletRequest, servletResponse);
    }


    /**
     * 获取公用请求信息
     * @param info info
     */
    private void doFetchCommReqInfo(StringBuilder info, HttpServletRequest httpReq){
        //UA
        info.append(LogFormatConst.FIELD_SEP);
        fillField(info, LogFieldConst.REQ_C_UA, httpReq.getHeader(HttpHeaders.USER_AGENT));
        //IP
        info.append(LogFormatConst.FIELD_SEP);
        String ip = httpReq.getHeader(CommReqParamKeyConst.HEADER_IP_FORWARDED);
        if(StringUtils.isBlank(ip)){
            ip = httpReq.getRemoteAddr();
        }
        fillField(info, LogFieldConst.REQ_C_IP, ip);
        //REQUESTED URI
        info.append(LogFormatConst.FIELD_SEP);
        fillField(info, LogFieldConst.REQ_C_URI, httpReq.getRequestURI());
    }


    /**
     * 填充字段
     * @param info 日志内容
     * @param key 字段键
     * @param value 字段值
     */
    private void fillField(StringBuilder info, String key, String value){
        if(value == null){
            value = StringUtils.EMPTY;
        }
        info.append(LogFormatConst.FIELD_BEGIN);
        info.append(key);
        info.append(LogFormatConst.FIELD_KV_SEP);
        info.append(value);
        info.append(LogFormatConst.FIELD_END);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }
}
