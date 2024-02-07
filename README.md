
# JAVA 高性能百万级任务重试框架 Fast-Retry
与主流的Spring-Retry, Guava-Retry等同步重试框架不同，Fast-Retry是一个异步重试框架，支持异步任务的重试、超时等待、回调。
而且基于内置的重试队列，Fast-Retry可以支持百万级任务的重试，
Spring-Retry, Guava-Retry无法支持大批量任务的重试，因为会占用过多线程资源导致大量任务在等待处理，随着任务数的增加，性能指数级降低，Fast-Retry的性能是前者的指数倍。

下图是三者的性能对比

- 测试线程池:  8个固定线程
- 单个任务逻辑:  轮询5次，隔2秒重试一次，总耗时10秒
- 未测预计公式：  当我们使用线程池的时候， 一般线程池中 总任务处理耗时 =  任务数/并发度 x 单个任务重试耗时


| 任务数  | FastRetry |    Spring-Retry     |     Guava-Retry     |
| :-----: | :-------: | :-----------------: | :-----------------: |
|    1    |   10秒    |        10秒         |        10秒         |
|   10    | 10.066秒  |      20.092秒       |      20.078秒       |
|   50    | 10.061秒  |      70.186秒       |      70.168秒       |
|   100   | 10.077秒  |      130.33秒       |      130.31秒       |
|   500   | 10.154秒  |      631.420秒      |      631.53秒       |
|  1000   | 10.237秒  |      1254.78秒      |      1256.28秒      |
|  5000   | 10.482秒  |  没测预计：6250秒   |  没测预计：6250秒   |
|  10000  | 10.686秒  |  没测预计：12520秒  |  没测预计：12520秒  |
| 100000  |  13.71秒  | 没测预计：125000秒  | 没测预计：125000秒  |
| 500000  |  28.89秒  | 没测预计：625000秒  | 没测预计：625000秒  |
| 1000000 |  58.05秒  | 没测预计：1250000秒 | 没测预计：1250000秒 |



可以看到即使是处理100万个任务，Fast-Retry的性能也比前者们处理在50个任务时的性能还要快的多的多，
这么快的秘密在于除了是异步，更重要是当别人在重试间隔里休息的时候，Fast-Retry还在不停忙命的工作着。

## 引入依赖
```xml
    <dependency>
        <groupId>com.github.burukeyou</groupId>
        <artifactId>fast-retry-core</artifactId>
        <version>0.0.1</version>
    </dependency>
```

# 快速开始

## 1、使用重试队列
```java
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        RetryQueue queue = new FastRetryQueue(executorService);
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
```

## 2、使用FastRetryBuilder

```java
        RetryResultPolicy<String> resultPolicy = result -> result.equals("444");
        FastRetryer<String> retryer = FastRetryBuilder.<String>builder()
                .attemptMaxTimes(3)
                .waitRetryTime(3, TimeUnit.SECONDS)
                .retryIfException(true)
                .retryIfExceptionOfType(TimeoutException.class)
                .exceptionRecover(true)
                .resultPolicy(resultPolicy)
                .build();

        CompletableFuture<String> future = retryer.submit(() -> {
            log.info("重试");
            //throw new Exception("test");
            //int i = 1/0;
            if (0 < 10){
                throw new TimeoutException("test");
            }
            return "444";
        });

        String o = future.get();
        log.info("结果{}", o);
```

## 3、使用注解
- 依赖Spring环境，所以需要在配置上加上@EnableFastRetry注解启用配置才生效
- 将结果类型使用CompletableFuture，自动进行异步轮询返回

下面定义等价于 FastRetryer.execute方法
```
    @FastRetry(retryWait = @RetryWait(delay = 2))
    public String retryTask(){
        return "success";
    }
``` 

下面定义等价于 FastRetryer.submit方法,支持异步轮询
```
    @FastRetry(retryWait = @RetryWait(delay = 2))
    public CompletableFuture<String> retryTask(){
        return CompletableFuture.completedFuture("success");
    }
```


