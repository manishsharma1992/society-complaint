package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * A message on a complaint ("plumber will come at 4 pm"). Comments cannot be edited or deleted.
 *
 * @param sequence 1-based position in the complaint's conversation
 */
@ValueObject
public record ComplaintComment(int sequence, UserId author, String text, Instant postedAt) {

    public static final int MAX_LENGTH = 1000;

    public ComplaintComment {
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(postedAt, "postedAt");
        if (text == null || text.isBlank()) {
            throw new InvalidComplaintException("A comment needs some text");
        }
        text = text.strip();
        if (text.length() > MAX_LENGTH) {
            throw new InvalidComplaintException("A comment can have at most " + MAX_LENGTH + " characters");
        }
    }
}
