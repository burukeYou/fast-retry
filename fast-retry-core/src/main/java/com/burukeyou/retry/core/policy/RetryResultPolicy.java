package com.burukeyou.retry.core.policy;

import java.util.function.Predicate;

import com.burukeyou.retry.core.policy.RetryPolicy;

/**
 * Retry result policy
 *
 * @author caizhihao
 */
public interface RetryResultPolicy<T> extends Predicate<T>,  RetryPolicy {

    /**
     * whether to continue retrying,
     * @param t        the result of the retry
     * @return         true if continue retrying
     */
    boolean canRetry(T t);

    /**
     * Adapt Predicate interface
     */
    @Override
    default boolean test(T t){
        return canRetry(t);
    }
}
