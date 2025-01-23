package com.burukeyou.retry.spring.core;

import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.spring.annotations.FastRetry;
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
                if (e instanceof Exception){
                    throw (Exception) e;
                }else {
                    throw new RuntimeException(e);
                }
            }
        };
        return new RetryAnnotationTask(supplier,retry,beanFactory,invocation);
    }
}
