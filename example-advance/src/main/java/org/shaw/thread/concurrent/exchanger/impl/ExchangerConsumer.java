package org.shaw.thread.concurrent.exchanger.impl;

import org.shaw.base.thread.SecurityTask;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

/**
 * @create: 2017-11-16
 * @description:
 */
public class ExchangerConsumer extends SecurityTask {

    private Exchanger<Integer> exchanger;

    private static int data = 0;

    private volatile boolean isDone;

    public ExchangerConsumer(Exchanger<Integer> exchanger, Boolean isDone) {
        this.exchanger = exchanger;
        this.isDone = isDone;
    }

    @Override
    protected void runTask() {
        while (!Thread.interrupted() && !isDone) {
            data = 0;
            System.out.println("consumer change before : " + data);
            try {
                TimeUnit.SECONDS.sleep(1);
                data = exchanger.exchange(data);
                System.out.println("consumer change after : " + data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
