package com.mycity.runwal.feed.infrastructure.storage;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import com.mycity.runwal.feed.application.port.ImageInspector;
import com.mycity.runwal.feed.domain.model.post.ImageFacts;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reads image headers with the metadata-extractor library (pure Java, no image decoding, so a "decompression
 * bomb" is never expanded). Any EXIF at all counts as leftover metadata: the app re-encodes photos through a canvas,
 * which writes none, so EXIF can only mean a photo that skipped that step.
 */
@Component
public class MetadataExtractorImageInspector implements ImageInspector {

    private static final Map<FileType, String> CONTENT_TYPES =
            Map.of(FileType.Jpeg, "image/jpeg", FileType.Png, "image/png", FileType.WebP, "image/webp");

    @Override
    public ImageFacts inspect(InputStream file) {
        try {
            BufferedInputStream in = new BufferedInputStream(file);
            String contentType = CONTENT_TYPES.get(FileTypeDetector.detectFileType(in));
            if (contentType == null) {
                return ImageFacts.notAnImage();
            }
            Metadata metadata = ImageMetadataReader.readMetadata(in);
            int[] size = sizeOf(metadata);
            if (size == null) {
                return ImageFacts.notAnImage();
            }
            return new ImageFacts(contentType, size[0], size[1], leftovers(metadata));
        } catch (IOException | ImageProcessingException | RuntimeException unreadable) {
            return ImageFacts.notAnImage();
        }
    }

    /** Width and height from the JPEG frame, PNG header or WebP header; null when there is none. */
    private static int[] sizeOf(Metadata metadata) {
        for (Directory directory : metadata.getDirectories()) {
            int[] size = switch (directory) {
                case JpegDirectory d -> pair(d, JpegDirectory.TAG_IMAGE_WIDTH, JpegDirectory.TAG_IMAGE_HEIGHT);
                case PngDirectory d -> pair(d, PngDirectory.TAG_IMAGE_WIDTH, PngDirectory.TAG_IMAGE_HEIGHT);
                case WebpDirectory d -> pair(d, WebpDirectory.TAG_IMAGE_WIDTH, WebpDirectory.TAG_IMAGE_HEIGHT);
                default -> null;
            };
            if (size != null) {
                return size;
            }
        }
        return null;
    }

    private static int[] pair(Directory directory, int widthTag, int heightTag) {
        Integer width = directory.getInteger(widthTag);
        Integer height = directory.getInteger(heightTag);
        return width == null || height == null ? null : new int[] {width, height};
    }

    private static Set<String> leftovers(Metadata metadata) {
        Set<String> found = new HashSet<>();
        for (Directory directory : metadata.getDirectories()) {
            switch (directory) {
                case GpsDirectory ignored -> found.add("GPS location");
                case ExifDirectoryBase ignored -> found.add("EXIF data (camera and device details)");
                case XmpDirectory ignored -> found.add("XMP metadata");
                case IptcDirectory ignored -> found.add("IPTC metadata");
                default -> {
                }
            }
        }
        return found;
    }
}
