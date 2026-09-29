package com.seiftech.hifadhio.transcription;

import java.io.File;

/**
 * Vendor-neutral transcription provider interface.
 * Adheres strictly to ADR-007 and Master Spec §10.1 & §15.3:
 * "Never hard-code one vendor into domain logic."
 */
public interface TranscriptionProvider {
    /**
     * Unique identifier for the provider (e.g. "subtitles_extractor", "offline_speech", "cloud_whisper").
     */
    String getProviderId();

    /**
     * Human-readable display name.
     */
    String getDisplayName();

    /**
     * Priority for selection when multiple providers are capable. Higher number = higher priority.
     */
    int getPriority();

    /**
     * Check if this provider is currently available (e.g., model downloaded, network reachable, API key present).
     */
    boolean isAvailable();

    /**
     * Whether this provider operates completely offline.
     */
    boolean isOffline();

    /**
     * Estimate cost in USD for the given audio duration.
     */
    double estimateCost(long audioDurationMs);

    /**
     * Transcribe speech from an audio file.
     */
    TranscriptionResult transcribe(long contentItemId, File audioFile, TranscriptionOptions options) throws Exception;

    /**
     * Direct extraction from a source URL or platform if supported (e.g. caption tracks, subtitle streams).
     * If not supported by this provider, returns failure with PROVIDER_UNAVAILABLE.
     */
    TranscriptionResult transcribeFromUrl(long contentItemId, String sourceUrl, TranscriptionOptions options) throws Exception;
}
