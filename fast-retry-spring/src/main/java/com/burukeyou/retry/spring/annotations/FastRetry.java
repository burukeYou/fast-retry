package com.burukeyou.retry.spring.annotations;


import com.burukeyou.retry.core.enums.LogEnum;
import com.burukeyou.retry.core.policy.FastResultPolicy;
import com.burukeyou.retry.core.policy.FastRetryPolicy;
import com.burukeyou.retry.spring.core.extend.AnnotationRetryTaskFactory;
import com.burukeyou.retry.spring.core.interceptor.FastRetryInterceptor;
import com.burukeyou.retry.spring.core.policy.FastInterceptorPolicy;

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
     * @return the maximum number of attempts , if -1, it means unlimited
     */
    int maxAttempts() default 3;

    /**
     * How long will it take to start the next retry, equals to {@link #delay},
     * If both are configured, retryWait() will take effect first
     */
    RetryWait[] retryWait() default {};

    /**
     * How long will it take to start the next retry, unit is MILLISECONDS
     */
    long delay() default 500;

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
     */
    LogEnum errLog() default LogEnum.EVERY;

    /**
     * Set whether to simplify the stack information of the printing exception,
     * if so, only the first three lines of stack information will be printed,
     * and if the first execution fails, this configuration will be ignored,
     * and the complete exception information will still be printed
     * @see #errLog()
     */
    boolean briefErrorLog() default false;

    /**
     * If the expression is true, the retry continues, otherwise the retry stops. 
     * You can use $.xx to represent the value of the field where the method returns a value,
     *  and use it in an expression, such as $.userId > 3 
     */
    String retryWhenExpression() default "";

    /**
     *  use custom  retry strategy,
     *  <p>Currently, the following policies are supported:</p>
     *  <ul>
     *      <li>{@link FastResultPolicy}</li>
     *      <li>{@link FastInterceptorPolicy}</li>
     *  </ul>
     * @return the class of retry-result-policy
     */
    Class<? extends FastRetryPolicy>[] policy() default {};

    /**
     *  use custom  retry interceptor,
     */
    Class<? extends FastRetryInterceptor>[] interceptor() default {};

    /**
     * Specify the factory  for building RetryTask, if this factory is replaced, then all retry functions of @FastRetry will prevail with this factory
     */
    Class<? extends AnnotationRetryTaskFactory>[] taskFactory() default {};

}
