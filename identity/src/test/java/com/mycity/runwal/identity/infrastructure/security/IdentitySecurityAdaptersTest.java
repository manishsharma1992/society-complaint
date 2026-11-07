package com.mycity.runwal.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class IdentitySecurityAdaptersTest {

    private static final SecretKey KEY =
            new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private static final PhoneNumber PHONE = PhoneNumber.parse("9876543210");

    @Test
    void tokenCarriesUserStatusAndScopedRoles() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        AppUser admin = new AppUser(new UserId(2), PHONE, now, "Tower Admin", UserStatus.ACTIVE, null, null,
                List.of(new RoleAssignment(Role.TOWER_ADMIN, new SocietyId(1), new TowerId(7)),
                        RoleAssignment.societyWide(Role.RESIDENT, new SocietyId(1))));

        String token = new JwtAccessTokenIssuer(KEY).issue(admin, now, now.plusSeconds(3600));
        Jwt jwt = NimbusJwtDecoder.withSecretKey(KEY).build().decode(token);

        assertThat(jwt.getSubject()).isEqualTo("2");
        assertThat(jwt.getClaimAsString("status")).isEqualTo("ACTIVE");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("TOWER_ADMIN:1:7", "RESIDENT:1");
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plusSeconds(3600));
        assertThat(token).doesNotContain("Tower Admin").doesNotContain("9876543210");
    }

    @Test
    void tokenSaysWhetherTheUserRunsThePlatform() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        AppUser owner = new AppUser(new UserId(1), PHONE, now, "Owner", UserStatus.ACTIVE, null, null,
                List.of(RoleAssignment.societyWide(Role.SOCIETY_ADMIN, new SocietyId(1))), true);
        AppUser resident = new AppUser(new UserId(4), PHONE, now, "Resident", UserStatus.ACTIVE, null, null, List.of());
        JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(KEY);

        assertThat(NimbusJwtDecoder.withSecretKey(KEY).build().decode(issuer.issue(owner, now, now.plusSeconds(60)))
                .getClaimAsBoolean("platformOwner")).isTrue();
        assertThat(NimbusJwtDecoder.withSecretKey(KEY).build().decode(issuer.issue(resident, now, now.plusSeconds(60)))
                .getClaimAsBoolean("platformOwner")).isFalse();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRefused() {
        SecretKey other = new SecretKeySpec("another-key-another-key-another-k".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        String token = new JwtAccessTokenIssuer(other).issue(
                AppUser.verified(new UserId(5), PHONE, Instant.now()), Instant.now(), Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> NimbusJwtDecoder.withSecretKey(KEY).build().decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void otpHashIsKeyedAndPerPhone() {
        HmacOtpHasher hasher = new HmacOtpHasher(KEY);

        assertThat(hasher.hash(PHONE, "123456")).hasSize(64).isEqualTo(hasher.hash(PHONE, "123456"));
        assertThat(hasher.hash(PHONE, "123456")).isNotEqualTo(hasher.hash(PhoneNumber.parse("9876543211"), "123456"));
        assertThat(hasher.hash(PHONE, "123456")).isNotEqualTo(new HmacOtpHasher(
                new SecretKeySpec("x".repeat(32).getBytes(StandardCharsets.UTF_8), "HmacSHA256")).hash(PHONE, "123456"));
    }

    @Test
    void demoNumbersGetTheDemoCodeOthersRandomCodes() {
        PhoneNumber demo = PhoneNumber.parse("9000000004");
        RandomOtpCodeSource codes = new RandomOtpCodeSource(Set.of(demo), "246810");

        assertThat(codes.newCode(demo)).isEqualTo("246810");
        assertThat(codes.isDemo(PHONE)).isFalse();
        assertThat(codes.newCode(PHONE)).matches("\\d{6}");
    }

    @Test
    void withoutDemoCodeThereAreNoDemoNumbers() {
        PhoneNumber demo = PhoneNumber.parse("9000000004");
        RandomOtpCodeSource codes = new RandomOtpCodeSource(Set.of(demo), "");

        assertThat(codes.isDemo(demo)).isFalse();
        assertThatThrownBy(() -> new RandomOtpCodeSource(Set.of(demo), "1234"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
