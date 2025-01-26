package com.burukeyou.retry.core;


import com.burukeyou.retry.core.exceptions.FastRetryTimeOutException;
import com.burukeyou.retry.core.exceptions.RetryFutureInterruptedException;
import com.burukeyou.retry.core.exceptions.RetryPolicyCastException;
import com.burukeyou.retry.core.support.FastRetryThreadPool;
import com.burukeyou.retry.core.support.RetryQueueFuture;
import com.burukeyou.retry.core.task.DelayedTask;
import com.burukeyou.retry.core.task.RetryTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Default implementation of retry queue
 *
 * @author caizhihao
 */
@Slf4j
public class FastRetryQueue implements RetryQueue {

    private final BlockingQueue<QueueTask> retryTaskQueue = new LinkedTransferQueue<>();

    private final ExecutorService pool;

    private final DelayQueue<ReQueueDelayedTask> delayQueue = new DelayQueue<>();

    private final Map<String, CompletableFuture<Object>> futureMap = new ConcurrentHashMap<>();

    public FastRetryQueue(ExecutorService pool) {
        this.pool = pool;
        start();
    }

    public FastRetryQueue(int corePoolSize) {
        this(new FastRetryThreadPool(corePoolSize,corePoolSize * 4,60, TimeUnit.SECONDS));
    }


    private void start() {
        new Thread(() -> {
            for (; ; ) {
                try {
                    delayQueue.take().run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        },"fastRetry-thread-delayQueue").start();

        new Thread(() -> {
            for (; ; ) {
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
                        throw new IllegalStateException("retry queue consumer process exception ", e);
                    }
                });
            }
        },"fastRetry-thread-retryTaskQueue").start();
    }

    private void consumer(QueueTask retryTask) {
        String taskId = retryTask.getTaskId();
        RetryTask<?> task = retryTask.getTask();
        boolean exceptionRecover = task.exceptionRecover();
        boolean retry = retryTask.isRetry();

        if (retry) {
            Runnable runnable = () -> retryTaskQueue.add(retryTask);
            delayQueue.put(new ReQueueDelayedTask(runnable, taskId, task.waitRetryTime(), TimeUnit.MILLISECONDS));
            return;
        }

        Exception retryTaskException = retryTask.getLastException();

        CompletableFuture<Object> completableFuture = futureMap.remove(taskId);
        if (completableFuture == null) {
            log.error("[fast-retry-queue] can not find queueTask by id:{} retryTaskClass:{}", taskId, retryTask.getTask().getClass().getName());
            return;
        }

        retryTask.getTask().retryAfter(retryTaskException);
        if (retryTaskException == null) {
            completableFuture.complete(retryTask.getTask().getResult());
            return;
        }

        if (exceptionRecover) {
            log.info("[fast-retry-queue] exception recover ", retryTaskException);
            completableFuture.complete(retryTask.getTask().getResult());
        } else {
            completableFuture.completeExceptionally(retryTask.getLastException());
        }

    }


    @Override
    public <R> CompletableFuture<R> submit(RetryTask<R> retryTask) {
        String taskId = getTaskId();
        QueueTask queueTask = new QueueTask(taskId, (RetryTask<Object>) retryTask);
        CompletableFuture<Object> future = new RetryQueueFuture<>(queueTask);
        futureMap.put(taskId, future);
        retryTask.retryBefore();
        retryTaskQueue.add(queueTask);
        return (CompletableFuture<R>) future;
    }

    private String getTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public <R> R execute(RetryTask<R> retryTask) {
        try {
            return submit(retryTask).get();
        } catch (InterruptedException e) {
            throw new RetryFutureInterruptedException("Thread interrupted while future get ", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public <R> R execute(RetryTask<R> retryTask, long timeout, TimeUnit timeUnit) throws TimeoutException {
        try {
            return submit(retryTask).get(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new RetryFutureInterruptedException("Thread interrupted while future get ", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Data
    public static class QueueTask {
        private String taskId;
        private RetryTask<Object> task;

        private boolean isStop = false;

        private long count = 0;

        private Exception lastException;

        public QueueTask(String taskId, RetryTask<Object> task) {
            this.taskId = taskId;
            this.task = task;
        }

        public boolean isRetry() {
            // reset lastException
            if (isStop) {
                return false;
            }
            long maxTimes = task.attemptMaxTimes();
            if (maxTimes >= 0 && count > maxTimes) {
                lastException = new FastRetryTimeOutException("The maximum retry count has been exceeded after " + maxTimes + " times. Stop retry",lastException);
                return false;
            }else {
                lastException = null;
            }

            this.count = this.count + 1;
            try {
                return task.retry(count);
            } catch (RetryPolicyCastException e) {
                // not retry
                lastException = e;
                return false;
            } catch (Exception e) {
                lastException = e;
                if (task.printExceptionLog()) {
                    log.info("[fast-retry-queue] 重试任务发生异常准备重试，当前执行次数[{}]", count-1,e);
                }
                if (maxTimes == 0){
                    // not need retry
                    return false;
                }

                if (!task.retryIfException()) {
                    return false;
                }

                // not retry
                if (isContainException(task.exclude(), lastException)) {
                    return false;
                }

                if (task.include() == null || task.include().isEmpty()) {
                    // not config include exception ,retry all exception
                    return true;
                }

                return isContainException(task.include(), lastException);
            }
        }

        public void stopRetry() {
            this.isStop = true;
        }

        public boolean isContainException(List<Class<? extends Exception>> excludeExceptionList, Exception e) {
            if (excludeExceptionList == null || excludeExceptionList.isEmpty()) {
                return false;
            }

            for (Class<? extends Throwable> ex : excludeExceptionList) {
                if (ex.isAssignableFrom(e.getClass())) {
                    return true;
                }
            }
            return false;
        }
    }


    static class ReQueueDelayedTask extends DelayedTask {

        private final Runnable task;

        ReQueueDelayedTask(Runnable task, String name, long delay, TimeUnit unit) {
            super(name, delay, unit);
            this.task = task;
        }

        public void run() {
            this.task.run();
        }
    }
}
