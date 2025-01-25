package com.burukeyou.retry.spring.core.extend;

import com.burukeyou.retry.core.support.FastRetryThreadPool;
import com.burukeyou.retry.spring.utils.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author  caihzihao
 */
@Slf4j
public class DefaultThreadPoolFactory implements GlobalThreadPoolFactory {

    @Override
    public ExecutorService getThreadPool() {
        int cpuCount = SystemUtil.CPU_COUNT;
        log.info("[fast-retry-spring] init default retry queue cpuSize:{}",cpuCount);
        return new FastRetryThreadPool(4,cpuCount * 4,60, TimeUnit.SECONDS);
    }

}
