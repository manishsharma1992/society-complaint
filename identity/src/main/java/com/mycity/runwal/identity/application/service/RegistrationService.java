package com.mycity.runwal.identity.application.service;

import com.mycity.runwal.identity.application.command.ApproveRegistrationCommand;
import com.mycity.runwal.identity.application.command.RegisterCommand;
import com.mycity.runwal.identity.application.command.RejectRegistrationCommand;
import com.mycity.runwal.identity.application.port.CurrentUser;
import com.mycity.runwal.identity.application.port.DomainEventPublisher;
import com.mycity.runwal.identity.application.port.FlatDirectory;
import com.mycity.runwal.identity.application.query.ListPendingRegistrationsQuery;
import com.mycity.runwal.identity.application.query.UserView;
import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.identity.domain.model.user.FlatClaim;
import com.mycity.runwal.identity.domain.model.user.Occupancy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self sign-up: the logged-in (verified) user registers for a flat, the society admin approves or rejects.
 * Approval publishes RegistrationApproved, which links the user to the flat in Residency.
 * Approve/reject return empty when the user has no registration in that society.
 */
@ApplicationService
public class RegistrationService {

    private final AppUserRepository users;
    private final FlatDirectory flatDirectory;
    private final CurrentUser currentUser;
    private final SocietyAdminCheck societyAdmin;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public RegistrationService(AppUserRepository users, FlatDirectory flatDirectory, CurrentUser currentUser,
                               DomainEventPublisher eventPublisher, Clock clock) {
        this.users = users;
        this.flatDirectory = flatDirectory;
        this.currentUser = currentUser;
        this.societyAdmin = new SocietyAdminCheck(users, currentUser);
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** The logged-in user. */
    @Transactional(readOnly = true)
    public Optional<UserView> me() {
        return users.findById(currentUser.userId()).map(UserView::of);
    }

    @Transactional(readOnly = true)
    public List<UserView> pending(ListPendingRegistrationsQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        societyAdmin.require(societyId, "see pending registrations");
        return users.findPendingRegistrations(societyId).stream().map(UserView::of).toList();
    }

    @Transactional
    public Optional<UserView> register(RegisterCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        FlatId flatId = new FlatId(command.flatId());
        if (!flatDirectory.exists(societyId, flatId)) {
            throw new InvalidIdentityException("Society " + societyId.value() + " has no flat " + flatId.value());
        }
        FlatClaim claim = new FlatClaim(societyId, flatId, Occupancy.from(command.occupancy()));
        return users.findById(currentUser.userId()).map(user -> {
            user.register(command.name(), claim, clock.instant());
            return saveAndPublish(user);
        });
    }

    @Transactional
    public Optional<UserView> approve(ApproveRegistrationCommand command) {
        AppUser admin = societyAdmin.require(new SocietyId(command.societyId()), "approve registrations");
        return decide(command.societyId(), command.userId(), (user, now) -> user.approveRegistration(admin.id(), now));
    }

    @Transactional
    public Optional<UserView> reject(RejectRegistrationCommand command) {
        AppUser admin = societyAdmin.require(new SocietyId(command.societyId()), "reject registrations");
        return decide(command.societyId(), command.userId(), (user, now) ->
                user.rejectRegistration(admin.id(), command.reason(), now));
    }

    private Optional<UserView> decide(long societyId, long userId, BiConsumer<AppUser, Instant> decision) {
        SocietyId society = new SocietyId(societyId);
        return users.findById(new UserId(userId))
                .filter(user -> user.claim().map(c -> c.societyId().equals(society)).orElse(false))
                .map(user -> {
                    decision.accept(user, clock.instant());
                    return saveAndPublish(user);
                });
    }

    private UserView saveAndPublish(AppUser user) {
        users.save(user);
        eventPublisher.publish(user.pullDomainEvents());
        return UserView.of(user);
    }
}
