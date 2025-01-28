package com.burukeyou.retry.spring.core.extend;

import java.util.concurrent.ExecutorService;

/**
 * create thread pool
 * @author  caizhihao
 */
public interface GlobalThreadPoolFactory {

    /**
     * get thread pool
     */
    ExecutorService getThreadPool();
}
