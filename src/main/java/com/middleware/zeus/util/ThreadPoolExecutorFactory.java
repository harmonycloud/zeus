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

    public static final ExecutorService executor = Executors.newFixedThreadPool(20);


}
