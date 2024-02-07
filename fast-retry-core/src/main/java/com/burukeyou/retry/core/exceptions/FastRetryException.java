package com.burukeyou.retry.core.exceptions;


public abstract class FastRetryException extends RuntimeException {

    private static final long serialVersionUID = 5439915454935047936L;

    public FastRetryException(String msg) {
        super(msg);
    }

    public FastRetryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public Throwable getRootCause() {
        return getRootCause(this);
    }

    public Throwable getMostSpecificCause() {
        Throwable rootCause = this.getRootCause();
        return (Throwable)(rootCause != null ? rootCause : this);
    }

    public static Throwable getRootCause(Throwable original) {
        if (original == null) {
            return null;
        }
        Throwable rootCause = null;
        Throwable cause = original.getCause();
        while (cause != null && cause != rootCause) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause;
    }
}
