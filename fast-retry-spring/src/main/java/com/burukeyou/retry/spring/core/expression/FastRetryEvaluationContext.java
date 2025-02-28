package com.burukeyou.retry.spring.core.expression;

import java.lang.reflect.Method;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * @author caizhihao
 */
public class FastRetryEvaluationContext extends MethodBasedEvaluationContext {

    public FastRetryEvaluationContext(Object rootObject, Method method, Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {
        super(rootObject, method, arguments, parameterNameDiscoverer);
    }


}
