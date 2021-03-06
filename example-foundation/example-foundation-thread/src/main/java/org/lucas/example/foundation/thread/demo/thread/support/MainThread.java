package org.lucas.example.foundation.thread.demo.thread.support;

/**
 * @create: 2018-01-23
 * @description:
 */
public class MainThread extends Thread {

    public String fileName;

    public volatile boolean keepRunning = true;

    public MainThread(String fileName){
        this.fileName = fileName;
    }

    @Override
    public void run() {
        System.out.println("进入主线程"+Thread.currentThread().getId());
        DaemonThread daemon = new DaemonThread(fileName);
        Thread daemonThread = new Thread(daemon);
        daemonThread.setDaemon(true);
        daemonThread.start();
        while (keepRunning){
            Thread.yield();
        }
        System.out.println("退出线程"+Thread.currentThread().getId());
    }
}
