package com.burukeyou.retry.spring.core.expression;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import com.burukeyou.retry.core.entity.Tuple2;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author caizhihao
 */
public class FastRetryExpressionEvaluator {

    private static final ConcurrentHashMap<Method, Expression> expressionCache = new ConcurrentHashMap<>();

    private final SpelExpressionParser parser = new SpelExpressionParser();

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private  BeanFactory beanFactory;

    public FastRetryExpressionEvaluator() {
    }

    public FastRetryExpressionEvaluator(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public <T> T parseExpression(Method method,
                                  Object[] arguments,
                                  Object methodReturnValue,
                                  String expressionString,Class<T> expressionValueClass) {
        Expression expression;
        EvaluationContext evaluationContext;
        expression = expressionCache.get(method);
        if (expression == null) {
            expression = parser.parseExpression(expressionString);
            expressionCache.put(method,expression);
        }
        evaluationContext = createEvaluationContext(method, arguments,methodReturnValue);
        return expression.getValue(evaluationContext,expressionValueClass);
    }

    public EvaluationContext createEvaluationContext(Method method, Object[] arguments,Object methodReturnValue) {
        FastRetryExpressionRootObject rootObject = new FastRetryExpressionRootObject();
        MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(rootObject, method, arguments, parameterNameDiscoverer);
        evaluationContext.setVariable("methodValue",methodReturnValue);
        if (beanFactory != null) {
            evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
        }
        return evaluationContext;
    }
}
