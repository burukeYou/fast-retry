package com.burukeyou.retry.spring;

import com.burukeyou.retry.core.task.RetryTask;
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
