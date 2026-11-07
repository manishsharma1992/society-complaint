package com.mycity.runwal.feed;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

/** Small images made in memory, with and without the metadata a phone camera would embed. */
public final class TestImages {

    private TestImages() {
    }

    /** A clean JPEG, as the app's canvas re-encoding produces it (JFIF, no EXIF). */
    public static byte[] jpeg(int width, int height) {
        return encode("jpg", width, height);
    }

    public static byte[] png(int width, int height) {
        return encode("png", width, height);
    }

    /** A JPEG whose EXIF still holds a GPS position (latitude reference "N"). */
    public static byte[] jpegWithGps() {
        byte[] tiff = {
                'M', 'M', 0, 42, 0, 0, 0, 8,                      // big-endian TIFF, IFD0 at offset 8
                0, 1,                                             // IFD0: one entry
                (byte) 0x88, 0x25, 0, 4, 0, 0, 0, 1, 0, 0, 0, 26, // GPSInfo pointer -> GPS IFD at 26
                0, 0, 0, 0,                                       // no next IFD
                0, 1,                                             // GPS IFD: one entry
                0, 1, 0, 2, 0, 0, 0, 2, 'N', 0, 0, 0,             // GPSLatitudeRef = "N"
                0, 0, 0, 0};
        return withApp1(jpeg(40, 30), concat("Exif\0\0".getBytes(StandardCharsets.US_ASCII), tiff));
    }

    /** A JPEG carrying an (empty) XMP packet. */
    public static byte[] jpegWithXmp() {
        String xmp = "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF "
                + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"></rdf:RDF></x:xmpmeta>";
        return withApp1(jpeg(40, 30), ("http://ns.adobe.com/xap/1.0/\0" + xmp).getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] encode(String format, int width, int height) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Inserts an APP1 segment right after the JPEG's start-of-image marker. */
    private static byte[] withApp1(byte[] jpeg, byte[] payload) {
        int length = payload.length + 2;
        byte[] segment = concat(new byte[] {(byte) 0xFF, (byte) 0xE1, (byte) (length >> 8), (byte) length}, payload);
        byte[] soi = {jpeg[0], jpeg[1]};
        byte[] rest = new byte[jpeg.length - 2];
        System.arraycopy(jpeg, 2, rest, 0, rest.length);
        return concat(concat(soi, segment), rest);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
