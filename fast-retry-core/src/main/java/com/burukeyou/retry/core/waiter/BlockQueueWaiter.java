package com.burukeyou.retry.core.waiter;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class BlockQueueWaiter implements Waiter {

    public static final Map<String, LinkedTransferQueue<QueueResult>> lockMap = new ConcurrentHashMap<>();


    @Override
    public <R> R wait(String seq) {
        LinkedTransferQueue<QueueResult> queue = new LinkedTransferQueue<>();
        QueueResult data = null;
        try {
            lockMap.put(seq,queue);
            data = queue.take();
        } catch (InterruptedException e) {
           throw new RuntimeException(e);
        }finally {
            lockMap.remove(seq);
        }
        return (R)data.getData();
    }

    @Override
    public <R> R wait(String seq, long timeout, TimeUnit unit) throws TimeoutException{
        LinkedTransferQueue<QueueResult> queue = new LinkedTransferQueue<>();

        QueueResult data = null;
        try {
            lockMap.put(seq,queue);
            data = queue.poll(timeout,unit); // 不会抛超时异常
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            lockMap.remove(seq);
        }

        if (data == null){
            throw new TimeoutException(seq + "任务超时");
        }

        return (R)data.getData();
    }

    @Override
    public <T> void notify(String seq, T data) {
        LinkedTransferQueue<QueueResult> queue = lockMap.remove(seq);
        if (queue == null){
            return;
        }

        boolean b = queue.tryTransfer(new QueueResult(data));
       /* if (b){
            log.info("唤醒成功 seqNo:{}",seq);
        }else {
            log.info("唤醒失败 seqNo:{}",seq);
        }*/
    }

    public static void main(String[] args) throws TimeoutException {
        BlockQueueWaiter waiter = new BlockQueueWaiter();

        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            waiter.notify("3","444");
        }).start();

        //waiter.wait("3");
        String wait = waiter.wait("3", 10, TimeUnit.SECONDS);
        //waiter.notify("3","1");
        System.out.println(2222222 + "--- " + wait);
    }


    @Data
    private static class QueueResult {
        private Object data;

        public QueueResult(Object data) {
            this.data = data;
        }

        public Object getData() {
            return data;
        }
    }
}
