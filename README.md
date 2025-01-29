[![License](http://img.shields.io/badge/license-apache%202-brightgreen.svg)](https://github.com/burukeYou/fast-retry/blob/main/LICENSE)

# 0、What is this?

Fast-Retry是一个高性能任务重试框架，只需几个线程就可以支持到百万级别任务的并发重试处理。
与主流的Spring-Retry, Guava-Retry等同步重试框架不同，Fast-Retry是一个支持异步重试框架，支持异步任务的重试、超时等待、回调，以及各种重试策略。
Spring-Retry,
Guava-Retry均无法支持大批量任务的重试，因为每一个重试任务是一个长阻塞任务，会占用过多线程资源导致大量任务在等待处理，随着任务数的增加，系统吞吐量大大降低，性能指数级降低，Fast-Retry的性能是前者的指数倍。

下图是三者的性能对比

- 测试线程池:  8个固定线程
- 单个任务逻辑:  轮询5次，隔2秒重试一次，总耗时10秒
- 未测预计公式： 当我们使用线程池的时候， 一般线程池中 总任务处理耗时 = 任务数/吞吐量 x 单个任务重试耗时

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

可以看到即使是处理100万个任务，Fast-Retry的性能也比Spring-Retry和Guava-Retry处理在50个任务时的性能还要快的多的多，
这么快的秘密在于除了是异步，还构建了对于批任务处理的重试队列以及对线程池的改造，当别人还在重试间隔里休息的时候，Fast-Retry还在不停忙命的工作着。

# 1、特性
- **高吞吐量**：  支持百万级并发的重试任务处理
- **低线程**：只需几个线程就可以支撑百万级的吞吐量。 
  - 对于一般的同步重试框架他们的吞吐量就等于线程数， 1000个吞吐量就需要1000个线程
- **支持编程式、声明式使用**
- **支持多种重试策略及使用方式**




# 2、快速开始

下面介绍如何去利用FastRetry去构建我们的重试任务

## 2.1、编程式使用 （JDK环境）

引入依赖

```xml

<dependency>
    <groupId>io.github.burukeyou</groupId>
    <artifactId>fast-retry-core</artifactId>
    <version>0.3.1</version>
</dependency>
```

然后使用FastRetryBuilder去构建我们的重试任务

```java
        FastResultPolicy<String> resultPolicy=result->result.equals("444");
        FastRetryer<String> retryer=FastRetryBuilder.<String>builder()
        .attemptMaxTimes(3)  // 指定最大重试次数
        .waitRetryTime(3,TimeUnit.SECONDS) // 指定下一次重试间隔时间
        .retryIfExceptionOfType(TimeoutException.class) // 指定，当发生指定异常TimeoutException才进行重试
        .retryPolicy(resultPolicy)   // 指定当结果为444是就进行重试
        .build();

        CompletableFuture<String> future=retryer.submit(()->{
        log.info("重试");
        if(0< 10){
        throw new TimeoutException("test");
        }
        return"444";
        });

        String o=future.get();
        log.info("结果{}",o);
```

## 2.1、声明式使用(Spring环境)

1） 引入依赖：
- 该依赖已包含fast-retry-core依赖， 所以也可以使用编程式FastRetry
```xml

<dependency>
    <groupId>io.github.burukeyou</groupId>
    <artifactId>fast-retry-spring</artifactId>
    <version>0.3.1</version>
</dependency>
```

如果不是Spring环境中，还需要手动依赖spring-context进来， 否则请忽略

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.2.0.RELEASE</version>
</dependency>
```

2） 使用@EnableFastRetry注解在Spring配置类中开启重试功能
比如
```java
@EnableFastRetry
public class Main {
    
}
```

3) 在Spring的Bean中为具体的方法配置重试逻辑

比如下面配置了当发生异常时进行重试， 最大重试3次， 每隔1000毫秒后重试. 
```java

@Component
public class UserService {
    
    @FastRetry(delay = 1000, maxAttempts=3)
    public void retryTask(){
        int count = 3 / 10;
    }
}

```

上面的方法默认是异步执行的， 如果需要同步执行，需要将方法返回值定义成非void类型 或者 非Future类型即可

```java

@Component
public class UserService {
    
    @FastRetry(delay = 1000, maxAttempts=3)
    public Integer retryTask(){
        int count = 3 / 10;
        return count;
    }
}

```

当然最重要就是异步重试了，可以将方法返回值用 CompletableFuture 进行包装，会自动进行异步轮询返回， 最大化提高吞吐量， 然后就可以使用CompletableFuture的whenComplete或者get方法拿到异步任务的结果了
- 注意你在这里用CompletableFuture 进行包装返回利用FastRetry的特性，跟你自己直接 CompletableFuture.runAsync 进行异步是完全不一样的，

```java

@Component
public class UserService {
    
    @FastRetry(delay = 1000, maxAttempts=3)
    public CompletableFuture<String> retryTask(){
        int count = 3 / 10;
        // 使用 CompletableFuture.completedFuture 方法将结果进行包装返回即可
        return  CompletableFuture.completedFuture(count);
    }
} 
```

# 3、FastRetry更多功能

其他更多高级的功能特性见[文档](https://github.com/burukeYou/fast-retry/wiki/%E6%96%87%E6%A1%A3)


# 4、赞赏
-------

纯个人维护，为爱发电， 如果有任何问题或者需求请提issue，会很快修复和发版

开源不易，目前待业中，如果觉得有用可以微信扫码鼓励支持下作者感谢!🙏


 <img src="docs/img/weChatShare.png" width = 200 height = 200 />