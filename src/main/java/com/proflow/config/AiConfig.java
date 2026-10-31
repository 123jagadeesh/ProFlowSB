package com.proflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AiConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
