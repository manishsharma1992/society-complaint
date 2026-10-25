package com.mycity.runwal.complaint.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResidencyClientTest {

    private static final String FLAT_URL = "http://residency/societies/1/flats/1204";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ResidencyClient client = new ResidencyClient(builder, "http://residency");

    @Test
    void translatesFlatToItsTower() {
        server.expect(requestTo(FLAT_URL)).andRespond(withSuccess(
                "{\"id\":1204,\"towerId\":3,\"number\":\"1204\"}", MediaType.APPLICATION_JSON));

        assertThat(client.towerOf(new SocietyId(1), new FlatId(1204))).contains(new TowerId(3));
    }

    @Test
    void unknownFlatIsEmpty() {
        server.expect(requestTo(FLAT_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.towerOf(new SocietyId(1), new FlatId(1204))).isEmpty();
    }

    @Test
    void serverErrorIsNotMistakenForUnknownFlat() {
        server.expect(requestTo(FLAT_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.towerOf(new SocietyId(1), new FlatId(1204)))
                .isInstanceOf(IllegalStateException.class);
    }
}
