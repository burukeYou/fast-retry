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
        RetryResultPolicy<String> resultPolicy=result->result.equals("444");
        FastRetryer<String> retryer=FastRetryBuilder.<String>builder()
        .attemptMaxTimes(3)
        .waitRetryTime(3,TimeUnit.SECONDS)
        .retryIfException(true)
        .retryIfExceptionOfType(TimeoutException.class)
        .exceptionRecover(true)
        .retryPolicy(resultPolicy)
        .build();

        CompletableFuture<String> future=retryer.submit(()->{
        log.info("重试");
        //throw new Exception("test");
        //int i = 1/0;
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

# 3、FastRetry功能特性

## 3.1 只对指定异常进行重试
默认FastRetry会对发生的所有异常进行重试， 可以配置只有当发生指定的异常的时候才进行重试

```java
   // 配置发生超时异常 和 IO异常时才进行重试
   @FastRetry(include={TimeoutException.class,IOException.class})
   Object getA(){
        
    }   
```

## 3.2 只对非指定异常进行重试
默认FastRetry会对发生的所有异常进行重试， 可以配置只有当发生非指定的异常的时候才进行重试

```java
   // 配置除了TimeoutException异常之外才进行重试
   @FastRetry(exclude=TimeoutException.class)
   Object getA(){
        
    }   
```

## 3.3 方法拦截器配置
可以配置在方法调用前或者调用执行指定的逻辑

1) 实现FastRetryInterceptor接口自定义方法拦截器
- `methodInvokeBefore`：  方法调用之前回调
- `methodInvokeAfter`： 方法调用之后回调， 无论成功还是失败都会回调此方法， 如果执行失败（就是抛出异常）就会将异常信息回调到Throwable参数，如果执行成功就会将方法结果回调到这里result参数

```java
    public class MyFastRetryInterceptor implements FastRetryInterceptor {

        // 方法调用之前回调
        @Override
        public void methodInvokeBefore(FastRetryInvocation methodInvocation) {
            log.info("开始之前： method:{} count:{} ", methodInvocation.getMethodAbsoluteName(),methodInvocation.getCurExecuteCount());
        }

        // 方法调用之后回调： 
        @Override
        public Object methodInvokeAfter(Object result, Throwable throwable, FastRetryInvocation methodInvocation) {
            if (throwable != null){
                // 发生异常
                return result;
            }
            
            log.info("结束： method:{} 实际执行重试次数:{} ", methodInvocation.getMethodAbsoluteName(),methodInvocation.getCurExecuteCount());
            return result;
        }
    }
    
    
```

2） 配置到FastRetry注解上

这里配置的MyFastRetryInterceptor如果是一个SpringBean 就会Spring容器获取， 如果不是就会手动New出来， 请确保无参构造函数存在
```java
@FastRetry(interceptor = MyFastRetryInterceptor.class)
```


## 3.4 重试策略（FastResultPolicy） - 根据返回结果判断重试
如果你需要根据返回结果的值进行重试，可以实现自定义的重试逻辑

1） 实现FastResultPolicy接口实现自定义的结果重试策略
- 该逻辑表示当UserInfo的id为空是就进行重试
```java
    public static class MyPolicy1 implements FastResultPolicy<UserInfo> {
    
        @Override
        public boolean canRetry(UserInfo userInfo) {
            return userInfo.getId() == null;
        }
    }
```

2） 配置重试策略Policy 到 FastRetry注解注解上
```java
@FastRetry(policy = MyPolicy1.class)
```

## 3.5 重试策略（FastInterceptorPolicy）-全链路判断重试
除了可以配置 在 3.4 的重试策略也可以配置更加细粒度的重试策略，即FastInterceptorPolicy， 它提供了重试之前，重试成功、重试失败后控制

1） 实现FastInterceptorPolicy接口
- beforeExecute： 在每次重试调用之前回调，`如果返回false就不会继续执行`
- afterExecuteFail:  每次重试调用后发生异常后回调， `如果返回false就直接退出重试`， 如果抛出异常或者返回true就会继续重试
- afterExecuteSuccess： 每次重试调用成功后回调，`如果返回false就直接退出重试`

```java
    public static class MyPolicy2 implements FastInterceptorPolicy<UserInfo> {
        // 流量计数
        private static Integer executeCount = 0;
    
        @Override
        public boolean beforeExecute(FastRetryInvocation invocation) throws Exception {
            executeCount++;
            return FastInterceptorPolicy.super.beforeExecute(invocation);
        }
    
        @Override
        public boolean afterExecuteFail(Exception exception, FastRetryInvocation invocation) throws Exception {
            log.info("记录异常 method:{} 当前执行次数：{}",invocation.getMethodAbsoluteName(), invocation.getCurExecuteCount(),exception);
            throw exception;
        }
    
        @Override
        public boolean afterExecuteSuccess(UserInfo userInfo, FastRetryInvocation invocation) {
            // 当流量小于 5 或者 用户id为空就继续进行重试
            if (executeCount <= 5 || userInfo.getId() == null){
                return true;
            }
            return false;
        }
    }
```

2） 配置重试策略Policy 到 FastRetry注解注解上
```java
@FastRetry(policy = MyPolicy2.class)
```

## 3.6 重试策略（Policy）的多样化配置方式

在3.4、3.5 章节中我们都是把重试策略配置到 @FastRetry注解的policy参数上，下面介绍其他配置方式

## 3.6.1 配置到具体方法参数上
只需在任意的一个方法参数上声明一个 Policy 即可，然后在具体调用时传递子类逻辑

```java
@Component
public class UserService {
    @FastRetry(delay = 5000, maxAttempts = 15)
    public UserInfo getUser(String cityName, FastResultPolicy<UserInfo> policy) {
        return new UserInfo();
    }
}
```

调用时, 传递具体重试逻辑
```java
    @Autowired
    private UserService userService;

    public void main(){
        userService.getUser("广东", userInfo -> userInfo.getId() > 5);
    }
```

## 3.6.1 配置到具体方法返回值上
可以将方法返回值定义成FastRetryFuture， 然后使用 retryWhen方法在里面配置重试策略Policy

```java
    @FastRetry(delay = 1000,maxAttempts = 10)
    public FastRetryFuture<UserInfo> getUser(String cityName){
        UserInfo user = new UserInfo()
        user.setId(new Random().nextInt(40););
        return FastRetryFuture
                .completedFuture(user) // 将返回直接包装返回
                .retryWhen(user -> user.getId() > 20); // 当用户Id大于20就进行重试 
    }
```


# 4、赞赏
-------

纯个人维护，为爱发电， 如果有任何问题或者需求请提issue，会很快修复和发版

开源不易，目前待业中，如果觉得有用可以微信扫码鼓励支持下作者感谢!🙏


 <img src="docs/img/weChatShare.png" width = 200 height = 200 />