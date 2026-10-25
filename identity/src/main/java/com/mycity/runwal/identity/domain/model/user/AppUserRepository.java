package com.mycity.runwal.identity.domain.model.user;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface AppUserRepository {

    UserId nextId();

    void save(AppUser user);

    Optional<AppUser> findById(UserId id);

    Optional<AppUser> findByPhone(PhoneNumber phone);

    /** Users whose registration for a flat in this society waits for approval, oldest first. */
    List<AppUser> findPendingRegistrations(SocietyId societyId);

    /** The users that exist among these IDs, in no particular order. */
    List<AppUser> findAllById(Collection<UserId> ids);

    /** Active users with this role in the society (any tower), by name. */
    List<AppUser> findWithRole(Role role, SocietyId societyId);
}
