package com.burukeyou.retry.core.exceptions;

/**
 *  When the retry times exceeds the maximum, this exception is thrown
 */
public class FastRetryTimeOutException extends FastRetryException {

    private static final long serialVersionUID = 1803213493808420040L;


    public FastRetryTimeOutException(String msg) {
        super(msg);
    }

    public FastRetryTimeOutException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
