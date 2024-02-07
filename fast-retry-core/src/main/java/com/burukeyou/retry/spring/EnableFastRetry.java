package com.burukeyou.retry.spring;


import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * @author burukeyou
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAspectJAutoProxy(proxyTargetClass = false)
@Import(FastRetryAutoConfiguration.class)
@Documented
public @interface EnableFastRetry {

    /**
     * Indicate the order in which the {@link FastRetryAutoConfiguration} AOP <b>advice</b> should
     * be applied.
     * <p>
     * The default is {@code Ordered.LOWEST_PRECEDENCE - 1} in order to make sure the
     * advice is applied before other advices with {@link Ordered#LOWEST_PRECEDENCE} order
     * (e.g. an advice responsible for {@code @Transactional} behavior).
     */
    int order() default Ordered.LOWEST_PRECEDENCE - 1;

}
