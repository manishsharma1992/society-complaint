package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.service.ActionNotAllowedException;
import com.mycity.runwal.feed.application.service.TranslationUnavailableException;
import com.mycity.runwal.shared.ddd.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.mycity.runwal.feed")
class FeedApiExceptionHandler {

    @ExceptionHandler(ActionNotAllowedException.class)
    ProblemDetail handleNotAllowed(ActionNotAllowedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Not allowed");
        return problem;
    }

    @ExceptionHandler(TranslationUnavailableException.class)
    ProblemDetail handleTranslationUnavailable(TranslationUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Translation is not available right now; please try again later");
        problem.setTitle("Translation not available");
        return problem;
    }

    /** A broken business rule is the caller's problem, not a server error. */
    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomainException(DomainException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), exception.getMessage());
        problem.setTitle("Business rule violated");
        return problem;
    }
}
