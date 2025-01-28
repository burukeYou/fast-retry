package com.burukeyou.retry.spring.core.retrytask;


import com.burukeyou.retry.core.exceptions.RetryPolicyCastException;
import com.burukeyou.retry.core.policy.MethodResultPolicy;
import com.burukeyou.retry.core.policy.RetryPolicy;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.core.interceptor.FastRetryInterceptor;
import com.burukeyou.retry.spring.core.invocation.FastRetryInvocation;
import com.burukeyou.retry.spring.core.invocation.impl.FastRetryInvocationImpl;
import com.burukeyou.retry.spring.core.policy.FastRetryInterceptorPolicy;
import com.burukeyou.retry.spring.core.policy.FastRetryFuture;
import com.burukeyou.retry.spring.support.FastFutureCallable;
import com.burukeyou.retry.spring.support.Tuple2;
import com.burukeyou.retry.spring.utils.BizUtil;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;

import java.util.concurrent.Callable;

@Slf4j
public class RetryAnnotationTask extends AbstractRetryAnnotationTask<Object> {

    protected final FastFutureCallable<Object> runnable;
    protected Object methodResult;

    protected FastRetryInterceptor fastRetryInterceptor;
    protected FastRetryInvocation retryInvocation;
    protected FastRetry fastRetry;

    public RetryAnnotationTask(Callable<Object> runnable,
                               FastRetry fastRetry,
                               BeanFactory beanFactory,
                               MethodInvocation methodInvocation) {
        super(new FastRetryAnnotationAdapter(fastRetry),methodInvocation,beanFactory);
        this.fastRetry = fastRetry;
        this.runnable = new FastFutureCallable<>(runnable);
        fastRetryInterceptor = BizUtil.getBeanOrNew(retryConfig.interceptor(), beanFactory);
        this.retryInvocation = new FastRetryInvocationImpl(methodInvocation, fastRetry,retryCounter);
    }


    @Override
    public void retryBefore() {
        if (fastRetryInterceptor != null) {
            fastRetryInterceptor.methodInvokeBefore(retryInvocation);
        }
    }

    @Override
    public void retryAfter(Exception exception) {
        if (fastRetryInterceptor != null) {
            methodResult = fastRetryInterceptor.methodInvokeAfter(methodResult, exception, retryInvocation);
        }
    }

    @Override
    protected void findMethodReturnRetryPolicy(Tuple2<Boolean, RetryPolicy> tuple2) throws Exception {
       if (FastRetryFuture.class.isAssignableFrom(methodInvocation.getMethod().getReturnType())) {
            doInvokeMethod();
            FastRetryFuture<Object> futurePolicy = runnable.getReturnValueFuturePolicy();
            MethodResultPolicy<Object> methodResultPolicy = futurePolicy.getRetryWhen();
            if (methodResultPolicy != null){
                tuple2.setC2(methodResultPolicy);
                tuple2.setC1(methodResultPolicy.canRetry(methodResult));
            }
       }
    }

    @Override
    protected boolean doRetry(RetryPolicy retryPolicy) throws Exception {
        if (retryPolicy == null) {
            return retryDoForNotRetryPolicy();
        }
        if (MethodResultPolicy.class.isAssignableFrom(retryPolicy.getClass())) {
            return retryDoForResultRetryPolicy((MethodResultPolicy<Object>) retryPolicy);
        }
        if (FastRetryInterceptorPolicy.class.isAssignableFrom(retryPolicy.getClass())) {
            return retryDoForInterceptorPolicy((FastRetryInterceptorPolicy<Object>) retryPolicy);
        }
        throw new IllegalArgumentException("FastRetry not support other RetryPolicy for " + retryPolicy.getClass().getName());
    }


    private boolean retryDoForInterceptorPolicy(FastRetryInterceptorPolicy<Object> policy) throws Exception {
        if (!policy.beforeExecute(retryInvocation)) {
            return false;
        }
        Exception exception = null;
        try {
            doInvokeMethod();
        } catch (Exception e) {
            exception = e;
        }
        if (exception == null) {
            return policy.afterExecuteSuccess(methodResult, retryInvocation);
        } else {
            return policy.afterExecuteFail(exception, retryInvocation);
        }
    }

    protected Object doInvokeMethod() throws Exception {
        methodResult = runnable.call();
        return methodResult;
    }

    private boolean retryDoForResultRetryPolicy(MethodResultPolicy<Object> policy) throws Exception {
        doInvokeMethod();
        return policy != null && invokerRetryResultPolicy(policy);
    }

    protected boolean invokerRetryResultPolicy(MethodResultPolicy<Object> policy) {
        try {
            return policy.test(methodResult);
        } catch (ClassCastException e) {
            Class<?> resultClass = methodResult == null ? null : methodResult.getClass();
            throw new RetryPolicyCastException("自定结果重试策略和方法结果类型不一致 实际结果类型:" + resultClass, e);
        }
    }

    protected boolean retryDoForNotRetryPolicy() throws Exception {
        doInvokeMethod();
        return false;
    }

    @Override
    public Object getResult() {
        return methodResult;
    }

}



