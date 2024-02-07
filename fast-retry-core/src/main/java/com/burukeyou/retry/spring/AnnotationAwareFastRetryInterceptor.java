package com.burukeyou.retry.spring;

import com.burukeyou.retry.core.FastRetryQueue;
import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.core.annotations.FastRetry;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationAwareFastRetryInterceptor implements IntroductionInterceptor, BeanFactoryAware {

    private static RetryQueue defaultRetryQueue;
    private BeanFactory beanFactory;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        MethodInterceptor delegate = getDelegate(invocation.getThis(), invocation.getMethod());
        return delegate != null ? delegate.invoke(invocation) : invocation.proceed();
    }

    private MethodInterceptor getDelegate(Object target, Method method) {
        FastRetry retryable = AnnotatedElementUtils.findMergedAnnotation(method, FastRetry.class);
        if (retryable == null) {
            retryable = findAnnotationOnTarget(target, method, FastRetry.class);
        }
        if (retryable == null){
            return null;
        }

        FastRetryOperationsInterceptor interceptor = new FastRetryOperationsInterceptor();
        interceptor.setRetryQueue(getRetryQueue(retryable));
        interceptor.setBeanFactory(beanFactory);
        return interceptor;
    }

    @Override
    public boolean implementsInterface(Class<?> intf) {
        return FastRetryable.class.isAssignableFrom(intf);
    }

    private <A extends Annotation> A findAnnotationOnTarget(Object target, Method method, Class<A> annotation) {

        try {
            Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            A retryable = AnnotatedElementUtils.findMergedAnnotation(targetMethod, annotation);
            return retryable;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private RetryQueue getRetryQueue(FastRetry retry) {
        RetryQueue bean = null;
        String queueName = retry.queueName();
        if (StringUtils.isNotBlank(queueName)){
            bean =  beanFactory.getBean(queueName, RetryQueue.class);
            return bean;
        }

        if (retry.queueClass() != null && retry.queueClass().length != 0){
            bean = beanFactory.getBean(retry.queueClass()[0]);
        }else {
            bean = getDefaultRetryQueue();
        }
        return bean;
    }

    private RetryQueue getDefaultRetryQueue() {
        if (defaultRetryQueue == null){
            synchronized (AnnotationAwareFastRetryInterceptor.class){
                if (defaultRetryQueue == null){
                    defaultRetryQueue = new FastRetryQueue(8);
                }
            }
        }
        return defaultRetryQueue;
    }
}
