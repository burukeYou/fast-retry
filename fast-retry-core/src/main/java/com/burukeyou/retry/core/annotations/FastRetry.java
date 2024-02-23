package com.burukeyou.retry.core.annotations;


import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.core.RetryResultPolicy;
import com.burukeyou.retry.spring.AnnotationRetryTaskFactory;
import com.burukeyou.retry.spring.FastRetryAnnotationRetryTaskFactory;

import java.lang.annotation.*;

/**
 * Retry annotation
 *
 * @author caizhihao
 *
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//@Inherited
public @interface FastRetry {

    /**
     * Use the BeanName of the specified retry queue
     *          If not specified, get it from spring-context according to the  value of {@link FastRetry#queueClass()}
     *          If none of them are configured, use the default built-in retry queue
     *
     * @return   the name of retry-queue
     */
    String queueName() default "";

    /**
     * Use the bean class of the specified retry queue
     * @return the class of retry-queue
     */
    Class<? extends RetryQueue>[] queueClass() default {};

    /**
     * Flag to say that whether try again when an exception occurs
     * @return try again if true
     */
    boolean retryIfException() default true;

    /**
     * Exception types that are retryable.
     *
     * @return exception types to retry
     */
    Class<? extends Exception>[] include() default {};

    /**
     * Exception types that are not retryable.
     *
     * @return exception types to stop retry
     */
    Class<? extends Exception>[] exclude() default {};

    /**
     * Flag to say that whether recover when an exception occurs
     *
     * @return throw exception if false, if true return null and print exception log
     */
    boolean exceptionRecover() default false;

    /**
     * Flag to say that whether print every time execute retry exception log, just prevent printing too many logs
     * but no matter how you set it up,it will print the last time exception log
     */
     boolean printExceptionLog() default true;

    /**
     *  use custom result retry strategy,
     *  this policy can determine whether a retry is needed based on the results
     *
     * @return the class of retry-result-policy
     */
    Class<? extends RetryResultPolicy<?>>[] retryStrategy() default {};

    /**
     * @return the maximum number of attempts , if -1, it means unlimited
     */
    int maxAttempts() default -1;

    /**
     * Specify the RetryWait properties for retrying this operation. The default is a simple
     * {@link RetryWait} specification with no properties - see its documentation for
     * defaults.
     * @return a RetryWait specification
     */
    RetryWait retryWait() default @RetryWait();

    /**
     * Specify the factory bean for building RetryTask
     */
    Class<? extends AnnotationRetryTaskFactory> factory() default FastRetryAnnotationRetryTaskFactory.class;
}
