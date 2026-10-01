package com.seiftech.hifadhio.ai;

import java.io.Serializable;

/**
 * Normalized result wrapper for AI enrichment operations.
 * Master Spec §921 (`AI_ENRICHMENT_FAILED`) & §18.
 */
public class AiResult implements Serializable {

    public static final String ERROR_AI_ENRICHMENT_FAILED = "AI_ENRICHMENT_FAILED";
    public static final String ERROR_EMPTY_INPUT = "EMPTY_INPUT";
    public static final String ERROR_INVALID_SCHEMA = "INVALID_SCHEMA";
    public static final String ERROR_PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE";
    public static final String ERROR_RATE_LIMITED = "RATE_LIMITED";
    public static final String ERROR_CONTEXT_LENGTH_EXCEEDED = "CONTEXT_LENGTH_EXCEEDED";

    private final boolean success;
    private final AiEnrichment enrichment;
    private final String errorCode;
    private final String errorMessage;
    private final long latencyMs;
    private final double costUsd;

    private AiResult(boolean success, AiEnrichment enrichment, String errorCode, String errorMessage, long latencyMs, double costUsd) {
        this.success = success;
        this.enrichment = enrichment;
        this.errorCode = errorCode != null ? errorCode : "";
        this.errorMessage = errorMessage != null ? errorMessage : "";
        this.latencyMs = latencyMs;
        this.costUsd = costUsd;
    }

    public static AiResult success(AiEnrichment enrichment, long latencyMs, double costUsd) {
        return new AiResult(true, enrichment, null, null, latencyMs, costUsd);
    }

    public static AiResult failure(String errorCode, String errorMessage) {
        return new AiResult(false, null, errorCode, errorMessage, 0L, 0.0);
    }

    public static AiResult failure(String errorCode, String errorMessage, long latencyMs) {
        return new AiResult(false, null, errorCode, errorMessage, latencyMs, 0.0);
    }

    public boolean isSuccess() {
        return success;
    }

    public AiEnrichment getEnrichment() {
        return enrichment;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public double getCostUsd() {
        return costUsd;
    }
}
