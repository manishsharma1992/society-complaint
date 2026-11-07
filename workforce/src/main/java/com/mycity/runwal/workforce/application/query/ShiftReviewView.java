package com.mycity.runwal.workforce.application.query;

import com.mycity.runwal.workforce.domain.model.shift.ShiftReview;
import java.time.Instant;

/** @param decision CONFIRMED or DISPUTED */
public record ShiftReviewView(String decision, long reviewerId, Instant reviewedAt, String reason) {

    public static ShiftReviewView of(ShiftReview review) {
        return new ShiftReviewView(review.decision().name(), review.reviewer().value(), review.reviewedAt(),
                review.reason());
    }
}
