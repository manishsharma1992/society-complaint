package com.mycity.runwal.feed.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.feed.TestImages;
import com.mycity.runwal.feed.domain.model.post.ImageFacts;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MetadataExtractorImageInspectorTest {

    private final MetadataExtractorImageInspector inspector = new MetadataExtractorImageInspector();

    private ImageFacts inspect(byte[] file) {
        return inspector.inspect(new ByteArrayInputStream(file));
    }

    @Test
    void cleanJpegHasItsRealTypeAndSizeAndNoMetadata() {
        assertThat(inspect(TestImages.jpeg(64, 48))).isEqualTo(new ImageFacts("image/jpeg", 64, 48, java.util.Set.of()));
    }

    @Test
    void typeComesFromTheBytesNotTheName() {
        assertThat(inspect(TestImages.png(10, 20))).satisfies(facts -> {
            assertThat(facts.contentType()).isEqualTo("image/png");
            assertThat(facts.widthPx()).isEqualTo(10);
            assertThat(facts.heightPx()).isEqualTo(20);
        });
    }

    @Test
    void gpsAndExifAreFound() {
        assertThat(inspect(TestImages.jpegWithGps()).leftoverMetadata())
                .contains("GPS location", "EXIF data (camera and device details)");
    }

    @Test
    void xmpIsFound() {
        assertThat(inspect(TestImages.jpegWithXmp()).leftoverMetadata()).containsExactly("XMP metadata");
    }

    @Test
    void anythingElseIsNotAnImage() {
        assertThat(inspect("<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(ImageFacts.notAnImage());
        assertThat(inspect(new byte[0])).isEqualTo(ImageFacts.notAnImage());
        byte[] truncated = java.util.Arrays.copyOf(TestImages.jpeg(64, 48), 20);
        assertThat(inspect(truncated).contentType()).isNull();
    }
}
