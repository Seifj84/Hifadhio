package com.seiftech.hifadhio.transcription;

/**
 * Options and parameters for speech-to-text transcription jobs.
 */
public class TranscriptionOptions {
    public static final long DEFAULT_MAX_DURATION_MS = 30 * 60 * 1000L; // 30 minutes
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024L; // 25 MB (standard Whisper limit)

    private String language;
    private String prompt;
    private double temperature;
    private boolean preferOffline;
    private boolean includeTimestamps;
    private long maxDurationMs;
    private long maxFileSizeBytes;

    public TranscriptionOptions() {
        this.language = "auto";
        this.prompt = null;
        this.temperature = 0.0;
        this.preferOffline = true;
        this.includeTimestamps = true;
        this.maxDurationMs = DEFAULT_MAX_DURATION_MS;
        this.maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE_BYTES;
    }

    public static TranscriptionOptions defaults() {
        return new TranscriptionOptions();
    }

    public String getLanguage() {
        return language;
    }

    public TranscriptionOptions setLanguage(String language) {
        this.language = language;
        return this;
    }

    public String getPrompt() {
        return prompt;
    }

    public TranscriptionOptions setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public double getTemperature() {
        return temperature;
    }

    public TranscriptionOptions setTemperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    public boolean isPreferOffline() {
        return preferOffline;
    }

    public TranscriptionOptions setPreferOffline(boolean preferOffline) {
        this.preferOffline = preferOffline;
        return this;
    }

    public boolean isIncludeTimestamps() {
        return includeTimestamps;
    }

    public TranscriptionOptions setIncludeTimestamps(boolean includeTimestamps) {
        this.includeTimestamps = includeTimestamps;
        return this;
    }

    public long getMaxDurationMs() {
        return maxDurationMs;
    }

    public TranscriptionOptions setMaxDurationMs(long maxDurationMs) {
        this.maxDurationMs = maxDurationMs;
        return this;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public TranscriptionOptions setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        return this;
    }
}
