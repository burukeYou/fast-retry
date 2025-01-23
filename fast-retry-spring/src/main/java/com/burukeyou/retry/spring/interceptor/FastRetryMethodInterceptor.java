package com.burukeyou.retry.spring.interceptor;


import com.burukeyou.retry.spring.support.FastRetryMethodInvocation;


/**
 * Retry Interceptor
 * @author  caizhihao
 */

public interface FastRetryMethodInterceptor {

    String INNER_USE_KEY = "INNER_USE_KEY";

    /**
     * Execute before retrying
     * @param invocation                proxy method
     * @return                          if true continue to execute , otherwise stop to execute
     */
    boolean beforeExecute(FastRetryMethodInvocation invocation) throws Exception;

    /**
     * Execute after retrying
     * @param exception                 the method execute exception
     * @param result                    the method execute result
     * @param invocation                proxy method
     * @return                          if true continue to execute , otherwise stop to execute
     */
    boolean afterExecute(Exception exception, Object result, FastRetryMethodInvocation invocation) throws Exception;

}
