package com.burukeyou.retry.spring.interceptor;


import com.burukeyou.retry.spring.support.FastRetryMethodInvocation;


/**
 * Retry Interceptor
 * @author  caizhihao
 */

public interface FastRetryMethodInterceptor<T>  {

    String INNER_USE_KEY = "INNER_USE_KEY";

    /**
     * Execute before retry
     * @param invocation                proxy method
     * @return                          if true continue to execute ,  or else stop to execute
     */
    default void beforeExecute(FastRetryMethodInvocation invocation) throws Exception{}

    /**
     * Execute after retry fail
     * @param exception                 the method execute exception
     * @param invocation                proxy method
     */
    default void afterExecuteFail(Exception exception, FastRetryMethodInvocation invocation) throws Exception {
    }

    /**
     * Execute after retry success
     * @param result            method return value
     * @param invocation        proxy method
     * @return                  if true continue to retry invoke ,  or else stop retry invoke
     */
    default boolean afterExecuteSuccess(T result,FastRetryMethodInvocation invocation){
        return false;
    }

}
