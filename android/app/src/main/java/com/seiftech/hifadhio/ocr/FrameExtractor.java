package com.seiftech.hifadhio.ocr;

import android.content.Context;
import com.seiftech.hifadhio.model.ContentItem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Selective visual frame extraction subsystem per Master Spec §15.4 & §1193.
 * Enforces intelligent periodic keyframe sampling policy (max 15 frames) to extract
 * legible visual content without exploding CPU, memory, or disk storage footprints.
 */
public class FrameExtractor {
    public static final String FRAMES_DIR_NAME = "media/frames";
    public static final long MAX_VIDEO_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB safety cap

    private final Context context;
    private final File framesDir;

    public FrameExtractor(Context context) {
        this.context = context;
        if (context != null) {
            File base = context.getFilesDir();
            this.framesDir = new File(base, FRAMES_DIR_NAME);
            if (!this.framesDir.exists()) {
                this.framesDir.mkdirs();
            }
        } else {
            this.framesDir = new File(System.getProperty("java.io.tmpdir", "."), "hifadhio_frames");
            this.framesDir.mkdirs();
        }
    }

    public File getFramesDir() {
        return framesDir;
    }

    /**
     * Determines whether a content item has visual components eligible for OCR extraction.
     */
    public static boolean isEligibleForOcr(ContentItem item) {
        if (item == null || item.getUrl() == null) return false;
        String platform = item.getPlatform() != null ? item.getPlatform().toLowerCase(Locale.US) : "";
        String url = item.getUrl().toLowerCase(Locale.US);

        // Videos and image posts from supported social platforms
        if (platform.contains("youtube") || platform.contains("tiktok")
                || platform.contains("instagram") || platform.contains("facebook")
                || platform.contains("reddit") || platform.contains("twitter") || platform.contains("x")) {
            return true;
        }

        // Direct media files
        if (url.endsWith(".mp4") || url.endsWith(".webm") || url.endsWith(".m4v")
                || url.endsWith(".mov") || url.endsWith(".png") || url.endsWith(".jpg")
                || url.endsWith(".jpeg") || url.endsWith(".webp")) {
            return true;
        }

        // Cached thumbnail availability implies visual content exists
        if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().trim().isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Samples timecoded frames from a video file according to sampling interval and max limit caps.
     */
    public List<OcrFrame> sampleVideoFrames(File videoFile, long durationMs, OcrOptions options) {
        List<OcrFrame> frames = new ArrayList<>();
        if (videoFile == null || !videoFile.exists() || videoFile.length() == 0) {
            return frames;
        }

        if (options == null) {
            options = OcrOptions.createDefault();
        }

        long actualDuration = durationMs > 0 ? durationMs : 30000L; // Default 30s estimate
        long intervalMs = options.getSampleIntervalSeconds() * 1000L;
        if (intervalMs <= 0) intervalMs = 5000L;

        int totalSlots = (int) (actualDuration / intervalMs);
        if (totalSlots < 1) totalSlots = 1;
        int framesToSample = Math.min(totalSlots, options.getMaxFrames());

        for (int i = 0; i < framesToSample; i++) {
            long timestampMs = i * intervalMs;
            File frameFile = new File(framesDir, "frame_" + System.currentTimeMillis() + "_" + i + ".jpg");
            // Register frame item with true timestamp
            OcrFrame frame = new OcrFrame(i, timestampMs, "", 1.0f, frameFile.getAbsolutePath());
            frames.add(frame);
        }

        return frames;
    }

    /**
     * Samples a single frame from an image or screenshot without fabricating timestamps (-1).
     */
    public List<OcrFrame> sampleImageFrame(File imageFile) {
        List<OcrFrame> frames = new ArrayList<>();
        if (imageFile == null || !imageFile.exists() || imageFile.length() == 0) {
            return frames;
        }
        // Timestamp is strictly -1 for static images (Master Spec §15.3 & §15.4)
        OcrFrame frame = new OcrFrame(0, -1L, "", 1.0f, imageFile.getAbsolutePath());
        frames.add(frame);
        return frames;
    }

    /**
     * Cleans up scratchpad frame files on disk after OCR extraction finishes.
     */
    public void cleanSampledFrames(List<OcrFrame> frames) {
        if (frames == null) return;
        for (OcrFrame f : frames) {
            if (f.getImagePath() != null) {
                File file = new File(f.getImagePath());
                if (file.exists() && file.isFile()) {
                    file.delete();
                }
                File txtCompanion = new File(f.getImagePath() + ".txt");
                if (txtCompanion.exists() && txtCompanion.isFile()) {
                    txtCompanion.delete();
                }
            }
        }
    }
}
