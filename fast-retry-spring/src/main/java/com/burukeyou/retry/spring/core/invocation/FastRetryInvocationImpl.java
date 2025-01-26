package com.burukeyou.retry.spring.core.invocation;

import java.util.Map;

public class FastRetryInvocationImpl implements FastRetryInvocation {



    @Override
    public long getMaxAttempts() {
        return 0;
    }

    @Override
    public long getActualExecuteCount() {
        return 0;
    }

    @Override
    public Map<String, Object> attachmentMap() {
        return null;
    }

    @Override
    public String getMethodAbsoluteName() {
        return null;
    }
}
