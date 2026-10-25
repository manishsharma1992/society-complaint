package com.mycity.runwal.identity.infrastructure.messaging;

import com.mycity.runwal.identity.application.port.OtpSender;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Until SMS works (MSG91, after DLT approval): writes the code to the server log, where an admin can read it
 * out. Demo numbers are skipped; they use the fixed demo code.
 */
public class LogOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LogOtpSender.class);

    private final Predicate<PhoneNumber> isDemo;

    public LogOtpSender(Predicate<PhoneNumber> isDemo) {
        this.isDemo = isDemo;
    }

    @Override
    public void send(PhoneNumber phone, String code) {
        if (isDemo.test(phone)) {
            log.info("OTP requested for demo number {}", phone.masked());
            return;
        }
        log.warn("OTP for {}: {} (SMS not configured yet)", phone.masked(), code);
    }
}
