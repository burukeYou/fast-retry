package com.burukeyou.retry.core.support;

import com.burukeyou.retry.core.exceptions.RetryFutureInterruptedException;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 *  support the future result
 * @param <T>
 */
public class FutureCallable<T> implements Callable<T> {

    private Callable<T> callable;

    public FutureCallable(Callable<T> callable) {
        this.callable = callable;
    }

    @Override
    public T call() throws Exception {
        T result = callable.call();
        if (result instanceof CompletableFuture){
            try {
                result = ((CompletableFuture<T>) result).get();
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
}
