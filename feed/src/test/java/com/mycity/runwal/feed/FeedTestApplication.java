package com.mycity.runwal.feed;

import com.mycity.runwal.feed.application.port.FeedAddOn;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Configuration root for this module's Spring slice tests; the real application lives in the app module. */
@SpringBootApplication
class FeedTestApplication {

    /** In the app, Residency answers this; module tests run with the add-on on (AddOnInterceptorTest covers off). */
    @Bean
    FeedAddOn addOnAlwaysOn() {
        return societyId -> true;
    }
}
