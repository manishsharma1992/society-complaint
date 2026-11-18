package com.mycity.runwal;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * The complaint app: complaints, staff shifts, accounts (identity) and society data (residency) in one JVM. It sits
 * in the base package so component, entity and repository scanning cover all its contexts.
 */
@SpringBootApplication
public class SocietyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocietyApplication.class, args);
    }

    /** Shared by all contexts (one bean per app, not per context). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
