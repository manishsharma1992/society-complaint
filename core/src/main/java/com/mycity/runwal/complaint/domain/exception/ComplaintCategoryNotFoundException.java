package com.mycity.runwal.complaint.domain.exception;

import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.shared.ddd.DomainException;
import com.mycity.runwal.shared.id.SocietyId;

/** The society has no complaint category with this ID. */
public class ComplaintCategoryNotFoundException extends DomainException {

    public ComplaintCategoryNotFoundException(SocietyId societyId, ComplaintCategoryId categoryId) {
        super("Society " + societyId.value() + " has no complaint category " + categoryId.value());
    }
}
