package com.burukeyou.retry.demo.data.policy;

import com.burukeyou.retry.demo.data.WeatherResult;
import com.burukeyou.retry.spring.core.policy.RetryInterceptorPolicy;
import com.burukeyou.retry.spring.support.FastRetryMethodInvocation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AllPolicy {


    public static class MyPolicy1 implements RetryInterceptorPolicy<WeatherResult> {

        private Integer executeCount = 0;

        @Override
        public boolean beforeExecute(FastRetryMethodInvocation invocation) throws Exception {
            executeCount++;
            return RetryInterceptorPolicy.super.beforeExecute(invocation);
        }

        @Override
        public boolean afterExecuteFail(Exception exception, FastRetryMethodInvocation invocation) throws Exception {
            log.info("记录异常 method:{} 当前执行次数：{} 是否:{}",invocation.getMethodAbsoluteName(), invocation.getCurExecuteCount(),invocation.isFirstExecute(),exception);
            throw exception;
        }

        @Override
        public boolean afterExecuteSuccess(WeatherResult methodReturnValue, FastRetryMethodInvocation invocation) {
            if (executeCount <= 5){
                return true;
            }
            return false;
        }
    }
}
