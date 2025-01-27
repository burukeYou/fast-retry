package com.burukeyou.retry.spring.core.retrytask;

import com.burukeyou.retry.core.policy.FastRetryPolicy;
import com.burukeyou.retry.core.policy.RetryPolicy;
import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.spring.annotations.RetryWait;
import com.burukeyou.retry.spring.core.policy.LogEnum;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractRetryAnnotationTask<R> implements RetryTask<R> {

    protected FastRetryAdapter retryConfig;
    protected MethodInvocation methodInvocation;
    protected BeanFactory beanFactory;

    protected RetryCounter retryCounter = new RetryCounter();

    protected static final Map<Method, RetryPolicy> methodToRetryPolicyCache = new ConcurrentHashMap<>();

    private static final FastRetryPolicy NULL_POLICY = new FastRetryPolicy(){};

    protected AbstractRetryAnnotationTask(FastRetryAdapter retryConfig,
                                          MethodInvocation methodInvocation,
                                          BeanFactory beanFactory) {
        this.retryConfig = retryConfig;
        this.methodInvocation = methodInvocation;
        this.beanFactory = beanFactory;
    }

    @Override
    public int attemptMaxTimes() {
        return retryConfig.maxAttempts();
    }

    @Override
    public long waitRetryTime() {
        if (retryConfig.retryWait() != null) {
            RetryWait retryWait = retryConfig.retryWait();
            long delay = retryWait.delay();
            TimeUnit timeUnit = retryWait.timeUnit();
            return timeUnit.toMillis(delay);
        }
        return retryConfig.delay();
    }

    @Override
    public boolean retryIfException() {
        return retryConfig.retryIfException();
    }

    @Override
    public List<Class<? extends Exception>> include() {
        return Arrays.asList(retryConfig.include());
    }

    @Override
    public List<Class<? extends Exception>> exclude() {
        return Arrays.asList(retryConfig.exclude());
    }

    @Override
    public boolean exceptionRecover() {
        return retryConfig.exceptionRecover();
    }

    @Override
    public boolean printExceptionLog() {
        return false;
    }

    @Override
    public boolean retry() throws Exception {
        retryCounter.incrementActualExecuteCount();
        long start = 0;
        try {
            if (!LogEnum.NOT.equals(retryConfig.errLog())) {
                start = System.currentTimeMillis();
            }
            RetryPolicy retryPolicy = findRetryPolicy();
            return doRetry(retryPolicy);
        } catch (Exception e) {
            printLogInfo(start, retryConfig.errLog(), e);
            throw e;
        }
    }

    protected RetryPolicy findRetryPolicy() {
        Method method = methodInvocation.getMethod();
        RetryPolicy methodRetryPolicy = null;
        if (!methodToRetryPolicyCache.containsKey(method)) {
            // 1、find from method param
            methodRetryPolicy = findMethodParamRetryPolicy();

            // 2、find from annotation config
            Class<? extends RetryPolicy> policyClass = retryConfig.policy();
            if (methodRetryPolicy == null && policyClass != null) {
                methodRetryPolicy = getBeanOrNew(policyClass);
            }

            // 3、find from method return value
//            if (methodRetryPolicy == null && FastRetryFuture.class.isAssignableFrom(methodInvocation.getMethod().getReturnType())) {
//                if (runnable.isCallFlag()){
//                    methodRetryPolicy  = runnable.getReturnValueFastRetryFuture().getRetryResultPolicy();
//                }else {
//                    doInvokeMethod();
//                    RetryResultPolicy<Object> policy  = runnable.getReturnValueFastRetryFuture().getRetryResultPolicy();
//                    methodToRetryPolicyCache.put(retryInvocation.getMethod(),policy);
//                    return policy != null && invokerRetryResultPolicy(policy);
//                }
//            }
            methodToRetryPolicyCache.put(method, methodRetryPolicy == null ? NULL_POLICY : methodRetryPolicy);
        } else {
            methodRetryPolicy = methodToRetryPolicyCache.get(method);
        }
        return methodRetryPolicy == NULL_POLICY ? null : methodRetryPolicy;
    }

    protected abstract boolean doRetry(RetryPolicy retryPolicy) throws Exception;

    private  RetryPolicy findMethodParamRetryPolicy() {
        RetryPolicy methodRetryPolicy = null;
        Object[] arguments = methodInvocation.getArguments();
        for (Object arg : arguments) {
            if (arg != null && RetryPolicy.class.isAssignableFrom(arg.getClass())) {
                methodRetryPolicy = (RetryPolicy) arg;
                break;
            }
        }
        return methodRetryPolicy;
    }


    protected void printLogInfo(long start, LogEnum logEnum, Exception exception) {
        long curExecuteCount = retryCounter.getCurExecuteCount();
        if (logEnum.equals(LogEnum.NOT)) {
            return;
        }
        long costTime = System.currentTimeMillis() - start;
        if (logEnum.equals(LogEnum.EVERY)) {
            printLog(costTime, exception);
            return;
        }

        if (logEnum.equals(LogEnum.AUTO)) {
            int maxAttempts = retryConfig.maxAttempts();
            if (maxAttempts == 0) {
                // 重试次数为0，不打印
                return;
            }
            boolean isLogFlag = false;
            if (maxAttempts == 1) {
                // 重试次数为1，打印本次
                isLogFlag = true;
            } else if (maxAttempts == 2) {
                // 重试次数为2, 每次都打
                isLogFlag = true;
            } else {
                // 重试次数大于2，打印前2次，最后1次
                isLogFlag = isPrintLogFlag(curExecuteCount, LogEnum.PRE_2_LAST_1);
            }
            if (isLogFlag) {
                printLog(costTime, exception);
            }
            return;
        }

        boolean isLogFlag = isPrintLogFlag(curExecuteCount, logEnum);
        if (isLogFlag) {
            printLog(costTime, exception);
        }
    }

    protected boolean isPrintLogFlag(long curExecuteCount, LogEnum logEnum) {
        boolean isLogFlag = false;
        Integer preN = logEnum.getPreN();
        Integer lastN = logEnum.getLastN();
        if (preN == null && lastN == null) {
            return false;
        }

        int executeCount = retryConfig.maxAttempts() + 1;
        if (preN == null) {
            preN = 0;
        }
        if (lastN == null) {
            lastN = 0;
        }
        if (preN + lastN >= executeCount) {
            isLogFlag = true;
        } else {
            // [start, pre, last, end] not print [pre,last]
            isLogFlag = !(curExecuteCount > preN && curExecuteCount <= (executeCount - lastN));
        }
        return isLogFlag;
    }

    private void printLog(long costTime, Exception info) {
        long curExecuteCount = retryCounter.getCurExecuteCount();
        if (!retryConfig.briefErrorLog() || info.getStackTrace().length <= 3) {
            log.info("[fast-retry-spring] 重试任务执行发生异常 执行方法:{}, 当前执行次数:{} 耗时:{}", getMethodAbsoluteName(), curExecuteCount, costTime, info);
            return;
        }

        String errInfo = info.getClass().getName() + ":" + info.getMessage();
        log.info("[fast-retry-spring] 重试任务执行发生异常 执行方法:{}, 当前执行次数:{} 耗时:{} \n{}", getMethodAbsoluteName(), curExecuteCount, costTime, errInfo);
    }

    public String getMethodAbsoluteName() {
        Method method = methodInvocation.getMethod();
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    public <T> T getBeanOrNew(Class<T> beanClass) {
        T beanSafe = getBeanSafe(beanClass);
        if (beanSafe == null) {
            try {
                return beanClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return beanSafe;
    }

    public <T> T getBeanSafe(Class<T> beanClass) {
        try {
            return beanFactory.getBean(beanClass);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }
}
