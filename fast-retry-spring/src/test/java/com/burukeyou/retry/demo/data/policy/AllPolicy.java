package com.burukeyou.retry.demo.data.policy;

import com.burukeyou.retry.demo.data.WeatherResult;
import com.burukeyou.retry.spring.core.invocation.FastRetryInvocation;
import com.burukeyou.retry.spring.core.policy.FastRetryInterceptorPolicy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AllPolicy {

    public static class MyPolicy1 implements FastRetryInterceptorPolicy<WeatherResult> {

        private Integer executeCount = 0;

        @Override
        public boolean beforeExecute(FastRetryInvocation invocation) throws Exception {
            executeCount++;
            return FastRetryInterceptorPolicy.super.beforeExecute(invocation);
        }

        @Override
        public boolean afterExecuteFail(Exception exception, FastRetryInvocation invocation) throws Exception {
            log.info("记录异常 method:{} 当前执行次数：{}",invocation.getMethodAbsoluteName(), invocation.getCurExecuteCount(),exception);
            throw exception;
        }

        @Override
        public boolean afterExecuteSuccess(WeatherResult methodReturnValue, FastRetryInvocation invocation) {
            if (executeCount <= 5){
                return true;
            }
            return false;
        }
    }
}
