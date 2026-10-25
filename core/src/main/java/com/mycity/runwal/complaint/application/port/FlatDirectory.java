package com.mycity.runwal.complaint.application.port;

import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Optional;
import java.util.Set;

/** What Complaint Management needs to know about flats, which the Residency context owns. */
public interface FlatDirectory {

    /** The tower the flat is in, or empty if the society has no such flat. */
    Optional<TowerId> towerOf(SocietyId societyId, FlatId flatId);

    /** The flats this person is linked to (owner or tenant) in the society. */
    Set<FlatId> flatsOf(SocietyId societyId, UserId userId);
}
