package com.burukeyou.retry.spring.core;

import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.core.task.RetryTask;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ListableBeanFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Setter
public class FastRetryOperationsInterceptor  implements MethodInterceptor {

    private RetryQueue retryQueue;
    private RetryAnnotationMeta retryAnnotation;
    private ListableBeanFactory beanFactory;

    private AnnotationRetryTaskFactory<Annotation> annotationRetryTaskFactory;

    public FastRetryOperationsInterceptor(ListableBeanFactory beanFactory, RetryAnnotationMeta retryAnnotation) {
        this.beanFactory = beanFactory;
        this.retryAnnotation = retryAnnotation;
        annotationRetryTaskFactory = getRetryTaskFactory(retryAnnotation);
    }

    private AnnotationRetryTaskFactory<Annotation> getRetryTaskFactory(RetryAnnotationMeta retryAnnotation) {
        return beanFactory.getBean(retryAnnotation.getFastRetry().factory());
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> returnType = method.getReturnType();
        RetryTask<Object> retryTask = annotationRetryTaskFactory.getRetryTask(invocation, retryAnnotation.getSubAnnotation());
        if (retryTask == null){
            return invocation.proceed();
        }

        CompletableFuture<Object> future = retryQueue.submit(retryTask);
        if (CompletableFuture.class.isAssignableFrom(returnType)){
            return future;
        }else {
            try {
                return future.get();
            } catch (InterruptedException e) {
                throw e;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException){
                    throw  e.getCause();
                }
                throw e;
            }
        }
    }
}
