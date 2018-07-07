import com.codahale.metrics.Gauge;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.xxx.car.flash.common.metrics.ZtcMetrics;
import com.xxx.car.flash.common.sharedthreadpool.SEPWorker.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xxx.tc.qtracer.QTracer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class SharedExecutorPool {
	
	private static Logger logger = LoggerFactory.getLogger(SharedExecutorPool.class);
	
	 // the name assigned to workers in the pool, and the id suffix
    final String poolName;
    final AtomicLong workerId = new AtomicLong();

    // the collection of executors serviced by this pool; periodically ordered by traffic volume
    final List<SEPExecutor> executors = new CopyOnWriteArrayList<SEPExecutor>();
    // record name
    final Map<String, SEPExecutor> executorsMap = new HashMap<String, SEPExecutor>();
    
    // the number of workers currently in a spinning state
    final AtomicInteger spinningCount = new AtomicInteger();
    // see SEPWorker.maybeStop() - used to self coordinate stopping of threads
    final AtomicLong stopCheck = new AtomicLong();
    // the collection of threads that are (most likely) in a spinning state - new workers are scheduled from here first
    // TODO: consider using a queue partially-ordered by scheduled wake-up time
    // (a full-fledged correctly ordered SkipList is overkill)
    final ConcurrentSkipListMap<Long, SEPWorker> spinning = new ConcurrentSkipListMap<Long, SEPWorker>();
    // the collection of threads that have been asked to stop/deschedule - new workers are scheduled from here last
    final ConcurrentSkipListMap<Long, SEPWorker> descheduled = new ConcurrentSkipListMap<Long, SEPWorker>();
    
    final AtomicLong totalWorks = new AtomicLong(0);
    
    //-------------
    
    public SharedExecutorPool(String poolName)
    {
        this.poolName = poolName;
        final String metricsName =  "shareexecutorpool:" + poolName;
        ZtcMetrics.newGauge(metricsName + ":spinning", new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				Integer spin = spinningCount.get();
				return spin;
			}
    		
		});
    	
    	
        ZtcMetrics.newGauge(metricsName + ":descheduled", new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				Integer descheduledCnt = descheduled.size();
				return descheduledCnt;
			}
    		
		});
    	
        ZtcMetrics.newGauge(metricsName + ":total", new Gauge<Long>() {

			@Override
			public Long getValue() {
				long active = totalWorks.get();
//				logger.debug("metricsName:{}, totalWorks:{}", metricsName, active);
				return active;
			}
    		
		});
    	
    	
        ZtcMetrics.newGauge(metricsName + ":active", new Gauge<Long>() {

			@Override
			public Long getValue() {
				long total = totalWorks.get();
				long active = total - spinningCount.get() - descheduled.size();
//				logger.debug("metricsName:{}, active:{}", metricsName, active);
				return active;
			}
    		
		});
    	
    }

    void schedule(Work work)
    {
        // we try to hand-off our work to the spinning queue before the descheduled queue, even though we expect it to be empty
        // all we're doing here is hoping to find a worker without work to do, but it doesn't matter too much what we find;
        // we atomically set the task so even if this were a collection of all workers it would be safe, and if they are both
        // empty we schedule a new thread
        Map.Entry<Long, SEPWorker> e;
        while (null != (e = spinning.pollFirstEntry()) || null != (e = descheduled.pollFirstEntry())){
        	if (e.getValue().assign(work, false))
        		return;
        }
        
        if (!work.isStop()){
        	SEPWorker wokrer = new SEPWorker(workerId.incrementAndGet(), work, this);
        	long act = totalWorks.incrementAndGet();
        	logger.info("new a worker, workId:" + wokrer.thread.getName() + ",totalWorks:" + act);
        }
            
    }

    void maybeStartSpinningWorker()
    {
        // in general the workers manage spinningCount directly; however if it is zero, we increment it atomically
        // ourselves to avoid starting a worker unless we have to
        int current = spinningCount.get();
        if (current == 0 && spinningCount.compareAndSet(0, 1))
            schedule(Work.SPINNING);
    }
    
    /****
     * 如果队列满，阻塞主线程，至到队列重新能够接受任务
     * **/
    public ExecutorService newExecutor(String name, int maxWorkers, int maxTasksQueued){
    	SEPExecutor executor = new SEPExecutor(this, name, maxWorkers, maxTasksQueued);
    	SEPExecutor elderExecutor = this.executorsMap.put(name, executor);
    	if (elderExecutor != null) {
			elderExecutor.shutdown();
			executors.remove(elderExecutor);
		}
    	executors.add(executor);
    	addMonitor(executor);
    	maybeStartSpinningWorker();//如果没有spinning线程，新建一个
    	return wrap(executor);//兼容qtrace
    }
    
    private void addMonitor(final SEPExecutor executor) {
    	//添加executor监控
    	String name = executor.getExecutorName();
    	final String metricsName =  "executor:" + name;
    	ZtcMetrics.newGauge(metricsName + ":actThread", new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				Integer value = executor.getActiveCount();
				return value;
			}
    		
		});

    	ZtcMetrics.newGauge(metricsName + ":pendings", new Gauge<Long>() {

			@Override
			public Long getValue() {
				Long v = executor.getPendingTasks();
				return v;
			}
    		
		});
    	
    	ZtcMetrics.newGauge(metricsName + ":blocks", new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				Integer blocks = executor.getCurrentlyBlockedTasks();
				return blocks;
			}
    		
		});
    	
    	ZtcMetrics.newGauge(metricsName + ":highThread", new Gauge<Integer>() {

			@Override
			public Integer getValue() {
				Integer v = executor.getHighestActiveThread();
				return v;
			}
    		
		});
    	
	}

	/***
     * 如果队列满，不阻塞主线程，reject task
     * **/
    public ListeningExecutorService newNoBlockExcecutor(String name, int maxWorkers, int maxTasksQueued, RejectedHandler rejectedHandler){
    	return newNoBlockExcecutor(name, maxWorkers, maxTasksQueued, rejectedHandler, NoBlockSEPExecutor.dedaultWaitTimeInMs, null);
    }
    /***
     * 如果队列满，不阻塞主线程，reject task
     * @param waitTimeInMs 添加任务最长等待时长
     * **/
    public ListeningExecutorService newNoBlockExcecutor(String name, int maxWorkers, int maxTasksQueued, RejectedHandler rejectedHandler, long waitTimeInMs, Integer coreThread){
    	NoBlockSEPExecutor executor = new NoBlockSEPExecutor(this, name, maxWorkers, maxTasksQueued, rejectedHandler, waitTimeInMs, coreThread);
    	SEPExecutor elderExecutor = this.executorsMap.put(name, executor);
    	if (elderExecutor != null) {
			elderExecutor.shutdown();
		}
    	addMonitor(executor);
    	maybeStartSpinningWorker();//如果没有spinning线程，新建一个
    	this.executors.add(executor);
    	this.executorsMap.put(name, executor);
    	return wrap(executor);//兼容qtrace
    }

	public List<SEPExecutor> getExecutors() {
		return ImmutableList.copyOf(executors);
	}
    
	/***
	 * 传递threadLocal变量
	 * **/
    private ListeningExecutorService wrap(SEPExecutor executor){
    	ExecutorService es =  QTracer.wrap(executor);
    	return new TraceExecutorServiceAdaptor(es);// 无线的trace信息
    }

    private SEPExecutor getExcutorByName(String name){
    	for(SEPExecutor executor : executors){
    		if (executor.getExecutorName().equals(name)) {
				return executor;
			}
    	}
    	return null;
    }
	
    public SEPExecutorStat getExecutorStat(String callerName) {
		SEPExecutor executor = getExcutorByName(callerName);
		if (executor != null) {
			return executor.getStat();
		}
		return null;
	}
	
    public SharedPoolStat getSharedPoolStat(){
    	SharedPoolStat stat = new SharedPoolStat();
    	stat.setPoolName(poolName);
    	stat.setDescheduled(this.descheduled.size());
    	stat.setExcutorSize(executors.size());
    	stat.setSpinning(this.spinning.size());
    	stat.setActiveThreads(totalWorks.get());
    	return stat;
    }
    
    public void maySchudleExector(String callerName){
    	SEPExecutor executor = getExcutorByName(callerName);
		if (executor != null) {
			 executor.maybeSchedule();
		}
    }
	
	
	
}
