package com.burukeyou.retry.spring.core.retrytask;

import lombok.Getter;

@Getter
public class RetryCounter {

    private long curExecuteCount = 0;

    public void incrementActualExecuteCount(){
        this.curExecuteCount = this.curExecuteCount + 1;
    }

}
