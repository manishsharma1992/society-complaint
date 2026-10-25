package com.mycity.runwal.complaint.interfaces.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

record AssignComplaintRequest(@NotNull @Positive Long staffId) {
}
