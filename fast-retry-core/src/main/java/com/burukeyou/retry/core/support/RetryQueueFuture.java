package com.burukeyou.retry.core.support;


import com.burukeyou.retry.core.FastRetryQueue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author caizhihao
 * @param <T>
 */
public class RetryQueueFuture<T> extends CompletableFuture<T> {

    private FastRetryQueue.QueueTask queueTask;

    public RetryQueueFuture() {
    }

    public RetryQueueFuture(FastRetryQueue.QueueTask queueTask) {
        this.queueTask = queueTask;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        queueTask.stopRetry();
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (ExecutionException e){
            if (e.getCause() instanceof RuntimeException){
                throw (RuntimeException)e.getCause();
            }else {
                throw e;
            }
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException){
                throw (RuntimeException)e.getCause();
            }else {
                throw e;
            }
        } catch (TimeoutException e) {
            throw e;
        }finally {
            queueTask.stopRetry();
        }
    }
}
