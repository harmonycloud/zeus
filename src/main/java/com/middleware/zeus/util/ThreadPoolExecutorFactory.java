package com.middleware.zeus.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author jiangmi
 * @Description
 * @Date created in 2017-12-14
 * @Modified
 */
public class ThreadPoolExecutorFactory {


    private static final Integer THREAD_SIZE;

    static {
        THREAD_SIZE = Integer.valueOf(SpringContextUtils.getProperty("system.thread.size", "20"));
    }


    public static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_SIZE);


}
