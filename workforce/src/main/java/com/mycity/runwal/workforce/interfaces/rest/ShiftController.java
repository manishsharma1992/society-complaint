package com.mycity.runwal.workforce.interfaces.rest;

import com.mycity.runwal.workforce.application.command.EndShiftCommand;
import com.mycity.runwal.workforce.application.command.RequestSelfieUploadCommand;
import com.mycity.runwal.workforce.application.command.ReviewShiftCommand;
import com.mycity.runwal.workforce.application.command.StartShiftCommand;
import com.mycity.runwal.workforce.application.query.ClockEvidenceView;
import com.mycity.runwal.workforce.application.query.MyShiftsQuery;
import com.mycity.runwal.workforce.application.query.OnDutyQuery;
import com.mycity.runwal.workforce.application.query.SelfieUploadView;
import com.mycity.runwal.workforce.application.query.ShiftHoursQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursView;
import com.mycity.runwal.workforce.application.query.ShiftReviewView;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.application.query.ShiftsQuery;
import com.mycity.runwal.workforce.application.service.ShiftQueryService;
import com.mycity.runwal.workforce.application.service.ShiftReviewService;
import com.mycity.runwal.workforce.application.service.ShiftService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clock in / clock out for maintenance staff, each time with a live selfie and the phone's location;
 * shift hours, the shift list and the review (confirm / dispute) for admins.
 */
@RestController
@RequestMapping("/societies/{societyId}/shifts")
class ShiftController {

    private final ShiftService shiftService;
    private final ShiftQueryService queryService;
    private final ShiftReviewService reviewService;

    ShiftController(ShiftService shiftService, ShiftQueryService queryService, ShiftReviewService reviewService) {
        this.shiftService = shiftService;
        this.queryService = queryService;
        this.reviewService = reviewService;
    }

    /** Step 1 of clocking in or out: where to PUT the selfie. */
    @PostMapping("/selfie-upload")
    @ResponseStatus(HttpStatus.CREATED)
    SelfieUploadResponse requestSelfieUpload(@PathVariable long societyId, @RequestBody SelfieUploadRequest request) {
        return SelfieUploadResponse.of(shiftService.requestSelfieUpload(
                new RequestSelfieUploadCommand(societyId, request.sizeBytes())));
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    ShiftResponse start(@PathVariable long societyId, @RequestBody ClockRequest request) {
        return ShiftResponse.of(shiftService.start(new StartShiftCommand(societyId, request.selfieKey(),
                request.latitude(), request.longitude(), request.accuracyMeters())));
    }

    @PostMapping("/end")
    ShiftResponse end(@PathVariable long societyId, @RequestBody ClockRequest request) {
        return ShiftResponse.of(shiftService.end(new EndShiftCommand(societyId, request.selfieKey(),
                request.latitude(), request.longitude(), request.accuracyMeters())));
    }

    /** Every staff member's shifts in [from, to), newest first, with selfies and locations (admins). */
    @GetMapping
    List<ShiftResponse> list(@PathVariable long societyId, @RequestParam Instant from, @RequestParam Instant to) {
        return queryService.list(new ShiftsQuery(societyId, from, to)).stream().map(ShiftResponse::of).toList();
    }

    /** The caller's own shifts in [from, to), newest first. */
    @GetMapping("/mine")
    List<ShiftResponse> mine(@PathVariable long societyId, @RequestParam Instant from, @RequestParam Instant to) {
        return queryService.mine(new MyShiftsQuery(societyId, from, to)).stream().map(ShiftResponse::of).toList();
    }

    @GetMapping("/on-duty")
    List<ShiftResponse> onDuty(@PathVariable long societyId) {
        return queryService.onDuty(new OnDutyQuery(societyId)).stream().map(ShiftResponse::of).toList();
    }

    @GetMapping("/hours")
    List<ShiftHoursResponse> hours(@PathVariable long societyId, @RequestParam Instant from,
                                   @RequestParam Instant to) {
        return queryService.hours(new ShiftHoursQuery(societyId, from, to)).stream()
                .map(ShiftHoursResponse::of)
                .toList();
    }

    /** An admin confirms (the selfies show this staff member, the locations are acceptable) or disputes a shift. */
    @PostMapping("/{shiftId}/review")
    ResponseEntity<ShiftResponse> review(@PathVariable long societyId, @PathVariable long shiftId,
                                         @RequestBody ReviewRequest request) {
        return reviewService.review(new ReviewShiftCommand(societyId, shiftId, request.decision(), request.reason()))
                .map(view -> ResponseEntity.ok(ShiftResponse.of(view)))
                .orElse(ResponseEntity.notFound().build());
    }

    record SelfieUploadRequest(long sizeBytes) {
    }

    /** @param latitude null (with longitude and accuracyMeters) when the phone gave no location */
    record ClockRequest(String selfieKey, Double latitude, Double longitude, Double accuracyMeters) {
    }

    record ReviewRequest(ReviewShiftCommand.Decision decision, String reason) {
    }

    /** Tells the client exactly how to upload: {@code method} the file to {@code uploadUrl} with {@code headers}. */
    record SelfieUploadResponse(String storageKey, String uploadUrl, String method, Map<String, String> headers,
                                Instant expiresAt) {
        static SelfieUploadResponse of(SelfieUploadView view) {
            return new SelfieUploadResponse(view.storageKey(), view.uploadUrl(), "PUT",
                    Map.of("Content-Type", view.contentType()), view.expiresAt());
        }
    }

    record EvidenceResponse(String placement, Long distanceMeters, Double latitude, Double longitude,
                            Double accuracyMeters, String selfieUrl) {
        static EvidenceResponse of(ClockEvidenceView view) {
            return view == null ? null : new EvidenceResponse(view.placement(), view.distanceMeters(),
                    view.latitude(), view.longitude(), view.accuracyMeters(), view.selfieUrl());
        }
    }

    record ReviewResponse(String decision, long reviewerId, Instant reviewedAt, String reason) {
        static ReviewResponse of(ShiftReviewView view) {
            return view == null ? null
                    : new ReviewResponse(view.decision(), view.reviewerId(), view.reviewedAt(), view.reason());
        }
    }

    record ShiftResponse(long id, long staffId, Instant startedAt, Instant endedAt, long minutesCounted,
                         EvidenceResponse start, EvidenceResponse end, ReviewResponse review,
                         boolean needsAttention) {
        static ShiftResponse of(ShiftView view) {
            return new ShiftResponse(view.id(), view.staffId(), view.startedAt(), view.endedAt(),
                    view.minutesCounted(), EvidenceResponse.of(view.start()), EvidenceResponse.of(view.end()),
                    ReviewResponse.of(view.review()), view.needsAttention());
        }
    }

    record ShiftHoursResponse(long staffId, int shifts, long minutesWorked, boolean onDutyNow) {
        static ShiftHoursResponse of(ShiftHoursView view) {
            return new ShiftHoursResponse(view.staffId(), view.shifts(), view.minutesWorked(), view.onDutyNow());
        }
    }
}
