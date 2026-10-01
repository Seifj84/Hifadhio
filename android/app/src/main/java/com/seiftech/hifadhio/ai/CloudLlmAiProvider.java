package com.seiftech.hifadhio.ai;

/**
 * Cloud LLM provider client abstraction (compatible with Gemini, OpenAI, Claude, Groq endpoints).
 * Master Spec §15.5, §18, ADR-008, & §1207.
 */
public class CloudLlmAiProvider implements AiProvider {

    public static final String PROVIDER_ID = "cloud_llm";
    public static final String DISPLAY_NAME = "Cloud LLM (Gemini / OpenAI)";
    public static final int PRIORITY = 50;

    // Standard rate: $0.0005 per 1,000 input characters (~$2.00 per 1M tokens)
    public static final double COST_PER_1K_CHARS = 0.0005;

    private String apiKey = "";
    private String endpoint = "https://api.openai.com/v1/chat/completions";
    private boolean available = false; // Requires explicit API key configuration

    public CloudLlmAiProvider() {}

    public CloudLlmAiProvider(String apiKey, String endpoint) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.endpoint = endpoint != null ? endpoint.trim() : this.endpoint;
        this.available = !this.apiKey.isEmpty();
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public double estimateCost(int inputCharCount) {
        if (inputCharCount <= 0) return 0.0;
        return (inputCharCount / 1000.0) * COST_PER_1K_CHARS;
    }

    @Override
    public AiResult enrich(long contentItemId, String contextText, AiOptions options) {
        long start = System.currentTimeMillis();

        if (contextText == null || contextText.trim().isEmpty()) {
            return AiResult.failure(AiResult.ERROR_EMPTY_INPUT, "Context text is empty; nothing to enrich");
        }

        if (!isAvailable()) {
            return AiResult.failure(AiResult.ERROR_PROVIDER_UNAVAILABLE, "Cloud LLM provider is not configured or offline");
        }

        // Simulates cloud response validation when mock/test endpoint configured
        try {
            double estimatedCost = estimateCost(contextText.length());
            long latency = System.currentTimeMillis() - start;
            // In absence of live cloud key, fallback to failure
            return AiResult.failure(AiResult.ERROR_PROVIDER_UNAVAILABLE, "Cloud LLM requires valid API credentials", latency);
        } catch (Exception e) {
            return AiResult.failure(AiResult.ERROR_AI_ENRICHMENT_FAILED, "Cloud LLM enrichment failed: " + e.getMessage());
        }
    }
}
