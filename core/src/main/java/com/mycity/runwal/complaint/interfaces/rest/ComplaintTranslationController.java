package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.query.ComplaintTranslationView;
import com.mycity.runwal.complaint.application.query.TranslateComplaintQuery;
import com.mycity.runwal.complaint.application.service.ComplaintTranslationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** A complaint machine-translated into en, hi or mr; for whoever may read the complaint. */
@RestController
class ComplaintTranslationController {

    private final ComplaintTranslationService translations;

    ComplaintTranslationController(ComplaintTranslationService translations) {
        this.translations = translations;
    }

    @GetMapping("/societies/{societyId}/complaints/{complaintId}/translations/{language}")
    ResponseEntity<TranslationResponse> translate(@PathVariable long societyId, @PathVariable long complaintId,
                                                  @PathVariable String language) {
        return ResponseEntity.of(translations.translate(new TranslateComplaintQuery(societyId, complaintId, language))
                .map(TranslationResponse::from));
    }

    /** Machine translated: the web app labels it so and keeps the original one tap away. */
    record TranslationResponse(String language, String title, String description, List<CommentText> comments) {

        static TranslationResponse from(ComplaintTranslationView view) {
            return new TranslationResponse(view.language(), view.title(), view.description(),
                    view.comments().stream().map(c -> new CommentText(c.sequence(), c.text())).toList());
        }
    }

    record CommentText(int sequence, String text) {
    }
}
