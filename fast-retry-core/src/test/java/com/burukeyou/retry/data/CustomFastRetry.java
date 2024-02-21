package com.burukeyou.retry.data;


import com.burukeyou.retry.core.annotations.FastRetry;
import com.burukeyou.retry.core.annotations.RetryWait;

import java.lang.annotation.*;


@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@FastRetry(factory = CustomRetryTaskFactory.class)
public @interface CustomFastRetry {


    int maxAttempts() default -1;


    RetryWait retryWait() default @RetryWait();
}
