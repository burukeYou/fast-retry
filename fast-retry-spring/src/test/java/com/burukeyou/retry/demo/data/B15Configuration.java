package com.burukeyou.retry.demo.data;


import com.burukeyou.retry.core.FastRetryQueue;
import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.spring.EnableFastRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableRetry
@EnableFastRetry
public class B15Configuration {

    public static final String USER_RETRY_QUEUE = "userRetryQueue";


    @Bean(B15Configuration.USER_RETRY_QUEUE)
    public RetryQueue retryQueueApi(){
        ExecutorService pool = Executors.newFixedThreadPool(8);
        return new FastRetryQueue(pool);
    }


}
