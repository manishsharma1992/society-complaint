package com.mycity.runwal;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * The public feed as its own app: the feed with its own accounts (identity) and society data (residency). It shares
 * no server, database or login with the complaint app. It sits in the base package so scanning covers its contexts.
 */
@SpringBootApplication
public class FeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedApplication.class, args);
    }

    /** Shared by all contexts (one bean per app, not per context). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
