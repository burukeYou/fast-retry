package com.burukeyou.retry.spring.core;

import com.burukeyou.retry.core.FastRetryQueue;
import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.core.exceptions.FastRetryException;
import com.burukeyou.retry.core.support.FastRetryThreadFactory;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.utils.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AnnotationAwareFastRetryInterceptor implements IntroductionInterceptor, BeanFactoryAware {

    private static RetryQueue defaultRetryQueue;
    private ListableBeanFactory beanFactory;

    private final ConcurrentReferenceHashMap<Object, ConcurrentMap<Method, MethodInterceptor>> delegatesCache = new ConcurrentReferenceHashMap<>();


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        MethodInterceptor delegate = getDelegate(invocation.getThis(), invocation.getMethod());
        return delegate != null ? delegate.invoke(invocation) : invocation.proceed();
    }

    private MethodInterceptor getDelegate(Object target, Method method) {
        ConcurrentMap<Method, MethodInterceptor> methodInterceptorMap = delegatesCache.getOrDefault(target, new ConcurrentHashMap<>());
        MethodInterceptor methodInterceptor = methodInterceptorMap.get(method);
        if (methodInterceptor != null) {
           return methodInterceptor;
        }

        RetryAnnotationMeta retryable = getFastRetryAnnotation(target, method);
        if (retryable == null) {
            return null;
        }

        FastRetryOperationsInterceptor interceptor = getRetryInterceptor(retryable);
        methodInterceptorMap.put(method,interceptor);
        delegatesCache.putIfAbsent(target, methodInterceptorMap);
        return interceptor;
    }

    private RetryAnnotationMeta getFastRetryAnnotation(Object target, Method method) {
        FastRetry retryable = AnnotatedElementUtils.findMergedAnnotation(method, FastRetry.class);
        if (retryable == null) {
            retryable = findAnnotationOnTarget(target, method, FastRetry.class);
        }

        if (retryable != null){
            Annotation[] annotations = method.getAnnotations();
            for (Annotation a : annotations){
                Class<? extends Annotation> annotationType = a.annotationType();
                FastRetry declaredAnnotation = annotationType.getAnnotation(FastRetry.class);
                if (declaredAnnotation != null){
                    return new RetryAnnotationMeta(retryable,a);
                }
            }
            return new RetryAnnotationMeta(retryable,retryable);
        }

        return null;
    }

    private FastRetryOperationsInterceptor getRetryInterceptor(RetryAnnotationMeta retryable) {
        FastRetryOperationsInterceptor tmpInterceptor = new FastRetryOperationsInterceptor(beanFactory,retryable);
        tmpInterceptor.setRetryQueue(getRetryQueue(retryable.getFastRetry()));
        return tmpInterceptor;
    }

    @Override
    public boolean implementsInterface(Class<?> intf) {
        return FastRetryable.class.isAssignableFrom(intf);
    }

    private <A extends Annotation> A findAnnotationOnTarget(Object target, Method method, Class<A> annotation) {

        try {
            Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            A retryable = AnnotatedElementUtils.findMergedAnnotation(targetMethod, annotation);
            if (retryable == null) {
                retryable = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), annotation);
            }
            return retryable;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ListableBeanFactory)beanFactory;
    }

    private RetryQueue getRetryQueue(FastRetry retry) {
        RetryQueue bean = null;
        try {
            String queueName = retry.queueName();
            if (queueName != null && !queueName.isEmpty()){
                bean =  beanFactory.getBean(queueName, RetryQueue.class);
                return bean;
            }

            if (retry.queueClass() != null && retry.queueClass().length != 0){
                bean = beanFactory.getBean(retry.queueClass()[0]);
            }else {
                bean = getDefaultRetryQueue();
            }
        } catch (BeansException e) {
            throw new FastRetryException("can not find custom retry queue bean from spring context",e);
        }
        return bean;
    }

    private RetryQueue getDefaultRetryQueue() {
        if (defaultRetryQueue == null){
            synchronized (AnnotationAwareFastRetryInterceptor.class){
                if (defaultRetryQueue == null){
                    int cpuCount = SystemUtil.CPU_COUNT;
                    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(cpuCount*2, cpuCount*2,
                            60L, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(),
                            new FastRetryThreadFactory("spring"));
                    log.info("[fast-retry-spring] init default retry queue cpuSize:{}",cpuCount);
                    defaultRetryQueue = new FastRetryQueue(poolExecutor);
                }
            }
        }
        return defaultRetryQueue;
    }
}
