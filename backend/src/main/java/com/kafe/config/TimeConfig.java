package com.kafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    // All pickup dates and times are local to Phnom Penh, regardless of the server's timezone
    public static final ZoneId CAFE_ZONE = ZoneId.of("Asia/Phnom_Penh");

    @Bean
    public Clock clock() {
        return Clock.system(CAFE_ZONE);
    }
}
