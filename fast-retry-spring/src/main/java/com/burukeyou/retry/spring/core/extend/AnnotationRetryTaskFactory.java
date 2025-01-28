package com.burukeyou.retry.spring.core.extend;

import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.spring.annotations.FastRetry;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;

/**
 * Annotation retry task build factory
 * @author caizhihao
 * @param <A>                      retry annotation, can other but must be marked with {@link FastRetry}
 */
public interface AnnotationRetryTaskFactory<A extends Annotation>  {

     RetryTask<Object> getRetryTask(MethodInvocation invocation, A retry);
}
