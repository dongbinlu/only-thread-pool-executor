package com.only.thread.pool.test;

import com.only.thread.pool.OnlyThreadPoolExecutor;
import com.only.thread.pool.policy.DefaultPolicyHandler;

public class Test {

    public static void main(String[] args) {
        OnlyThreadPoolExecutor pool = new OnlyThreadPoolExecutor(3,3,60,new DefaultPolicyHandler());
        for (int i=0;i<10;i++){
            pool.execute(new Task(i));
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pool.shutdown();
    }

}