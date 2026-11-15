package com.mycity.runwal.feed.infrastructure.media;

import com.mycity.runwal.feed.application.port.VideoTranscoder;
import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.model.post.Video;
import com.mycity.runwal.feed.domain.model.post.VideoFacts;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Re-encodes videos with FFmpeg. The upload is untrusted, so: ffprobe checks it before ffmpeg ever decodes it, both
 * tools only read local files ({@code -protocol_whitelist file}), they are started with an argument list (never a
 * shell), each run is killed after a timeout, and every file is made in a directory of its own that is deleted
 * afterwards.
 */
@Component
public class FfmpegVideoTranscoder implements VideoTranscoder {

    static final Duration PROBE_TIMEOUT = Duration.ofSeconds(30);
    static final Duration ENCODE_TIMEOUT = Duration.ofMinutes(5);

    /** Demuxer names ffprobe reports for MP4/MOV and for WebM. */
    private static final List<String> ACCEPTED_FORMATS = List.of("mov,mp4,m4a,3gp,3g2,mj2", "matroska,webm");

    private final String ffmpeg;
    private final String ffprobe;
    private final Path workRoot;

    public FfmpegVideoTranscoder(@Value("${feed.media.tools-dir:}") String toolsDir,
                                 @Value("${feed.media.work-dir:${java.io.tmpdir}/feed-media}") Path workRoot) {
        this.ffmpeg = toolsDir.isBlank() ? "ffmpeg" : Path.of(toolsDir, "ffmpeg").toString();
        this.ffprobe = toolsDir.isBlank() ? "ffprobe" : Path.of(toolsDir, "ffprobe").toString();
        this.workRoot = workRoot;
    }

    @Override
    public VideoFacts transcode(InputStream original, BiConsumer<Path, Path> store) {
        Path work = null;
        try {
            Files.createDirectories(workRoot);
            work = Files.createTempDirectory(workRoot, "video-");
            Path in = work.resolve("original");
            Path playable = work.resolve("playable.mp4");
            Path poster = work.resolve("poster.jpg");
            Files.copy(original, in);
            if (Files.size(in) > Video.MAX_SIZE_BYTES) {
                throw new EvidenceRejectedException("A video can be at most " + Video.MAX_SIZE_BYTES / (1024 * 1024) + " MB");
            }

            checkOriginal(probe(in, work));
            run(work, ENCODE_TIMEOUT, List.of(ffmpeg, "-hide_banner", "-nostdin", "-protocol_whitelist", "file",
                    "-i", "file:" + in,
                    "-map", "0:v:0", "-map", "0:a:0?", "-map_metadata", "-1", "-map_chapters", "-1",
                    // 720p either way round: the long side at most 1280 px (phone videos are mostly portrait); even sizes for x264
                    "-vf", "scale='if(gt(iw,ih),min(1280,trunc(iw/2)*2),-2)':'if(gt(iw,ih),-2,min(1280,trunc(ih/2)*2))',"
                            + "fps='min(30,source_fps)'",
                    "-c:v", "libx264", "-preset", "medium", "-crf", "26", "-maxrate", "2M", "-bufsize", "4M",
                    "-profile:v", "high", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-b:a", "96k", "-ac", "2",
                    "-movflags", "+faststart", "-fflags", "+bitexact", "-flags:v", "+bitexact", "-flags:a", "+bitexact",
                    "-y", "file:" + playable));

            Probe out = probe(playable, work);
            VideoFacts facts = out.facts();
            String at = String.format(Locale.ROOT, "%.3f", Math.min(1.0, out.durationSeconds() / 2));
            run(work, PROBE_TIMEOUT, List.of(ffmpeg, "-hide_banner", "-nostdin", "-protocol_whitelist", "file",
                    "-ss", at, "-i", "file:" + playable, "-frames:v", "1", "-map_metadata", "-1",
                    "-q:v", "3", "-fflags", "+bitexact", "-y", "file:" + poster));
            if (!Files.isRegularFile(poster) || Files.size(poster) == 0) {
                throw new IllegalStateException("ffmpeg made no poster frame");
            }
            store.accept(playable, poster);
            return facts;
        } catch (IOException e) {
            throw new UncheckedIOException("Video processing failed", e);
        } finally {
            deleteQuietly(work);
        }
    }

    /** Only a real MP4, MOV or WebM with exactly one video stream, at most 60 seconds long, is ever decoded. */
    private static void checkOriginal(Probe probe) {
        if (!ACCEPTED_FORMATS.contains(probe.formatName()) || probe.videoStreams() == 0) {
            throw new EvidenceRejectedException("This file is not an MP4, MOV or WebM video");
        }
        if (probe.videoStreams() > 1) {
            throw new EvidenceRejectedException("A video can have only one picture track");
        }
        if (probe.durationSeconds() <= 0) {
            throw new EvidenceRejectedException("The length of this video cannot be read");
        }
        if (probe.durationSeconds() * 1000 > Video.MAX_DURATION_MS) {
            throw new EvidenceRejectedException("A video can be at most " + Video.MAX_DURATION_MS / 1000 + " seconds long");
        }
    }

    private Probe probe(Path file, Path work) {
        String output = run(work, PROBE_TIMEOUT, List.of(ffprobe, "-v", "error", "-protocol_whitelist", "file",
                "-show_entries", "format=format_name,duration:stream=codec_type,width,height",
                "-of", "default=noprint_wrappers=1", "file:" + file));
        return Probe.parse(output);
    }

    /** Runs a tool and returns what it printed; a tool that fails or takes too long is killed. */
    private static String run(Path work, Duration timeout, List<String> command) {
        Path stdout = work.resolve("stdout.txt");
        Path stderr = work.resolve("stderr.txt");
        Process process = null;
        try {
            process = new ProcessBuilder(command).directory(work.toFile())
                    .redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(toolName(command) + " took longer than " + timeout.toSeconds() + " s");
            }
            if (process.exitValue() != 0) {
                return rejectOrFail(command, process.exitValue(), Files.readString(stderr, StandardCharsets.UTF_8));
            }
            return Files.readString(stdout, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not run " + toolName(command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(toolName(command) + " was interrupted", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** ffprobe failing on the upload means it is not a readable video; any other failure is ours. */
    private static String rejectOrFail(List<String> command, int exitCode, String stderr) {
        if (command.getFirst().endsWith("ffprobe") && command.getLast().endsWith("original")) {
            throw new EvidenceRejectedException("This file is not an MP4, MOV or WebM video");
        }
        String tail = stderr.length() > 500 ? stderr.substring(stderr.length() - 500) : stderr;
        throw new IllegalStateException(toolName(command) + " failed with exit code " + exitCode + ": " + tail.strip());
    }

    private static String toolName(List<String> command) {
        return Path.of(command.getFirst()).getFileName().toString();
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // a leftover temp directory is harmless and is reused by nobody
        }
    }

    /** What ffprobe printed, one "key=value" per line: the container, then each stream's type and size. */
    record Probe(String formatName, double durationSeconds, int videoStreams, int widthPx, int heightPx) {

        static Probe parse(String output) {
            Map<String, String> format = new HashMap<>();
            List<int[]> videoSizes = new ArrayList<>();
            String streamType = null;
            for (String line : output.split("\\R")) {
                int eq = line.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String key = line.substring(0, eq);
                String value = line.substring(eq + 1).strip();
                switch (key) {
                    case "codec_type" -> {
                        streamType = value;
                        if (value.equals("video")) {
                            videoSizes.add(new int[] {0, 0});
                        }
                    }
                    case "width" -> { if ("video".equals(streamType)) videoSizes.getLast()[0] = toInt(value); }
                    case "height" -> { if ("video".equals(streamType)) videoSizes.getLast()[1] = toInt(value); }
                    default -> format.put(key, value);
                }
            }
            int[] size = videoSizes.isEmpty() ? new int[] {0, 0} : videoSizes.getFirst();
            return new Probe(format.getOrDefault("format_name", ""), toDouble(format.get("duration")),
                    videoSizes.size(), size[0], size[1]);
        }

        VideoFacts facts() {
            return new VideoFacts(widthPx, heightPx, Math.round(durationSeconds * 1000));
        }

        private static int toInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static double toDouble(String value) {
            try {
                return value == null ? 0 : Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
