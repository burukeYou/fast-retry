package com.burukeyou.retry.spring;

import com.burukeyou.retry.core.annotations.FastRetry;
import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//@Component
public class FastRetryAutoConfiguration extends AbstractPointcutAdvisor
        implements InitializingBean, IntroductionAdvisor, ImportAware, BeanFactoryAware {

    private static final long serialVersionUID = -1699797326589993828L;

    protected AnnotationAttributes enableRetry;
    private final List<Class<? extends Annotation>> pointcutAnnotations;
    private Pointcut pointcut;
    private AnnotationAwareFastRetryInterceptor advice;

    private BeanFactory beanFactory;

    public FastRetryAutoConfiguration() {
        this.pointcutAnnotations = Collections.singletonList(FastRetry.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.pointcut = buildPointcut(pointcutAnnotations);
        this.advice = buildAdvice();
        if (this.enableRetry != null) {
            setOrder(enableRetry.getNumber("order"));
        }
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public ClassFilter getClassFilter() {
        return this.pointcut.getClassFilter();
    }

    @Override
    public void validateInterfaces() throws IllegalArgumentException {

    }

    @Override
    public Class<?>[] getInterfaces() {
        return new Class[] { FastRetryable.class };
    }

    private Pointcut buildPointcut(List<Class<? extends Annotation>> pointcutAnnotations) {
        ComposablePointcut result = null;
        for (Class<? extends Annotation> retryAnnotationType : pointcutAnnotations) {
            Pointcut filter = new AnnotationClassOrMethodPointcut(retryAnnotationType);
            if (result == null) {
                result = new ComposablePointcut(filter);
            }
            else {
                result.union(filter);
            }
        }
        return result;
    }

    private AnnotationAwareFastRetryInterceptor buildAdvice() {
        AnnotationAwareFastRetryInterceptor retryAdvice = new AnnotationAwareFastRetryInterceptor();
        retryAdvice.setBeanFactory(beanFactory);
        return retryAdvice;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableRetry = AnnotationAttributes
                .fromMap(importMetadata.getAnnotationAttributes(EnableFastRetry.class.getName()));
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }


    private final class AnnotationClassOrMethodPointcut extends StaticMethodMatcherPointcut {

        private final MethodMatcher methodResolver;

        AnnotationClassOrMethodPointcut(Class<? extends Annotation> annotationType) {
            this.methodResolver = new AnnotationMethodMatcher(annotationType);
            setClassFilter(new AnnotationClassOrMethodFilter(annotationType));
        }

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            ClassFilter classFilter = getClassFilter();
            return classFilter.matches(targetClass) || this.methodResolver.matches(method, targetClass);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AnnotationClassOrMethodPointcut)) {
                return false;
            }
            AnnotationClassOrMethodPointcut otherAdvisor = (AnnotationClassOrMethodPointcut) other;
            return ObjectUtils.nullSafeEquals(this.methodResolver, otherAdvisor.methodResolver);
        }

    }

    private final class AnnotationClassOrMethodFilter extends AnnotationClassFilter {

        private final AnnotationMethodsResolver methodResolver;

        AnnotationClassOrMethodFilter(Class<? extends Annotation> annotationType) {
            super(annotationType, true);
            this.methodResolver = new AnnotationMethodsResolver(annotationType);
        }

        @Override
        public boolean matches(Class<?> clazz) {
            return super.matches(clazz) || this.methodResolver.hasAnnotatedMethods(clazz);
        }
    }

    private static class AnnotationMethodsResolver {

        private final Class<? extends Annotation> annotationType;

        public AnnotationMethodsResolver(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        public boolean hasAnnotatedMethods(Class<?> clazz) {
            final AtomicBoolean found = new AtomicBoolean(false);
            ReflectionUtils.doWithMethods(clazz, method -> {
                if (found.get()) {
                    return;
                }
                Annotation annotation = AnnotationUtils.findAnnotation(method,
                        AnnotationMethodsResolver.this.annotationType);
                if (annotation != null) {
                    found.set(true);
                }
            });
            return found.get();
        }

    }
}
