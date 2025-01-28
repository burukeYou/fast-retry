package com.burukeyou.retry.spring.core.invocation;

import com.burukeyou.retry.spring.annotations.FastRetry;
import org.aopalliance.intercept.MethodInvocation;

import java.util.Map;

/**
 * Retry Invocation Context
 * @author caizhihao
 */
public interface FastRetryInvocation extends MethodInvocation {

    /**
     * get the config FastRetry annotation
     */
    FastRetry getFastRetry();

    /**
     * get the current execute count
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
