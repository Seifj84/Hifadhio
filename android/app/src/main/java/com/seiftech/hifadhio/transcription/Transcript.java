package com.seiftech.hifadhio.transcription;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a verified speech transcript for a ContentItem.
 * Adheres strictly to Master Spec §15.3 and §20.2.
 */
public class Transcript {
    private long id;
    private long contentItemId;
    private String fullText;
    private String language;
    private String providerId;
    private String model;
    private long durationMs;
    private List<TranscriptSegment> segments;
    private double confidence;
    private double costUsd;
    private long createdAt;
    private long updatedAt;

    public Transcript() {
        this.segments = new ArrayList<>();
        this.confidence = 1.0;
        this.costUsd = 0.0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Transcript(long contentItemId, String fullText, String language, String providerId, String model) {
        this();
        this.contentItemId = contentItemId;
        this.fullText = fullText != null ? fullText.trim() : "";
        this.language = language != null ? language.trim() : "auto";
        this.providerId = providerId != null ? providerId.trim() : "unknown";
        this.model = model != null ? model.trim() : "default";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getContentItemId() {
        return contentItemId;
    }

    public void setContentItemId(long contentItemId) {
        this.contentItemId = contentItemId;
    }

    public String getFullText() {
        return fullText != null ? fullText : "";
    }

    public void setFullText(String fullText) {
        this.fullText = fullText != null ? fullText.trim() : "";
    }

    public String getLanguage() {
        return language != null ? language : "auto";
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getProviderId() {
        return providerId != null ? providerId : "unknown";
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getModel() {
        return model != null ? model : "default";
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public List<TranscriptSegment> getSegments() {
        return segments != null ? segments : new ArrayList<>();
    }

    public void setSegments(List<TranscriptSegment> segments) {
        this.segments = segments != null ? segments : new ArrayList<>();
    }

    public void addSegment(TranscriptSegment segment) {
        if (segment != null) {
            if (this.segments == null) {
                this.segments = new ArrayList<>();
            }
            this.segments.add(segment);
        }
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public double getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(double costUsd) {
        this.costUsd = Math.max(0.0, costUsd);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean hasTimestamps() {
        if (segments == null || segments.isEmpty()) {
            return false;
        }
        for (TranscriptSegment seg : segments) {
            if (seg.hasTimestamps()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats duration in mm:ss or hh:mm:ss.
     */
    public String getFormattedDuration() {
        if (durationMs <= 0) {
            return "00:00";
        }
        long totalSeconds = durationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * In-transcript search for Search-on-Detail (§1181).
     */
    public List<TranscriptSegment> searchSegments(String query) {
        List<TranscriptSegment> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty() || segments == null) {
            return results;
        }
        String q = query.trim().toLowerCase();
        for (TranscriptSegment seg : segments) {
            if (seg.getText().toLowerCase().contains(q)) {
                results.add(seg);
            }
        }
        return results;
    }

    public String toSegmentsJson() {
        if (segments == null || segments.isEmpty()) {
            return "[]";
        }
        JSONArray arr = new JSONArray();
        for (TranscriptSegment seg : segments) {
            arr.put(seg.toJson());
        }
        return arr.toString();
    }

    public static List<TranscriptSegment> parseSegmentsJson(String json) {
        List<TranscriptSegment> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                TranscriptSegment seg = TranscriptSegment.fromJson(obj);
                if (seg != null) {
                    list.add(seg);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public String getPreview(int maxChars) {
        if (fullText == null || fullText.isEmpty()) {
            return "";
        }
        if (fullText.length() <= maxChars) {
            return fullText;
        }
        return fullText.substring(0, maxChars).trim() + "...";
    }
}
