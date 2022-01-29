package com.lxj.rapid.client;

import com.lxj.rapid.codec.RpcRequest;
import com.lxj.rapid.codec.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Xingjing.Li
 * @since 2022/1/28
 */
@Slf4j
public class RpcFuture implements Future<Object> {

    private RpcRequest request;

    private RpcResponse response;

    private long startTime;

    private final static long TIME_THRESHOLD = 5000;

    private List<RpcCallback> pendingCallbacks = new ArrayList<>();

    private Sync sync;

    private ReentrantLock callBackLock = new ReentrantLock();

    private ThreadPoolExecutor executor =
            new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024));

    public RpcFuture(RpcRequest request) {
        this.startTime = System.currentTimeMillis();
        this.request = request;
        this.sync = new Sync();
    }

    /**
     * 实际的回调处理
     * @param rpcResponse
     */
    public void done(RpcResponse rpcResponse) {
        this.response = rpcResponse;
        boolean success = sync.release(1);
        if (success){
            invokeCallbacks();
        }
        //rpc调用的耗时时间
        long consumeTime = System.currentTimeMillis() - startTime;
        if (TIME_THRESHOLD < consumeTime){
            log.warn("rpc response is too low, requestId {}, time cost {}", this.request.getRequestId(), consumeTime);
        }
    }

    /**
     * 依次执行回调函数
     */
    private void invokeCallbacks() {
        callBackLock.lock();
        try {
            if (CollectionUtils.isNotEmpty(pendingCallbacks)){
                for (final RpcCallback callback : pendingCallbacks) {
                    runCallback(callback);
                }
            }
        } finally {
            callBackLock.unlock();
        }
    }

    private void runCallback(RpcCallback callback) {
        final RpcResponse response = this.response;
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (response.getThrowable() == null){
                    callback.success(response.getResult());
                }else {
                    callback.failure(response.getThrowable());
                }
            }
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (this.response != null){
            return this.response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean isSuccess = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (isSuccess){
            if (this.response != null){
                return this.response.getResult();
            }
        }else {
            throw new RuntimeException("timeout exception requestId "+this.request.getRequestId()+", method "+this.request.getMethodName());
        }
        return null;
    }

    class Sync extends AbstractQueuedSynchronizer{
        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int acquires) {
            return getState() == done ? true : false ;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (getState() == pending){
                if (compareAndSetState(pending, done)){
                    return true;
                }
            }
            return false;
        }

        public boolean isDone(){
            return getState() == done;
        }
    }

    public RpcFuture addCallback(RpcCallback callback){
        callBackLock.lock();
        try {
            if (isDone()){
                runCallback(callback);
            }else {
                pendingCallbacks.add(callback);
            }
        } finally {
            callBackLock.unlock();
        }
        return this;
    }
}
