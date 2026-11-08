package com.mycity.runwal.complaint.application.command;

import java.util.List;

/** The society admin sets which complaint categories a staff member handles (empty: none, assigned by hand only). */
public record SetStaffCategoriesCommand(long societyId, long staffId, List<Long> categoryIds) {
}
