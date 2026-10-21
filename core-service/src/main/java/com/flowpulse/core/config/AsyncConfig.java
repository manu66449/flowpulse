package com.flowpulse.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "payoutTaskExecutor")
    public Executor payoutTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // Minimum threads always alive waiting for jobs
        executor.setMaxPoolSize(5);       // Maximum threads allowed during traffic spikes
        executor.setQueueCapacity(100);   // Number of tasks allowed to wait in line if all threads are busy
        executor.setThreadNamePrefix("PayoutWorker-");
        executor.initialize();
        return executor;
    }
}