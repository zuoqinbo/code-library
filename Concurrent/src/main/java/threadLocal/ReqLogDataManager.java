package threadLocal;

import org.apache.commons.lang.StringUtils;


/**
 * 统一日志中请求数据管理器
 * @version 1.0.0
 */
public  abstract class ReqLogDataManager {
	/** 默认的traceId */
	public static final String TRACEID_DEFAULT = "0";
	
	/** 请求开始时间 */
	private static final ThreadLocal<Long> reqStartTime = new ThreadLocal<Long>();
	/** 追踪标识 */
	private static final ThreadLocal<String> traceIdHolder = new ThreadLocal<String>();
	/** 日志格式类型 */
	private static final ThreadLocal<Integer> logTypeHolder = new ThreadLocal<Integer>();
	/** 请求信息 */
	private static final ThreadLocal<String> reqInfoHolder = new ThreadLocal<String>();
	
	/**
	 * 初始化请求的日志数据
	 */
	public static void reqInit(){
		reqStartTime.set(System.currentTimeMillis());
		traceIdHolder.set(null);
		logTypeHolder.set(null);
		reqInfoHolder.set(null);
	}
	
	/**
	 * 清除请求的日志数据
	 */
	public static void reqClear(){
		reqStartTime.set(null);
		traceIdHolder.set(null);
		logTypeHolder.set(null);
		reqInfoHolder.set(null);
	}
		
	/**
	 * 设置追踪标识
	 * @param traceId
	 */
	public static void setTraceId(String traceId){
		traceIdHolder.set(traceId);
	}
	
	/**
	 * 获取追踪标识
	 * <br>如果为空则默认为0
	 * @return
	 */
	public static String getTraceId(){
		String tid = traceIdHolder.get();
		if(StringUtils.isBlank(tid)){
			tid = TRACEID_DEFAULT;
		}
		return tid;
	}
	
	/**
	 * 设置日志格式类型
	 * @param type
	 */
	public static void setLogType(Integer type){
		logTypeHolder.set(type);
	}

	/**
	 * 获取日志格式类型
	 * <br>如为空则默认为非请求类型
	 * @return
	 */
	public static Integer getLogType(){
		Integer type = logTypeHolder.get();
		if(type == null){
			type = LogTypeConst.NON_REQ;
		}
		return type;
	}
	
	/**
	 * 设置请求信息
	 * @param reqInfo
	 */
	public static void setReqInfo(String reqInfo){
		reqInfoHolder.set(reqInfo);
	}
	
	/**
	 * 获取请求信息
	 * @return
	 */
	public static String getReqInfo(){
		return reqInfoHolder.get();
	}
	
	/**
	 * 请求已经处理的时长（从请求开始到当前时间点）
	 * @return 时长，如果不是请求，则返回0
	 */
	public static long getReqDealTime(){
		long len = 0;
		Long start = reqStartTime.get();
		if(start != null){
			len = System.currentTimeMillis() - start;
		}
		return len;
	}
}
