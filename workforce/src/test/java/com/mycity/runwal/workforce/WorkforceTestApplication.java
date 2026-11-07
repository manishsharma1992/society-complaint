package com.mycity.runwal.workforce;

import com.mycity.runwal.workforce.application.port.ComplaintsAddOn;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Configuration root for this module's Spring slice tests; the real application lives in the app module. */
@SpringBootApplication
class WorkforceTestApplication {

    /** In the app, Residency answers this; module tests run with the add-on on (AddOnInterceptorTest covers off). */
    @Bean
    ComplaintsAddOn addOnAlwaysOn() {
        return societyId -> true;
    }
}
