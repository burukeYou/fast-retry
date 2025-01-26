package com.burukeyou.retry.spring.core.invocation.impl;


import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.core.invocation.AbstractMethodInvocation;
import com.burukeyou.retry.spring.core.invocation.FastRetryInvocation;
import org.aopalliance.intercept.MethodInvocation;

import java.util.HashMap;
import java.util.Map;

public class FastRetryInvocationImpl extends AbstractMethodInvocation implements FastRetryInvocation {

    private FastRetry fastRetry;
    private long actualExecuteCount;
    private Map<String,Object> attachments;

    public FastRetryInvocationImpl(MethodInvocation methodInvocation, FastRetry fastRetry) {
        super(methodInvocation);
        this.fastRetry = fastRetry;
        this.attachments = new HashMap<>();
    }

    @Override
    public FastRetry getFastRetryAnnotation() {
        return fastRetry;
    }

    @Override
    public long getCurExecuteCount() {
        return actualExecuteCount;
    }

    @Override
    public Map<String, Object> attachmentMap() {
        return attachments;
    }

    public void incrementActualExecuteCount(){
        this.actualExecuteCount = this.actualExecuteCount + 1;
    }

}
