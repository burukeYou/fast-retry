package com.burukeyou.retry.demo.data;


import com.burukeyou.retry.core.FastRetryBuilder;
import com.burukeyou.retry.core.policy.RetryResultPolicy;
import com.burukeyou.retry.demo.anno.BQRetry;
import com.burukeyou.retry.demo.data.policy.AllPolicy;
import com.burukeyou.retry.spring.annotations.FastRetry;
import com.burukeyou.retry.spring.annotations.RetryWait;
import com.burukeyou.retry.spring.core.policy.LogEnum;
import com.burukeyou.retry.spring.core.policy.RetryInterceptorPolicy;
import com.burukeyou.retry.spring.support.FastRetryFuture;
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
import java.util.Random;
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

    @FastRetry(
            delay = 1000,
            briefErrorLog = true,
            errLog = LogEnum.PRE_1_LAST_1,
            maxAttempts = 3)
    public WeatherResult getWeatherForTestFastRetry(String cityName){
        //log.info("WeatherService进行重试  次数:{} 城市: {}",index,cityName);
        if (++index < 7 ){
          //  log.info("模拟异常进行重试  {}",index);
            throw new RuntimeException("模拟异常进行重试");
        }

        return new WeatherResult(cityName + "-哈哈");
    }

    @FastRetry(retryWait = @RetryWait(delay = 2),maxAttempts = 100, policy = WeatherServiceResultPredicate.class)
    public WeatherResult getWeatherForTestRetryStrategy(String cityName){
        log.info("WeatherService进行重试  次数:{} 城市: {}",++index,cityName);
        return new WeatherResult(cityName + "-哈哈");
    }

    @FastRetry(delay = 5000,maxAttempts = 15)
    public WeatherResult getWeatherForTestRetryStrategy2(String cityName, RetryResultPolicy<WeatherResult> policy){
        int i = new Random().nextInt(40);
        log.info("WeatherService进行重试  次数:{} 城市: {} 随机数:{} ",++index,cityName, i);
        return new WeatherResult(cityName, i);
    }

    @FastRetry(delay = 3000,maxAttempts = 4)
    public WeatherResult getWeatherForTestRetryStrategy3(String cityName, RetryInterceptorPolicy<WeatherResult> interceptor){
        int i = new Random().nextInt(40);
        log.info("WeatherService进行重试  次数:{} 城市: {} 随机数:{} ",++index,cityName, i);
        return new WeatherResult(cityName, i);
    }

    @FastRetry(delay = 3000,maxAttempts = 4)
    public FastRetryFuture<WeatherResult> getWeatherForTestRetryStrategy4(String cityName){
        int i = new Random().nextInt(40);
        log.info("WeatherService进行重试  次数:{} 城市: {} 随机数:{} ",++index,cityName, i);
        return FastRetryFuture
                .completedFuture(new WeatherResult(cityName, i))
                .retryWhen(weatherResult -> weatherResult.getCount() > 20);
    }

    @FastRetry(
            delay = 1000,
            maxAttempts = 6,
            briefErrorLog = true,
            errLog = LogEnum.NOT,
            policy = AllPolicy.MyPolicy1.class)
    public WeatherResult getWeatherForTestIntepctor(String cityName){
        log.info("WeatherService进行重试  次数:{} 城市: {}",++index,cityName);
        //return new WeatherResult(cityName + "-哈哈");
        throw new IllegalArgumentException("can xxx " + cityName);
    }

    @BQRetry
    public WeatherResult getWeatherForCusttomAnno(String cityName){
        log.info("WeatherService进行重试  次数:{} 城市: {}",++index,cityName);
        return new WeatherResult(cityName + "-哈哈");
        //throw new IllegalArgumentException("can xxx " + cityName);
    }


    @FastRetry(
            retryWait = @RetryWait(delay = 6,timeUnit = TimeUnit.SECONDS),
            policy = WeatherServiceResultPredicate.class)
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
                 policy = WeatherServiceResultPredicate.class)
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
