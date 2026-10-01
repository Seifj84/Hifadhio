package com.seiftech.hifadhio.ai;

import com.seiftech.hifadhio.data.ContentItem;
import com.seiftech.hifadhio.ocr.OcrRecord;
import com.seiftech.hifadhio.transcription.Transcript;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Manages AI prompt templates, prompt versioning, context assembly, and strict schema validation.
 * Master Spec §15.5, §18, & §1208–§1209.
 */
public class PromptManager {

    public static final String PROMPT_VERSION_1 = "v1.0.0";
    public static final String CURRENT_PROMPT_VERSION = PROMPT_VERSION_1;

    /**
     * Assembles all available captured content and derived processing outputs into a unified context text.
     */
    public static String assembleContext(ContentItem item, Transcript transcript, OcrRecord ocrRecord) {
        StringBuilder sb = new StringBuilder();

        if (item != null) {
            if (item.getTitle() != null && !item.getTitle().trim().isEmpty()) {
                sb.append("TITLE: ").append(item.getTitle().trim()).append("\n");
            }
            if (item.getPlatform() != null && !item.getPlatform().trim().isEmpty()) {
                sb.append("PLATFORM: ").append(item.getPlatform().trim()).append("\n");
            }
            if (item.getCaption() != null && !item.getCaption().trim().isEmpty()) {
                sb.append("DESCRIPTION/CAPTION: ").append(item.getCaption().trim()).append("\n");
            }
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                sb.append("USER NOTES: ").append(item.getNotes().trim()).append("\n");
            }
            if (item.getUrl() != null && !item.getUrl().trim().isEmpty()) {
                sb.append("URL: ").append(item.getUrl().trim()).append("\n");
            }
        }

        if (transcript != null && transcript.getFullText() != null && !transcript.getFullText().trim().isEmpty()) {
            sb.append("\nSPOKEN AUDIO TRANSCRIPT:\n");
            sb.append(transcript.getFullText().trim()).append("\n");
        }

        if (ocrRecord != null && ocrRecord.getFullText() != null && !ocrRecord.getFullText().trim().isEmpty()) {
            sb.append("\nON-SCREEN VISUAL OCR TEXT:\n");
            sb.append(ocrRecord.getFullText().trim()).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Builds the system and user prompt enforcing the strict JSON contract per Master Spec §18.
     */
    public static String buildPrompt(String contextText, AiOptions options) {
        String version = options != null ? options.getPromptVersion() : CURRENT_PROMPT_VERSION;
        return String.format(Locale.US,
                "You are the Hifadhio AI Enrichment engine (Prompt %s).\n"
                        + "Analyze the following saved content and extract structured enrichment information.\n"
                        + "You MUST output ONLY a valid JSON object matching the exact schema below. Do not wrap in markdown or backticks.\n"
                        + "{\n"
                        + "  \"summary_short\": \"1-2 sentence high-level executive summary.\",\n"
                        + "  \"summary_detailed\": \"Comprehensive 3-5 sentence breakdown of the concepts.\",\n"
                        + "  \"key_points\": [\"Point 1\", \"Point 2\", \"Point 3\"],\n"
                        + "  \"topics\": [\"Topic 1\", \"Topic 2\"],\n"
                        + "  \"suggested_tags\": [\"tag1\", \"tag2\"],\n"
                        + "  \"entities\": [\n"
                        + "    {\"name\": \"Entity Name\", \"type\": \"tool|person|organization|product|concept\", \"evidence\": \"Context quote\"}\n"
                        + "  ],\n"
                        + "  \"action_items\": [\"Actionable item or takeaway\"],\n"
                        + "  \"suggested_collection\": \"Dev|Design|Research|Articles|Inbox\"\n"
                        + "}\n\n"
                        + "CONTENT TO ENRICH:\n%s",
                version,
                contextText != null ? contextText : ""
        );
    }

    /**
     * Strictly validates the AI enrichment JSON output per Master Spec §18.
     * Rejects missing required keys or invalid structures.
     */
    public static boolean validateSchema(JSONObject json) {
        if (json == null) return false;

        // Check required fields
        if (!json.has("summary_short") || !json.has("summary_detailed")) {
            return false;
        }

        String summaryShort = json.optString("summary_short", "").trim();
        String summaryDetailed = json.optString("summary_detailed", "").trim();
        if (summaryShort.isEmpty() || summaryDetailed.isEmpty()) {
            return false;
        }

        // Validate key_points array
        if (!json.has("key_points")) return false;
        JSONArray kp = json.optJSONArray("key_points");
        if (kp == null) return false;

        // Validate topics array
        if (!json.has("topics")) return false;
        JSONArray tp = json.optJSONArray("topics");
        if (tp == null) return false;

        // Validate suggested_tags array
        if (!json.has("suggested_tags")) return false;
        JSONArray st = json.optJSONArray("suggested_tags");
        if (st == null) return false;

        // Validate entities array if present
        if (json.has("entities")) {
            JSONArray ent = json.optJSONArray("entities");
            if (ent == null) return false;
            for (int i = 0; i < ent.length(); i++) {
                JSONObject e = ent.optJSONObject(i);
                if (e == null || !e.has("name") || e.optString("name", "").trim().isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }
}
