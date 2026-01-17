package com.sib.portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration for handling asynchronous logging operations.
 * Prevents logging from blocking API responses.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a dedicated thread pool executor for async logging operations.
     *
     * Configuration:
     * - Core Pool Size: 5 threads (minimum threads always alive)
     * - Max Pool Size: 10 threads (maximum threads during peak load)
     * - Queue Capacity: 500 (pending tasks queue before rejection)
     * - Thread Name Prefix: "async-logging-" for easy identification in logs
     * - Rejection Policy: CallerRunsPolicy (caller thread executes if queue full)
     *
     * @return Executor configured for async logging
     */
    @Bean(name = "loggingExecutor")
    public Executor loggingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum threads always alive
        executor.setCorePoolSize(5);

        // Maximum pool size - scales up to 10 during high load
        executor.setMaxPoolSize(10);

        // Queue capacity - holds up to 500 pending log tasks
        executor.setQueueCapacity(500);

        // Thread name prefix for debugging
        executor.setThreadNamePrefix("async-logging-");

        // Rejection policy - if queue is full, caller thread executes the task
        // This prevents log loss while providing back-pressure
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Allow core threads to timeout after 60 seconds of inactivity
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        // Wait for all tasks to complete on shutdown (max 30 seconds)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
