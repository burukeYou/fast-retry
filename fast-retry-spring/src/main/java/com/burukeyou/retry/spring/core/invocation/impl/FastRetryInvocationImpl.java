package com.burukeyou.retry.spring.core.invocation.impl;


import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.core.invocation.AbstractMethodInvocation;
import com.burukeyou.retry.spring.core.invocation.FastRetryInvocation;
import com.burukeyou.retry.spring.core.retrytask.RetryCounter;
import org.aopalliance.intercept.MethodInvocation;

import java.util.HashMap;
import java.util.Map;

public class FastRetryInvocationImpl extends AbstractMethodInvocation implements FastRetryInvocation {

    private FastRetry fastRetry;
    private Map<String, Object> attachments;
    private RetryCounter retryCounter;

    public FastRetryInvocationImpl(MethodInvocation methodInvocation,
                                   FastRetry fastRetry,
                                   RetryCounter retryCounter) {
        super(methodInvocation);
        this.fastRetry = fastRetry;
        this.attachments = new HashMap<>();
        this.retryCounter = retryCounter;
    }


    @Override
    public FastRetry getFastRetryAnnotation() {
        return fastRetry;
    }

    @Override
    public long getCurExecuteCount() {
        return retryCounter.getCurExecuteCount();
    }

    @Override
    public Map<String, Object> attachmentMap() {
        return attachments;
    }


}
