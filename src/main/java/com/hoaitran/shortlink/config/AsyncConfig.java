package com.hoaitran.shortlink.config;

import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    private final TaskExecutionProperties taskExecutionProperties;

    public AsyncConfig(TaskExecutionProperties taskExecutionProperties) {
        this.taskExecutionProperties = taskExecutionProperties;
    }

    @Bean(name = "analyticsTaskExecutor")
    public Executor analyticsTaskExecutor() {
        TaskExecutionProperties.Pool pool = taskExecutionProperties.getPool();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(pool.getCoreSize());
        executor.setMaxPoolSize(pool.getMaxSize());
        executor.setQueueCapacity(pool.getQueueCapacity());
        executor.setThreadNamePrefix(taskExecutionProperties.getThreadNamePrefix());
        executor.initialize();
        return executor;
    }
}
