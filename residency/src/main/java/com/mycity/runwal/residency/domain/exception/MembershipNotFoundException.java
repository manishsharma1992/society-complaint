package com.mycity.runwal.residency.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.UserId;

/** The user is not linked to this flat. */
public class MembershipNotFoundException extends DomainException {

    public MembershipNotFoundException(FlatId flatId, UserId userId) {
        super("User " + userId.value() + " is not linked to flat " + flatId.value());
    }
}
