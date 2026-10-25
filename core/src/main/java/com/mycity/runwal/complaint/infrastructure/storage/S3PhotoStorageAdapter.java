package com.mycity.runwal.complaint.infrastructure.storage;

import com.mycity.runwal.complaint.application.port.PhotoStorage;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.OptionalLong;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Photos in a private S3-compatible bucket (Lightsail Object Storage). Links are presigned locally,
 * so creating them costs no network call; only {@link #sizeOf} talks to the bucket.
 */
public class S3PhotoStorageAdapter implements PhotoStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3PhotoStorageAdapter(S3Client s3, S3Presigner presigner, String bucket) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    /** Content-Type and Content-Length are signed, so the upload must send exactly these values. */
    @Override
    public URI uploadUrl(String storageKey, String contentType, long sizeBytes, Duration validFor) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .contentLength(sizeBytes)
                .build();
        return toUri(presigner.presignPutObject(r -> r.signatureDuration(validFor).putObjectRequest(put)).url());
    }

    @Override
    public OptionalLong sizeOf(String storageKey) {
        try {
            return OptionalLong.of(s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(storageKey).build())
                    .contentLength());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return OptionalLong.empty();
            }
            throw e;
        }
    }

    @Override
    public URI viewUrl(String storageKey, Duration validFor) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(storageKey).build();
        return toUri(presigner.presignGetObject(r -> r.signatureDuration(validFor).getObjectRequest(get)).url());
    }

    private static URI toUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("S3 returned an invalid URL: " + url, e);
        }
    }
}
