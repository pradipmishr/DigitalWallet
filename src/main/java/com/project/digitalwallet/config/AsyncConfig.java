package com.project.digitalwallet.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Enable spring @Async execution for event listeners
}
