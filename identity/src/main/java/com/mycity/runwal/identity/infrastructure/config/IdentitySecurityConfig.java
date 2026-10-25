package com.mycity.runwal.identity.infrastructure.config;

import com.mycity.runwal.identity.application.port.AccessTokenIssuer;
import com.mycity.runwal.identity.application.port.OtpHasher;
import com.mycity.runwal.identity.application.port.OtpSender;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.identity.infrastructure.messaging.LogOtpSender;
import com.mycity.runwal.identity.infrastructure.security.HmacOtpHasher;
import com.mycity.runwal.identity.infrastructure.security.JwtAccessTokenIssuer;
import com.mycity.runwal.identity.infrastructure.security.RandomOtpCodeSource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Login secrets and OTP delivery. One server secret (AUTH_SECRET, environment only) is turned into two separate
 * keys: one signs login tokens, one hashes OTP codes.
 */
@Configuration
class IdentitySecurityConfig {

    static final int MIN_SECRET_LENGTH = 32;

    private final byte[] secret;

    IdentitySecurityConfig(@Value("${identity.auth-secret}") String authSecret) {
        if (authSecret == null || authSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("AUTH_SECRET must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        this.secret = authSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Bean
    AccessTokenIssuer accessTokenIssuer() {
        return new JwtAccessTokenIssuer(tokenSigningKey());
    }

    /** Also used to verify tokens on every request. */
    @Bean
    SecretKey tokenSigningKey() {
        return derivedKey("access-token");
    }

    @Bean
    OtpHasher otpHasher() {
        return new HmacOtpHasher(derivedKey("otp-code"));
    }

    @Bean
    RandomOtpCodeSource otpCodeSource(@Value("${identity.otp.demo-phones}") List<String> demoPhones,
                                      @Value("${identity.otp.demo-code}") String demoCode) {
        Set<PhoneNumber> phones = demoPhones.stream().map(PhoneNumber::parse).collect(Collectors.toSet());
        return new RandomOtpCodeSource(phones, demoCode);
    }

    @Bean
    OtpSender otpSender(RandomOtpCodeSource codeSource) {
        return new LogOtpSender(codeSource::isDemo);
    }

    private SecretKey derivedKey(String purpose) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return new SecretKeySpec(mac.doFinal(purpose.getBytes(StandardCharsets.UTF_8)), "HmacSHA256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
