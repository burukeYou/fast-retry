package com.burukeyou.retry.spring.core;

import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.core.interceptor.FastRetryInterceptor;
import com.burukeyou.retry.spring.support.FastRetryFuture;
import com.burukeyou.retry.spring.utils.BizUtil;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ListableBeanFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author  caizhihao
 */
@Setter
public class FastRetryOperationsInterceptor  implements MethodInterceptor {

    private RetryQueue retryQueue;
    private RetryAnnotationMeta retryAnnotation;
    private ListableBeanFactory beanFactory;

    private FastRetry fastRetryAnno;
    private AnnotationRetryTaskFactory<Annotation> annotationRetryTaskFactory;
    private FastRetryInterceptor fastRetryInterceptor;

    private static final Map<Class<?>,Boolean> annotationRetryTaskFactoryMap = new ConcurrentHashMap<>();

    public FastRetryOperationsInterceptor(ListableBeanFactory beanFactory, RetryAnnotationMeta retryAnnotation) {
        this.beanFactory = beanFactory;
        this.retryAnnotation = retryAnnotation;
        this.fastRetryAnno = retryAnnotation.getFastRetry();
        this.annotationRetryTaskFactory = getRetryTaskFactory(retryAnnotation);

        //
        if (annotationRetryTaskFactory != null && !annotationRetryTaskFactoryMap.containsKey(annotationRetryTaskFactory.getClass())){
            Class<? extends AnnotationRetryTaskFactory> retryTaskFactoryClass = annotationRetryTaskFactory.getClass();
            Class<?> superClassParamFirstClass = BizUtil.getSuperClassParamFirstClass(retryTaskFactoryClass, AnnotationRetryTaskFactory.class);
            annotationRetryTaskFactoryMap.put(retryTaskFactoryClass, FastRetry.class  == superClassParamFirstClass);
        }

        //
        if (fastRetryAnno.interceptor().length > 0){
            fastRetryInterceptor = BizUtil.getBeanOrNew(fastRetryAnno.interceptor()[0], beanFactory);
        }
    }

    private AnnotationRetryTaskFactory<Annotation> getRetryTaskFactory(RetryAnnotationMeta retryAnnotation) {
        FastRetry fastRetry = retryAnnotation.getFastRetry();
        if (fastRetry.taskFactory().length > 0){
            return beanFactory.getBean(fastRetry.taskFactory()[0]);
        }
        return null;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> returnType = method.getReturnType();

        RetryTask<Object> retryTask;
        if (annotationRetryTaskFactory == null){
            retryTask = getDefaultRetryTask(invocation);
        }else {
            boolean isFastRetryAnno = annotationRetryTaskFactoryMap.get(annotationRetryTaskFactory.getClass());
            retryTask = annotationRetryTaskFactory.getRetryTask(invocation, isFastRetryAnno ? retryAnnotation.getFastRetry() : retryAnnotation.getSubAnnotation());
        }

        // retry queue future
        CompletableFuture<Object> future = retryQueue.submit(retryTask);
        if (!Future.class.isAssignableFrom(returnType)){
            return getResult(future);
        }

        if (FastRetryFuture.class.equals(returnType)){
            return new FastRetryFuture<>(future);
        }
        return future;
    }

    private static Object getResult(CompletableFuture<Object> future) throws Throwable {
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


    private RetryTask<Object> getDefaultRetryTask(MethodInvocation invocation) {
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
        return new RetryAnnotationTask(supplier,fastRetryAnno,beanFactory,invocation,fastRetryInterceptor);
    }
}
