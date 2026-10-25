package com.mycity.runwal.complaint.infrastructure.config;

import com.mycity.runwal.complaint.application.port.PhotoStorage;
import com.mycity.runwal.complaint.infrastructure.storage.S3PhotoStorageAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** The photo bucket. Credentials come only from environment variables (see application.yml). */
@Configuration
class PhotoStorageConfig {

    @Bean
    StaticCredentialsProvider photoBucketCredentials(@Value("${photos.access-key}") String accessKey,
                                                     @Value("${photos.secret-key}") String secretKey) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    @Bean(destroyMethod = "close")
    S3Client photoBucketClient(@Value("${photos.region}") String region, StaticCredentialsProvider credentials) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner photoBucketPresigner(@Value("${photos.region}") String region, StaticCredentialsProvider credentials) {
        return S3Presigner.builder().region(Region.of(region)).credentialsProvider(credentials).build();
    }

    @Bean
    PhotoStorage photoStorage(S3Client s3, S3Presigner presigner, @Value("${photos.bucket}") String bucket) {
        return new S3PhotoStorageAdapter(s3, presigner, bucket);
    }
}
