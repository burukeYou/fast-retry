package com.burukeyou.retry.spring.core.invocation;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

public abstract class AbstractMethodInvocation implements MethodInvocation {

    protected final MethodInvocation methodInvocation;

    protected AbstractMethodInvocation(MethodInvocation methodInvocation) {
        this.methodInvocation = methodInvocation;
    }


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
