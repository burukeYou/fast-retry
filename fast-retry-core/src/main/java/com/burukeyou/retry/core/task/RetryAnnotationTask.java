package com.burukeyou.retry.core.task;

import com.burukeyou.retry.core.annotations.FastRetry;
import com.burukeyou.retry.core.annotations.RetryWait;
import com.burukeyou.retry.core.exceptions.RetryPolicyCastException;
import com.burukeyou.retry.core.support.FutureCallable;
import org.springframework.beans.factory.BeanFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


public class RetryAnnotationTask implements RetryTask<Object> {

    private final Callable<Object> runnable;
    private final FastRetry retry;
    private final RetryWait retryWait;

    private Object methodResult;
    private final Predicate<Object> resultRetryPredicate;

    private final BeanFactory beanFactory;

    public RetryAnnotationTask(Callable<Object> runnable,
                               FastRetry retry,
                               BeanFactory beanFactory) {
        this.runnable = new FutureCallable<>(runnable);
        this.retry = retry;
        this.retryWait = retry.retryWait();
        this.beanFactory = beanFactory;
        this.resultRetryPredicate = getPredicateStrategy(retry);
    }

    @Override
    public int attemptMaxTimes() {
        return retry.maxAttempts();
    }

    @Override
    public long waitRetryTime() {
        long delay = retryWait.delay();
        TimeUnit timeUnit = retryWait.timeUnit();
        return timeUnit.toMillis(delay);
    }


    @Override
    public boolean retry()  throws Exception {
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
        return retry.retryIfException();
    }

    @Override
    public List<Class<? extends Exception>> retryIfExceptionByType() {
        return Arrays.asList(retry.retryIfExceptionOfType());
    }

    @Override
    public boolean exceptionRecover() {
        return retry.exceptionRecover();
    }

    protected Predicate<Object> getPredicateStrategy(FastRetry retryAnnotation) {
        Predicate<Object> predicate = null;
        if (retryAnnotation.retryStrategy().length > 0){
            Class<? extends Predicate<?>> retryStrategy = retryAnnotation.retryStrategy()[0];
            if (beanFactory != null){
                try {
                    predicate = (Predicate<Object>) beanFactory.getBean(retryStrategy);
                } catch (Exception e) {
                    predicate = null;
                }
            }

            if (predicate == null){
                try {
                    predicate = (Predicate<Object>)retryStrategy.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return predicate;
    }
}
