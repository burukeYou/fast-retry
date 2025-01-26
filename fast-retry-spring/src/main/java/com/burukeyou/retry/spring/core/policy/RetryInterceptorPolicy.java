package com.burukeyou.retry.spring.core.policy;

import com.burukeyou.retry.core.policy.RetryPolicy;
import com.burukeyou.retry.spring.core.invocation.FastRetryInvocation;

/**
 * Provides policy and lifecycle handling before and after the retry
 *
 * @param <T> method return value
 * @author caizhihao
 */
public interface RetryInterceptorPolicy<T> extends RetryPolicy {

    /**
     * Before each retry execution
     *
     * @param invocation proxy method
     * @return if true continue to execute ,  or else stop to execute
     */
    default boolean beforeExecute(FastRetryInvocation invocation) throws Exception {
        return true;
    }

    /**
     * After each failed retry execution
     *
     * @param exception  the method execute exception
     * @param invocation proxy method
     */
    default boolean afterExecuteFail(Exception exception, FastRetryInvocation invocation) throws Exception {
        throw exception;
    }

    /**
     * After each successful retry execution
     *
     * @param methodReturnValue method return value
     * @param invocation        proxy method
     * @return if true continue to retry invoke ,  or else stop retry invoke
     */
    default boolean afterExecuteSuccess(T methodReturnValue, FastRetryInvocation invocation) {
        return false;
    }

}
