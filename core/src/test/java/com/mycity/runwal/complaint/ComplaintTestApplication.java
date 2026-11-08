package com.mycity.runwal.complaint;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.complaint.application.port.ComplaintsAddOn;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Configuration root for this module's Spring slice tests; the real application lives in the app module. */
@SpringBootApplication
class ComplaintTestApplication {

    /** In the app, Residency answers this; module tests run with the add-on on (AddOnInterceptorTest covers off). */
    @Bean
    ComplaintsAddOn addOnAlwaysOn() {
        return new ComplaintsAddOn() {
            @Override
            public boolean isOnFor(SocietyId societyId) {
                return true;
            }

            @Override
            public boolean isOnIn(SocietyId societyId, TowerId towerId) {
                return true;
            }
        };
    }
}
