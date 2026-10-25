package com.mycity.runwal.residency.interfaces.rest;

import com.mycity.runwal.residency.application.command.AddFlatMemberCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

record AddFlatMemberRequest(@NotNull @Positive Long userId, @NotNull @Pattern(regexp = "OWNER|TENANT") String type) {

    AddFlatMemberCommand toCommand(long societyId, long flatId) {
        return new AddFlatMemberCommand(societyId, flatId, userId, type);
    }
}
