package com.burukeyou.retry.core.task;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author caizhihao
 */
public class DelayedTask implements Delayed {
    private final String name;
    private final long delay;
    private final long expireTime;

    public DelayedTask(String name, long delay, TimeUnit unit) {
        this.name = name;
        this.delay = unit.toMillis(delay);
        this.expireTime = System.currentTimeMillis() + this.delay;
    }

    String getName() {
        return name;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(expireTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }


    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.expireTime, ((DelayedTask) other).expireTime);
    }
}