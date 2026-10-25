package com.mycity.runwal.complaint.application.command;

/**
 * Input to raise a complaint. The person raising it comes from the login token.
 *
 * @param scope   "FLAT" or "COMMON_AREA"
 * @param flatId  required for FLAT, must be {@code null} for COMMON_AREA
 * @param towerId only used for COMMON_AREA (optional); for FLAT the tower comes from the Residency context
 */
public record RaiseComplaintCommand(
        long societyId,
        String scope,
        Long flatId,
        Long towerId,
        long categoryId,
        String title,
        String description) {
}
