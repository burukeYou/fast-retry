package com.burukeyou.demo.retry;


import com.burukeyou.demo.retry.data.B15Configuration;
import com.burukeyou.demo.retry.data.BaseSpringTest;
import com.burukeyou.demo.retry.data.WeatherResult;
import com.burukeyou.demo.retry.data.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *  比较测试
    测试线程池:  8个线程
    单个任务逻辑:  轮询5次，隔2秒重试一次，总耗时10秒

         任务数        FastRetry              Spring-Retry                  Guava-Retry
          1            10秒                     10秒                         10秒
          10           10.066秒                 20.092秒                     20.078秒
          50           10.061秒                 70.186秒                     70.168秒
          100          10.077秒                 130.33秒                     130.31秒
          500          10.154秒                 631.420秒                    631.53秒
          1000         10.237秒                 1254.78秒                    1256.28秒
          5000         10.482秒                 没测预计： 6250秒              没测预计： 6250秒
          10000        10.686秒                 没测预计： 12520秒             没测预计： 12520秒
          100000       13.71秒                  没测预计： 125000秒            没测预计： 125000秒
          500000       28.89秒                  没测预计： 625000秒            没测预计： 625000秒
          1000000      58.05秒                  没测预计： 1250000秒           没测预计： 1250000秒



   总结
 *      同步型轮询任务：  总耗时 =  任务数/并发度 * 单个任务耗时
 *                           =  任务数/8 * 10
 *
 *
 */
@Slf4j
public class FriendlyComparisonTest extends BaseSpringTest {

    static {
        initContext(B15Configuration.class, WeatherService.class);
    }

/*
    private WeatherService weatherService;

    @Before
    public void init() {
        this.weatherService = context.getBean(WeatherService.class);
    }
*/




    /**
     * spring-retry注解-性能测试
     * @throws Exception
     */
    @Test
    public void testFastRetryManyTaskForSpring() throws Exception {
        List<CompletableFuture<WeatherResult>> futures = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(8);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int taskSize = 1;
        for (int i = 0; i < taskSize; i++) {
            WeatherService taskWeatherService = context.getBean(WeatherService.class);
            CompletableFuture<WeatherResult> testFuture = new CompletableFuture<>();
            futures.add(testFuture);

            String cityName = "北京" + i;
            pool.execute(() -> {
                //log.info("提交新任务 city:{}",cityName);
                WeatherResult weather = taskWeatherService.getSpringWeatherForCompare(cityName);
                testFuture.complete(weather);
            });
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("所有任务完成");
        for (CompletableFuture<WeatherResult> future : futures) {
            WeatherResult weatherResult = future.get();
            log.info("城市轮询结束  result:{}",weatherResult.data);
        }

        stopWatch.stop();
        log.info("Spring-Retry测试总耗时  任务数:{} 耗时:{}",taskSize,stopWatch.getTotalTimeSeconds());
    }

    /**
     * Guava-retry注解-性能测试
     * @throws Exception
     */
    @Test
    public void testFastRetryManyTaskForGuava() throws Exception {
        List<CompletableFuture<WeatherResult>> futures = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(8);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int taskSize = 500;
        for (int i = 0; i < taskSize; i++) {
            WeatherService taskWeatherService = context.getBean(WeatherService.class);
            CompletableFuture<WeatherResult> testFuture = new CompletableFuture<>();
            futures.add(testFuture);

            String cityName = "北京" + i;
            pool.execute(() -> {
                //log.info("提交新任务 city:{}",cityName);
                WeatherResult weather = taskWeatherService.getGuavaRetryWeather(cityName);
                testFuture.complete(weather);
            });
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("所有任务完成");
        for (CompletableFuture<WeatherResult> future : futures) {
            WeatherResult weatherResult = future.get();
            //log.info("城市轮询结束  result:{}",weatherResult.data);
        }

        stopWatch.stop();
        log.info("Guava-Retry测试总耗时  任务数:{} 耗时:{}",taskSize,stopWatch.getTotalTimeSeconds());
    }

    /**
     * 测试FastRetry注解-性能测试
     * @throws Exception
     */
    @Test
    public  void testFastRetryManyTask() throws Exception {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int taskSize = 1;

        List<CompletableFuture<WeatherResult>> futures = new ArrayList<>();
        for (int i = 0; i < taskSize; i++) {
            WeatherService taskWeatherService = context.getBean(WeatherService.class);
            String cityName = "北京" + i;
            //log.info("提交新任务 city:{}",cityName);
            CompletableFuture<WeatherResult> weather = taskWeatherService.getFutureWeatherForCompare(cityName);
//            log.info("城市轮询结束 city:{} result:{}",cityName,weather);
            futures.add(weather);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("所有任务完成");
        for (CompletableFuture<WeatherResult> future : futures) {
            WeatherResult weatherResult = future.get();
            log.info("城市轮询结束  result:{}",weatherResult.data);
        }

        stopWatch.stop();
        log.info("FastRetry测试总耗时  任务数:{} 耗时:{}",taskSize,stopWatch.getTotalTimeSeconds());
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int taskSize = 10;

        List<CompletableFuture<WeatherResult>> futures = new ArrayList<>();
        for (int i = 0; i < taskSize; i++) {
            WeatherService taskWeatherService = context.getBean(WeatherService.class);
            String cityName = "北京" + i;
            //log.info("提交新任务 city:{}",cityName);
            CompletableFuture<WeatherResult> weather = taskWeatherService.getFutureWeatherForCompare(cityName);
//            log.info("城市轮询结束 city:{} result:{}",cityName,weather);
            futures.add(weather);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("所有任务完成");
        for (CompletableFuture<WeatherResult> future : futures) {
            WeatherResult weatherResult = future.get();
            //log.info("城市轮询结束  result:{}",weatherResult.data);
        }

        stopWatch.stop();
        log.info("FastRetry测试总耗时  任务数:{} 耗时:{}",taskSize,stopWatch.getTotalTimeSeconds());
    }
}
