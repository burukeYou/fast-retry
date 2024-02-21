package com.burukeyou.retry.spring;

import com.burukeyou.retry.core.annotations.FastRetry;
import com.burukeyou.retry.core.task.RetryAnnotationTask;
import com.burukeyou.retry.core.task.RetryTask;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;

import java.util.concurrent.Callable;


public class FastRetryAnnotationRetryTaskFactory implements AnnotationRetryTaskFactory<FastRetry> {

    private final BeanFactory beanFactory;

    public FastRetryAnnotationRetryTaskFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public  RetryTask<Object> getRetryTask(MethodInvocation invocation, FastRetry retry) {
        Callable<Object> supplier = () -> {
            try {
                return invocation.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
        return new RetryAnnotationTask(supplier,retry,beanFactory);
    }
}
