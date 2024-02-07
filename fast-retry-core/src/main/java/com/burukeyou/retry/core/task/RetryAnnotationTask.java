package com.burukeyou.retry.core.task;

import com.burukeyou.retry.core.annotations.FastRetry;
import com.burukeyou.retry.core.annotations.RetryWait;
import com.burukeyou.retry.core.exceptions.RetryPolicyCastException;
import org.springframework.beans.factory.BeanFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class RetryAnnotationTask implements RetryTask<Object> {

    private final Supplier<Object> runnable;
    private final FastRetry retry;
    private final RetryWait retryWait;

    private Object methodResult;
    private final Predicate<Object> resultRetryPredicate;

    private final BeanFactory  applicationContext;

    public RetryAnnotationTask(Supplier<Object> runnable,
                               FastRetry retry,
                               RetryWait retryWait,
                               BeanFactory applicationContext) {
        this.runnable = runnable;
        this.retry = retry;
        this.retryWait = retryWait;
        this.applicationContext = applicationContext;
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
        methodResult = runnable.get();
        if (methodResult instanceof CompletableFuture){
            try {
                methodResult = ((CompletableFuture<?>) methodResult).get();
            } catch (InterruptedException e) {
                throw e;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception){
                    throw (Exception) cause;
                } else {
                    throw e;
                }
            }
        }

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
            if (applicationContext != null){
                try {
                    predicate = (Predicate<Object>) applicationContext.getBean(retryStrategy);
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
