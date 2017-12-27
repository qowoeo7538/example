package org.shaw.core.task.support;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @see org.springframework.util.ConcurrencyThrottleSupport
 */
public abstract class ThrottleSupport {

    /** 当前线程数量 （{@link ThreadPoolExecutor#getActiveCount()} 返回值并不准确） */
    private AtomicInteger concurrencyCount = new AtomicInteger(0);

    /** 运行前 */
    public void beforeAccess(Runnable task) {
        beforeProcess(task, concurrencyCount.incrementAndGet());
    }

    /**
     * 运行前处理
     *
     * @param task             当前任务
     * @param concurrencyCount 当前任务数量
     */
    protected abstract void beforeProcess(Runnable task, int concurrencyCount);

    /** 运行后 */
    public void afterAccess() {
        int count = this.concurrencyCount.decrementAndGet();
        afterProcess(count);
    }

    /**
     * 运行后处理
     *
     * @param concurrencyCount 当前任务数量
     */
    protected abstract void afterProcess(int concurrencyCount);



    /**
     * 返回当前任务数量
     *
     * @return
     */
    public AtomicInteger getConcurrencyCount() {
        return concurrencyCount;
    }
}
