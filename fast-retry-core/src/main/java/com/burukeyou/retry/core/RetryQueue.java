package com.burukeyou.retry.core;

import com.burukeyou.retry.core.task.RetryTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * retry queue
 * @author burukeyou
 */
public interface RetryQueue {

    /**
     * submit a retry task
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * @param retryTask     the retry-task to submit
     * @param <R>           the type of the retry-task's result
     * @return              a Future representing pending completion of the task
     */
    <R> CompletableFuture<R> submit(RetryTask<R> retryTask);

    /**
     * execute retry task and block waiting for results
     * @param retryTask     the retry-task to submit
     * @param <R>           the type of the retry-task's result
     * @return              the result of the retry-task
     */
    <R> R execute(RetryTask<R> retryTask);

    /**
     * Timed out task execution and blocked waiting for results
     * @param retryTask     the retry-task to submit
     * @param <R>           the type of the retry-task's result
     * @param timeout       the maximum time to wait
     * @param timeUnit      the time unit of the timeout argument
     * @return              the result of the retry-task
     */
    <R> R execute(RetryTask<R> retryTask, long timeout, TimeUnit timeUnit) throws TimeoutException;

}
