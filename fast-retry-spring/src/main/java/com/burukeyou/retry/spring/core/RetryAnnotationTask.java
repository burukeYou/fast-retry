package com.burukeyou.retry.spring.core;


import com.burukeyou.retry.core.exceptions.RetryPolicyCastException;
import com.burukeyou.retry.core.policy.RetryPolicy;
import com.burukeyou.retry.core.policy.RetryResultPolicy;
import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.annotations.RetryWait;
import com.burukeyou.retry.spring.core.interceptor.FastRetryInterceptor;
import com.burukeyou.retry.spring.core.invocation.FastRetryInvocation;
import com.burukeyou.retry.spring.core.policy.LogEnum;
import com.burukeyou.retry.spring.core.policy.RetryInterceptorPolicy;
import com.burukeyou.retry.spring.support.FastFutureCallable;
import com.burukeyou.retry.spring.support.FastRetryFuture;
import com.burukeyou.retry.spring.support.FastRetryMethodInvocationImpl;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressWarnings("ALL")
public class RetryAnnotationTask implements RetryTask<Object> {

    private final FastFutureCallable<Object> runnable;
    private final FastRetry retry;

    private Object methodResult;
    private final RetryResultPolicy<Object> resultRetryPredicate;

    private final BeanFactory beanFactory;

    private RetryInterceptorPolicy<Object> retryMethodInterceptor;

    private MethodInvocation methodInvocation;

    private FastRetryInterceptor fastRetryInterceptor;
    private FastRetryInvocation retryInvocation;

    private static final Map<Method,RetryPolicy> methodToRetryPolicyCache = new ConcurrentHashMap<>();

    public RetryAnnotationTask(Callable<Object> runnable,
                               FastRetry retry,
                               BeanFactory beanFactory, MethodInvocation methodInvocation,
                               FastRetryInterceptor fastRetryInterceptor) {
        this.runnable = new FastFutureCallable<>(runnable);
        this.retry = retry;
        this.fastRetryInterceptor = fastRetryInterceptor;
        this.retryInvocation = getFastRetryInvocation();
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
        if (retry.retryWait().length > 0){
            RetryWait retryWait = retry.retryWait()[0];
            long delay = retryWait.delay();
            TimeUnit timeUnit = retryWait.timeUnit();
            return timeUnit.toMillis(delay);
        }
        return retry.delay();
    }

    private FastRetryInvocation getFastRetryInvocation() {
        // todo
        return null;
    }

    @Override
    public void retryBefore() {
        if (fastRetryInterceptor != null){
            fastRetryInterceptor.methodInvokeBefore(retryInvocation);
        }
    }

    @Override
    public void retryAfter(Exception exception) {
        if (fastRetryInterceptor != null){
            methodResult = fastRetryInterceptor.methodInvokeAfter(methodResult,exception,retryInvocation);
        }
    }

    @Override
    public boolean retry(long curExecuteCount) throws Exception {
        long start = 0;
        try {
            if(!LogEnum.NOT.equals(retry.errLog())){
                start = System.currentTimeMillis();
            }
            return doRetry(curExecuteCount);
        } catch (Exception e) {
            printLogInfo(curExecuteCount,start,retry.errLog(),e);
            throw e;
        }
    }

    private void printLogInfo(long curExecuteCount,long start,LogEnum logEnum,Exception exception) {
        if (logEnum.equals(LogEnum.NOT)) {
            return;
        }
        long costTime = System.currentTimeMillis() - start;
        if (logEnum.equals(LogEnum.EVERY)) {
            printLog(curExecuteCount, costTime,exception);
            return;
        }

        if (logEnum.equals(LogEnum.DEFAULT)) {
            int maxAttempts = retry.maxAttempts();
            if(maxAttempts == 0){
                // 重试次数为0，不打印
                return;
            }
            boolean isLogFlag = false;
            if(maxAttempts == 1){
                // 重试次数为1，打印本次
                isLogFlag = true;
            }else if(maxAttempts == 2){
                // 重试次数为2, 每次都打
                isLogFlag = true;
            }else {
                // 重试次数大于2，打印前2次，最后1次
                isLogFlag = isPrintLogFlag(curExecuteCount,LogEnum.PRE_2_LAST_1);
            }
            if (isLogFlag) {
                printLog(curExecuteCount, costTime,exception);
            }
            return;
        }

        boolean isLogFlag = isPrintLogFlag(curExecuteCount, logEnum);
        if (isLogFlag) {
            printLog(curExecuteCount, costTime,exception);
        }
    }

    private boolean isPrintLogFlag(long curExecuteCount, LogEnum logEnum) {
        boolean isLogFlag = false;
        Integer preN = logEnum.getPreN();
        Integer lastN = logEnum.getLastN();
        if (preN == null && lastN == null) {
            return false;
        }

        int executeCount =  retry.maxAttempts() + 1;
        if (preN == null) {
            preN = 0;
        }
        if (lastN == null){
            lastN = 0;
        }
        if (preN + lastN >= executeCount) {
            isLogFlag = true;
        }else {
            // [start, pre, last, end] not print [pre,last]
            isLogFlag = !(curExecuteCount > preN && curExecuteCount <= (executeCount- lastN));
        }
        return isLogFlag;
    }

    private void printLog(long curExecuteCount, long costTime,Exception info) {
        if (!retry.briefErrorLog() || info.getStackTrace().length <= 3){
            log.info("[fast-retry-spring] 重试任务执行发生异常 执行方法:{}, 当前执行次数:{} 耗时:{}",getMethodAbsoluteName(), curExecuteCount, costTime,info);
            return;
        }

        String errInfo = info.getClass().getName() + ":" + info.getMessage();
        log.info("[fast-retry-spring] 重试任务执行发生异常 执行方法:{}, 当前执行次数:{} 耗时:{} \n{}",getMethodAbsoluteName(), curExecuteCount, costTime,info,errInfo);

    }

    public String getMethodAbsoluteName() {
        Method method = methodInvocation.getMethod();
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    private boolean doRetry(long curExecuteCount) throws Exception {
        RetryPolicy methodRetryPolicy = null;
        if (!methodToRetryPolicyCache.containsKey(methodInvocation.getMethod())){
            // 1、find from method param
            methodRetryPolicy = findMethodParamRetryPolicy();

            // 2、find from annotation config
            Class<? extends RetryPolicy> retryPolicyClass = getRetryPolicyClass();
            if (methodRetryPolicy == null && retryPolicyClass != null) {
                methodRetryPolicy = RetryResultPolicy.class.isAssignableFrom(retryPolicyClass) ? resultRetryPredicate : retryMethodInterceptor;
            }

            // 3、find from method return value
            if (methodRetryPolicy == null && FastRetryFuture.class.isAssignableFrom(methodInvocation.getMethod().getReturnType())) {
                if (runnable.isCallFlag()){
                    methodRetryPolicy  = runnable.getReturnValueFastRetryFuture().getRetryResultPolicy();
                }else {
                    doInvokeMethod();
                    RetryResultPolicy<Object> policy  = runnable.getReturnValueFastRetryFuture().getRetryResultPolicy();
                    return policy != null && invokerRetryResultPolicy(policy);
                }
            }
            methodToRetryPolicyCache.put(methodInvocation.getMethod(),methodRetryPolicy);
        }else {
            methodRetryPolicy = methodToRetryPolicyCache.get(methodInvocation.getMethod());
        }

        if (methodRetryPolicy == null){
            return retryDoForNotRetryPolicy();
        }

        if (RetryResultPolicy.class.isAssignableFrom(methodRetryPolicy.getClass())) {
            return retryDoForResultRetryPolicy((RetryResultPolicy<Object>)methodRetryPolicy);
        }

        if (RetryInterceptorPolicy.class.isAssignableFrom(methodRetryPolicy.getClass())) {
            return retryDoForInterceptorPolicy(curExecuteCount,(RetryInterceptorPolicy<Object>)methodRetryPolicy);
        }

        throw new IllegalArgumentException("Unsupported RetryPolicy type for " + methodRetryPolicy.getClass());
    }


    private RetryPolicy findMethodParamRetryPolicy() {
        RetryPolicy methodRetryPolicy = null;
        Object[] arguments = methodInvocation.getArguments();
        for (Object arg : arguments) {
            if (arg != null && RetryPolicy.class.isAssignableFrom(arg.getClass())){
                methodRetryPolicy = (RetryPolicy)arg;
                break;
            }
        }
        return methodRetryPolicy;
    }

    private boolean retryDoForInterceptorPolicy(long curExecuteCount,RetryInterceptorPolicy<Object> policy) throws Exception {
        FastRetryMethodInvocationImpl retryMethodInvocation = new FastRetryMethodInvocationImpl(curExecuteCount,retry, methodInvocation);
        if (!policy.beforeExecute(retryMethodInvocation)){
            return false;
        }

        Exception exception = null;
        try {
            doInvokeMethod();
        } catch (Exception e) {
            exception = e;
        }
        if (exception == null) {
            return policy.afterExecuteSuccess(methodResult, retryMethodInvocation);
        }else {
            return policy.afterExecuteFail(exception, retryMethodInvocation);
        }
    }

    private Object doInvokeMethod() throws Exception {
        methodResult =  runnable.call();
        return methodResult;
    }

    private boolean retryDoForResultRetryPolicy(RetryResultPolicy<Object> policy) throws Exception {
        doInvokeMethod();
        return policy != null && invokerRetryResultPolicy(policy);
    }

    private boolean invokerRetryResultPolicy(RetryResultPolicy<Object> policy) {
        try {
            return policy.test(methodResult);
        } catch (ClassCastException e) {
            Class<?> resultClass = methodResult == null ? null : methodResult.getClass();
            throw new RetryPolicyCastException("自定结果重试策略和方法结果类型不一致 实际结果类型:" + resultClass, e);
        }
    }

    private boolean retryDoForNotRetryPolicy() throws Exception {
        doInvokeMethod();
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
        return false;
    }

    protected RetryResultPolicy<Object> getPredicateStrategy(FastRetry retryAnnotation) {
        RetryResultPolicy<Object> predicate = null;
        if (retryAnnotation.policy().length == 0) {
            return null;
        }
        Class<? extends RetryPolicy> policyClass = retryAnnotation.policy()[0];
        if (!RetryResultPolicy.class.isAssignableFrom(policyClass)) {
            return null;
        }
        Class<? extends RetryResultPolicy<Object>> retryStrategy = (Class<? extends RetryResultPolicy<Object>>) policyClass;
        return getBeanOrNew(retryStrategy);
    }

    private RetryInterceptorPolicy<Object> getFastRetryMethodInterceptor(FastRetry fastRetry) {
        if (fastRetry.policy().length == 0) {
            return null;
        }
        Class<? extends RetryPolicy> policyClass = fastRetry.policy()[0];
        if (!RetryInterceptorPolicy.class.isAssignableFrom(policyClass)) {
            return null;
        }
        Class<RetryInterceptorPolicy<Object>> beanClass = (Class<RetryInterceptorPolicy<Object>>) policyClass;
        return getBeanOrNew(beanClass);
    }

    public <T> T getBeanSafe(Class<T> beanClass) {
        try {
            return beanFactory.getBean(beanClass);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    public <T> T getBeanOrNew(Class<T> beanClass) {
        T beanSafe = getBeanSafe(beanClass);
        if (beanSafe == null) {
            try {
                return beanClass.newInstance();
            } catch (InstantiationException   | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return beanSafe;
    }

    private Class<? extends RetryPolicy> getRetryPolicyClass() {
        return retry.policy().length > 0 ?  retry.policy()[0] : null;
    }
}



