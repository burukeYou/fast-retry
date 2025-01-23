package com.burukeyou.retry.spring.support;

import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;


/**
 * @author caizhihao
 */
public interface FastRetryMethodInvocation extends MethodInvocation {

    /**
     * The current execute count
     */
    long getCurExecuteCount();

    /**
     * There is no special meaning, just to facilitate the transfer of parameters between different methods
     */
    Map<String,Object> attachmentMap();

    /**
     * Get the absolute name of the method, including the class name
     */
    String getMethodAbsoluteName();
}
