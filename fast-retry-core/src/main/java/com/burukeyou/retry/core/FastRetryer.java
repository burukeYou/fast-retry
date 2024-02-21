package com.burukeyou.retry.core;

import com.burukeyou.retry.core.task.RetryBuilderRetryTask;
import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.core.task.RetryTaskContext;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * a retryer that can submit and execute retry tasks
 * but all is delegate to the retry queue
 * @author  burukeyou
 * @param <T>
 */
public class FastRetryer<T>  {

    private final RetryQueue retryQueue;
    private final RetryTaskContext retryTaskContext;

    public FastRetryer(RetryQueue retryQueue, RetryTaskContext retryTaskContext) {
        this.retryQueue = retryQueue;
        this.retryTaskContext = retryTaskContext;
    }

    /**
     *
     * @param callable          The function of executing each retry
     * @return                  a Future representing pending completion of the task
     */
    public CompletableFuture<T> submit(Callable<T> callable){
        RetryTask<T> retryTask = buildRetryTask(callable);
        return retryQueue.submit(retryTask);
    }

    /**
     * execute retry task and block waiting for results
     * @param callable          The function of executing each retry
     * @return                  the result of the Callable
     */
    public  T execute(Callable<T> callable){
        RetryTask<T> retryTask = buildRetryTask(callable);
        return retryQueue.execute(retryTask);
    }

    /**
     * timed out task execution and blocked waiting for results
     * @param callable          The function of executing each retry
     * @param timeout           the maximum time to wait
     * @param timeUnit          the time unit of the timeout argument
     * @return                  the result of the Callable
     * @throws TimeoutException if the task execution exceeds the specified time
     */
    public  T execute(Callable<T> callable, long timeout, TimeUnit timeUnit) throws TimeoutException {
        RetryTask<T> retryTask = buildRetryTask(callable);
        return retryQueue.execute(retryTask);
    }

    private RetryTask<T> buildRetryTask(Callable<T> callable) {
        RetryBuilderRetryTask retryTask = new RetryBuilderRetryTask((Callable<Object>) callable, retryTaskContext);
        return (RetryTask<T>) retryTask;
    }


}
