package com.mycity.runwal.complaint.application.query;

import com.mycity.runwal.complaint.domain.model.complaint.ComplaintComment;
import java.time.Instant;

public record CommentView(int sequence, long authorId, String text, Instant postedAt) {

    static CommentView of(ComplaintComment comment) {
        return new CommentView(comment.sequence(), comment.author().value(), comment.text(), comment.postedAt());
    }
}
