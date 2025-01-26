package com.burukeyou.retry.spring.support;

import java.util.Map;

import com.burukeyou.retry.spring.annotations.FastRetry;
import org.aopalliance.intercept.MethodInvocation;


/**
 * @author caizhihao
 */
public interface FastRetryMethodInvocation extends MethodInvocation {

    /**
     * Get the maximum number of retries, Equal to {@link FastRetry#maxAttempts()}
     */
    long getMaxAttempts();

    /**
     * {@link #getMaxAttempts()} plus 1 is ExecuteCount
     */
    default long getMaxExecuteCount(){
        return getMaxAttempts() + 1;
    }

    /**
     * The current execute count
     */
    long getCurExecuteCount();

    /**
     * It is whether {@link #getCurExecuteCount()} is equal to one
     */
    default boolean isFirstExecute(){
        return getCurExecuteCount() == 1;
    }

    /**
     * There is no special meaning, just to facilitate the transfer of parameters between different methods
     */
    Map<String,Object> attachmentMap();

    /**
     * Get the absolute name of the method, including the class name
     */
    String getMethodAbsoluteName();
}
