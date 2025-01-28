package com.burukeyou.retry.core.exceptions;

import com.burukeyou.retry.core.policy.FastMethodPolicy;

/**
 *  When using a custom result retry strategy {@link FastMethodPolicy},
 *  if the actual method return value is different from the return value type defined by the {@link FastMethodPolicy},
 *  this exception is thrown and the retry is stopped
 *
 *
 */
public class RetryPolicyCastException extends RuntimeException {

    private static final long serialVersionUID = 6167771894695344211L;

    public RetryPolicyCastException() {
    }

    public RetryPolicyCastException(String message) {
        super(message);
    }

    public RetryPolicyCastException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryPolicyCastException(Throwable cause) {
        super(cause);
    }

    public RetryPolicyCastException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
