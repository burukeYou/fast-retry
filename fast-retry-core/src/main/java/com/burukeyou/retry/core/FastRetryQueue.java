package com.burukeyou.retry.core;


import com.burukeyou.retry.core.exceptions.FastRetryTimeOutException;
import com.burukeyou.retry.core.exceptions.RetryFutureInterruptedException;
import com.burukeyou.retry.core.exceptions.RetryPolicyCastException;
import com.burukeyou.retry.core.support.RetryQueueFuture;
import com.burukeyou.retry.core.task.DelayedTask;
import com.burukeyou.retry.core.task.RetryTask;
import com.burukeyou.retry.core.waiter.ExchangerWaiter;
import com.burukeyou.retry.core.waiter.Waiter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 *  Default implementation of retry queue
 * @author burukeyou
 */
@Slf4j
public class FastRetryQueue implements RetryQueue {

    private final Waiter waiter = new ExchangerWaiter();

    private final BlockingQueue<QueueTask> retryTaskQueue = new LinkedTransferQueue<>();

    private final ExecutorService pool;

    private  final DelayQueue<ReQueueDelayedTask> delayQueue = new DelayQueue<>();

    private final Map<String,CompletableFuture<Object>> futureMap = new ConcurrentHashMap<>();

    public FastRetryQueue(ExecutorService pool){
        this.pool = pool;
        start();
    }

    public FastRetryQueue(int corePoolSize) {
        this(Executors.newScheduledThreadPool(corePoolSize));
    }

    private void start() {
        new Thread(() -> {
            for(;;){
                try {
                    delayQueue.take().run();
                } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                }
            }
        }).start();

        new Thread(() -> {
            for(;;){
                final QueueTask retryTask;
                try {
                    retryTask = retryTaskQueue.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    continue;
                }

                pool.submit(() -> {
                    try {
                        consumer(retryTask);
                    } catch (Exception e) {
                        throw new IllegalStateException("retry queue consumer process exception ",e);
                    }
                });
            }
        }).start();
    }

    private void consumer(QueueTask retryTask){
        String taskId = retryTask.getTaskId();
        RetryTask<?> task = retryTask.getTask();
        boolean exceptionRecover = task.exceptionRecover();
        boolean retry = retryTask.isRetry();

        if (retry){
            Runnable runnable = () -> retryTaskQueue.add(retryTask);
            delayQueue.put(new ReQueueDelayedTask(runnable,taskId, task.waitRetryTime(), TimeUnit.MILLISECONDS));
            return;
        }

        CompletableFuture<Object> completableFuture = futureMap.remove(taskId);
        if (completableFuture == null){
            return;
        }

        Object result = retryTask.getTask().getResult();
        Exception retryTaskException = retryTask.getLastException();
        if (retryTaskException == null){
            completableFuture.complete(result);
            return;
        }

        if (exceptionRecover){
            log.info("",retryTaskException);
            completableFuture.complete(null);
        }else {
            completableFuture.completeExceptionally(retryTask.getLastException());
        }

    }


    @Override
    public <R> CompletableFuture<R> submit(RetryTask<R> retryTask) {
        String taskId = getTaskId();
        QueueTask queueTask = new QueueTask(taskId, (RetryTask<Object>)retryTask);
        retryTaskQueue.add(queueTask);
        CompletableFuture<Object> future = new RetryQueueFuture<>(queueTask);
        futureMap.put(taskId,future);
        return (CompletableFuture<R>)future;
    }

    private String getTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public <R> R execute(RetryTask<R> retryTask){
        try {
            return submit(retryTask).get();
        } catch (InterruptedException e) {
            throw new RetryFutureInterruptedException("Thread interrupted while future get ",e);
        } catch (ExecutionException e) {
            if(e.getCause() instanceof RuntimeException){
                throw (RuntimeException)e.getCause();
            }else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public <R> R execute(RetryTask<R> retryTask, long timeout, TimeUnit timeUnit) throws TimeoutException {
        try {
            return submit(retryTask).get(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new RetryFutureInterruptedException("Thread interrupted while future get ",e);
        } catch (ExecutionException e) {
            if(e.getCause() instanceof RuntimeException){
                throw (RuntimeException)e.getCause();
            }else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Data
    public static class QueueTask {
        private String taskId;
        private RetryTask<Object> task;

        private boolean isStop = false;

        private Integer count = 0;

        private Exception lastException;

        public QueueTask(String taskId, RetryTask<Object> task) {
            this.taskId = taskId;
            this.task = task;
        }

        public boolean isRetry(){
            // reset lastException
            lastException = null;
            if (isStop){
                return false;
            }
            int maxTimes = task.attemptMaxTimes();
            if (maxTimes > 0 && count > maxTimes){
                lastException =  new FastRetryTimeOutException("The maximum retry count has been exceeded after"+maxTimes+" times. Stop retry");
                return false;
            }

            this.count = this.count + 1;
            try {
                return task.retry();
            }catch (RetryPolicyCastException e){
                // not retry
                lastException = e;
                return false;
            } catch (Exception e) {
                lastException = e;
                log.info("",e);
                if (!task.retryIfException()){
                    return false;
                }
                List<Class<? extends Exception>> exceptionTypeList = task.retryIfExceptionByType();
                if (CollectionUtils.isEmpty(exceptionTypeList)){
                    return true;
                }

                for (Class<? extends Throwable> aClass : task.retryIfExceptionByType()) {
                    if (aClass.isAssignableFrom(e.getClass())){
                        return true;
                    }
                }
                return false;
            }
        }

        public void stopRetry() {
            this.isStop = true;
        }
    }


     static class ReQueueDelayedTask extends DelayedTask {

        private final Runnable task;

        ReQueueDelayedTask(Runnable task,String name, long delay, TimeUnit unit) {
            super(name, delay, unit);
            this.task = task;
        }

        public void run(){
            this.task.run();
        }
    }
}
