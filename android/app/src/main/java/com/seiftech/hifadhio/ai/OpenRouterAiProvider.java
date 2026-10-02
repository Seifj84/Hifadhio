package com.seiftech.hifadhio.ai;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Free Cloud AI Provider using OpenRouter API.
 * Integrates open-access models (Qwen, Apodex, Gemma) with strict schema compliance (§18).
 * Provides deep multimodal synthesis for short-form social posts, Reels, and TikToks.
 */
public class OpenRouterAiProvider implements AiProvider {

    private static final String TAG = "OpenRouterAiProvider";

    public static final String PROVIDER_ID = "openrouter";
    public static final String DISPLAY_NAME = "OpenRouter AI (Free)";
    public static final int PRIORITY = 120; // Preferred when online & configured

    private static final String OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final int TIMEOUT_MS = 25000;

    private final AiConfig config;

    public OpenRouterAiProvider() {
        this.config = null;
    }

    public OpenRouterAiProvider(AiConfig config) {
        this.config = config;
    }

    private AiConfig getEffectiveConfig() {
        return config != null ? config : AiConfig.getInstance();
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
        AiConfig cfg = getEffectiveConfig();
        if (cfg != null) {
            return cfg.isCloudAiEnabled() && cfg.hasValidKey();
        }
        return AiConfig.DEFAULT_OPENROUTER_KEY != null && !AiConfig.DEFAULT_OPENROUTER_KEY.isEmpty();
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public double estimateCost(int inputCharCount) {
        // OpenRouter :free models incur zero financial cost
        return 0.0;
    }

    @Override
    public AiResult enrich(long contentItemId, String contextText, AiOptions options) {
        long startTime = System.currentTimeMillis();

        if (contextText == null || contextText.trim().isEmpty()) {
            return AiResult.failure(AiResult.ERROR_EMPTY_INPUT, "Context text is empty; nothing to enrich");
        }

        if (!isAvailable()) {
            return AiResult.failure(AiResult.ERROR_PROVIDER_UNAVAILABLE, "OpenRouter provider is not configured or disabled");
        }

        AiConfig cfg = getEffectiveConfig();
        String apiKey = cfg != null ? cfg.getOpenRouterApiKey() : AiConfig.DEFAULT_OPENROUTER_KEY;
        String primaryModel = cfg != null ? cfg.getActiveModel() : AiConfig.DEFAULT_FREE_MODEL;
        String fallbackModel = AiConfig.FALLBACK_FREE_MODEL;

        // Try primary model first, fallback on error or rate-limit
        AiResult result = executeModelRequest(contentItemId, contextText, primaryModel, apiKey, startTime);
        if (!result.isSuccess() && !primaryModel.equals(fallbackModel)) {
            Log.w(TAG, "Primary model " + primaryModel + " failed (" + result.getErrorMessage() + "), retrying with " + fallbackModel);
            result = executeModelRequest(contentItemId, contextText, fallbackModel, apiKey, startTime);
        }

        return result;
    }

    private AiResult executeModelRequest(long contentItemId, String contextText, String model, String apiKey, long startTime) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(OPENROUTER_ENDPOINT);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("HTTP-Referer", "https://github.com/Seifj84/Hifadhio");
            conn.setRequestProperty("X-Title", "Hifadhio");

            String prompt = buildPrompt(contextText);

            JSONObject body = new JSONObject();
            body.put("model", model);

            JSONArray messages = new JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", "You are an expert content enrichment AI. You must respond strictly in JSON matching the exact requested schema. Never output markdown fences or commentary outside the JSON.");
            messages.put(sysMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.put(userMsg);

            body.put("messages", messages);

            byte[] outBytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(outBytes);
                os.flush();
            }

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                String errorBody = readStream(conn.getErrorStream());
                Log.w(TAG, "OpenRouter HTTP " + status + ": " + errorBody);
                return AiResult.failure(AiResult.ERROR_AI_ENRICHMENT_FAILED, "OpenRouter returned HTTP " + status + ": " + errorBody, System.currentTimeMillis() - startTime);
            }

            String respBody = readStream(conn.getInputStream());
            JSONObject respJson = new JSONObject(respBody);
            JSONArray choices = respJson.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return AiResult.failure(AiResult.ERROR_AI_ENRICHMENT_FAILED, "No choices returned by OpenRouter", System.currentTimeMillis() - startTime);
            }

            JSONObject msgObj = choices.getJSONObject(0).optJSONObject("message");
            if (msgObj == null) {
                return AiResult.failure(AiResult.ERROR_AI_ENRICHMENT_FAILED, "No message in OpenRouter choice", System.currentTimeMillis() - startTime);
            }

            String rawContent = msgObj.optString("content", "");
            rawContent = stripMarkdownFences(rawContent);

            // Strict Schema Validation (§18)
            AiResult validationResult = PromptManager.validateSchema(rawContent);
            if (!validationResult.isSuccess()) {
                Log.w(TAG, "Schema validation failed on OpenRouter output: " + validationResult.getErrorMessage());
                return validationResult;
            }

            JSONObject parsed = new JSONObject(rawContent);
            parsed.put("prompt_version", PromptManager.CURRENT_PROMPT_VERSION);
            parsed.put("provider_id", PROVIDER_ID);

            AiEnrichment enrichment = AiEnrichment.fromStrictJson(contentItemId, parsed);
            enrichment.setRawJson(rawContent);
            enrichment.setModel(model);
            enrichment.setCostUsd(0.0);

            long latency = System.currentTimeMillis() - startTime;
            Log.i(TAG, "OpenRouter enrichment completed in " + latency + " ms using " + model);
            return AiResult.success(enrichment, latency, 0.0);

        } catch (Exception e) {
            Log.e(TAG, "OpenRouter request exception", e);
            return AiResult.failure(AiResult.ERROR_AI_ENRICHMENT_FAILED, "OpenRouter error: " + e.getMessage(), System.currentTimeMillis() - startTime);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String buildPrompt(String contextText) {
        return "Analyze the following saved content and extract structured knowledge.\n"
                + "Return strictly valid JSON matching this schema:\n"
                + "{\n"
                + "  \"summary_short\": \"1-2 sentence executive summary.\",\n"
                + "  \"summary_detailed\": \"3-5 sentence comprehensive breakdown of concepts.\",\n"
                + "  \"key_points\": [\"Point 1\", \"Point 2\", \"Point 3\"],\n"
                + "  \"topics\": [\"Topic 1\", \"Topic 2\"],\n"
                + "  \"suggested_tags\": [\"tag1\", \"tag2\"],\n"
                + "  \"entities\": [\n"
                + "    {\"name\": \"ToolName\", \"type\": \"tool|product|organization\", \"evidence\": \"Context quote\"}\n"
                + "  ],\n"
                + "  \"action_items\": [\"Action 1\", \"Action 2\"],\n"
                + "  \"suggested_collection\": \"Dev|Design|Research|Articles|Inbox\"\n"
                + "}\n\n"
                + "CONTENT TO ANALYZE:\n"
                + contextText;
    }

    private String stripMarkdownFences(String text) {
        if (text == null) return "{}";
        String s = text.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    private String readStream(InputStream is) {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
