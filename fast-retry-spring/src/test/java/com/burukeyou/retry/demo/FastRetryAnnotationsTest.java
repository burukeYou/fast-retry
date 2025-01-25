package com.burukeyou.retry.demo;

import com.burukeyou.retry.core.exceptions.FastRetryTimeOutException;
import com.burukeyou.retry.demo.data.B15Configuration;
import com.burukeyou.retry.demo.data.BaseSpringTest;
import com.burukeyou.retry.demo.data.WeatherResult;
import com.burukeyou.retry.demo.data.WeatherService;
import com.burukeyou.retry.spring.core.policy.RetryInterceptorPolicy;
import com.burukeyou.retry.spring.support.FastRetryFuture;
import com.burukeyou.retry.spring.support.FastRetryMethodInvocation;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 */

@Slf4j
public class FastRetryAnnotationsTest extends BaseSpringTest {

    static {
        initContext(B15Configuration.class, WeatherService.class);
    }

    private WeatherService weatherService;

    @Before
    public void init() {
        this.weatherService = context.getBean(WeatherService.class);
    }

    /**
     * 测试FastRetry注解
     * @throws Exception
     */
    @Test
    public void testFastRetry() throws Exception {
        WeatherResult result = weatherService.getWeatherForTestFastRetry("北京");
        log.info("城市轮询结束 result:{}",result);

    }

    /**
     * 测试FastRetry注解-自定义重试策略
     * @throws Exception
     */
    @Test
    public void testFastRetryRetryStrategy() throws Exception {
        WeatherResult result = weatherService.getWeatherForTestRetryStrategy("北京");
        log.info("城市轮询结束 result:{}",result);
    }

    @Test
    public void testFastRetryRetryStrategy2() throws Exception {
        WeatherResult result = weatherService.getWeatherForTestRetryStrategy2("北京", weatherResult -> weatherResult.getCount() > 5);
        log.info("城市轮询结束 result:{}",result);
    }

    @Test
    public void testFastRetryRetryStrategy3() throws Exception {
        WeatherResult result = weatherService.getWeatherForTestRetryStrategy3("北京", new RetryInterceptorPolicy<WeatherResult>() {
            @Override
            public boolean afterExecuteSuccess(WeatherResult methodReturnValue, FastRetryMethodInvocation invocation) {
                return methodReturnValue.getCount() > 5;
            }
        });
        log.info("城市轮询结束 result:{}",result);
    }

    @Test
    public void testFastRetryRetryStrategy4() throws Exception {
        FastRetryFuture<WeatherResult> future = weatherService.getWeatherForTestRetryStrategy4("北京");
        log.info("城市轮询结束 result:{}",future.get());

        WeatherResult result = weatherService.getWeatherForTestRetryStrategy3("北京", new RetryInterceptorPolicy<WeatherResult>() {
            @Override
            public boolean afterExecuteSuccess(WeatherResult methodReturnValue, FastRetryMethodInvocation invocation) {
                return methodReturnValue.getCount() > 5;
            }
        });
        log.info("城市轮询结束 result:{}",result);
    }


    /**
     * 测试拦截器策略
     * @throws Exception
     */
    @Test
    public void testFastRetryRetryInteceptor() throws Exception {
        WeatherResult result = weatherService.getWeatherForTestIntepctor("北京");
        log.info("城市轮询结束 result:{}",result);
    }

    /**
     *  测试自定义重试注解
     * @throws Exception
     */
    @Test
    public void testFastRetryRetryAnno() throws Exception {
        WeatherResult result = weatherService.getWeatherForCusttomAnno("北京");
        log.info("城市轮询结束 result:{}",result);
    }


    /**
     * 测试FastRetry注解-CompletableFuture返回值
     * @throws Exception
     */
    @Test
    public void testFastRetryFutureResult() throws Exception {
        CompletableFuture<WeatherResult> weather = weatherService.getFutureWeather("北京");

        weather.whenComplete((weatherResult, throwable) -> {
            if (throwable instanceof FastRetryTimeOutException){
                log.error("城市回调超时异常 result:{}",weatherResult,throwable);
            }else {
                log.info("城市回调结束 result:{}",weatherResult);
            }
        });

        WeatherResult weatherResult = weather.get();

        //WeatherResult weatherResult = weather.get();
        log.info("城市轮询结束22 result:{}",weatherResult);


        Thread.currentThread().join();
    }

    /**
     * 测试FastRetry注解-提交多任务
     * @throws Exception
     */
    @Test
    public void testFastRetryManyTask() throws Exception {

        List<CompletableFuture<WeatherResult>> futures = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String cityName = "北京" + i;
            //log.info("提交新任务 city:{}",cityName);
            CompletableFuture<WeatherResult> weather = weatherService.getFutureWeather(cityName);
//            log.info("城市轮询结束 city:{} result:{}",cityName,weather);
            futures.add(weather);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("所有任务完成");
        for (CompletableFuture<WeatherResult> future : futures) {
            WeatherResult weatherResult = future.get();
            log.info("城市轮询结束  result:{}",weatherResult.data);
        }
    }

}
