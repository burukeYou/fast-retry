package com.burukeyou.retry.core.annotations;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Next retry interval time
 *
 * @author caizhihao
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RetryWait {

    /**
     * @return the delay time
     */
    long delay() default 1;

    /**
     * @return the delay time unit
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
