package l12.v5.clink.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import l12.v5.clink.core.Scheduler;

/**
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2018/12/9 17:39
 */
public class SchedulerImpl implements Scheduler {

    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService deliveryPool;

    public SchedulerImpl(int poolSize) {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(poolSize, new NameableThreadFactory("Scheduler-Thread-"));
        this.deliveryPool = Executors.newFixedThreadPool(4, new NameableThreadFactory("Delivery-Thread-"));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        return scheduledExecutorService.schedule(runnable, delay, unit);
    }

    @Override
    public void delivery(Runnable runnable) {
        deliveryPool.execute(runnable);
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdownNow();
        deliveryPool.shutdownNow();
    }

}