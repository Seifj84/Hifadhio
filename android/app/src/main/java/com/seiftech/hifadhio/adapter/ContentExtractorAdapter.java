package com.seiftech.hifadhio.adapter;

/**
 * Common extraction interface implemented by all platform adapters.
 * Enables modular plug-and-play platform integrations without altering core content logic.
 */
public interface ContentExtractorAdapter {
    /**
     * Unique identifier for this platform adapter (e.g., "GenericWeb", "YouTube", "TikTok").
     */
    String getPlatformId();

    /**
     * Priority order for adapter selection. Higher values are evaluated first.
     * Specific platform adapters should return > 0, while GenericWebAdapter returns 0.
     */
    int getPriority();

    /**
     * Check if this adapter can process the given URL.
     */
    boolean canHandle(String url);

    /**
     * Extract canonical metadata from the given URL.
     */
    ExtractedMetadata extract(String url) throws ExtractionException;

    /**
     * Canonicalize and normalize the URL according to platform rules.
     */
    String canonicalize(String url);

    /**
     * Perform an adapter health check.
     */
    AdapterHealth checkHealth();
}
