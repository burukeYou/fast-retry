package com.burukeyou.retry.demo;

import com.burukeyou.retry.core.exceptions.FastRetryTimeOutException;
import com.burukeyou.retry.demo.data.B15Configuration;
import com.burukeyou.retry.demo.data.BaseSpringTest;
import com.burukeyou.retry.demo.data.WeatherResult;
import com.burukeyou.retry.demo.data.WeatherService;
import com.burukeyou.retry.spring.core.invocation.FastRetryInvocation;
import com.burukeyou.retry.spring.core.policy.FastInterceptorPolicy;
import com.burukeyou.retry.spring.core.policy.FastRetryFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        WeatherResult result = weatherService.getWeatherForTestRetryStrategy3("北京", new FastInterceptorPolicy<WeatherResult>() {
            @Override
            public boolean afterExecuteSuccess(WeatherResult methodReturnValue, FastRetryInvocation invocation) {
                log.info("当前执行次数：count:{}", invocation.getCurExecuteCount());
                return methodReturnValue.getCount() > 5;
            }
        });
        log.info("城市轮询结束 result:{}",result);
    }

    @Test
    public void testFastRetryRetryStrategy4() throws Exception {

        for (int i = 0; i < 10; i++) {
            final int j = i;
            new Thread(() -> {
                FastRetryFuture<WeatherResult> future = weatherService.getWeatherForTestRetryStrategy4("城市[" + j+"]");
                try {
                    log.info("城市[{}]轮询结束 result:{}",j,future.get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

         Thread.sleep(3000000);
//        FastRetryFuture<WeatherResult> future = weatherService.getWeatherForTestRetryStrategy4("北京");
//        log.info("城市轮询结束 result:{}",future.get());
//
//        FastRetryFuture<WeatherResult> future333 = weatherService.getWeatherForTestRetryStrategy4("北京");
//        log.info("城市轮询结束2222 result:{}",future333.get());
    }

    public void testFFF5(){
        WeatherResult result = weatherService.getWeatherForTestRetryStrategy3("北京", new FastInterceptorPolicy<WeatherResult>() {
            @Override
            public boolean afterExecuteSuccess(WeatherResult methodReturnValue, FastRetryInvocation invocation) {
                return methodReturnValue.getCount() > 5;
            }
        });
        log.info("城市轮询结束 result:{}",result);
    }

    @Test
    public void testFastRetryRetryStrategy5() throws Exception {
        WeatherResult future = weatherService.getWeatherForTestRetryStrategy5("北京", result -> result.getCount() > 5);
        log.info("城市轮询结束 result:{}",future);
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
