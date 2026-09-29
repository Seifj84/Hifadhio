package com.seiftech.hifadhio.transcription;

/**
 * Result of a transcription operation, capturing either a successful Transcript
 * or normalized error diagnostic details per Master Spec §20.2.
 */
public class TranscriptionResult {
    public static final String ERROR_TRANSCRIPTION_FAILED = "TRANSCRIPTION_FAILED";
    public static final String ERROR_AUDIO_FETCH_FAILED = "AUDIO_FETCH_FAILED";
    public static final String ERROR_FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String ERROR_DURATION_EXCEEDED = "DURATION_EXCEEDED";
    public static final String ERROR_PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE";
    public static final String ERROR_AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String ERROR_NO_SPEECH_DETECTED = "NO_SPEECH_DETECTED";

    private final boolean success;
    private final Transcript transcript;
    private final String errorCode;
    private final String errorMessage;
    private final long processingTimeMs;

    private TranscriptionResult(boolean success, Transcript transcript, String errorCode, String errorMessage, long processingTimeMs) {
        this.success = success;
        this.transcript = transcript;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.processingTimeMs = processingTimeMs;
    }

    public static TranscriptionResult success(Transcript transcript, long processingTimeMs) {
        return new TranscriptionResult(true, transcript, null, null, processingTimeMs);
    }

    public static TranscriptionResult failure(String errorCode, String errorMessage) {
        return new TranscriptionResult(false, null, errorCode, errorMessage, 0);
    }

    public static TranscriptionResult failure(String errorCode, String errorMessage, long processingTimeMs) {
        return new TranscriptionResult(false, null, errorCode, errorMessage, processingTimeMs);
    }

    public boolean isSuccess() {
        return success;
    }

    public Transcript getTranscript() {
        return transcript;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
}
