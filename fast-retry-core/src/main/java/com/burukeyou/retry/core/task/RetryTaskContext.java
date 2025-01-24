package com.burukeyou.retry.core.task;

import com.burukeyou.retry.core.policy.RetryResultPolicy;
import lombok.Data;

import java.util.List;

@Data
public class RetryTaskContext {

    private Integer attemptMaxTimes;
    private Long waitRetryTime;
    private Boolean retryIfException;
    private Boolean exceptionRecover;
    private RetryResultPolicy<Object> resultPolicy;
    private List<Class<? extends Exception>> exceptionsType;
    private List<Class<? extends Exception>> excludeExceptionsType;
}
