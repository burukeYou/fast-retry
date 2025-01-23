package com.burukeyou.retry.spring.annotations;


import com.burukeyou.retry.spring.core.FastRetryAdvisorConfiguration;
import com.burukeyou.retry.spring.config.FastRetrySpringConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * @author caizhihao
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAspectJAutoProxy(proxyTargetClass = false)
@Import({FastRetryAdvisorConfiguration.class, FastRetrySpringConfiguration.class})
@Documented
public @interface EnableFastRetry {

    /**
     * Indicate the order in which the {@link FastRetryAdvisorConfiguration} AOP <b>advice</b> should
     * be applied.
     * <p>
     * The default is {@code Ordered.LOWEST_PRECEDENCE - 1} in order to make sure the
     * advice is applied before other advices with {@link Ordered#LOWEST_PRECEDENCE} order
     * (e.g. an advice responsible for {@code @Transactional} behavior).
     */
    int order() default Ordered.LOWEST_PRECEDENCE - 1;

}
