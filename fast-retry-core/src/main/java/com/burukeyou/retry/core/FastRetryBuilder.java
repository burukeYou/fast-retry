package com.burukeyou.retry.core;

import com.burukeyou.retry.core.policy.FastResultPolicy;
import com.burukeyou.retry.core.task.RetryTaskContext;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A builder used to configure and create a {@link FastRetryer}.
 * Fluent API to configure new instance of FastRetryer. For detailed description of each
 * builder method - see it's doc.
 *
 * <p>
 * Examples: <pre>{@code
 * FastRetryBuilder.builder()
 *          .attemptMaxTimes(3)
 *          .waitRetryTime(3, TimeUnit.SECONDS)
 *          .retryIfException(true)
 *          .retryIfExceptionOfType(TimeoutException.class)
 *          .exceptionRecover(true)
 *          .retryPolicy(resultPolicy)
 *          .build();
 *
 * }</pre>
 *
 *
 * <p>
 * Not thread safe. Building should be performed in a single thread. Also, there is no
 * guarantee that all constructors of all fields are thread safe in-depth (means employing
 * only volatile and final writes), so, in concurrent environment, it is recommended to
 * ensure presence of happens-before between publication and any usage. (e.g. publication
 * via volatile write, or other safe publication technique)
 *
 * @author buruekeyou
 */
public class FastRetryBuilder<V> {

    private final RetryQueue retryQueue;

    private static RetryQueue defaultRetryQueue;

    private List<Class<? extends Exception>> exceptions;
    private List<Class<? extends Exception>> excludeExceptions;

    private Integer attemptMaxTimes = 100;
    private Long waitRetryTime = 2 * 1000L;
    private Boolean retryIfException = true;

    private Boolean exceptionRecover = false;
    private FastResultPolicy<V> resultPolicy;

    public static <T> CompletableFuture<T> of(T data) {
        return CompletableFuture.completedFuture(data);
    }

    /**
     *  builder by default retry queue
     */
    public static <V> FastRetryBuilder<V> builder() {
        return new FastRetryBuilder<>();
    }

    /**
     *  builder by custom retry queue
     */
    public static <V> FastRetryBuilder<V> builder(RetryQueue retryQueue) {
        return new FastRetryBuilder<>(retryQueue);
    }

    private FastRetryBuilder() {
        this.retryQueue = getDefaultRetryQueue();
    }
    private FastRetryBuilder(RetryQueue retryQueue) {
        this.retryQueue = retryQueue;
    }

    private RetryQueue getDefaultRetryQueue() {
        if (defaultRetryQueue == null) {
            synchronized (FastRetryBuilder.class) {
                if (defaultRetryQueue == null) {
                    defaultRetryQueue = new FastRetryQueue(8);
                }
            }
        }
        return defaultRetryQueue;
    }


    /**
     * @param flag    whether try again when an exception occurs
     */
    public FastRetryBuilder<V> retryIfException(boolean flag) {
        this.retryIfException = flag;
        return this;
    }

    /**
     * @param exceptionClass     Exception types that are retryable.
     */
    public FastRetryBuilder<V> retryIfExceptionOfType(Class<? extends Exception>...exceptionClass) {
        exceptions = Arrays.asList(exceptionClass);
        return this;
    }

    /**
     * @param exceptionClass     Exception types that are not retryable.
     */
    public FastRetryBuilder<V> notRetryIfExceptionOfType(Class<? extends Exception>...exceptionClass) {
        excludeExceptions = Arrays.asList(exceptionClass);
        return this;
    }

    /**
     * @param flag   whether recover when an exception occurs
     */
    public FastRetryBuilder<V> exceptionRecover(boolean flag) {
        this.exceptionRecover = flag;
        return this;
    }

    /**
     * @param maxTimes      the maximum number of attempts , if -1, it means unlimited
     */
    public FastRetryBuilder<V> attemptMaxTimes(int maxTimes) {
        this.attemptMaxTimes = maxTimes;
        return this;
    }

    /**
     * @param time   retry wait time
     * @param unit   the retry wait time param unit
     */
    public FastRetryBuilder<V> waitRetryTime(long time, TimeUnit unit) {
        this.waitRetryTime = unit.toMillis(time);
        return this;
    }

    /**
     * @param resultPolicy      use custom result retry policy,
     */
    public FastRetryBuilder<V> retryPolicy(FastResultPolicy<V> resultPolicy) {
        this.resultPolicy = resultPolicy;
        return this;
    }

    /**
     * by configuration and build resulting {@link FastRetryer}. For default
     * behaviour and concurrency note see class-level doc of {@link FastRetryBuilder}.

     * @return new instance of {@link FastRetryer}
     */
    public  <T> FastRetryer<T> build() {
        RetryTaskContext retryTaskContext = new RetryTaskContext();
        retryTaskContext.setAttemptMaxTimes(attemptMaxTimes);
        retryTaskContext.setWaitRetryTime(waitRetryTime);
        retryTaskContext.setRetryIfException(retryIfException);
        retryTaskContext.setExceptionRecover(exceptionRecover);
        retryTaskContext.setResultPolicy((FastResultPolicy<Object>) resultPolicy);
        retryTaskContext.setExceptionsType(exceptions);
        retryTaskContext.setExcludeExceptionsType(excludeExceptions);
        return new FastRetryer<>(retryQueue,retryTaskContext);
    }


}
