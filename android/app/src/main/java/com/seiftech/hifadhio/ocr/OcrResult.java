package com.seiftech.hifadhio.ocr;

/**
 * Result envelope for OCR visual text extraction operations.
 * Standardizes normalized error codes per Master Spec §20.2 & §920.
 */
public class OcrResult {
    // Normalized Error Codes
    public static final String ERROR_OCR_FAILED = "OCR_FAILED";
    public static final String ERROR_NO_FRAMES_EXTRACTED = "NO_FRAMES_EXTRACTED";
    public static final String ERROR_FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String ERROR_UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT";
    public static final String ERROR_PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE";
    public static final String ERROR_AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String ERROR_NO_TEXT_DETECTED = "NO_TEXT_DETECTED";

    private final boolean success;
    private final OcrRecord record;
    private final String errorCode;
    private final String errorMessage;

    private OcrResult(boolean success, OcrRecord record, String errorCode, String errorMessage) {
        this.success = success;
        this.record = record;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static OcrResult success(OcrRecord record) {
        return new OcrResult(true, record, null, null);
    }

    public static OcrResult failure(String errorCode, String errorMessage) {
        return new OcrResult(false, null, errorCode, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public OcrRecord getRecord() { return record; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
}
