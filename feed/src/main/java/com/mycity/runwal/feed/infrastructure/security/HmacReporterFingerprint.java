package com.mycity.runwal.feed.infrastructure.security;

import com.mycity.runwal.feed.application.port.ReporterFingerprint;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 of the reporter's address and the post, keyed with AUTH_SECRET (with a "feed-report" prefix so the
 * value cannot be confused with anything else signed with that secret). Without the secret, a key cannot be turned
 * back into an address, and one reporter's keys on two posts cannot be linked.
 */
@Component
class HmacReporterFingerprint implements ReporterFingerprint {

    private final SecretKeySpec key;

    HmacReporterFingerprint(@Value("${identity.auth-secret}") String authSecret) {
        this.key = new SecretKeySpec(authSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public String keyFor(String reporterAddress, PostSlug post) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal(("feed-report:" + post.value() + ":" + reporterAddress)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
