package com.only.thread.pool.policy;

import com.only.thread.pool.OnlyThreadPoolExecutor;

public interface PolicyHandler {

    void rejected(Runnable task , OnlyThreadPoolExecutor executor) ;

}
