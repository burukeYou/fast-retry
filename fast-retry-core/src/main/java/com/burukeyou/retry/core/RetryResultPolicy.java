package com.burukeyou.retry.core;

import java.util.function.Predicate;

/**
 * Retry result policy
 *
 * @author caizhihao
 */
public interface RetryResultPolicy<T> extends Predicate<T> {

    /**
     * whether to continue retrying
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
