package com.burukeyou.retry.core.waiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExchangerWaiter implements Waiter {
    public static final Map<String, Exchanger<Object>> lockMap = new ConcurrentHashMap<>();

    public <R> R wait(String seq) {
        Exchanger<Object> exchanger = new Exchanger<>();
        lockMap.put(seq,exchanger);
        try {
            Object data = exchanger.exchange(seq);
            return (R)data;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public <R> R wait(String seq, long timeout, TimeUnit unit) throws TimeoutException {
        Exchanger<Object> exchanger = new Exchanger<>();
        lockMap.put(seq,exchanger);
        try {
            Object data = exchanger.exchange(seq,timeout, unit);
            return (R)data;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw e;
        }
    }
    public <T> void notify(String seq, T data) {
        Exchanger<Object> exchanger = lockMap.get(seq);
        if (exchanger == null){
            return;
        }

        try {
            exchanger.exchange(data);
            lockMap.remove(seq);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
