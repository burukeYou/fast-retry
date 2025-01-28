package com.burukeyou.retry.core.policy;

import java.util.function.Predicate;


/**
 * Retry result policy,
 *  this policy can determine whether a retry is needed based on the results
 *
 * @author caizhihao
 */
public interface FastMethodPolicy<T> extends Predicate<T>, FastRetryPolicy {

    /**
     * whether to continue retrying,
     * @param t        the result of the retry, sometime is the method return value
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
