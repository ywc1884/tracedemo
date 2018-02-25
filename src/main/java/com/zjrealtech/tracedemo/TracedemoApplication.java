package com.zjrealtech.tracedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
@ComponentScan("com.zjrealtech.tracedemo.**")
public class TracedemoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TracedemoApplication.class, args);
        TraceDownDemo demo = context.getBean(TraceDownDemo.class);
        demo.traceDownTest();
        context.close();
    }

    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(100);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("tracedown-");
        executor.initialize();
        return executor;
    }
}
