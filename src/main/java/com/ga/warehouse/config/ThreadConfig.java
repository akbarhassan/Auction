package com.ga.warehouse.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig {

    /**
     * Thread pool for bid processing
     * Fixed thread pool ensures we don't create unlimited threads
     */
    @Bean(name = "bidExecutorService")
    public ExecutorService bidExecutorService() {
        return Executors.newFixedThreadPool(10); // 10 concurrent bid threads
    }

    /**
     * Thread pool for async notifications
     */
    @Bean(name = "notificationExecutorService")
    public ExecutorService notificationExecutorService() {
        return Executors.newFixedThreadPool(5); // 5 concurrent notification threads
    }
}
