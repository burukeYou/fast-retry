package com.burukeyou.retry.demo.anno;

import com.burukeyou.retry.core.enums.LogEnum;
import com.burukeyou.retry.demo.data.policy.AllPolicy;
import com.burukeyou.retry.spring.annotations.FastRetry;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@FastRetry(
        briefErrorLog = true,
        errLog = LogEnum.NOT,
        policy = AllPolicy.MyPolicy1.class)
public @interface BQRetry {

    @AliasFor(annotation = FastRetry.class)
    int maxAttempts() default 4;

    @AliasFor(annotation = FastRetry.class)
    long delay() default 2000;

}
