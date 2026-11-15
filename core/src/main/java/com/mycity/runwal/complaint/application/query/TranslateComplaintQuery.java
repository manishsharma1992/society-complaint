package com.mycity.runwal.complaint.application.query;

/** @param language the web app's language code: en, hi or mr */
public record TranslateComplaintQuery(long societyId, long complaintId, String language) {
}
