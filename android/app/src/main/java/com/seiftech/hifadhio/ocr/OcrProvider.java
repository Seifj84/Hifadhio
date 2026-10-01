package com.seiftech.hifadhio.ocr;

import java.io.File;
import java.util.List;

/**
 * Vendor-neutral interface for Optical Character Recognition (OCR) engines.
 * Adheres to Master Spec §1194 & ADR-007 design pattern: never hardcode one OCR vendor into domain logic.
 */
public interface OcrProvider {
    /**
     * Unique identifier for this provider (e.g. "on_device_ocr", "cloud_vision").
     */
    String getProviderId();

    /**
     * Human-readable display name (e.g. "On-Device Visual OCR", "Cloud Vision").
     */
    String getDisplayName();

    /**
     * Priority rank. Higher numbers are attempted first.
     */
    int getPriority();

    /**
     * Health check: verify whether this provider is ready for execution.
     */
    boolean isAvailable();

    /**
     * Whether this provider processes strictly on-device without external networks.
     */
    boolean isOffline();

    /**
     * Financial cost estimation in USD for processing the given number of frames.
     */
    double estimateCost(int frameCount);

    /**
     * Extract visual text from a list of sampled frames.
     */
    OcrResult processFrames(long contentItemId, List<OcrFrame> frames, OcrOptions options);
}
