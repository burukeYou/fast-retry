package com.burukeyou.retry;

import com.burukeyou.retry.core.FastRetryBuilder;
import com.burukeyou.retry.core.FastRetryer;
import com.burukeyou.retry.core.RetryResultPolicy;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class FastRetryBuilderTest {

    @Test
    public void testBuild() throws ExecutionException, InterruptedException {
        RetryResultPolicy<String> resultPolicy = result -> result.equals("444");
        FastRetryer<String> retryer = FastRetryBuilder.<String>builder()
                .attemptMaxTimes(3)
                .waitRetryTime(3, TimeUnit.SECONDS)
                .retryIfException(true)
                .retryIfExceptionOfType(IllegalArgumentException.class)
                .notRetryIfExceptionOfType(TimeoutException.class)
                .exceptionRecover(true)
                .resultPolicy(resultPolicy)
                .build();

        CompletableFuture<String> future = retryer.submit(() -> {
            log.info("重试");
            //throw new Exception("test");
            //int i = 1/0;
            if (0 < 10){
                throw new IllegalArgumentException("test");
            }
            return "444";
        });

        String o = future.get();
        log.info("结果{}", o);
    }
}
