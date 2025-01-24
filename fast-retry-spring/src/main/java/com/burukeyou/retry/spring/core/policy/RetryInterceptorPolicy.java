package com.burukeyou.retry.spring.core.policy;

import com.burukeyou.retry.core.policy.RetryPolicy;
import com.burukeyou.retry.spring.support.FastRetryMethodInvocation;

/**
 * Provides policy and lifecycle handling before and after the retry
 *
 * @param <T> method return value
 * @author caizhihao
 */
public interface RetryInterceptorPolicy<T> extends RetryPolicy {

    /**
     * Execute before retry
     *
     * @param invocation proxy method
     * @return if true continue to execute ,  or else stop to execute
     */
    default boolean beforeExecute(FastRetryMethodInvocation invocation) throws Exception {
        return true;
    }

    /**
     * Execute after retry fail
     *
     * @param exception  the method execute exception
     * @param invocation proxy method
     */
    default boolean afterExecuteFail(Exception exception, FastRetryMethodInvocation invocation) throws Exception {
        throw exception;
    }

    /**
     * Execute after retry success
     *
     * @param methodReturnValue method return value
     * @param invocation        proxy method
     * @return if true continue to retry invoke ,  or else stop retry invoke
     */
    default boolean afterExecuteSuccess(T methodReturnValue, FastRetryMethodInvocation invocation) {
        return false;
    }

}
