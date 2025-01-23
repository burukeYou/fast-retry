package com.burukeyou.retry.spring.interceptor;

import com.burukeyou.retry.spring.support.FastRetryMethodInvocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author caizhihao
 */
@Slf4j
//@Component
public class DefaultFastRetryMethodInterceptor implements FastRetryMethodInterceptor<Object> {

    @Override
    public void beforeExecute(FastRetryMethodInvocation invocation) throws Exception {
        invocation.attachmentMap().put(INNER_USE_KEY,System.currentTimeMillis());
    }

    @Override
    public void afterExecuteFail(Exception exception, FastRetryMethodInvocation invocation) throws Exception {
            long startTime =  (long)invocation.attachmentMap().get(INNER_USE_KEY);
            long costTime = System.currentTimeMillis() - startTime;
            log.info("{} 方法执行异常准备开始重试 当前重试次数: {} 耗时: {}",invocation.getMethodAbsoluteName(),invocation.getCurExecuteCount(),costTime,exception);
    }

}
