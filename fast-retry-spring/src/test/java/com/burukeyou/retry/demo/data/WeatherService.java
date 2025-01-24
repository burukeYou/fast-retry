package com.burukeyou.retry.demo.data;


import com.burukeyou.retry.core.FastRetryBuilder;
import com.burukeyou.retry.core.policy.RetryResultPolicy;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.annotations.RetryWait;
import com.github.rholder.retry.*;
import com.google.common.base.Predicates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)// 设置多例，方便index单独计数
public class WeatherService {

    private int index = 0;

    public static class WeatherServiceResultPredicate implements RetryResultPolicy<WeatherResult> {

        private int i = 0;

        public WeatherServiceResultPredicate() {
            System.out.println("WeatherServiceResultPredicate init");
        }

        @Override
        public boolean canRetry(WeatherResult weatherResult) {
            //log.info("指定重试策略 i:{} result:{}",i,weatherResult);
            return ++i <5;
        }
    }

    @FastRetry(retryWait = @RetryWait(delay = 2),briefErrorLog = true)
    public WeatherResult getWeatherForTestFastRetry(String cityName){
        //log.info("WeatherService进行重试  次数:{} 城市: {}",index,cityName);
        if (++index < 5 ){
          //  log.info("模拟异常进行重试  {}",index);
            throw new RuntimeException("模拟异常进行重试");
        }

        return new WeatherResult(cityName + "-哈哈");
    }

    @FastRetry(retryWait = @RetryWait(delay = 2),retryStrategy = WeatherServiceResultPredicate.class,exceptionRecover = true)
    public WeatherResult getWeatherForTestRetryStrategy(String cityName){
        log.info("WeatherService进行重试  次数:{} 城市: {}",++index,cityName);
        return new WeatherResult(cityName + "-哈哈");
    }


    @FastRetry(queueName = "userRetryQueue",
            retryWait = @RetryWait(delay = 6,timeUnit = TimeUnit.SECONDS),
            retryStrategy = WeatherServiceResultPredicate.class)
    public WeatherResult getWeather(String cityName){
        try {
            Thread.sleep(1000);
            log.info("getWeather  {}",cityName);
            int i = 3 / 0;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new WeatherResult(cityName + "-哈哈");
    }

    @FastRetry(  retryWait = @RetryWait(delay = 2,timeUnit = TimeUnit.SECONDS),
                 retryStrategy = WeatherServiceResultPredicate.class)
    public CompletableFuture<WeatherResult> getFutureWeather(String cityName){
        try {
            //Thread.sleep(1000);
            log.info("WeatherService进行重试  次数:{} 城市: {}",++index,cityName);
           //int i = 3 / 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return FastRetryBuilder.of(new WeatherResult(cityName + "-哈哈"));
    }

    @FastRetry(
            maxAttempts = 100,
            queueName = B15Configuration.USER_RETRY_QUEUE,
            retryWait = @RetryWait(delay = 2,timeUnit = TimeUnit.SECONDS))
    public CompletableFuture<WeatherResult> getFutureWeatherForCompare(String cityName){
        //log.info("WeatherService进行重试  次数:{} 城市: {}",++index,cityName);
        WeatherResult weather = WeatherServer.getWeather(cityName);
        if (weather == null){
            //继续重试
            throw new RuntimeException("模拟异常进行重试");
        }

        return FastRetryBuilder.of(weather);
    }



    @Retryable(maxAttempts = 100,backoff = @Backoff(delay = 2000), recover = "revover111")
    public WeatherResult getSpringWeatherForCompare(String cityName){
        log.info("WeatherService进行重试  次数:{} 城市: {}",++index,cityName);
        WeatherResult weather = WeatherServer.getWeather(cityName);
        if (weather == null){
            //继续重试
            throw new RuntimeException("模拟异常进行重试");
        }
        return weather;
    }
    @Recover
    public WeatherResult revover111(Throwable throwable,String cityName){
        return null;
    }

    public WeatherResult getGuavaRetryWeather(String cityName){
        Retryer<WeatherResult> retryer = RetryerBuilder.<WeatherResult>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(100))
                .withWaitStrategy(WaitStrategies.fixedWait(2, TimeUnit.SECONDS))
                .retryIfResult(Predicates.<WeatherResult>isNull())
                .build();

        Callable<WeatherResult> callable = new Callable<WeatherResult>() {
            @Override
            public WeatherResult call() throws Exception {
                //log.info("WeatherService进行重试  次数:{} 城市: {}",++index,cityName);
                return WeatherServer.getWeather(cityName);
            }
        };

        try {
            return retryer.call(callable);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (RetryException e) {
            throw new RuntimeException(e);
        }
    }

}
