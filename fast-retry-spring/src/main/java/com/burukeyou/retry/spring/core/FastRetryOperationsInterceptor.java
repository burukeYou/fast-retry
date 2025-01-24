package com.burukeyou.retry.spring.core;

import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.utils.BizUtil;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ListableBeanFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author  caizhihao
 */
@Setter
public class FastRetryOperationsInterceptor  implements MethodInterceptor {

    private RetryQueue retryQueue;
    private RetryAnnotationMeta retryAnnotation;
    private ListableBeanFactory beanFactory;

    private AnnotationRetryTaskFactory<Annotation> annotationRetryTaskFactory;

    private static final Map<Class<?>,Boolean> annotationRetryTaskFactoryMap = new ConcurrentHashMap<>();

    public FastRetryOperationsInterceptor(ListableBeanFactory beanFactory, RetryAnnotationMeta retryAnnotation) {
        this.beanFactory = beanFactory;
        this.retryAnnotation = retryAnnotation;
        annotationRetryTaskFactory = getRetryTaskFactory(retryAnnotation);

        Class<?> retryTaskFactoryClass = annotationRetryTaskFactory.getClass();
        if (!annotationRetryTaskFactoryMap.containsKey(retryTaskFactoryClass)){
            Class<?> superClassParamFirstClass = BizUtil.getSuperClassParamFirstClass(retryTaskFactoryClass, AnnotationRetryTaskFactory.class);
            annotationRetryTaskFactoryMap.put(retryTaskFactoryClass, FastRetry.class  == superClassParamFirstClass);
        }
    }

    private AnnotationRetryTaskFactory<Annotation> getRetryTaskFactory(RetryAnnotationMeta retryAnnotation) {
        return beanFactory.getBean(retryAnnotation.getFastRetry().factory());
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> returnType = method.getReturnType();

        Boolean isFastRetryAnno = annotationRetryTaskFactoryMap.get(annotationRetryTaskFactory.getClass());
        RetryTask<Object> retryTask = annotationRetryTaskFactory.getRetryTask(invocation, isFastRetryAnno ? retryAnnotation.getFastRetry() : retryAnnotation.getSubAnnotation());
        if (retryTask == null){
            return invocation.proceed();
        }

        CompletableFuture<Object> future = retryQueue.submit(retryTask);
        if (Future.class.isAssignableFrom(returnType)){
            return future;
        }else {
            try {
                return future.get();
            } catch (InterruptedException e) {
                throw e;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException){
                    throw  e.getCause();
                }else {
                    throw e;
                }
            }
        }
    }
}
