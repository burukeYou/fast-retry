package com.burukeyou.retry.spring.support;

import com.burukeyou.retry.core.exceptions.RetryFutureInterruptedException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *  support the future result
 * @param <T>
 */
public class FastFutureCallable<T> implements Callable<T> {

    private Callable<T> callable;

    private FastRetryFuture<T> returnValueFastRetryFuture;

    private boolean callFlag = false;

    public FastFutureCallable(Callable<T> callable) {
        this.callable = callable;
    }

    @Override
    public T call() throws Exception {
        T result = null;
        try {
            result = callable.call();
        } finally {
            if (!callFlag){
                if (result instanceof FastRetryFuture){
                    returnValueFastRetryFuture =  (FastRetryFuture<T>) result;
                }
                callFlag = true;
            }
        }

        if (result instanceof Future){
            try {
                result = ((Future<T>) result).get();
            } catch (InterruptedException e) {
                throw new RetryFutureInterruptedException("Thread interrupted while futureCallable get result ",e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception){
                    throw (Exception) cause;
                } else {
                    throw e;
                }
            }
        }
        return result;
    }

    public FastRetryFuture<T> getReturnValueFastRetryFuture() {
        return returnValueFastRetryFuture;
    }

    public boolean isCallFlag() {
        return callFlag;
    }
}
