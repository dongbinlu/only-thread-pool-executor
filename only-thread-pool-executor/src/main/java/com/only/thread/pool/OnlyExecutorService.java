package com.only.thread.pool;


import com.only.thread.pool.execption.PolicyException;

public interface OnlyExecutorService {

    void execute(Runnable task) throws PolicyException;

    Runnable getTask();

    void shutdown();


}
