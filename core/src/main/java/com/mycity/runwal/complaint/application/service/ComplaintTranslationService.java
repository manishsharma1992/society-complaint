package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.port.Translator;
import com.mycity.runwal.complaint.application.query.ComplaintTranslationView;
import com.mycity.runwal.complaint.application.query.GetComplaintQuery;
import com.mycity.runwal.complaint.application.query.TranslateComplaintQuery;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import java.util.Optional;
import java.util.Set;

/**
 * A complaint in the reader's language, for staff who don't read English and residents who don't read the staff's
 * Marathi. Only for those who may see the complaint; the original stays what was written.
 */
@ApplicationService
public class ComplaintTranslationService {

    private static final Set<String> LANGUAGES = Set.of("en", "hi", "mr");

    private final ComplaintQueryService complaints;
    private final Translator translator;

    public ComplaintTranslationService(ComplaintQueryService complaints, Translator translator) {
        this.complaints = complaints;
        this.translator = translator;
    }

    /** Empty when there is no such complaint or the user may not see it (as for reading it). */
    public Optional<ComplaintTranslationView> translate(TranslateComplaintQuery query) {
        String language = query.language();
        if (!LANGUAGES.contains(language)) {
            throw new InvalidComplaintException("Unknown language: choose one of en, hi, mr");
        }
        return complaints.get(new GetComplaintQuery(query.societyId(), query.complaintId()))
                .map(complaint -> new ComplaintTranslationView(language,
                        translator.translate(complaint.title(), language),
                        complaint.description() == null || complaint.description().isBlank()
                                ? complaint.description() : translator.translate(complaint.description(), language),
                        complaint.comments().stream()
                                .map(c -> new ComplaintTranslationView.TranslatedCommentView(c.sequence(),
                                        translator.translate(c.text(), language)))
                                .toList()));
    }
}
