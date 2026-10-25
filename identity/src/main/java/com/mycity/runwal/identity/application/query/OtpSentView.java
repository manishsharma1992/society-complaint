package com.mycity.runwal.identity.application.query;

/** A code is on its way; {@code sentTo} is masked, e.g. "+91******3210". */
public record OtpSentView(String sentTo, long expiresInSeconds, long resendAfterSeconds) {
}
