package com.mycity.runwal.workforce.application.service;

import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.workforce.application.command.EndShiftCommand;
import com.mycity.runwal.workforce.application.command.RequestSelfieUploadCommand;
import com.mycity.runwal.workforce.application.command.StartShiftCommand;
import com.mycity.runwal.workforce.application.port.CurrentActor;
import com.mycity.runwal.workforce.application.port.DomainEventPublisher;
import com.mycity.runwal.workforce.application.port.SelfieStorage;
import com.mycity.runwal.workforce.application.port.SocietySites;
import com.mycity.runwal.workforce.application.query.SelfieUploadView;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import com.mycity.runwal.workforce.domain.exception.SelfieNotUploadedException;
import com.mycity.runwal.workforce.domain.model.actor.Actor;
import com.mycity.runwal.workforce.domain.model.shift.ClockEvidence;
import com.mycity.runwal.workforce.domain.model.shift.GeoPoint;
import com.mycity.runwal.workforce.domain.model.shift.Location;
import com.mycity.runwal.workforce.domain.model.shift.SelfieKey;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import com.mycity.runwal.workforce.domain.service.WorkforceAccessPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff clock in and out, each time with a live selfie and the phone's location:
 * 1. {@link #requestSelfieUpload} hands out a short-lived upload link under a key naming the staff member and the time;
 * 2. the phone PUTs the selfie there;
 * 3. {@link #start} or {@link #end} checks the selfie is there, fresh and theirs, and checks the location against the
 *    society's site (outside or missing locations are flagged for review, not refused).
 */
@ApplicationService
public class ShiftService {

    public static final String SELFIE_CONTENT_TYPE = "image/jpeg";
    public static final long MAX_SELFIE_BYTES = 2 * 1024 * 1024;

    private final ShiftRepository shifts;
    private final CurrentActor currentActor;
    private final WorkforceAccessPolicy accessPolicy;
    private final DomainEventPublisher eventPublisher;
    private final SelfieStorage selfies;
    private final SocietySites sites;
    private final Clock clock;

    public ShiftService(ShiftRepository shifts, CurrentActor currentActor, WorkforceAccessPolicy accessPolicy,
                        DomainEventPublisher eventPublisher, SelfieStorage selfies, SocietySites sites, Clock clock) {
        this.shifts = shifts;
        this.currentActor = currentActor;
        this.accessPolicy = accessPolicy;
        this.eventPublisher = eventPublisher;
        this.selfies = selfies;
        this.sites = sites;
        this.clock = clock;
    }

    public SelfieUploadView requestSelfieUpload(RequestSelfieUploadCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor actor = staffOf(societyId);
        if (command.sizeBytes() <= 0 || command.sizeBytes() > MAX_SELFIE_BYTES) {
            throw new InvalidShiftException("A selfie must be a JPEG of at most " + MAX_SELFIE_BYTES / (1024 * 1024) + " MB");
        }
        Instant now = clock.instant();
        SelfieKey key = SelfieKey.issue(societyId, actor.userId(), now, UUID.randomUUID().toString());
        String url = selfies.uploadUrl(key.value(), SELFIE_CONTENT_TYPE, command.sizeBytes(), SelfieKey.MAX_AGE)
                .toString();
        return new SelfieUploadView(key.value(), url, SELFIE_CONTENT_TYPE, command.sizeBytes(),
                now.plus(SelfieKey.MAX_AGE));
    }

    @Transactional
    public ShiftView start(StartShiftCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor actor = staffOf(societyId);
        if (shifts.findOpen(societyId, actor.userId()).isPresent()) {
            throw new ShiftAlreadyOpenException();
        }
        Instant now = clock.instant();
        ClockEvidence evidence = evidence(societyId, command.selfieKey(),
                location(command.latitude(), command.longitude(), command.accuracyMeters()));
        Shift shift = Shift.start(shifts.nextId(), societyId, actor.userId(), now, evidence);
        shifts.save(shift);
        eventPublisher.publish(shift.pullDomainEvents());
        return ShiftView.of(shift, now, selfies);
    }

    @Transactional
    public ShiftView end(EndShiftCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor actor = staffOf(societyId);
        Shift shift = shifts.findOpen(societyId, actor.userId()).orElseThrow(NoOpenShiftException::new);
        Instant now = clock.instant();
        shift.end(now, evidence(societyId, command.selfieKey(),
                location(command.latitude(), command.longitude(), command.accuracyMeters())));
        shifts.save(shift);
        eventPublisher.publish(shift.pullDomainEvents());
        return ShiftView.of(shift, now, selfies);
    }

    private ClockEvidence evidence(SocietyId societyId, String selfieKey, Location location) {
        if (selfieKey == null || selfieKey.isBlank()) {
            throw new InvalidShiftException("Clocking in or out needs a selfie");
        }
        SelfieKey key = new SelfieKey(selfieKey);
        if (selfies.sizeOf(key.value()).isEmpty()) {
            throw new SelfieNotUploadedException(key.value());
        }
        return ClockEvidence.at(sites.siteOf(societyId), key, location);
    }

    /** A location needs all three numbers; none of them means the phone gave no location. */
    private static Location location(Double latitude, Double longitude, Double accuracyMeters) {
        if (latitude == null && longitude == null && accuracyMeters == null) {
            return null;
        }
        if (latitude == null || longitude == null || accuracyMeters == null) {
            throw new InvalidShiftException("A location needs latitude, longitude and accuracy");
        }
        return new Location(new GeoPoint(latitude, longitude), accuracyMeters);
    }

    private Actor staffOf(SocietyId societyId) {
        Actor actor = currentActor.actor();
        if (!accessPolicy.mayWorkShifts(actor, societyId)) {
            throw new ActionNotAllowedException("Only maintenance staff of this society work shifts");
        }
        return actor;
    }
}
