package com.mycity.runwal.identity.application.service;

import com.mycity.runwal.identity.application.port.AccessTokenIssuer;
import com.mycity.runwal.identity.application.port.CurrentLogin;
import com.mycity.runwal.identity.application.port.CurrentUser;
import com.mycity.runwal.identity.application.query.SessionView;
import com.mycity.runwal.identity.application.query.UserView;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renews the login token from the stored user, so an approval or a new role takes effect without a new code.
 * The renewed token ends when the login ends: renewing never extends a login.
 */
@ApplicationService
public class SessionService {

    private final AppUserRepository users;
    private final CurrentUser currentUser;
    private final CurrentLogin currentLogin;
    private final AccessTokenIssuer tokenIssuer;
    private final Clock clock;

    public SessionService(AppUserRepository users, CurrentUser currentUser, CurrentLogin currentLogin,
                          AccessTokenIssuer tokenIssuer, Clock clock) {
        this.users = users;
        this.currentUser = currentUser;
        this.currentLogin = currentLogin;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<SessionView> renew() {
        Instant now = clock.instant();
        Instant endsAt = currentLogin.endsAt();
        return users.findById(currentUser.userId())
                .map(user -> new SessionView(tokenIssuer.issue(user, now, endsAt), endsAt, UserView.of(user)));
    }
}
