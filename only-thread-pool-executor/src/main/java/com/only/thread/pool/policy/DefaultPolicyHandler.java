package com.only.thread.pool.policy;

import com.only.thread.pool.OnlyThreadPoolExecutor;
import com.only.thread.pool.execption.PolicyException;


public class DefaultPolicyHandler implements PolicyHandler {

    public DefaultPolicyHandler() {
    }

    /**
     * 拒绝策略
     *
     * @param task
     * @param executor
     */
    @Override
    public void rejected(Runnable task, OnlyThreadPoolExecutor executor){
        try {
            throw new PolicyException("任务已经满了");
        } catch (PolicyException e) {
            e.printStackTrace();
        }
    }
}
