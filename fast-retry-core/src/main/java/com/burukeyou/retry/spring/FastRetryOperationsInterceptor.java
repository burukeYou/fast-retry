package com.burukeyou.retry.spring;

import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.core.annotations.FastRetry;
import com.burukeyou.retry.core.annotations.RetryWait;
import com.burukeyou.retry.core.task.RetryAnnotationTask;
import com.burukeyou.retry.core.task.RetryTask;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Setter
public class FastRetryOperationsInterceptor  implements MethodInterceptor {

    private RetryQueue retryQueue;
    private BeanFactory beanFactory;


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> returnType = method.getReturnType();
        FastRetry retry = AnnotatedElementUtils.getMergedAnnotation(method, FastRetry.class);
        RetryWait retryWait = retry.retryWait();
        RetryTask<Object> retryTask = getRetryTask(invocation, retry, retryWait);
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

    private RetryTask<Object> getRetryTask(MethodInvocation invocation,
                                           FastRetry retry,
                                           RetryWait retryWait)  {
        Supplier<Object> supplier = () -> {
            try {
                return invocation.getMethod().invoke(invocation.getThis(), invocation.getArguments());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException){
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            }

        };
        return new RetryAnnotationTask(supplier,retry,retryWait,beanFactory);
    }

}
