package com.burukeyou.retry.core.support;


import java.util.concurrent.*;

/**
 * Prioritize creating the maximum number of threads instead of putting them in the queue first
 *
 */
public class FastRetryThreadPool extends ThreadPoolExecutor {

    private static final RejectedExecutionHandler rejectedExecutionHandler = new AbortPolicy();

    public FastRetryThreadPool(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, null, new FastRetryThreadFactory("spring"), rejectedExecutionHandler);
    }

    public FastRetryThreadPool(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, null, threadFactory, rejectedExecutionHandler);
    }

    public FastRetryThreadPool(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               Integer workQueueSize) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueueSize, new FastRetryThreadFactory("spring"), rejectedExecutionHandler);
    }


    public FastRetryThreadPool(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               Integer workQueueSize,
                               ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueueSize, threadFactory, rejectedExecutionHandler);
    }


    public FastRetryThreadPool(int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               Integer workQueueSize,
                               ThreadFactory threadFactory,
                               RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new FastTaskQueue<>(workQueueSize), threadFactory, handler);
        ((FastTaskQueue<?>)this.getQueue()).setExecutor(this);
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }

        try {
            super.execute(command);
        } catch (RejectedExecutionException rx) {
            // retry to offer the task into queue.
            final FastTaskQueue<?> queue = (FastTaskQueue<?>) super.getQueue();
            try {
                if (!queue.retryOffer(command, 0, TimeUnit.MILLISECONDS)) {
                    throw new RejectedExecutionException("Queue capacity is full.", rx);
                }
            } catch (InterruptedException x) {
                throw new RejectedExecutionException(x);
            }
        }
    }


    private static class FastTaskQueue<R extends Runnable> extends LinkedBlockingQueue<Runnable> {

        private static final long serialVersionUID = -2635853580887179627L;

        private FastRetryThreadPool executor;

        public FastTaskQueue(Integer capacity) {
            super(capacity == null ? Integer.MAX_VALUE : capacity);
        }

        public void setExecutor(FastRetryThreadPool exec) {
            executor = exec;
        }

        @Override
        public boolean offer(Runnable runnable) {
            if (executor == null) {
                throw new RejectedExecutionException("The task queue does not have executor!");
            }

            int currentPoolThreadSize = executor.getPoolSize();
            // have free worker. put task into queue to let the worker deal with task.
            if (executor.getActiveCount() < currentPoolThreadSize) {
                return super.offer(runnable);
            }

            // return false to let executor create new worker.
            if (currentPoolThreadSize < executor.getMaximumPoolSize()) {
                return false;
            }

            // currentPoolThreadSize >= max
            return super.offer(runnable);
        }

        /**
         * retry offer task
         *
         * @param o task
         * @return offer success or not
         * @throws RejectedExecutionException if executor is terminated.
         */
        public boolean retryOffer(Runnable o, long timeout, TimeUnit unit) throws InterruptedException {
            if (executor.isShutdown()) {
                throw new RejectedExecutionException("Executor is shutdown!");
            }
            return super.offer(o, timeout, unit);
        }
    }

}
