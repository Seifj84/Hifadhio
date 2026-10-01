package com.seiftech.hifadhio.ai;

/**
 * Vendor-neutral interface defining the contract for AI enrichment engines.
 * Master Spec §15.5, §18, ADR-008, & §1207.
 */
public interface AiProvider {

    /**
     * Unique machine-readable identifier (e.g. "local_nlp", "cloud_llm").
     */
    String getProviderId();

    /**
     * User-facing display name for badges and diagnostics.
     */
    String getDisplayName();

    /**
     * Priority rank for automated selection (higher integer = preferred).
     */
    int getPriority();

    /**
     * Checks whether provider dependencies and prerequisites are operational.
     */
    boolean isAvailable();

    /**
     * Denotes whether the engine operates fully locally without network transmission.
     */
    boolean isOffline();

    /**
     * Estimates anticipated financial cost in USD for the given input length.
     */
    double estimateCost(int inputCharCount);

    /**
     * Executes AI enrichment on the assembled context text.
     */
    AiResult enrich(long contentItemId, String contextText, AiOptions options);
}
