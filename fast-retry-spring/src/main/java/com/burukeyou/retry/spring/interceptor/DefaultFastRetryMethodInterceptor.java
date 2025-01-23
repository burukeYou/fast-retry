package com.burukeyou.retry.spring.interceptor;

import com.burukeyou.retry.spring.support.FastRetryMethodInvocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author caizhihao
 */
@Slf4j
@Component
public class DefaultFastRetryMethodInterceptor implements FastRetryMethodInterceptor {

    @Override
    public boolean beforeExecute(FastRetryMethodInvocation invocation) throws Exception {
        invocation.attachmentMap().put(INNER_USE_KEY,System.currentTimeMillis());
        return true;
    }

    @Override
    public boolean afterExecute(Exception exception, Object result, FastRetryMethodInvocation invocation) throws Exception {
        if (exception != null) {
            long startTime =  (long)invocation.attachmentMap().get(INNER_USE_KEY);
            long costTime = System.currentTimeMillis() - startTime;
            log.info("{} 方法执行失败准备开始重试 当前重试次数: {} 耗时: {}",invocation.getMethodAbsoluteName(),invocation.getCurExecuteCount(),costTime,exception);
            throw exception;
        }
        return true;
    }
}
