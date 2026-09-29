package com.seiftech.hifadhio.adapter;

public class ExtractionException extends Exception {
    private final String errorCode;

    public ExtractionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ExtractionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
