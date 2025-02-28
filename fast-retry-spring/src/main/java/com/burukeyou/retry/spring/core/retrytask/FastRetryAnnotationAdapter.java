package com.burukeyou.retry.spring.core.retrytask;

import com.burukeyou.retry.core.enums.LogEnum;
import com.burukeyou.retry.core.policy.FastRetryPolicy;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.core.retrytask.base.FastRetryAdapter;

/**
 * @author  caizhihao
 */
public class FastRetryAnnotationAdapter implements FastRetryAdapter {

    private final FastRetry fastRetry;

    public FastRetryAnnotationAdapter(FastRetry fastRetry) {
        this.fastRetry = fastRetry;
    }

    @Override
    public int maxAttempts() {
        return fastRetry.maxAttempts();
    }

    @Override
    public long delay() {
        return fastRetry.delay();
    }

    @Override
    public boolean retryIfException() {
        return fastRetry.retryIfException();
    }

    @Override
    public Class<? extends Exception>[] include() {
        return fastRetry.include();
    }

    @Override
    public Class<? extends Exception>[] exclude() {
        return fastRetry.exclude();
    }

    @Override
    public boolean exceptionRecover() {
        return fastRetry.exceptionRecover();
    }

    @Override
    public LogEnum errLog() {
        return fastRetry.errLog();
    }

    @Override
    public boolean briefErrorLog() {
        return fastRetry.briefErrorLog();
    }

    @Override
    public Class<? extends FastRetryPolicy> policy() {
        return fastRetry.policy().length > 0 ? fastRetry.policy()[0] : null;
    }

    @Override
    public String retryWhenExpression() {
        return fastRetry.retryWhen();
    }
}
