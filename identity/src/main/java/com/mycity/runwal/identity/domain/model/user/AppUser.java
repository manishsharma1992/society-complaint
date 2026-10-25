package com.mycity.runwal.identity.domain.model.user;

import com.mycity.runwal.identity.domain.event.RegistrationApproved;
import com.mycity.runwal.identity.domain.event.RegistrationRejected;
import com.mycity.runwal.identity.domain.event.RegistrationSubmitted;
import com.mycity.runwal.identity.domain.event.RoleGranted;
import com.mycity.runwal.identity.domain.event.RoleRevoked;
import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A person who uses the app, known by their verified phone number. Self sign-up:
 * VERIFIED (phone proved by OTP) -> register for a flat -> PENDING -> approved: ACTIVE resident, or REJECTED
 * (may register again). Staff and admins become ACTIVE when the society grants them a role.
 * Personal data is limited to what the society needs: phone number and name (India DPDP Act).
 */
@AggregateRoot
public class AppUser extends AbstractAggregateRoot {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_REASON_LENGTH = 500;

    private final UserId id;
    private final PhoneNumber phone;
    private final Instant createdAt;
    private final List<RoleAssignment> roles;
    private String name;
    private UserStatus status;
    private FlatClaim claim;
    private String rejectionReason;

    public AppUser(UserId id, PhoneNumber phone, Instant createdAt, String name, UserStatus status, FlatClaim claim,
                   String rejectionReason, List<RoleAssignment> roles) {
        this.id = Objects.requireNonNull(id, "id");
        this.phone = Objects.requireNonNull(phone, "phone");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.name = name;
        this.status = Objects.requireNonNull(status, "status");
        this.claim = claim;
        this.rejectionReason = rejectionReason;
        this.roles = new ArrayList<>(roles);
    }

    /** Someone proved they own this phone number for the first time. */
    public static AppUser verified(UserId id, PhoneNumber phone, Instant at) {
        return new AppUser(id, phone, at, null, UserStatus.VERIFIED, null, null, List.of());
    }

    /** Asks the society to accept this user as a resident of the claimed flat. */
    public void register(String fullName, FlatClaim flatClaim, Instant at) {
        Objects.requireNonNull(flatClaim, "flatClaim");
        if (status != UserStatus.VERIFIED && status != UserStatus.REJECTED) {
            throw new InvalidIdentityException("Only a new or rejected user can register, but this user is " + status);
        }
        this.name = checkedName(fullName);
        this.claim = flatClaim;
        this.rejectionReason = null;
        this.status = UserStatus.PENDING;
        recordEvent(new RegistrationSubmitted(id, flatClaim, at));
    }

    public void approveRegistration(UserId approvedBy, Instant at) {
        requirePending();
        RoleAssignment resident = RoleAssignment.societyWide(Role.RESIDENT, claim.societyId());
        if (!roles.contains(resident)) {
            roles.add(resident);
        }
        status = UserStatus.ACTIVE;
        recordEvent(new RegistrationApproved(id, claim, Objects.requireNonNull(approvedBy, "approvedBy"), at));
    }

    public void rejectRegistration(UserId rejectedBy, String reason, Instant at) {
        requirePending();
        if (reason == null || reason.isBlank() || reason.strip().length() > MAX_REASON_LENGTH) {
            throw new InvalidIdentityException(
                    "Rejecting a registration needs a reason of at most " + MAX_REASON_LENGTH + " characters");
        }
        status = UserStatus.REJECTED;
        rejectionReason = reason.strip();
        recordEvent(new RegistrationRejected(id, claim, rejectionReason,
                Objects.requireNonNull(rejectedBy, "rejectedBy"), at));
    }

    /** Gives a role (staff, admin, ...). The user becomes ACTIVE. A pending registration must be decided first. */
    public void grantRole(RoleAssignment role, UserId grantedBy, Instant at) {
        Objects.requireNonNull(role, "role");
        if (status == UserStatus.PENDING) {
            throw new InvalidIdentityException("Approve or reject the pending registration before granting roles");
        }
        if (roles.contains(role)) {
            throw new InvalidIdentityException("The user already has this role");
        }
        roles.add(role);
        status = UserStatus.ACTIVE;
        recordEvent(new RoleGranted(id, role, Objects.requireNonNull(grantedBy, "grantedBy"), at));
    }

    public void revokeRole(RoleAssignment role, UserId revokedBy, Instant at) {
        if (!roles.remove(role)) {
            throw new InvalidIdentityException("The user does not have this role");
        }
        recordEvent(new RoleRevoked(id, role, Objects.requireNonNull(revokedBy, "revokedBy"), at));
    }

    /** True when the user has this role in the society, society-wide or for any tower. */
    public boolean hasRoleIn(Role role, SocietyId societyId) {
        return roles.stream().anyMatch(r -> r.role() == role && r.societyId().equals(societyId));
    }

    private void requirePending() {
        if (status != UserStatus.PENDING) {
            throw new InvalidIdentityException("There is no pending registration; the user is " + status);
        }
    }

    private static String checkedName(String fullName) {
        if (fullName == null || fullName.isBlank() || fullName.strip().length() > MAX_NAME_LENGTH) {
            throw new InvalidIdentityException("A name of at most " + MAX_NAME_LENGTH + " characters is needed");
        }
        return fullName.strip();
    }

    public UserId id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public UserStatus status() {
        return status;
    }

    public Optional<FlatClaim> claim() {
        return Optional.ofNullable(claim);
    }

    public Optional<String> rejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }

    public List<RoleAssignment> roles() {
        return List.copyOf(roles);
    }
}
