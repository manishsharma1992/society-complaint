package com.mycity.runwal.complaint.application.query;

import java.util.List;

/** A complaint's words machine-translated: the title, the description and every comment (by its sequence). */
public record ComplaintTranslationView(String language, String title, String description,
                                       List<TranslatedCommentView> comments) {

    public record TranslatedCommentView(int sequence, String text) {
    }
}
