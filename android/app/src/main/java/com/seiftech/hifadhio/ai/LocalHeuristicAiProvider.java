package com.seiftech.hifadhio.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-priority, zero-cost on-device heuristic AI enrichment provider.
 * Implements deterministic local NLP extraction complying strictly with Master Spec §15.5 & §18.
 */
public class LocalHeuristicAiProvider implements AiProvider {

    public static final String PROVIDER_ID = "local_nlp";
    public static final String DISPLAY_NAME = "Local On-Device NLP";
    public static final int PRIORITY = 100;

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w{2,30})");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w./?&=-]+");

    // Known common developer & tech entities (ordered deterministically)
    private static final Map<String, String> KNOWN_ENTITIES = new LinkedHashMap<>();
    static {
        KNOWN_ENTITIES.put("docker", "tool");
        KNOWN_ENTITIES.put("github", "tool");
        KNOWN_ENTITIES.put("git", "tool");
        KNOWN_ENTITIES.put("sqlite", "tool");
        KNOWN_ENTITIES.put("android", "product");
        KNOWN_ENTITIES.put("kotlin", "tool");
        KNOWN_ENTITIES.put("java", "tool");
        KNOWN_ENTITIES.put("python", "tool");
        KNOWN_ENTITIES.put("figma", "tool");
        KNOWN_ENTITIES.put("openai", "organization");
        KNOWN_ENTITIES.put("google", "organization");
        KNOWN_ENTITIES.put("youtube", "organization");
        KNOWN_ENTITIES.put("tiktok", "organization");
        KNOWN_ENTITIES.put("instagram", "organization");
        KNOWN_ENTITIES.put("reddit", "organization");
        KNOWN_ENTITIES.put("aws", "organization");
        KNOWN_ENTITIES.put("postgresql", "tool");
        KNOWN_ENTITIES.put("firebase", "tool");
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
        return true;
    }

    @Override
    public boolean isOffline() {
        return true;
    }

    @Override
    public double estimateCost(int inputCharCount) {
        return 0.0;
    }

    @Override
    public AiResult enrich(long contentItemId, String contextText, AiOptions options) {
        long start = System.currentTimeMillis();

        if (contextText == null || contextText.trim().isEmpty()) {
            return AiResult.failure(AiResult.ERROR_EMPTY_INPUT, "Context text is empty; nothing to enrich");
        }

        try {
            String text = contextText.trim();
            String promptVersion = (options != null) ? options.getPromptVersion() : PromptManager.CURRENT_PROMPT_VERSION;

            // 1. Sentence splitting
            List<String> sentences = extractSentences(text);

            // 2. Summary Generation
            String summaryShort;
            String summaryDetailed;
            if (sentences.isEmpty()) {
                summaryShort = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                summaryDetailed = text;
            } else if (sentences.size() == 1) {
                summaryShort = sentences.get(0);
                summaryDetailed = sentences.get(0);
            } else {
                summaryShort = sentences.get(0);
                StringBuilder sb = new StringBuilder();
                int count = Math.min(sentences.size(), 4);
                for (int i = 0; i < count; i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(sentences.get(i));
                }
                summaryDetailed = sb.toString();
            }

            // 3. Key Points Extraction
            List<String> keyPoints = new ArrayList<>();
            for (String s : sentences) {
                String clean = s.trim();
                if (clean.startsWith("-") || clean.startsWith("•") || clean.matches("^\\d+\\..*")) {
                    clean = clean.replaceFirst("^[-•\\d.]+\\s*", "");
                    if (!clean.isEmpty()) keyPoints.add(clean);
                } else if (clean.length() > 30 && clean.length() < 160 && (clean.contains("should") || clean.contains("important") || clean.contains("ensure") || clean.contains("key") || clean.contains("always") || clean.contains("never") || clean.contains("architecture"))) {
                    keyPoints.add(clean);
                }
                if (keyPoints.size() >= 5) break;
            }
            if (keyPoints.isEmpty() && !sentences.isEmpty()) {
                keyPoints.add(sentences.get(0));
                if (sentences.size() > 1) keyPoints.add(sentences.get(1));
            }

            // 4. Topic & Tag Extraction
            List<String> topics = new ArrayList<>();
            List<String> suggestedTags = new ArrayList<>();
            Set<String> seenTags = new HashSet<>();

            // Extract existing hashtags
            Matcher hashMatcher = HASHTAG_PATTERN.matcher(text);
            while (hashMatcher.find() && suggestedTags.size() < 6) {
                String tag = hashMatcher.group(1).toLowerCase();
                if (!seenTags.contains(tag)) {
                    seenTags.add(tag);
                    suggestedTags.add(tag);
                }
            }

            // Keyword topic detection
            String lower = text.toLowerCase();
            if (lower.contains("android") || lower.contains("kotlin") || lower.contains("mobile")) {
                topics.add("Android Development");
                addTagIfNotPresent("android", suggestedTags, seenTags);
            }
            if (lower.contains("architecture") || lower.contains("clean architecture") || lower.contains("patterns")) {
                topics.add("Software Architecture");
                addTagIfNotPresent("architecture", suggestedTags, seenTags);
            }
            if (lower.contains("ai") || lower.contains("machine learning") || lower.contains("llm") || lower.contains("ocr") || lower.contains("gpt")) {
                topics.add("Artificial Intelligence");
                addTagIfNotPresent("ai", suggestedTags, seenTags);
            }
            if (lower.contains("design") || lower.contains("ui") || lower.contains("ux") || lower.contains("figma")) {
                topics.add("UI/UX Design");
                addTagIfNotPresent("design", suggestedTags, seenTags);
            }
            if (lower.contains("python") || lower.contains("code") || lower.contains("programming")) {
                topics.add("Programming");
                addTagIfNotPresent("coding", suggestedTags, seenTags);
            }

            if (topics.isEmpty()) {
                topics.add("General Knowledge");
            }
            if (suggestedTags.isEmpty()) {
                suggestedTags.add("saved");
                suggestedTags.add("hifadhio");
            }

            // 5. Entity Detection
            List<AiEntity> entities = new ArrayList<>();
            for (Map.Entry<String, String> entry : KNOWN_ENTITIES.entrySet()) {
                String word = entry.getKey();
                Pattern wordPattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = wordPattern.matcher(text);
                if (matcher.find()) {
                    int idx = matcher.start();
                    int startIdx = Math.max(0, idx - 20);
                    int endIdx = Math.min(text.length(), idx + word.length() + 30);
                    String evidence = text.substring(startIdx, endIdx).trim();
                    entities.add(new AiEntity(capitalize(word), entry.getValue(), evidence));
                    if (entities.size() >= 15) break;
                }
            }

            // 6. Action Items
            List<String> actionItems = new ArrayList<>();
            for (String s : sentences) {
                String low = s.toLowerCase();
                if (low.startsWith("read ") || low.startsWith("check ") || low.startsWith("try ") || low.startsWith("install ") || low.startsWith("use ") || low.startsWith("build ") || low.startsWith("learn ")) {
                    actionItems.add(s.trim());
                }
                if (actionItems.size() >= 3) break;
            }
            if (actionItems.isEmpty()) {
                actionItems.add("Review content and archive key insights");
            }

            // 7. Suggested Collection
            String suggestedCollection = "Inbox";
            if (lower.contains("android") || lower.contains("coding") || lower.contains("code") || lower.contains("architecture") || lower.contains("git") || lower.contains("python") || lower.contains("dev")) {
                suggestedCollection = "Dev";
            } else if (lower.contains("design") || lower.contains("figma") || lower.contains("ux") || lower.contains("color")) {
                suggestedCollection = "Design";
            } else if (lower.contains("research") || lower.contains("paper") || lower.contains("study") || lower.contains("analysis")) {
                suggestedCollection = "Research";
            } else if (lower.contains("article") || lower.contains("essay") || lower.contains("post") || lower.contains("read")) {
                suggestedCollection = "Articles";
            }

            // 8. Strict Schema JSON Construction & Validation
            JSONObject json = new JSONObject();
            json.put("summary_short", summaryShort);
            json.put("summary_detailed", summaryDetailed);

            JSONArray kpArray = new JSONArray();
            for (String kp : keyPoints) kpArray.put(kp);
            json.put("key_points", kpArray);

            JSONArray tpArray = new JSONArray();
            for (String tp : topics) tpArray.put(tp);
            json.put("topics", tpArray);

            JSONArray stArray = new JSONArray();
            for (String st : suggestedTags) stArray.put(st);
            json.put("suggested_tags", stArray);

            JSONArray entArray = new JSONArray();
            for (AiEntity ent : entities) entArray.put(ent.toJson());
            json.put("entities", entArray);

            JSONArray aiArray = new JSONArray();
            for (String ai : actionItems) aiArray.put(ai);
            json.put("action_items", aiArray);

            json.put("suggested_collection", suggestedCollection);
            json.put("prompt_version", promptVersion);
            json.put("provider_id", PROVIDER_ID);
            json.put("model", "heuristic-v1");

            if (!PromptManager.validateSchema(json)) {
                return AiResult.failure(AiResult.ERROR_INVALID_SCHEMA, "Heuristic output failed strict schema validation");
            }

            AiEnrichment enrichment = AiEnrichment.fromStrictSchemaJson(contentItemId, json);
            enrichment.setProviderId(PROVIDER_ID);
            enrichment.setModel("heuristic-v1");
            enrichment.setPromptVersion(promptVersion);
            enrichment.setCostUsd(0.0);

            long latency = System.currentTimeMillis() - start;
            return AiResult.success(enrichment, latency, 0.0);
        } catch (Exception e) {
            return AiResult.failure(AiResult.ERROR_AI_ENRICHMENT_FAILED, "Local heuristic enrichment failed: " + e.getMessage());
        }
    }

    private static List<String> extractSentences(String text) {
        List<String> list = new ArrayList<>();
        if (text == null) return list;
        // Split by standard sentence delimiters or newlines
        String[] raw = text.split("(?<=[.!?])\\s+|\\n+");
        for (String s : raw) {
            String clean = s.trim();
            if (clean.length() > 10) {
                list.add(clean);
            }
        }
        return list;
    }

    private static void addTagIfNotPresent(String tag, List<String> tags, Set<String> seen) {
        if (!seen.contains(tag) && tags.size() < 8) {
            seen.add(tag);
            tags.add(tag);
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase(Locale.US) + str.substring(1);
    }
}
