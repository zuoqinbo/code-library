	package com.xxx.car.flash.common.sharedthreadpool;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.xxx.car.flash.common.constants.ResponseCode;
import com.xxx.car.flash.common.exceptions.BusinessException;
import com.xxx.car.flash.common.pojo.BStatus;
import com.xxx.mobile.car.common.log.UnifyLogger;

public class AsynCallerBuilder {
	
	private static  SharedExecutorPool sdepool = new SharedExecutorPool("sharedAsyncCaller");
	
	AsyncSchema schema;
	
	private AsynCallerBuilder(String callerName){
		this.schema = new AsyncSchema();
		this.schema.name = callerName;
	}
	
	public  static AsynCallerBuilder newBuilder(String callerName){
		return new AsynCallerBuilder(callerName);
	}
	
	public AsynCallerBuilder setName(String name) {
		schema.name = name;
		return this;
	}

	public AsynCallerBuilder setMaxWorkers(int maxWorkers) {
		schema.maxWorkers = maxWorkers;
		return this;
	}

	public AsynCallerBuilder setMaxTasksQueued(int maxTasksQueued) {
		schema.maxTasksQueued = maxTasksQueued;
		return this;
	}

	public <REQ, RES> AsynCallerBuilder setRejectedHandler(RejectedHandler rejectedHandler) {
		schema.rejectedHandler = rejectedHandler;
		return this;
	}

	public AsynCallerBuilder setWaitTimeInMs(long waitTimeInMs) {
		schema.waitTimeInMs = waitTimeInMs;
		return this;
	}

	public AsynCallerBuilder setCoreThread(Integer coreThread) {
		schema.coreThread = coreThread;
		return this;
	}

	public AsynCallerBuilder setTrySynCallWhenReject(boolean trySynCallWhenReject) {
		schema.trySynCallWhenReject = trySynCallWhenReject;
		return this;
	}

	public AsynCallerBuilder setDoNotInvokeWhenTimeOut(boolean doNotInvokeWhenTimeOut) {
		schema.doNotInvokeWhenTimeOut = doNotInvokeWhenTimeOut;
		return this;
	}
	
	public <REQ, RES> AsynCaller<REQ, RES> build(Invoker<REQ, RES> invoker){
		RejectedHandler rejectedHandler = schema.rejectedHandler;
		if (rejectedHandler == null) {
			rejectedHandler = new WriteLogRejectHandler(schema);
		}
		ListeningExecutorService executorService = sdepool.newNoBlockExcecutor(schema.name, schema.maxWorkers, schema.maxTasksQueued, schema.rejectedHandler, schema.waitTimeInMs, schema.coreThread);
		return new AsynCaller<REQ, RES>(executorService, invoker, schema);
	}

	public ListeningExecutorService buildExecutor(){
		RejectedHandler rejectedHandler = schema.rejectedHandler;
		if (rejectedHandler == null) {
			rejectedHandler = new WriteLogRejectHandler(schema);
		}
		ListeningExecutorService executorService = sdepool.newNoBlockExcecutor(schema.name, schema.maxWorkers, schema.maxTasksQueued, schema.rejectedHandler, schema.waitTimeInMs, schema.coreThread);
		return executorService;
	}
	
	public AsynInvoker builder(){
		RejectedHandler rejectedHandler = schema.rejectedHandler;
		if (rejectedHandler == null) {
			rejectedHandler = new WriteLogRejectHandler(schema);
		}
		ListeningExecutorService executorService = sdepool.newNoBlockExcecutor(schema.name, schema.maxWorkers, schema.maxTasksQueued, schema.rejectedHandler, schema.waitTimeInMs, schema.coreThread);
		return new AsynInvoker(executorService, schema);
	}
	
	public static interface Invoker<REQ, RES>{
		RES invoke(REQ request);
	}
	
	
	static class WriteLogRejectHandler implements RejectedHandler{
		private AsyncSchema schema;
		
		public WriteLogRejectHandler(AsyncSchema schema) {
			this.schema = schema;
		}

		@Override
		public void rejectedExecution(Runnable r, SEPExecutor executor) {
			UnifyLogger.error("异步任务-rejected", "", false, "异步任务-rejected,name:" + schema.name);
			RejectedHandler.abortPolicy.rejectedExecution(r, executor);
		}
		
	}
	
	static class AsyncSchema{
		String name;
		int maxWorkers;
		int maxTasksQueued;
		RejectedHandler rejectedHandler;
		long waitTimeInMs = 2000;
		Integer coreThread;
		/**
		 * 当reject时，异步转同步
		 * **/
		boolean trySynCallWhenReject = true;
		/**
		 * 如果等待时间超过watiTimeInMs设定的时间，不再请求该接口
		 * **/
		boolean doNotInvokeWhenTimeOut = false;
	} 
	
	public static class AsynInvoker{
		private ListeningExecutorService executorService;
		private AsyncSchema schema;
		public AsynInvoker(ListeningExecutorService executorService, AsyncSchema schema) {
			this.executorService = executorService;
			this.schema = schema;
		}
		public <RES> ListenableFuture<RES> call(Callable<RES> callable){
			CallableAdapter<RES> adapter = new CallableAdapter<RES>(callable, schema);
			try {
				ListenableFuture<RES> future = executorService.submit(adapter);
				return future;
			} catch (RejectedExecutionException e) {
				UnifyLogger.error("AsynCaller-rejected", "AsynCaller-rejected", true, "task:" + schema.name);
				if (schema.trySynCallWhenReject) {
					return ListenableFutureTask.create(callable);
				}
				throw e;
			}
		} 
	}
	
	public static class AsynCaller<REQ,RES>{
		
		private ListeningExecutorService executorService;
		private Invoker<REQ, RES> invoker;
		private AsyncSchema schema;
		
		public AsynCaller(ListeningExecutorService executorService,
				Invoker<REQ, RES> invoker, AsyncSchema schema) {
			this.executorService = executorService;
			this.invoker = invoker;
			this.schema = schema;
		}

		public ListenableFuture<RES> call(final REQ request){
			try {
				ListenableFuture<RES> future = executorService.submit(new CallableWithParam<REQ, RES>(invoker, request, schema));
				return future;
			} catch (RejectedExecutionException e) {
				UnifyLogger.error("asynCaller-rejected", "asynCaller-rejected", true, "task:" + schema.name);
				if (schema.trySynCallWhenReject) {
					UnifyLogger.info("INFO", false, "设置了同步调用，改同步调用");
					return MoreExecutors.sameThreadExecutor().submit(new Callable<RES>() {

						@Override
						public RES call() throws Exception {
							return invoker.invoke(request);
						}
					});
				}
				throw e;
			}
		} 
	}
	
	static class CallableWithParam<REQ, RES> implements Callable<RES>{
		private Invoker<REQ, RES> invoker;
		private REQ request;
		private long ts;
		private AsyncSchema schema;
		
		public CallableWithParam(Invoker<REQ, RES> invoker, REQ request, AsyncSchema schema) {
			this.invoker = invoker;
			this.request = request;
			this.ts = System.currentTimeMillis();
			this.schema = schema;
		}
		@Override
		public RES call() throws Exception {
			long wt = System.currentTimeMillis() - ts;
			if (schema.doNotInvokeWhenTimeOut && wt > schema.waitTimeInMs) {
				UnifyLogger.error("CallableWithParam", "", true, "等待超时，name:"+ schema.name  + "且设置了doNotInvokeWhenTimeOut,不再执行接口" + "配置值为:" + schema.waitTimeInMs + "ms, 实际等待:" + wt + "ms");
				throw new TimeoutException("asynCaller:" + schema.name + ",等待超时,配置值为:" + schema.waitTimeInMs + "ms, 实际等待:" + wt + "ms");
			}
			return invoker.invoke(request);
		}
		
	}
	
	static class CallableAdapter<RES> implements Callable<RES>{
		Callable<RES> callable;
		private long ts;
		private AsyncSchema schema;
		public CallableAdapter(Callable<RES> callable,
				AsyncSchema schema) {
			this.callable = callable;
			this.ts = System.currentTimeMillis();
			this.schema = schema;
		}
		@Override
		public RES call() throws Exception {
			long wt = System.currentTimeMillis() - ts;
			if (schema.doNotInvokeWhenTimeOut && wt > schema.waitTimeInMs) {
				UnifyLogger.error("CallableAdapter", "", true, "等待超时，name:"+ schema.name  + "且设置了doNotInvokeWhenTimeOut,不再执行接口" + "配置值为:" + schema.waitTimeInMs + "ms, 实际等待:" + wt + "ms");
				throw new TimeoutException("asynCaller:" + schema.name + ",等待超时,配置值为:" + schema.waitTimeInMs + "ms, 实际等待:" + wt + "ms");
			}
			return callable.call();
		}
		
	}
	
	static class LazyFuture<REQ, RES> implements Future<RES>{
		
		private REQ key;
		private Invoker<REQ, RES> invoker;
		private CallableAdapter<RES> callableAdapter;
		
		public LazyFuture(REQ key, Invoker<REQ, RES> invoker) {
			this.key = key;
			this.invoker = invoker;
		}

		public LazyFuture(CallableAdapter<RES> callableAdapter) {
			this.callableAdapter = callableAdapter;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public RES get() throws InterruptedException, ExecutionException {
			if (invoker != null) {
				return invoker.invoke(key);
			}else {
				try {
					return callableAdapter.call();
				} catch (Exception e) {
					throw new ExecutionException(e);
				}
			}
		}

		@Override
		public RES get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return get();
		}
		
	}
	
}
