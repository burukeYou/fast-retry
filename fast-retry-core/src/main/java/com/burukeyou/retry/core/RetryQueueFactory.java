package com.burukeyou.retry.core;

import java.util.concurrent.ExecutorService;

public interface RetryQueueFactory {

    static RetryQueue get(){
        return get(8);
    }

    static RetryQueue get(int corePoolSize){
        return new FastRetryQueue(corePoolSize);
    }

    static RetryQueue get(ExecutorService executor){
        return new FastRetryQueue(executor);
    }
}
