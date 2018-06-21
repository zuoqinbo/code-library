import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppServerMonitor {

	private static Logger LOG = LoggerFactory.getLogger(AppServerMonitor.class);

 	private static final int processors = Runtime.getRuntime().availableProcessors();

    private static final List<String> youngGenCollectorNames = new ArrayList<String>();

    private static final List<String> fullGenCollectorNames = new ArrayList<String>();

    private static final Set<String> edenSpace = new HashSet<String>();

    private static final Set<String> survivorSpace = new HashSet<String>();

    private static final Set<String> oldSpace = new HashSet<String>();

    private static final Set<String> permSpace = new HashSet<String>();

    private static final Set<String> codeCacheSpace = new HashSet<String>();

    private static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    static {
        //young GC参数
        youngGenCollectorNames.add("Copy");
        youngGenCollectorNames.add("ParNew");
        youngGenCollectorNames.add("PS Scavenge");

        //full GC参数
        fullGenCollectorNames.add("MarkSweepCompact");
        fullGenCollectorNames.add("PS MarkSweep");
        fullGenCollectorNames.add("ConcurrentMarkSweep");

        // 各种GC下的eden名字
        edenSpace.add("Eden Space");// -XX:+UseSerialGC
        edenSpace.add("PS Eden Space");// –XX:+UseParallelGC
        edenSpace.add("Par Eden Space");// -XX:+UseConcMarkSweepGC
        edenSpace.add("Par Eden Space");// -XX:+UseParNewGC
        edenSpace.add("PS Eden Space");// -XX:+UseParallelOldGC

        // 各种gc下survivorSpace的名字
        survivorSpace.add("Survivor Space");// -XX:+UseSerialGC
        survivorSpace.add("PS Survivor Space");// –XX:+UseParallelGC
        survivorSpace.add("Par Survivor Space");// -XX:+UseConcMarkSweepGC
        survivorSpace.add("Par survivor Space");// -XX:+UseParNewGC
        survivorSpace.add("PS Survivor Space");// -XX:+UseParallelOldGC

        // 各种gc下oldspace的名字
        oldSpace.add("Tenured Gen");// -XX:+UseSerialGC
        oldSpace.add("PS Old Gen");// –XX:+UseParallelGC
        oldSpace.add("CMS Old Gen");// -XX:+UseConcMarkSweepGC
        oldSpace.add("Tenured Gen  Gen");// Tenured Gen Gen
        oldSpace.add("PS Old Gen");// -XX:+UseParallelOldGC

        // 各种gc下持久代的名字
        permSpace.add("Perm Gen");// -XX:+UseSerialGC
        permSpace.add("PS Perm Gen");// –XX:+UseParallelGC
        permSpace.add("CMS Perm Gen");// -XX:+UseConcMarkSweepGC
        permSpace.add("Perm Gen");// -XX:+UseParNewGC
        permSpace.add("PS Perm Gen");// -XX:+UseParallelOldGC
        // codeCache的名字
        codeCacheSpace.add("Code Cache");
    }

    private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    private static final OperatingSystemMXBean osmbean = ManagementFactory.getOperatingSystemMXBean();

    private static boolean isTomcatServer = false;

    public static ScheduledExecutorService collectorScheduler;

    static {
        try {
            Set<ObjectName> catalinaNames = mbeanServer.queryNames(new ObjectName("Catalina:*"), null);
            if (catalinaNames != null && catalinaNames.size() > 0) {
                isTomcatServer = true;
            }
        } catch (JMException e) {
        	 LOG.error(e.getMessage(), e);
        }

     // 收集状态指标
        collectorScheduler = Executors.newScheduledThreadPool(1);
        collectorScheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName("Collect-Status-Diff-Thread");
                try {
                    innerCollectStatus();
                } catch (Throwable e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }, 1, ZtcMonitorMetricsSender.COUNTER_SEND_TIME_INTERNAL_SEC, TimeUnit.SECONDS);
    }

    public static boolean isTomcat() {
        return isTomcatServer;
    }

    public static String getOsName() {
        return osmbean.getName();
    }

    public static String getOsArch() {
        return osmbean.getArch();
    }

    public static String getOsVersion() {
        return osmbean.getVersion();
    }

    public static int getAvailableProcessors() {
        return osmbean.getAvailableProcessors();
    }

    public static double getSystemLoadAverage() {
        return osmbean.getSystemLoadAverage();
    }


    public static int getProcessors() {
		return processors;
	}

	public Map<String, Object> getServerInfo() {
        if (!isTomcat()) Collections.emptyMap();
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> result = new HashMap<String, Object>(8);
        result.put("proc_name", bean.getName());
        result.put("start_time", bean.getStartTime());
        result.put("jvm_info", bean.getVmName() + " " + bean.getVmVendor() + "(" + bean.getVmVendor() + ")");
        result.put("os_name", getOsName() + "(" + getOsArch() + ")");

        fillTomcatProperty(mBeanServer, result);
        fillHostProperty(mBeanServer, result);
        fillEngineProperty(mBeanServer, result);

        return result;
    }


    public Map<String, Object> getSummaryStat() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("load", getSystemLoadAverage());

        ret.putAll(getGcStat());
        ret.putAll(getNetworkStat());

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ret.put("mem_committed", memoryMXBean.getHeapMemoryUsage().getCommitted()
                + memoryMXBean.getNonHeapMemoryUsage().getCommitted());
        ret.put("mem_init", memoryMXBean.getHeapMemoryUsage().getInit()
                + memoryMXBean.getNonHeapMemoryUsage().getInit());
        ret.put("mem_max", memoryMXBean.getHeapMemoryUsage().getMax() + memoryMXBean.getNonHeapMemoryUsage().getMax());
        ret.put("mem_used", memoryMXBean.getHeapMemoryUsage().getUsed()
                + memoryMXBean.getNonHeapMemoryUsage().getUsed());
        return ret;
    }


    public static Map<String, Object> getMemStat() {
        Map<String, Object> ret = new HashMap<String, Object>(4);


        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

        if (memoryPoolMXBeans != null && memoryPoolMXBeans.size() > 0) {
            Map<String, Object> pool = new HashMap<String, Object>(4);
            ret.put("mem_pool", pool);
            pool.put("mem_collection", getMemoryPoolCollectionUsage(memoryPoolMXBeans));
            pool.put("mem_peak", getMemoryPoolPeakUsage(memoryPoolMXBeans));
            pool.put("mem_usage", getMemoryPoolUsage(memoryPoolMXBeans));
        }


        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        if (memoryMXBean != null) {
            ret.put("mem_heap", memoryMXBean.getHeapMemoryUsage());
            ret.put("mem_nonheap", memoryMXBean.getNonHeapMemoryUsage());
        }

        return ret;
    }


    public Map<String, Object> getNetworkStat() {
        if (!isTomcat()) Collections.emptyMap();
        Map<String, Object> result = new HashMap<String, Object>(2);

        //get all the resource beans
        Set<ObjectName> objectNames = null;
        try {
            objectNames = mBeanServer.queryNames(new ObjectName("Catalina:type=GlobalRequestProcessor,*"), null);
        } catch (JMException e) {
            LOG.error("queryNames `Catalina:type=GlobalRequestProcessor,*` error!", e);
        }

        //get from bytes read and writes from mbean
        long readBytes = 0, writeBytes = 0;
        if (objectNames != null && mBeanServer != null) {
            for (ObjectName oName : objectNames) {
                try {
                    readBytes += (Long) mBeanServer.getAttribute(oName, "bytesReceived");
                    writeBytes += (Long) mBeanServer.getAttribute(oName, "bytesSent");
                } catch (JMException e) {
                    LOG.warn("get attribute bytesReceived exception", e);
                }
            }
        }

        result.put("net_received", readBytes);
        result.put("net_sent", writeBytes);
        return result;
    }


    public static Map<String, Object> getRequestStat() {
        if (!isTomcat()) Collections.emptyMap();
        Map<String, Object> result = new HashMap<String, Object>(2);

        //get all the resource beans
        Set<ObjectName> objectNames = null;
        try {
            objectNames = mBeanServer.queryNames(new ObjectName("Catalina:type=GlobalRequestProcessor,*"), null);
        } catch (JMException e) {
            LOG.error("queryNames `Catalina:type=GlobalRequestProcessor,*` error!", e);
        }

        //get from bytes read and writes from mbean
        long processingTime = 0, maxTime = 0, requestCount = 0, errCount = 0;
        if (objectNames != null && mBeanServer != null) {
            for (ObjectName oName : objectNames) {
                try {
                    processingTime += (Long) mBeanServer.getAttribute(oName, "processingTime");
                    maxTime += Math.max((Long) mBeanServer.getAttribute(oName, "maxTime"), maxTime);
                    requestCount += (Integer) mBeanServer.getAttribute(oName, "requestCount");
                    errCount += (Integer) mBeanServer.getAttribute(oName, "errorCount");
                } catch (JMException e) {
                    LOG.warn("get attribute bytesReceived exception", e);
                }
            }
        }

        result.put("req_proc_time", processingTime);
        result.put("req_max_time", maxTime);
        result.put("req_count", requestCount);
        result.put("req_err_count", errCount);
        return result;
    }


    public static Map<String, Object> getGcStat() {
        Map<String, Object> result = new HashMap<String, Object>(8);
        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

        if (gcMXBeans != null && gcMXBeans.size() > 0) {
            for (GarbageCollectorMXBean item : gcMXBeans) {
                //young gc
                if (youngGenCollectorNames.contains(item.getName())) {
                    result.put("gc_young_count",
                            MapUtils.getInteger(result, "gc_young_count", 0) + item.getCollectionCount());
                    result.put("gc_young_time",
                            MapUtils.getInteger(result, "gc_young_time", 0) + item.getCollectionTime());
                    result.put("gc_young_mem_pools", item.getMemoryPoolNames());
                    //full gc
                } else if (fullGenCollectorNames.contains(item.getName())) {
                    result.put("gc_full_count",
                            MapUtils.getInteger(result, "gc_full_count", 0) + item.getCollectionCount());
                    result.put("gc_full_time",
                            MapUtils.getInteger(result, "gc_full_time", 0) + item.getCollectionTime());
                    result.put("gc_full_mem_pools", item.getMemoryPoolNames());
                }
            }
        }
        return result;
    }


    public Map<String, Object> getThreadStat() {
        Map<String, Object> result = new HashMap<String, Object>();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        if (threadMXBean != null) {
            result.put("thread_total", threadMXBean.getThreadCount());
            result.put("thread_peak", threadMXBean.getPeakThreadCount());
            result.put("thread_daemon", threadMXBean.getDaemonThreadCount());
            result.put("thread_started", threadMXBean.getTotalStartedThreadCount());

            long[] threadids = threadMXBean.findDeadlockedThreads();
            result.put("dead_lock_count", threadids == null ? 0 : threadids.length);
        }

        return result;
    }


    public Map<String, Object> getThreadDetails() {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();

        if (map != null && map.size() > 0) {
            for (Thread thread : map.keySet()) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("thread_id", thread.getId());
                item.put("thread_name", thread.getName());
                item.put("thread_state", thread.getState());

                List<String> stackTraces = new ArrayList<String>();
                for (StackTraceElement element : map.get(thread)) {
                    stackTraces.add(String.format("%s.%s", element.getClassName(), element.getMethodName()));
                }
                item.put("thread_stack_traces", stackTraces);
                result.put(String.format("thread-%d", thread.getId()), item);
            }
        }

        return result;
    }


    public static Map<String, Object> getTomcatStat() {
        if (!isTomcat() || mBeanServer == null) return Collections.emptyMap();

        Map<String, Object> result = new HashMap<String, Object>(8);
        Set<ObjectName> objectNames = null;
        // set connector info
        try {
            objectNames = mBeanServer.queryNames(new ObjectName("Catalina:type=Connector,*"), null);
            if (objectNames != null && objectNames.size() > 0) {
                for (ObjectName connector : objectNames) {
                    Map<String, Object> connectorItem = new HashMap<String, Object>();
                    String key = fillTomcatConnectorInfo(mBeanServer, connector, connectorItem);
                    if (key != null) {
                        result.put(key, connectorItem);
                    }
                }
            }
        } catch (JMException e) {
            LOG.error("queryNames `Catalina:type=Connector,*` error!", e);
        }

        // set Catalina ThreadPool
        try {
            objectNames = mBeanServer.queryNames(new ObjectName("Catalina:type=ThreadPool,*"), null);
            if (objectNames != null && objectNames.size() > 0) {
                for (ObjectName threadPool : objectNames) {
                    Map<String, Object> threadPoolItem = new HashMap<String, Object>();
                    String key = fillTomcatThreadPool(mBeanServer, threadPool, threadPoolItem);
                    if (key != null) {
                        result.put(key, threadPoolItem);
                    }
                }
            }
        } catch (JMException e) {
            LOG.error("queryNames `Catalina:type=ThreadPool,*` error!", e);
        }
        return result;
    }

    /**
     * 取connector的各个属性
     *
     * @param beanServer
     * @param connector connector的ObjectName
     * @param result 填充各个属性
     * @return 返回object port
     */
    private static String fillTomcatConnectorInfo(MBeanServer beanServer, ObjectName connector, Map<String, Object> result) {
        if (beanServer == null || connector == null) return null;
        try {
            Integer port = (Integer) beanServer.getAttribute(connector, "port");
           // result.put("port", port);
            result.put("max_threads", beanServer.getAttribute(connector, "maxThreads"));
            result.put("max_http_header_size", beanServer.getAttribute(connector, "maxHttpHeaderSize"));
            result.put("max_keep_alive_requests", beanServer.getAttribute(connector, "maxKeepAliveRequests"));
            result.put("max_parameter_count", beanServer.getAttribute(connector, "maxParameterCount"));
            result.put("max_post_size", beanServer.getAttribute(connector, "maxPostSize"));
            result.put("accept_count", beanServer.getAttribute(connector, "acceptCount"));
            return "connector"+port;
        } catch (JMException e) {
            LOG.warn("get tomcat connector attribute exception", e);
        }
        return null;
    }

    /**
     * 取threadpool的各个属性
     *
     * @param beanServer
     * @param threadPool threadPool ObjectName
     * @param result 填充各个属性
     * @return 返回threadPool name
     */
    private static String fillTomcatThreadPool(MBeanServer beanServer, ObjectName threadPool, Map<String, Object> result) {
        if (beanServer == null || threadPool == null) return null;
        try {
            String name = (String) beanServer.getAttribute(threadPool, "name");
            result.put("name", name);
            result.put("acceptor_count", beanServer.getAttribute(threadPool, "acceptorThreadCount"));
            result.put("current_count", beanServer.getAttribute(threadPool, "currentThreadCount"));
            result.put("current_busy", beanServer.getAttribute(threadPool, "currentThreadsBusy"));
            result.put("max_threads", beanServer.getAttribute(threadPool, "maxThreads"));
           // result.put("port", beanServer.getAttribute(threadPool, "port"));
            return "threadPool";
        } catch (JMException e) {
            LOG.error("get attribute name exception", e);
        }
        return null;
    }

    /**
     * Collection
     *
     * @param memoryPoolMXBeans
     * @return
     */
    private static Map<String, MemoryUsage> getMemoryPoolCollectionUsage(List<MemoryPoolMXBean> memoryPoolMXBeans) {
        Map<String, MemoryUsage> gcMemory = new HashMap<String, MemoryUsage>(8);
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            String name = memoryPoolMXBean.getName();
            if (edenSpace.contains(name)) {
                gcMemory.put("eden", memoryPoolMXBean.getCollectionUsage());
            } else if (survivorSpace.contains(name)) {
                gcMemory.put("survivor", memoryPoolMXBean.getCollectionUsage());
            } else if (oldSpace.contains(name)) {
                gcMemory.put("old", memoryPoolMXBean.getCollectionUsage());
            } else if (permSpace.contains(name)) {
                gcMemory.put("perm", memoryPoolMXBean.getCollectionUsage());
            } else if (codeCacheSpace.contains(name)) {
                gcMemory.put("code_cache", memoryPoolMXBean.getCollectionUsage());
            }
        }
        return gcMemory;
    }

    /**
     * memory pool
     *
     * @param memoryPoolMXBeans
     * @return
     */
    private static Map<String, MemoryUsage> getMemoryPoolUsage(List<MemoryPoolMXBean> memoryPoolMXBeans) {
        Map<String, MemoryUsage> gcMemory = new HashMap<String, MemoryUsage>();
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            String name = memoryPoolMXBean.getName();
            if (edenSpace.contains(name)) {
                gcMemory.put("eden", memoryPoolMXBean.getUsage());
            } else if (survivorSpace.contains(name)) {
                gcMemory.put("survivor", memoryPoolMXBean.getUsage());
            } else if (oldSpace.contains(name)) {
                gcMemory.put("old", memoryPoolMXBean.getUsage());
            } else if (permSpace.contains(name)) {
                gcMemory.put("perm", memoryPoolMXBean.getUsage());
            } else if (codeCacheSpace.contains(name)) {
                gcMemory.put("code_cache", memoryPoolMXBean.getUsage());
            }
        }
        return gcMemory;
    }

    /**
     * peak memory pool
     *
     * @param memoryPoolMXBeans
     * @return
     */
    public static Map<String, MemoryUsage> getMemoryPoolPeakUsage(List<MemoryPoolMXBean> memoryPoolMXBeans) {
        Map<String, MemoryUsage> gcMemory = new HashMap<String, MemoryUsage>();
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            String name = memoryPoolMXBean.getName();
            if (edenSpace.contains(name)) {
                gcMemory.put("eden", memoryPoolMXBean.getPeakUsage());
            } else if (survivorSpace.contains(name)) {
                gcMemory.put("survivor", memoryPoolMXBean.getPeakUsage());
            } else if (oldSpace.contains(name)) {
                gcMemory.put("old", memoryPoolMXBean.getPeakUsage());
            } else if (permSpace.contains(name)) {
                gcMemory.put("perm", memoryPoolMXBean.getPeakUsage());
            } else if (codeCacheSpace.contains(name)) {
                gcMemory.put("code_cache", memoryPoolMXBean.getPeakUsage());
            }
        }
        return gcMemory;
    }

    /**
     * Server基本信息
     *
     * @param beanServer
     * @return
     */
    private static void fillTomcatProperty(MBeanServer beanServer, Map<String, Object> result) {
        if (beanServer == null) return;
        try {
            Set<ObjectName> objectNames = beanServer.queryNames(new ObjectName("Catalina:type=Connector,*"), null);
            List<Map<String, Object>> servers = new ArrayList<Map<String, Object>>(4);
            if (objectNames != null && objectNames.size() > 0) {
                for (ObjectName connector : objectNames) {
                    Map<String, Object> map = new HashMap<String, Object>(2);
                    map.put("port", (Integer) beanServer.getAttribute(connector, "port"));
                    map.put("protocol", beanServer.getAttribute(connector, "protocol"));
                    servers.add(map);
                }
                result.put("connectors", servers);
            }
        } catch (JMException e) {
            LOG.error("queryNames 'Catalina:type=Server' error!", e);
            return;
        }
    }

    /**
     * Host基本信息
     *
     * @param beanServer
     * @return
     */
    private static void fillHostProperty(MBeanServer beanServer, Map<String, Object> result) {
        if (beanServer == null) return;
        Set<ObjectName> objectNames = null;
        try {
            objectNames = beanServer.queryNames(new ObjectName("Catalina:type=Host"), null);
        } catch (JMException e) {
            LOG.error("queryNames 'Catalina:type=Host' error!", e);
            return;
        }

        if (objectNames != null && objectNames.size() == 1) {
            for (ObjectName host : objectNames) {
                try {
                    result.put("app_base", beanServer.getAttribute(host, "appBase"));
                    break;
                } catch (JMException e) {
                    LOG.warn("get attribute appBase exception", e);
                }
            }
        }
    }

    /**
     * engine 概要信息
     *
     * @param beanServer
     * @return
     */
    private static void fillEngineProperty(MBeanServer beanServer, Map<String, Object> result) {
        if (beanServer == null) return;
        Set<ObjectName> objectNames = null;
        try {
            objectNames = beanServer.queryNames(new ObjectName("Catalina:type=Engine"), null);
        } catch (JMException e) {
            LOG.error("queryNames 'Catalina:type=Engine' error!", e);
            return;
        }

        if (objectNames != null && objectNames.size() == 1) {
            for (ObjectName engine : objectNames) {
                try {
                    result.put("base_dir", beanServer.getAttribute(engine, "baseDir"));
                    result.put("engine_name", beanServer.getAttribute(engine, "name"));
                    break;
                } catch (JMException e) {
                    LOG.warn("get attribute baseDir exception", e);
                }
            }
        }
    }

    //-------------------------------指标---------------------------

    private static Map<String, GrowingMeter> growingMeters = new HashMap<String, GrowingMeter>();
    private static void addStatus(String group, String name, Long value) {
    	String key = group + "." + name;
    	GrowingMeter meter = growingMeters.get(key);
    	if (meter == null) {
			meter = new GrowingMeter(16);
			growingMeters.put(key, meter);
		}
    	meter.recordValue(value);
	}

    //----------------------指标收集--------------------------------
    protected static void innerCollectStatus() {
        Map<String, Object> m = getRequestStat();
        if (m.containsKey("req_count")) {
            addStatus("tomcat", "req.Sum",
                    Long.valueOf("" + m.get("req_count")));
        }
        if (m.containsKey("req_err_count")) {
            addStatus("tomcat", "req_err.Sum",
                    Long.valueOf("" + m.get("req_err_count")));
        }

        // gc
        m = getGcStat();
        if (m.containsKey("gc_young_count")) {
            addStatus("jvm.gc", "young.Sum",
                    Long.valueOf("" + m.get("gc_young_count")));
        }
        if (m.containsKey("gc_young_time")) {
            addStatus("jvm.gc", "young_time.Sum",
                    Long.valueOf("" + m.get("gc_young_time")));
        }
        if (m.containsKey("gc_full_count")) {
            addStatus("jvm.gc", "full.Sum",
                    Long.valueOf("" + m.get("gc_full_count")));
        }
        if (m.containsKey("gc_full_time")) {
            addStatus("jvm.gc", "full_time.Sum",
                    Long.valueOf("" + m.get("gc_full_time")));
        }
    }

    public static void sendMetrics(IMetricsPusher metricsPusher) {

        Iterator<Entry<String, GrowingMeter>> it = growingMeters.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, GrowingMeter> entry = (Entry<String, GrowingMeter>) it.next();
            GrowingMeter meter = entry.getValue();
            KeyValuePair<Long, Long>[] kv = meter.getLastTwoValue();
            if (kv != null && kv.length == 2) {
                long timestamp = kv[0].getKey();
                long diff = Math.max(0, kv[0].getValue() - kv[1].getValue());
                try {
                    metricsPusher.push(timestamp, entry.getKey(), new Object[] { diff });
                } catch (Exception e) {
                    UnifyLogger.error("sendMetrics", "sendMetrics-fail", false, "timestamp:" + timestamp + ",key:"
                            + entry.getKey() + ",value:" + diff);
                }
            }
        }

        // memory 指标
        Map<String, Object> memStat = getMemStat();
        Map<String, Object> memeroyIndex = Maps.newHashMap();
        Map<String, Object> pool = (Map<String, Object>) MapUtils.getObject(memStat, "mem_pool");
        MemoryUsage defualt = new MemoryUsage(0, 0, 0, 0);
        if (MapUtils.isNotEmpty(pool)) {
            Map<String, MemoryUsage> mem_collection = (Map<String, MemoryUsage>) pool.get("mem_collection");
            memeroyIndex.put("jvm.memeroy.pool.mem_collection.eden",
                    ((MemoryUsage) MapUtils.getObject(mem_collection, "eden", defualt)).getCommitted());
            memeroyIndex.put("jvm.memeroy.pool.mem_collection.survivor",
                    ((MemoryUsage) MapUtils.getObject(mem_collection, "survivor", defualt)).getCommitted());
            memeroyIndex.put("jvm.memeroy.pool.mem_collection.old",
                    ((MemoryUsage) MapUtils.getObject(mem_collection, "old", defualt)).getCommitted());
           /* memeroyIndex.put("jvm.memeroy.pool.mem_collection.perm",
                    ((MemoryUsage) MapUtils.getObject(mem_collection, "perm", defualt)).getCommitted());*/
        /*    memeroyIndex.put("jvm.memeroy.pool.mem_collection.code_cache",
                    ((MemoryUsage) MapUtils.getObject(mem_collection, "code_cache", defualt)).getCommitted());
*/
            Map<String, MemoryUsage> mem_peak = (Map<String, MemoryUsage>) pool.get("mem_peak");
            memeroyIndex.put("jvm.memeroy.pool.mem_peak.eden",
                    ((MemoryUsage) MapUtils.getObject(mem_peak, "eden", defualt)).getCommitted());
            memeroyIndex.put("jvm.memeroy.pool.mem_peak.survivor",
                    ((MemoryUsage) MapUtils.getObject(mem_peak, "survivor", defualt)).getCommitted());
            memeroyIndex.put("jvm.memeroy.pool.mem_peak.old",
                    ((MemoryUsage) MapUtils.getObject(mem_peak, "old", defualt)).getCommitted());
         /*   memeroyIndex.put("jvm.memeroy.pool.mem_peak.perm",
                    ((MemoryUsage) MapUtils.getObject(mem_peak, "perm", defualt)).getCommitted());*/
            memeroyIndex.put("jvm.memeroy.pool.mem_peak.code_cache",
                    ((MemoryUsage) MapUtils.getObject(mem_peak, "code_cache", defualt)).getCommitted());


            Map<String, MemoryUsage> mem_usage = (Map<String, MemoryUsage>) pool.get("mem_usage");

            memeroyIndex.put("jvm.memeroy.pool.mem_usage.eden",
                    ((MemoryUsage) MapUtils.getObject(mem_usage, "eden", defualt)).getCommitted());
            memeroyIndex.put("jvm.memeroy.pool.mem_usage.survivor",
                    ((MemoryUsage) MapUtils.getObject(mem_usage, "survivor", defualt)).getCommitted());
            memeroyIndex.put("jvm.memeroy.pool.mem_usage.old",
                    ((MemoryUsage) MapUtils.getObject(mem_usage, "old", defualt)).getCommitted());
           /* memeroyIndex.put("jvm.memeroy.pool.mem_usage.perm",
                    ((MemoryUsage) MapUtils.getObject(mem_usage, "perm", defualt)).getCommitted());*/
            memeroyIndex.put("jvm.memeroy.pool.mem_usage.code_cache",
                    ((MemoryUsage) MapUtils.getObject(mem_usage, "code_cache", defualt)).getCommitted());
        }
        memeroyIndex.put("jvm.memeroy.mem_heap",
                ((MemoryUsage) MapUtils.getObject(memStat, "mem_heap", defualt)).getCommitted());
        memeroyIndex.put("jvm.memeroy.mem_nonheap",
                ((MemoryUsage) MapUtils.getObject(memStat, "mem_nonheap", defualt)).getCommitted());

        // tomcat 指标
        Map<String, Object> tomcatStat = getTomcatStat();
        Map<String, Object> tomcatIndex = Maps.newHashMap();
        for (Entry<String, Object> entry : tomcatStat.entrySet()) {
            Map<String, Object> value = (Map<String, Object>) entry.getValue();
            if (MapUtils.isEmpty(value)){
                continue;
            }
            for (Entry<String, Object> objectEntry : value.entrySet()) {
                if (!(objectEntry.getValue() instanceof Number)){
                    continue;
                }
                tomcatIndex.put("tomcat." + entry.getKey() + "." + objectEntry.getKey(), objectEntry.getValue());
            }
        }
        List<Map<String, Object>> indexList = Lists.newArrayList(memeroyIndex, tomcatIndex);
        for (Map<String, Object> index : indexList) {
            for (Entry<String, Object> entry : index.entrySet()) {
                long timestamp = System.currentTimeMillis();
                try {
                    metricsPusher.push(System.currentTimeMillis(), entry.getKey(), new Object[] { entry.getValue() });
                } catch (Exception e) {
                    UnifyLogger.error("sendMetrics", "sendMetrics-fail", false, "timestamp:" + timestamp + ",key:"
                            + entry.getKey() + ",value:" + entry.getValue());
                }
            }
        }

    }

}
