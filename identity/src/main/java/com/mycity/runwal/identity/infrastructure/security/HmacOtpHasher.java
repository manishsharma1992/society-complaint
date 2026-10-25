package com.mycity.runwal.identity.infrastructure.security;

import com.mycity.runwal.identity.application.port.OtpHasher;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/** HMAC-SHA256 of phone and code with a server-only key: a leaked table cannot be brute-forced offline. */
public class HmacOtpHasher implements OtpHasher {

    private final SecretKey key;

    public HmacOtpHasher(SecretKey key) {
        this.key = key;
    }

    @Override
    public String hash(PhoneNumber phone, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal((phone.value() + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
