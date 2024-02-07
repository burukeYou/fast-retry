package com.burukeyou.demo.retry.data;

import com.burukeyou.retry.core.task.RetryTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 *  自定义重试任务
 *
 */
@Slf4j
@Data
public class MyRetryTask implements RetryTask<String> {
    private String taskName;

    private int i = 0;


    public MyRetryTask(String taskName) {
        this.taskName = taskName;
    }


    @Override
    public long waitRetryTime() {
        return 2000;
    }


    @Override
    public boolean retry() {
        log.info("MyRetryTask-任务开始重试 taskName:{} i:{}",taskName,i);
        try {
            //Thread.sleep(500);
            //int i = 3 / 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ++i < 5;
    }

    @Override
    public String getResult() {
        return taskName + "_" + i + "_哈哈结果";
    }
}
