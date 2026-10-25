package com.mycity.runwal.identity.application.port;

import com.mycity.runwal.identity.domain.model.user.PhoneNumber;

/** Delivers a one-time code to a phone (SMS through MSG91 once DLT is approved; the server log until then). */
public interface OtpSender {

    void send(PhoneNumber phone, String code);
}
