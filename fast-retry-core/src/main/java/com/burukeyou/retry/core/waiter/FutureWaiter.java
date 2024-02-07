package com.burukeyou.retry.core.waiter;

import java.util.Map;
import java.util.concurrent.*;

public class FutureWaiter implements Waiter {

    private final Map<String, CompletableFuture<Object>> futureMap = new ConcurrentHashMap<>();

    @Override
    public <R> R wait(String seq) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        futureMap.put(seq,future);
        try {
            return (R)future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <R> R wait(String seq, long timeout, TimeUnit unit) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        futureMap.put(seq,future);
        try {
            return (R)future.get(timeout,unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> void notify(String seq, T data) {
        CompletableFuture<Object> future = futureMap.get(seq);
        if (future != null) {
            futureMap.remove(seq);
            future.complete(data);
        }
    }
}
