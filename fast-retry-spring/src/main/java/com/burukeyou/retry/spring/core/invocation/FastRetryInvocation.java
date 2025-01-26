package com.burukeyou.retry.spring.core.invocation;

import java.util.Map;

import com.burukeyou.retry.spring.annotations.FastRetry;

public interface FastRetryInvocation {

    /**
     * Get the maximum number of retries, Equal to {@link FastRetry#maxAttempts()}
     */
    long getMaxAttempts();

    long getActualExecuteCount();

    /**
     * There is no special meaning, just to facilitate the transfer of parameters between different methods
     */
    Map<String,Object> attachmentMap();

    /**
     * Get the absolute name of the method, including the class name
     */
    String getMethodAbsoluteName();


}
