package com.mycity.runwal.complaint.infrastructure.client;

import com.mycity.runwal.complaint.application.port.FlatDirectory;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Anticorruption layer to the Residency service: translates its flat representation into our {@link TowerId}.
 * Only used once Residency runs as its own service (set residency.base-url); while all contexts share one app,
 * the app module provides an in-process {@link FlatDirectory} instead.
 */
@Component
@ConditionalOnProperty("residency.base-url")
class ResidencyClient implements FlatDirectory {

    private final RestClient restClient;

    ResidencyClient(RestClient.Builder builder, @Value("${residency.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<TowerId> towerOf(SocietyId societyId, FlatId flatId) {
        return restClient.get()
                .uri("/societies/{societyId}/flats/{flatId}", societyId.value(), flatId.value())
                .exchange((request, response) -> {
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return Optional.<TowerId>empty();
                    }
                    if (response.getStatusCode().isError()) {
                        throw new IllegalStateException(
                                "Residency service answered " + response.getStatusCode() + " for flat " + flatId.value());
                    }
                    FlatResponse flat = response.bodyTo(FlatResponse.class);
                    return Optional.of(new TowerId(flat.towerId()));
                });
    }

    @Override
    public Set<FlatId> flatsOf(SocietyId societyId, UserId userId) {
        FlatResponse[] flats = restClient.get()
                .uri("/societies/{societyId}/flats?memberId={userId}", societyId.value(), userId.value())
                .retrieve()
                .body(FlatResponse[].class);
        return flats == null ? Set.of() : Arrays.stream(flats).map(f -> new FlatId(f.id())).collect(Collectors.toSet());
    }

    /** The part of the Residency service's flat representation that we use. */
    record FlatResponse(long id, long towerId) {
    }
}
