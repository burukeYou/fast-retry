package com.burukeyou.retry.core.task;

import com.burukeyou.retry.core.exceptions.RetryPolicyCastException;
import com.burukeyou.retry.core.support.FutureCallable;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;


public class RetryBuilderRetryTask implements RetryTask<Object> {

    private final Callable<Object> runnable;
    private final RetryTaskContext retryTaskContext;
    private Object methodResult;
    private final Predicate<Object> resultRetryPredicate;


    public RetryBuilderRetryTask(Callable<Object> runnable,RetryTaskContext retryTaskContext) {
        this.runnable = new FutureCallable<>(runnable);
        this.retryTaskContext = retryTaskContext;
        this.resultRetryPredicate = getPredicateStrategy(retryTaskContext);
    }

    @Override
    public int attemptMaxTimes() {
        return retryTaskContext.getAttemptMaxTimes();
    }

    @Override
    public long waitRetryTime() {
        return retryTaskContext.getWaitRetryTime();
    }


    @Override
    public boolean retry() throws Exception {
        methodResult = runnable.call();
        if (resultRetryPredicate != null){
            try {
                return resultRetryPredicate.test(methodResult);
            } catch (ClassCastException e) {
                Class<?> resultClass = methodResult == null ? null : methodResult.getClass();
                throw new RetryPolicyCastException("自定结果重试策略和方法结果类型不一致 实际结果类型:" + resultClass,e);
            }
        }

        return false;
    }

    @Override
    public Object getResult() {
        return methodResult;
    }

    @Override
    public boolean retryIfException() {
        return retryTaskContext.getRetryIfException();
    }

    @Override
    public List<Class<? extends Exception>> include() {
        return retryTaskContext.getExceptionsType();
    }

    @Override
    public List<Class<? extends Exception>> exclude() {
        return retryTaskContext.getExcludeExceptionsType();
    }

    @Override
    public boolean exceptionRecover() {
        return retryTaskContext.getExceptionRecover();
    }

    protected Predicate<Object> getPredicateStrategy(RetryTaskContext retryTaskContext) {
        return retryTaskContext.getResultPolicy();
    }
}
