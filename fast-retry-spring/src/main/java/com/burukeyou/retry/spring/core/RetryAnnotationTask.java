package com.burukeyou.retry.spring.core;


import com.burukeyou.retry.core.exceptions.RetryPolicyCastException;
import com.burukeyou.retry.core.support.FutureCallable;
import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.annotations.RetryWait;
import com.burukeyou.retry.spring.interceptor.FastRetryMethodInterceptor;
import com.burukeyou.retry.spring.support.FastRetryMethodInvocationImpl;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

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

    private FastRetryMethodInterceptor retryMethodInterceptor;

    private MethodInvocation methodInvocation;

    public RetryAnnotationTask(Callable<Object> runnable,
                               FastRetry retry,
                               BeanFactory beanFactory, MethodInvocation methodInvocation) {
        this.runnable = new FutureCallable<>(runnable);
        this.retry = retry;
        this.retryWait = retry.retryWait();
        this.beanFactory = beanFactory;
        this.methodInvocation = methodInvocation;
        this.resultRetryPredicate = getPredicateStrategy(retry);
        this.retryMethodInterceptor = getFastRetryMethodInterceptor(retry);
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
    public boolean retry(long curExecuteCount) throws Exception {
        FastRetryMethodInvocationImpl retryMethodInvocation = null;
        boolean stopFlag = false;
        if (retryMethodInterceptor != null) {
            retryMethodInvocation = new FastRetryMethodInvocationImpl(curExecuteCount, methodInvocation);
            stopFlag = retryMethodInterceptor.beforeExecute(retryMethodInvocation);
            if (!stopFlag) {
                return false;
            }
        }

        Exception exception = null;
        try {
            methodResult = runnable.call();
        } catch (Exception e) {
            exception = e;
        }

        if (retryMethodInterceptor != null) {
            stopFlag = retryMethodInterceptor.afterExecute(exception, methodResult, retryMethodInvocation);
            if (!stopFlag) {
                return false;
            }
        } else if (exception != null) {
            throw exception;
        }

        if (resultRetryPredicate != null) {
            try {
                return resultRetryPredicate.test(methodResult);
            } catch (ClassCastException e) {
                Class<?> resultClass = methodResult == null ? null : methodResult.getClass();
                throw new RetryPolicyCastException("自定结果重试策略和方法结果类型不一致 实际结果类型:" + resultClass, e);
            }
        }

        // 没有抛出异常，也没走结果重试判断， 则停止重试
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
    public List<Class<? extends Exception>> include() {
        return Arrays.asList(retry.include());
    }

    @Override
    public List<Class<? extends Exception>> exclude() {
        return Arrays.asList(retry.exclude());
    }

    @Override
    public boolean exceptionRecover() {
        return retry.exceptionRecover();
    }

    @Override
    public boolean printExceptionLog() {
        return retry.printExceptionLog();
    }

    protected Predicate<Object> getPredicateStrategy(FastRetry retryAnnotation) {
        Predicate<Object> predicate = null;
        if (retryAnnotation.retryStrategy().length > 0) {
            Class<? extends Predicate<?>> retryStrategy = retryAnnotation.retryStrategy()[0];
            if (beanFactory != null) {
                try {
                    predicate = (Predicate<Object>) beanFactory.getBean(retryStrategy);
                } catch (Exception e) {
                    predicate = null;
                }
            }

            if (predicate == null) {
                try {
                    predicate = (Predicate<Object>) retryStrategy.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return predicate;
    }

    private FastRetryMethodInterceptor getFastRetryMethodInterceptor(FastRetry fastRetry) {
        if (fastRetry.interceptor() == null || fastRetry.interceptor().length == 0) {
            return null;
        }
        Class<? extends FastRetryMethodInterceptor> beanClass = fastRetry.interceptor()[0];
        FastRetryMethodInterceptor bean = getBeanSafe(beanClass);
        if (bean != null) {
            return bean;
        }
        try {
            return beanClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T getBeanSafe(Class<T> beanClass) {
        try {
            return beanFactory.getBean(beanClass);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }
}
