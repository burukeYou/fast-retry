package com.burukeyou.retry.spring.core.interceptor;

import com.burukeyou.retry.spring.core.invocation.FastRetryInvocation;

/**
 * method invoke Interceptor
 * @author  caizhihao
 */
public interface FastRetryInterceptor {

    /**
     * This method is called before the method is executed
     * @param methodInvocation          the method invocation
     */
    void methodInvokeBefore(FastRetryInvocation methodInvocation);

    /**
     * This method is called back regardless of whether an exception is executed
     * @param result                method invoke result, if  exception is thrown, the result is null
     * @param throwable             the exception thrown by the  method
     * @param methodInvocation      the method invocation
     * @return                      the new result of the method, Usually its value is the same as param result
     */
    Object methodInvokeAfter(Object result, Throwable throwable, FastRetryInvocation methodInvocation);

}
