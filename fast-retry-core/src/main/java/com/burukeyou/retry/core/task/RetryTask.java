package com.burukeyou.retry.core.task;


import java.util.Collections;
import java.util.List;

/**
 * retry task
 * @author caizhihao
 */
public interface RetryTask<R> {

    /**
     * @return the maximum number of attempts , if -1, it means unlimited
     */
    default int attemptMaxTimes() {
        return -1;
    }

    /**
     * @return next retry wait time
     */
    default long waitRetryTime() {
        return 5 * 1000;
    }

    /**
     * Perform retry
     * @param curExecuteCount             current execute count
     * @return                            whether try again, try again if true
     */
    boolean retry(long curExecuteCount) throws Exception;

    /**
     * @return the  retry result
     */
    R getResult();

    /**
     * Flag to say that whether try again when an exception occurs
     * @return try again if true
     */
    default boolean retryIfException() {
        return true;
    }

    /**
     * Exception types that are retryable.
     *
     * @return exception types to retry
     */
    default List<Class<? extends Exception>> include() {
        return Collections.emptyList();
    }

    /**
     * Exception types that are not retryable.
     *
     * @return exception types to stop retry
     */
    default List<Class<? extends Exception>> exclude(){
        return Collections.emptyList();
    }

    /**
     * Flag to say that whether recover when an exception occurs
     *
     * @return throw exception if false, if true return null and print exception log
     */
    default boolean exceptionRecover() {
        return false;
    }

    /**
     * Flag to say that whether print every time execute retry exception log, just prevent printing too many logs
     * but no matter how you set it up,it will print the last time exception log
     */
    default boolean printExceptionLog() {
        return true;
    }

}
