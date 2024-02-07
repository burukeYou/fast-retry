package com.burukeyou.retry.core.support;


import com.burukeyou.retry.core.FastRetryQueue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            T result = super.get(timeout, unit);
            return result;
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw e;
        } catch (TimeoutException e) {
            throw e;
        }finally {
            queueTask.stopRetry();
        }
    }
}
