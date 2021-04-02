package org.lucas.example.foundation.thread.kata.safe;

import org.junit.jupiter.api.Test;
import org.lucas.example.foundation.core.task.ExampleThreadExecutor;
import org.lucas.example.foundation.thread.kata.safe.support.ReentrantLockThread;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入锁实践
 */
public class ReentrantLockKata {

    private final ReentrantLock lock = new ReentrantLock();

    @Test
    public void reentrantLockTest() {
        for (int i = 0; i < 10; i++) {
            ExampleThreadExecutor.execute(new ReentrantLockThread(lock));
        }
        ExampleThreadExecutor.destroy();
    }

}
