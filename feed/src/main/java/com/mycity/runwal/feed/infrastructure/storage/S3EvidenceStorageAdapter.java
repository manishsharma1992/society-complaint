package com.mycity.runwal.feed.infrastructure.storage;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.function.Function;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Feed photos in the private S3-compatible bucket (Lightsail Object Storage), under feed/... keys. Links are
 * presigned locally; copy, size, read and delete talk to the bucket. Owns its client and presigner and closes them.
 */
public class S3EvidenceStorageAdapter implements EvidenceStorage, AutoCloseable {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3EvidenceStorageAdapter(S3Client s3, S3Presigner presigner, String bucket) {
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
    public boolean copy(String fromKey, String toKey) {
        try {
            s3.copyObject(r -> r.sourceBucket(bucket).sourceKey(fromKey).destinationBucket(bucket).destinationKey(toKey));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public OptionalLong sizeOf(String storageKey) {
        try {
            return OptionalLong.of(s3.headObject(r -> r.bucket(bucket).key(storageKey)).contentLength());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return OptionalLong.empty();
            }
            throw e;
        }
    }

    @Override
    public <T> T read(String storageKey, Function<InputStream, T> reader) {
        try (InputStream in = s3.getObject(r -> r.bucket(bucket).key(storageKey))) {
            return reader.apply(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        s3.deleteObject(r -> r.bucket(bucket).key(storageKey));
    }

    @Override
    public URI viewUrl(String storageKey, Duration validFor) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(storageKey).build();
        return toUri(presigner.presignGetObject(r -> r.signatureDuration(validFor).getObjectRequest(get)).url());
    }

    @Override
    public void close() {
        presigner.close();
        s3.close();
    }

    private static URI toUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("S3 returned an invalid URL: " + url, e);
        }
    }
}
