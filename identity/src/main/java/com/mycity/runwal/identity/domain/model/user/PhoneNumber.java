package com.mycity.runwal.identity.domain.model.user;

import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.util.regex.Pattern;

/**
 * An Indian mobile number in E.164 form, e.g. "+919876543210". Accepts the ways people type it:
 * "98765 43210", "098765-43210", "+91 98765 43210", "919876543210".
 */
@ValueObject
public record PhoneNumber(String value) {

    private static final Pattern E164_INDIA_MOBILE = Pattern.compile("\\+91[6-9][0-9]{9}");

    public PhoneNumber {
        if (value == null || !E164_INDIA_MOBILE.matcher(value).matches()) {
            throw new InvalidIdentityException("Not a valid Indian mobile number");
        }
    }

    public static PhoneNumber parse(String input) {
        if (input == null) {
            throw new InvalidIdentityException("Not a valid Indian mobile number");
        }
        String digits = input.replaceAll("[\\s()-]", "");
        if (digits.startsWith("+")) {
            return new PhoneNumber(digits);
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return new PhoneNumber("+" + digits);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return new PhoneNumber("+91" + digits.substring(1));
        }
        return new PhoneNumber("+91" + digits);
    }

    /** For messages and logs: "+91******3210". */
    public String masked() {
        return value.substring(0, 3) + "******" + value.substring(value.length() - 4);
    }
}
