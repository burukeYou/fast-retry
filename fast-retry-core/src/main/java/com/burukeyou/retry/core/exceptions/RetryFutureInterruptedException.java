package com.burukeyou.retry.core.exceptions;


import java.util.concurrent.CompletableFuture;

/**
 * Exception class signifiying that an attempt to get result  from {@link CompletableFuture}
 * was interrupted
 *
 * @author Rob Harrop
 * @since 2.1
 */
public class RetryFutureInterruptedException extends FastRetryException{

    public RetryFutureInterruptedException(String msg) {
        super(msg);
    }

    public RetryFutureInterruptedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
