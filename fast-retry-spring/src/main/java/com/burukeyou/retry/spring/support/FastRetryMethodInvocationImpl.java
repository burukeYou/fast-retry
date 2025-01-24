package com.burukeyou.retry.spring.support;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.burukeyou.retry.spring.annotations.FastRetry;
import lombok.Getter;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author  caizhihao
 */
@Getter
@Setter
public class FastRetryMethodInvocationImpl implements FastRetryMethodInvocation {

    private final long curRetryCount;
    private final MethodInvocation methodInvocation;

    private final Map<String,Object> extMap;
    private final FastRetry fastRetry;

    public FastRetryMethodInvocationImpl(long curRetryCount, FastRetry fastRetry,MethodInvocation methodInvocation) {
        this.curRetryCount = curRetryCount;
        this.fastRetry = fastRetry;
        this.methodInvocation = methodInvocation;
        this.extMap = new HashMap<>();
    }

    @Override
    public long getMaxAttempts() {
        return fastRetry.maxAttempts();
    }


    @Override
    public long getCurExecuteCount() {
        return curRetryCount;
    }

    @Override
    public Map<String, Object> attachmentMap() {
        return extMap;
    }

    @Override
    public String getMethodAbsoluteName() {
        Method method = getMethod();
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    @Override
    public Method getMethod() {
        return methodInvocation.getMethod();
    }

    @Override
    public Object[] getArguments() {
        return methodInvocation.getArguments();
    }

    @Override
    public Object proceed() throws Throwable {
        return methodInvocation.proceed();
    }

    @Override
    public Object getThis() {
        return methodInvocation.getThis();
    }

    @Override
    public AccessibleObject getStaticPart() {
        return methodInvocation.getStaticPart();
    }
}
