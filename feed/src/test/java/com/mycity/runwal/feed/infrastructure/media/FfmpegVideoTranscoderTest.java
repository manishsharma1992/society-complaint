package com.mycity.runwal.feed.infrastructure.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.model.post.VideoFacts;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the real ffprobe and ffmpeg on videos made here with ffmpeg's own test source. Skipped when ffmpeg is not
 * installed (on PATH, or in ~/bin).
 */
class FfmpegVideoTranscoderTest {

    private static String toolsDir;

    @TempDir
    static Path fixtures;

    @TempDir
    Path work;

    @BeforeAll
    static void findFfmpeg() {
        Optional<Path> dir = Stream.concat(
                        Stream.of(System.getenv().getOrDefault("PATH", "").split(":")).map(Path::of),
                        Stream.of(Path.of(System.getProperty("user.home"), "bin")))
                .filter(d -> Files.isExecutable(d.resolve("ffmpeg")) && Files.isExecutable(d.resolve("ffprobe")))
                .findFirst();
        assumeTrue(dir.isPresent(), "ffmpeg and ffprobe are not installed");
        toolsDir = dir.get().toString();
    }

    private FfmpegVideoTranscoder transcoder() {
        return new FfmpegVideoTranscoder(toolsDir, work.resolve("media"));
    }

    /** Makes a test video; {@code args} go between the generated input and the output file. */
    private static Path video(String name, String source, String... args) throws Exception {
        Path out = fixtures.resolve(name);
        List<String> command = new ArrayList<>(List.of(toolsDir + "/ffmpeg", "-hide_banner", "-loglevel", "error",
                "-f", "lavfi", "-i", source));
        command.addAll(List.of(args));
        command.addAll(List.of("-y", out.toString()));
        run(command);
        return out;
    }

    private static String run(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor(2, TimeUnit.MINUTES)).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        return output;
    }

    private static String probeAll(Path file) throws Exception {
        return run(List.of(toolsDir + "/ffprobe", "-v", "error", "-show_format", "-show_streams", "-show_data",
                "-show_entries", "stream_side_data", file.toString()));
    }

    /** A phone video: landscape pixels shown upright (rotated), with the phone's location and model. */
    private static Path phoneVideo() throws Exception {
        Path plain = video("plain.mov", "testsrc2=size=640x360:rate=30:duration=3",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=3",
                "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac",
                "-movflags", "use_metadata_tags",
                "-metadata", "location=+19.1860+073.0748/",
                "-metadata", "com.apple.quicktime.location.ISO6709=+19.1860+073.0748+010.000/",
                "-metadata", "com.apple.quicktime.make=Apple",
                "-metadata", "com.apple.quicktime.model=iPhone 13",
                "-metadata", "creation_time=2026-08-06T01:43:31Z");
        Path rotated = fixtures.resolve("phone.mov");
        run(List.of(toolsDir + "/ffmpeg", "-hide_banner", "-loglevel", "error", "-display_rotation", "90",
                "-i", plain.toString(), "-c", "copy", "-map_metadata", "0", "-movflags", "use_metadata_tags",
                "-y", rotated.toString()));
        assertThat(probeAll(rotated)).contains("19.1860").contains("iPhone 13").containsIgnoringCase("rotation");
        return rotated;
    }

    private static InputStream open(Path file) throws IOException {
        return Files.newInputStream(file);
    }

    @Test
    void aPhoneVideoComesOutUprightAsH264WithoutLocationOrDeviceAndWithAPoster() throws Exception {
        Path original = phoneVideo();
        Path keptVideo = fixtures.resolve("kept.mp4");
        Path keptPoster = fixtures.resolve("kept.jpg");

        VideoFacts facts;
        try (InputStream in = open(original)) {
            facts = transcoder().transcode(in, (playable, poster) -> {
                try {
                    Files.copy(playable, keptVideo);
                    Files.copy(poster, keptPoster);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }

        assertThat(facts.widthPx()).isEqualTo(360);
        assertThat(facts.heightPx()).isEqualTo(640);
        assertThat(facts.durationMs()).isBetween(2_900L, 3_200L);
        String probe = probeAll(keptVideo);
        assertThat(probe).contains("codec_name=h264").contains("codec_name=aac")
                .doesNotContain("19.1860").doesNotContain("073.0748")
                .doesNotContainIgnoringCase("iphone").doesNotContainIgnoringCase("com.apple")
                .doesNotContain("creation_time").doesNotContain("encoder=Lavf")
                .doesNotContainIgnoringCase("rotation=");
        assertThat(probeAll(keptPoster)).contains("codec_name=mjpeg").contains("width=360").contains("height=640");
        try (Stream<Path> left = Files.list(work.resolve("media"))) {
            assertThat(left).as("temp files are deleted").isEmpty();
        }
    }

    @Test
    void aLandscapeVideoIsCappedAt1280By720() throws Exception {
        Path original = video("wide.mp4", "testsrc2=size=1920x1080:rate=60:duration=1",
                "-c:v", "libx264", "-preset", "ultrafast");

        VideoFacts facts;
        try (InputStream in = open(original)) {
            facts = transcoder().transcode(in, (playable, poster) -> { });
        }

        assertThat(facts.widthPx()).isEqualTo(1280);
        assertThat(facts.heightPx()).isEqualTo(720);
    }

    @Test
    void longerThanSixtySecondsIsRejectedBeforeItIsEncoded() throws Exception {
        Path original = video("long.mp4", "color=c=gray:size=32x32:rate=1:duration=61",
                "-c:v", "libx264", "-preset", "ultrafast");

        try (InputStream in = open(original)) {
            assertThatThrownBy(() -> transcoder().transcode(in, (playable, poster) -> { }))
                    .isInstanceOf(EvidenceRejectedException.class)
                    .hasMessageContaining("60 seconds");
        }
    }

    @Test
    void twoPictureTracksAreRejected() throws Exception {
        Path original = video("two.mp4", "testsrc2=size=64x64:duration=1",
                "-f", "lavfi", "-i", "testsrc2=size=64x64:duration=1",
                "-map", "0", "-map", "1", "-c:v", "libx264", "-preset", "ultrafast");

        try (InputStream in = open(original)) {
            assertThatThrownBy(() -> transcoder().transcode(in, (playable, poster) -> { }))
                    .isInstanceOf(EvidenceRejectedException.class)
                    .hasMessageContaining("one picture track");
        }
    }

    @Test
    void aFileThatIsNotAVideoIsRejectedAndNothingIsLeftBehind() throws Exception {
        Path text = fixtures.resolve("notes.mov");
        Files.writeString(text, "this is not a video");

        try (InputStream in = open(text)) {
            assertThatThrownBy(() -> transcoder().transcode(in, (playable, poster) -> { }))
                    .isInstanceOf(EvidenceRejectedException.class)
                    .hasMessageContaining("not an MP4, MOV or WebM");
        }
        try (Stream<Path> left = Files.list(work.resolve("media"))) {
            assertThat(left).isEmpty();
        }
    }

    @Test
    void aStillImageIsNotAVideo() throws Exception {
        Path png = video("still.png", "color=c=red:size=64x64", "-frames:v", "1");

        try (InputStream in = open(png)) {
            assertThatThrownBy(() -> transcoder().transcode(in, (playable, poster) -> { }))
                    .isInstanceOf(EvidenceRejectedException.class);
        }
    }
}
