package com.mycity.runwal.workforce.interfaces.rest;

import com.mycity.runwal.shared.ddd.DomainException;
import com.mycity.runwal.workforce.application.service.ActionNotAllowedException;
import com.mycity.runwal.workforce.application.service.NoOpenShiftException;
import com.mycity.runwal.workforce.application.service.ShiftAlreadyOpenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.mycity.runwal.workforce")
class WorkforceApiExceptionHandler {

    @ExceptionHandler(ActionNotAllowedException.class)
    ProblemDetail handleNotAllowed(ActionNotAllowedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Not allowed", exception.getMessage());
    }

    @ExceptionHandler(NoOpenShiftException.class)
    ProblemDetail handleNoOpenShift(NoOpenShiftException exception) {
        return problem(HttpStatus.NOT_FOUND, "No open shift", exception.getMessage());
    }

    @ExceptionHandler(ShiftAlreadyOpenException.class)
    ProblemDetail handleAlreadyOpen(ShiftAlreadyOpenException exception) {
        return problem(HttpStatus.CONFLICT, "Already on a shift", exception.getMessage());
    }

    /** Two clock-ins raced; the database's one-open-shift index stopped the second. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleRace(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "Already on a shift", new ShiftAlreadyOpenException().getMessage());
    }

    /** A broken business rule is the caller's problem, not a server error. */
    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomainException(DomainException exception) {
        return problem(HttpStatusCode.valueOf(422), "Business rule violated", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
