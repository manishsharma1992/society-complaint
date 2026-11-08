package com.mycity.runwal.complaint.application.query;

import java.util.List;

/**
 * @param on    complaints are assigned automatically (everyone may know: residents are told their complaint waits)
 * @param staff who handles which categories; only admins see it, empty for everyone else
 */
public record AutoAssignmentView(boolean on, List<StaffCategoriesView> staff) {

    /** @param categoryIds the categories this staff member handles, sorted */
    public record StaffCategoriesView(long staffId, List<Long> categoryIds) {
    }
}
