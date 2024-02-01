package com.only.thread.pool;

import com.only.thread.pool.policy.DefaultPolicyHandler;
import com.only.thread.pool.policy.PolicyHandler;

import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class OnlyThreadPoolExecutor implements OnlyExecutorService {


    /**
     * 默认队列大小
     */
    private static final int defaultQueueSize = 5;

    /**
     * 默认线程池大小
     */
    private static final int defaultPoolSize = 5;

    private static final long defaultAliveTime = 60L;

    /**
     * 线程池最大的大小
     */
    private static final int maxPoolSize = 50;

    /**
     * 线程池大小
     */
    private volatile int poolSize;

    /**
     * 任务数量
     */
    private long completedTaskCount;


    /**
     * 拒绝策略
     */
    private volatile PolicyHandler handler;

    /**
     * 是否已经中断
     */
    private volatile boolean isShutDown = false;

    /**
     * 当前激活线程数
     */
    private AtomicInteger ctl = new AtomicInteger();

    /**
     * 对列
     */
    private final BlockingQueue<Runnable> workQueue;

    /**
     * Lock
     */
    private final ReentrantLock mainLock = new ReentrantLock();
    /**
     * worker集合
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * 是否允许超时
     */
    private volatile boolean allowThreadTimeOut;

    private volatile long keepAliveTime;

    public OnlyThreadPoolExecutor() {
        this(defaultPoolSize, defaultQueueSize, defaultAliveTime, new DefaultPolicyHandler());
    }

    public OnlyThreadPoolExecutor(int poolsize) {
        this(poolsize, defaultQueueSize, defaultAliveTime, new DefaultPolicyHandler());
    }


    public OnlyThreadPoolExecutor(int poolSize,
                                  int queueSize,
                                  long keepAliveTime,
                                  PolicyHandler handler) {
        if (poolSize <= 0 || poolSize > maxPoolSize) {
            throw new IllegalArgumentException("线程池大小不能<=0");
        }
        this.poolSize = poolSize;
        this.workQueue = new ArrayBlockingQueue<Runnable>(queueSize);
        this.keepAliveTime = keepAliveTime;
        this.handler = handler;
        if (keepAliveTime > 0) {
            allowThreadTimeOut = true;
        }
    }


    @Override
    public void execute(Runnable task) {

        if (task == null) {
            throw new NullPointerException("任务不允许为空");
        }

        if (isShutDown) {
            throw new IllegalArgumentException("线程池已销毁,禁止提交任务");
        }
        int c = ctl.get();
        //当前激活的线程数小于最大线程数
        if (c < maxPoolSize) {
            if (addWorker(task, true)) {
                return;
            }
        } else if (workQueue.offer(task)) {

        } else {
            handler.rejected(task, this);// 任务拒绝策略
        }

    }

    @Override
    public Runnable getTask() {
        try {
            return allowThreadTimeOut ? workQueue.poll(keepAliveTime, TimeUnit.SECONDS) : workQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            isShutDown = true;
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (Exception e) {
                        //e.printStackTrace();
                    } finally {
                        w.unlock();
                    }
                }
            }
        } finally {
            mainLock.unlock();
        }
    }


    private boolean addWorker(Runnable task, boolean startNew) {

        if (startNew) {
            ctl.incrementAndGet();
        }

        boolean worderAdded = false;
        boolean worderStarted = false;
        Worker w = new Worker(task);
        Thread t = w.thread;
        if (t != null) {
            ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (!isShutDown) {
                    // 检查线程是否已经处于运行状态，start方法不能重复调用执行
                    if (t.isAlive()) {
                        throw new IllegalThreadStateException();
                    }
                    workers.add(w);
                    worderAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            if (worderAdded) {
                t.start();
                worderStarted = true;
            }
        }
        return worderStarted;
    }


    private void runWorker(Worker worker) {

        Thread thread = Thread.currentThread();
        Runnable task = worker.firstTask;
        worker.firstTask = null;
        boolean completedAbruptly = true;
        try {

            while (task != null || (task = getTask()) != null) {
                worker.lock();
                if (isShutDown && !thread.isInterrupted()) {
                    thread.interrupt();
                }
                try {
                    task.run();
                } finally {
                    task = null;
                    worker.completedTask++;
                    worker.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(worker, completedAbruptly);
        }


    }

    private void processWorkerExit(Worker worker, boolean completedAbruptly) {
        if (completedAbruptly) {
            ctl.decrementAndGet();
        }
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += worker.completedTask;
            workers.remove(worker);
        } finally {
            mainLock.unlock();
        }
        if (completedAbruptly && !workQueue.isEmpty()) {
            addWorker(null, false);
        }
    }

    static AtomicInteger atomic = new AtomicInteger();

    class Worker extends ReentrantLock implements Runnable {

        volatile long completedTask;
        final Thread thread;
        Runnable firstTask;

        public Worker(Runnable task) {
            this.firstTask = task;
            this.thread = new Thread(this, "thread-name-" + atomic.incrementAndGet());
        }

        @Override
        public void run() {
            runWorker(this);
        }
    }

}
