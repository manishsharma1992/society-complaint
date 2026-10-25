package com.mycity.runwal.complaint.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Presigning happens locally, so this runs without network or real credentials. */
class S3PhotoStorageAdapterTest {

    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_SOUTHEAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATEST", "secret")))
            .build();
    private final S3PhotoStorageAdapter storage = new S3PhotoStorageAdapter(null, presigner, "runwal-complaint-photos");

    @AfterEach
    void close() {
        presigner.close();
    }

    @Test
    void uploadLinkIsAPutToThisKeyWithTypeAndSizeSigned() {
        URI url = storage.uploadUrl("complaints/1/7/u1.jpg", "image/jpeg", 250_000, Duration.ofMinutes(10));

        assertThat(url.getHost()).isEqualTo("runwal-complaint-photos.s3.ap-southeast-1.amazonaws.com");
        assertThat(url.getPath()).isEqualTo("/complaints/1/7/u1.jpg");
        assertThat(url.getQuery())
                .contains("X-Amz-Expires=600")
                .contains("X-Amz-SignedHeaders=content-length;content-type;host");
    }

    @Test
    void viewLinkExpires() {
        URI url = storage.viewUrl("complaints/1/7/u1.jpg", Duration.ofMinutes(15));

        assertThat(url.getHost()).isEqualTo("runwal-complaint-photos.s3.ap-southeast-1.amazonaws.com");
        assertThat(url.getQuery()).contains("X-Amz-Expires=900").contains("X-Amz-Signature=");
    }
}
