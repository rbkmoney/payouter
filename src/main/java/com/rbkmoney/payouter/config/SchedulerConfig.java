package com.rbkmoney.payouter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class SchedulerConfig {

    @Bean
    @DependsOn("dataSource")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setDaemon(true);
        ThreadGroup threadGroup = new ThreadGroup("Schedulers");
        taskScheduler.setThreadFactory(new ThreadFactory() {
            AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(threadGroup, r, "Scheduler-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
        return taskScheduler;
    }

}
