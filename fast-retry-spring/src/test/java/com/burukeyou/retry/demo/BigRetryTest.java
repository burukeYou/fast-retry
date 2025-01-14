package com.burukeyou.retry.demo;


import com.burukeyou.retry.demo.data.B15Configuration;
import com.burukeyou.retry.demo.data.BaseSpringTest;
import com.burukeyou.retry.demo.data.WeatherResult;
import com.burukeyou.retry.demo.data.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BigRetryTest extends BaseSpringTest {


    static {
        initContext(WeatherService.class, B15Configuration.class);
    }

    @Test
    public void test1() throws InterruptedException {
        WeatherService bean = context.getBean(WeatherService.class);
        //bean.getWeather2222("北京");
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 1; i++) {
            final int t = i;
            executorService.execute(() -> {
                String cityName = "北京" + t;
                log.info("提交新任务 city:{}",cityName);
                WeatherResult weather = bean.getWeather(cityName);
                log.info("城市轮询结束 city:{} result:{}",cityName,weather);
            });
        }

        Thread.currentThread().join();
    }


    @Test
    public void test2() throws Exception {

        WeatherService bean = context.getBean(WeatherService.class);

        CompletableFuture<WeatherResult> future = bean.getFutureWeather("北京");

 /*       future.whenComplete((weatherResult, throwable) -> {
           // log.info("城市轮询结束  result:{}",weatherResult.data);
            System.out.println("城市轮询结束  result:" + weatherResult.data);
        });*/

        WeatherResult weatherResult = future.get(2, TimeUnit.SECONDS);
        log.info("城市轮询结束  result:{}",weatherResult.data);

        System.out.println("等待任务完成");
        Thread.sleep(300000);
    }
    @Test
    public void test3() throws Exception {

        WeatherService bean = context.getBean(WeatherService.class);
        //bean.getWeather2222("北京");

        List<CompletableFuture<WeatherResult>> futures = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            String cityName = "北京" + i;
            log.info("提交新任务 city:{}",cityName);
            CompletableFuture<WeatherResult> weather = bean.getFutureWeather(cityName);
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
