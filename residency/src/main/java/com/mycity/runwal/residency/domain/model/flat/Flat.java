package com.mycity.runwal.residency.domain.model.flat;

import com.mycity.runwal.residency.domain.event.FlatMemberAdded;
import com.mycity.runwal.residency.domain.event.FlatMemberRemoved;
import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.residency.domain.exception.MembershipNotFoundException;
import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A flat in a tower, with the people linked to it (owners and tenants). A person can be linked to several flats,
 * e.g. an owner of two neighbouring flats; each flat then lists that person once.
 */
@AggregateRoot
public class Flat extends AbstractAggregateRoot {

    private final FlatId id;
    private final SocietyId societyId;
    private final TowerId towerId;
    private final FlatNumber number;
    private final List<Membership> members;

    public Flat(FlatId id, SocietyId societyId, TowerId towerId, FlatNumber number, List<MembershipSnapshot> members) {
        this.id = Objects.requireNonNull(id, "id");
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        this.towerId = Objects.requireNonNull(towerId, "towerId");
        this.number = Objects.requireNonNull(number, "number");
        this.members = new ArrayList<>(members.stream().map(Membership::from).toList());
    }

    public void addMember(UserId userId, MembershipType type, UserId addedBy, Instant at) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(addedBy, "addedBy");
        Objects.requireNonNull(at, "at");
        if (hasMember(userId)) {
            throw new InvalidResidencyException("User " + userId.value() + " is already linked to flat " + id.value());
        }
        members.add(new Membership(userId, type, at));
        recordEvent(new FlatMemberAdded(id, societyId, userId, type, addedBy, at));
    }

    public void removeMember(UserId userId, UserId removedBy, Instant at) {
        Objects.requireNonNull(removedBy, "removedBy");
        Objects.requireNonNull(at, "at");
        if (!members.removeIf(member -> member.userId().equals(userId))) {
            throw new MembershipNotFoundException(id, userId);
        }
        recordEvent(new FlatMemberRemoved(id, societyId, userId, removedBy, at));
    }

    public boolean hasMember(UserId userId) {
        return members.stream().anyMatch(member -> member.userId().equals(userId));
    }

    public FlatId id() {
        return id;
    }

    public SocietyId societyId() {
        return societyId;
    }

    public TowerId towerId() {
        return towerId;
    }

    public FlatNumber number() {
        return number;
    }

    /** The people linked to this flat, in the order they were linked. */
    public List<MembershipSnapshot> members() {
        return members.stream().map(Membership::snapshot).toList();
    }
}
