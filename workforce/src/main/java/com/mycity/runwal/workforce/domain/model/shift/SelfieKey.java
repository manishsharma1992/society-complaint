package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where a clock-in or clock-out selfie is stored: {@code shifts/{societyId}/{staffId}/{issuedAtMillis}-{unique}.jpg}.
 * The key names its owner and when its upload link was issued, so a staff member can use only their own, fresh selfie.
 */
@ValueObject
public record SelfieKey(String value) {

    /** A selfie must be taken just before clocking in or out, not reused from an earlier shift. */
    public static final Duration MAX_AGE = Duration.ofMinutes(10);
    private static final Pattern FORMAT = Pattern.compile("shifts/(\\d+)/(\\d+)/(\\d+)-[A-Za-z0-9-]{8,64}\\.jpg");

    public SelfieKey {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidShiftException("Not a selfie key: " + value);
        }
    }

    public static SelfieKey issue(SocietyId societyId, UserId staffId, Instant issuedAt, String unique) {
        return new SelfieKey("shifts/" + societyId.value() + "/" + staffId.value() + "/" + issuedAt.toEpochMilli()
                + "-" + unique + ".jpg");
    }

    public boolean belongsTo(SocietyId societyId, UserId staffId) {
        Matcher m = matcher();
        return Long.parseLong(m.group(1)) == societyId.value() && Long.parseLong(m.group(2)) == staffId.value();
    }

    public Instant issuedAt() {
        return Instant.ofEpochMilli(Long.parseLong(matcher().group(3)));
    }

    /** Issued at most {@link #MAX_AGE} before {@code now} (and not in the future). */
    public boolean isFreshAt(Instant now) {
        Instant issued = issuedAt();
        return !issued.isAfter(now) && !issued.isBefore(now.minus(MAX_AGE));
    }

    private Matcher matcher() {
        Matcher m = FORMAT.matcher(value);
        if (!m.matches()) {
            throw new InvalidShiftException("Not a selfie key: " + value);
        }
        return m;
    }
}
