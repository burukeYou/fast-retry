package com.burukeyou.retry.demo;

import com.burukeyou.retry.demo.data.MyRetryTask;
import com.burukeyou.retry.core.FastRetryQueue;
import com.burukeyou.retry.core.RetryQueue;
import com.burukeyou.retry.core.task.RetryTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RetryQueueTest  {

    @Test
    public void testExecute1111() throws Exception {
        //ExecutorService executorService = Executors.newFixedThreadPool(3);
        RetryQueue queue = new FastRetryQueue(8);
        RetryTask<String> task = new RetryTask<String>() {
            int result = 0 ;
            @Override
            public long waitRetryTime() {
                return 2000;
            }

            @Override
            public boolean retry() {
                return ++result < 5;
            }

            @Override
            public String getResult() {
                return  result + "";
            }
        };
        CompletableFuture<String> future = queue.submit(task);
        log.info("任务结束 结果:{}",future.get());
    }

    /**
     * 测试执行任务
     * @throws Exception
     */
    @Test
    public void testExecute() throws Exception {
        //ExecutorService executorService = Executors.newFixedThreadPool(3);
        RetryQueue queue = new FastRetryQueue(8);

        MyRetryTask task = new MyRetryTask("北京");
        Object execute = queue.execute(task);
        log.info("任务结束 结果:{}",execute);

        System.out.println("testExecute");
    }

    /**
     * 测试超时执行任务
     * @throws Exception
     */
    @Test
    public void testExecuteTimeOut() throws Exception {
        //ExecutorService executorService = Executors.newFixedThreadPool(3);
        RetryQueue queue = new FastRetryQueue(8);

        MyRetryTask task = new MyRetryTask("北京");
        Object execute = queue.execute(task,3, TimeUnit.SECONDS);
        log.info("任务结束 结果:{}",execute);

        System.out.println("testExecuteTimeOut");
    }

    /**
     * 测试提交任务-等待get
     * @throws Exception
     */
    @Test
    public void testSubmitGet() throws Exception {
        //ExecutorService executorService = Executors.newFixedThreadPool(3);
        RetryQueue queue = new FastRetryQueue(8);

        MyRetryTask task = new MyRetryTask("北京");
        CompletableFuture<String> future = queue.submit(task);

        //Object execute = future.get();
        Object execute = future.get(3, TimeUnit.SECONDS);
        log.info("任务结束 结果:{}",execute);

        System.out.println("testSubmitGet");
        Thread.currentThread().join();
    }

    /**
     * 测试提交任务-回调
     * @throws Exception
     */
    @Test
    public void testSubmitCallback() throws Exception {
        //ExecutorService executorService = Executors.newFixedThreadPool(3);
        RetryQueue queue = new FastRetryQueue(8);

        MyRetryTask task = new MyRetryTask("北京");
        CompletableFuture<String> future = queue.submit(task);

        future.whenComplete((result, throwable) -> {
            log.info("任务回调结束 结果:{}",result);
        });

        System.out.println("testSubmitCallback");
        Thread.currentThread().join();
    }

    /**
     *  测试提交多任务
     */
    @Test
    public void testSubmitManyTask() throws Exception {
        //ExecutorService executorService = Executors.newFixedThreadPool(3);
        RetryQueue queue = new FastRetryQueue(8);
        List<CompletableFuture<String>> allList = new CopyOnWriteArrayList<>();

        StopWatch watch = new StopWatch();
        watch.start();

        int taskSize = 10;
        for (int i = 0; i < taskSize; i++) {
            CompletableFuture<String> testFuture = new CompletableFuture<>();
            allList.add(testFuture);
            CompletableFuture<String> future = queue.submit( new MyRetryTask("北京" + i));
            future.whenComplete((result, throwable) -> {
                log.info("任务回调结束 结果:{}",result);
                testFuture.complete(result);
            });
        }

        CompletableFuture.allOf(allList.toArray(new CompletableFuture[0])).join();
        System.out.println("所有任务执行结束");

        watch.stop();
        log.info("耗时(秒):{}",watch.getTotalTimeMillis()/1000);
        Thread.currentThread().join();
    }

    @Test
    public void testSubmitManyTask2() throws Exception {
        //ExecutorService executorService = Executors.newFixedThreadPool(3);
        RetryQueue queue = new FastRetryQueue(8);
        List<CompletableFuture<String>> allList = new CopyOnWriteArrayList<>();

        StopWatch watch = new StopWatch();
        watch.start();
        int taskSize = 10;
        for (int i = 0; i < taskSize; i++) {
            CompletableFuture<String> future = queue.submit(new MyRetryTask("北京" + i));
            allList.add(future);
        }

        CompletableFuture.allOf(allList.toArray(new CompletableFuture[0])).join();
        for (CompletableFuture<String> future : allList) {
            log.info("任务结果:{}",future.get());
        }
        System.out.println("所有任务执行结束");
        watch.stop();
        log.info("耗时(秒):{}",watch.getTotalTimeMillis()/1000);
        Thread.currentThread().join();
    }

}
