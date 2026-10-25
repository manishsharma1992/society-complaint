package com.mycity.runwal.identity.infrastructure.security;

import com.mycity.runwal.identity.application.port.OtpCodeSource;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import java.security.SecureRandom;
import java.util.Set;

/**
 * Random 6-digit codes. Demo numbers (only when a demo code is configured) always get the demo code,
 * so the app can be shown before real SMS works.
 */
public class RandomOtpCodeSource implements OtpCodeSource {

    private final SecureRandom random = new SecureRandom();
    private final Set<PhoneNumber> demoPhones;
    private final String demoCode;

    /** @param demoCode a 6-digit code, or blank to turn demo logins off */
    public RandomOtpCodeSource(Set<PhoneNumber> demoPhones, String demoCode) {
        boolean demoOn = demoCode != null && !demoCode.isBlank();
        if (demoOn && !demoCode.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("OTP_DEMO_CODE must be 6 digits");
        }
        this.demoPhones = demoOn ? Set.copyOf(demoPhones) : Set.of();
        this.demoCode = demoCode;
    }

    @Override
    public String newCode(PhoneNumber phone) {
        if (demoPhones.contains(phone)) {
            return demoCode;
        }
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public boolean isDemo(PhoneNumber phone) {
        return demoPhones.contains(phone);
    }
}
