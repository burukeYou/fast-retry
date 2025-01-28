package com.burukeyou.retry.spring.core.retrytask;

import com.burukeyou.retry.core.policy.RetryPolicy;
import com.burukeyou.retry.spring.annotations.RetryWait;
import com.burukeyou.retry.spring.core.interceptor.FastRetryInterceptor;
import com.burukeyou.retry.spring.core.policy.LogEnum;

/**
 * @author  caizhihao
 */
public interface FastRetryAdapter {

    /**
     * @return the maximum number of attempts , if -1, it means unlimited
     */
    int maxAttempts();

    /**
     * How long will it take to start the next retry, equals to {@link #delay},
     * If both are configured, retryWait() will take effect first
     */
    RetryWait retryWait() ;

    /**
     * How long will it take to start the next retry, unit is MILLISECONDS
     */
    long delay();

    /**
     * Flag to say that whether try again when an exception occurs
     * @return try again if true
     */
    boolean retryIfException();

    /**
     * Exception types that are retryable.
     *
     * @return exception types to retry
     */
    Class<? extends Exception>[] include();

    /**
     * Exception types that are not retryable.
     *
     * @return exception types to stop retry
     */
    Class<? extends Exception>[] exclude();

    /**
     * Flag to say that whether recover when an exception occurs
     *
     * @return throw exception if false, if true return null and print exception log
     */
    boolean exceptionRecover();

    /**
     * Flag to say that whether print every time execute retry exception log, just prevent printing too many logs
     */
    LogEnum errLog();

    /**
     * Set whether to simplify the stack information of the printing exception,
     * if so, only the first three lines of stack information will be printed,
     * and if the first execution fails, this configuration will be ignored,
     * and the complete exception information will still be printed
     * @see #errLog()
     */
    boolean briefErrorLog();

    /**
     *  use custom  retry strategy,
     * @return the class of retry-result-policy
     */
    Class<? extends RetryPolicy> policy() ;

    /**
     *  use custom  retry interceptor,
     */
    Class<? extends FastRetryInterceptor> interceptor() ;

}
