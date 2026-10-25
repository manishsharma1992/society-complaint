package com.mycity.runwal.complaint.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;

/** The Residency context knows no such flat in this society. */
public class FlatNotFoundException extends DomainException {

    public FlatNotFoundException(SocietyId societyId, FlatId flatId) {
        super("Society " + societyId.value() + " has no flat " + flatId.value());
    }
}
