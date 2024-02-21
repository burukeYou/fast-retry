package com.burukeyou.retry.data;

import com.burukeyou.retry.core.annotations.RetryWait;
import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.spring.AnnotationRetryTaskFactory;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomRetryTaskFactory implements AnnotationRetryTaskFactory<CustomFastRetry> {

    @Override
    public RetryTask<Object> getRetryTask(MethodInvocation invocation, CustomFastRetry retry) {
        return new RetryTask<Object>() {
            private int result = 0 ;

            @Override
            public boolean exceptionRecover() {
                return false;
            }

            @Override
            public int attemptMaxTimes() {
                return retry.maxAttempts();
            }

            @Override
            public long waitRetryTime() {
                RetryWait retryWait = retry.retryWait();
                return retryWait.timeUnit().toMillis(retryWait.delay());
            }

            @Override
            public boolean retry() {
                ++result;
                if (result == 1){
                    log.info("第一次执行");
                }
                try {
                    invocation.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                return result < 5;
            }

            @Override
            public String getResult() {
                return  result + "";
            }
        };
    }

}
