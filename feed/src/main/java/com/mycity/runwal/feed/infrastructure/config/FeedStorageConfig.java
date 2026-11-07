package com.mycity.runwal.feed.infrastructure.config;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.infrastructure.storage.S3EvidenceStorageAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Feed photos use the same private bucket and credentials as complaint photos (the photos.* settings), under
 * feed/... keys. The S3 client and presigner are created here rather than as beans: other contexts in this app
 * inject S3Client by type, and a second bean would make that ambiguous.
 */
@Configuration
class FeedStorageConfig {

    @Bean(destroyMethod = "close")
    EvidenceStorage feedEvidenceStorage(@Value("${photos.bucket}") String bucket,
                                        @Value("${photos.region}") String region,
                                        @Value("${photos.access-key}") String accessKey,
                                        @Value("${photos.secret-key}") String secretKey) {
        StaticCredentialsProvider credentials =
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        S3Client s3 = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        S3Presigner presigner = S3Presigner.builder().region(Region.of(region)).credentialsProvider(credentials).build();
        return new S3EvidenceStorageAdapter(s3, presigner, bucket);
    }
}
