package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.command.RaiseComplaintCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record RaiseComplaintRequest(
        @NotNull @Pattern(regexp = "FLAT|COMMON_AREA") String scope,
        @Positive Long flatId,
        @Positive Long towerId,
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description) {

    RaiseComplaintCommand toCommand(long societyId) {
        return new RaiseComplaintCommand(societyId, scope, flatId, towerId, categoryId, title, description);
    }
}
