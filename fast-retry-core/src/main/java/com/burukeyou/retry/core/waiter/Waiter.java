package com.burukeyou.retry.core.waiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public interface Waiter {

    /**
     * 等待
     */
    <R> R wait(String seqNo);

    /**
     * 超时等待
     */
    <R> R wait(String seqNo, long timeout, TimeUnit unit) throws TimeoutException;

    /**
     * 唤醒
     */
    <T> void notify(String seqNo, T data);
}
