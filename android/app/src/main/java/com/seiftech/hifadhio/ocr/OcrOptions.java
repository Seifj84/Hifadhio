package com.seiftech.hifadhio.ocr;

/**
 * Options configuring visual frame extraction and OCR text recognition.
 * Enforces Master Spec §15.4 & §1434 policies (selective sampling, bounds on frame count).
 */
public class OcrOptions {
    public static final int DEFAULT_MAX_FRAMES = 15;
    public static final int DEFAULT_SAMPLE_INTERVAL_SECONDS = 5;
    public static final float DEFAULT_MIN_CONFIDENCE = 0.5f;

    private int maxFrames;
    private int sampleIntervalSeconds;
    private float minConfidence;
    private boolean deduplicate;
    private String languageHint;
    private boolean offlineOnly;

    public OcrOptions() {
        this.maxFrames = DEFAULT_MAX_FRAMES;
        this.sampleIntervalSeconds = DEFAULT_SAMPLE_INTERVAL_SECONDS;
        this.minConfidence = DEFAULT_MIN_CONFIDENCE;
        this.deduplicate = true;
        this.languageHint = "en";
        this.offlineOnly = false;
    }

    public static OcrOptions createDefault() {
        return new OcrOptions();
    }

    public int getMaxFrames() { return maxFrames; }
    public void setMaxFrames(int maxFrames) {
        // Enforce hard cap between 1 and 30 frames
        this.maxFrames = Math.max(1, Math.min(maxFrames, 30));
    }

    public int getSampleIntervalSeconds() { return sampleIntervalSeconds; }
    public void setSampleIntervalSeconds(int sampleIntervalSeconds) {
        this.sampleIntervalSeconds = Math.max(1, sampleIntervalSeconds);
    }

    public float getMinConfidence() { return minConfidence; }
    public void setMinConfidence(float minConfidence) { this.minConfidence = minConfidence; }

    public boolean isDeduplicate() { return deduplicate; }
    public void setDeduplicate(boolean deduplicate) { this.deduplicate = deduplicate; }

    public String getLanguageHint() { return languageHint; }
    public void setLanguageHint(String languageHint) { this.languageHint = languageHint; }

    public boolean isOfflineOnly() { return offlineOnly; }
    public void setOfflineOnly(boolean offlineOnly) { this.offlineOnly = offlineOnly; }
}
