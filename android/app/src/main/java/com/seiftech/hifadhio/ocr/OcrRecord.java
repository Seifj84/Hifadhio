package com.seiftech.hifadhio.ocr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Domain model representing an aggregated OCR visual text extraction record.
 * Stored in SQLite Schema v6 `ocr_records` table and mirrored to object storage.
 */
public class OcrRecord {
    private long id;
    private long contentItemId;
    private String fullText;
    private String providerId;
    private String model;
    private int framesCount;
    private List<OcrFrame> frames;
    private double costUsd;
    private long createdAt;
    private long updatedAt;

    public OcrRecord() {
        this.frames = new ArrayList<>();
        this.fullText = "";
        this.providerId = "unknown";
        this.model = "";
        this.costUsd = 0.0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public OcrRecord(long contentItemId, String fullText, String providerId, String model) {
        this();
        this.contentItemId = contentItemId;
        this.fullText = fullText != null ? fullText.trim() : "";
        this.providerId = providerId != null ? providerId : "unknown";
        this.model = model != null ? model : "";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getContentItemId() { return contentItemId; }
    public void setContentItemId(long contentItemId) { this.contentItemId = contentItemId; }

    public String getFullText() { return fullText != null ? fullText : ""; }
    public void setFullText(String fullText) { this.fullText = fullText != null ? fullText.trim() : ""; }

    public String getProviderId() { return providerId != null ? providerId : "unknown"; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getModel() { return model != null ? model : ""; }
    public void setModel(String model) { this.model = model; }

    public int getFramesCount() {
        return frames != null && !frames.isEmpty() ? frames.size() : framesCount;
    }
    public void setFramesCount(int framesCount) { this.framesCount = framesCount; }

    public List<OcrFrame> getFrames() {
        return frames != null ? frames : Collections.emptyList();
    }
    public void setFrames(List<OcrFrame> frames) {
        this.frames = frames != null ? frames : new ArrayList<>();
        this.framesCount = this.frames.size();
    }

    public void addFrame(OcrFrame frame) {
        if (frame != null) {
            if (this.frames == null) {
                this.frames = new ArrayList<>();
            }
            this.frames.add(frame);
            this.framesCount = this.frames.size();
        }
    }

    public double getCostUsd() { return costUsd; }
    public void setCostUsd(double costUsd) { this.costUsd = costUsd; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Search within frames for live search-on-detail (§1181).
     */
    public List<OcrFrame> searchFrames(String query) {
        if (query == null || query.trim().isEmpty() || frames == null) {
            return getFrames();
        }
        String lower = query.trim().toLowerCase(Locale.US);
        List<OcrFrame> matches = new ArrayList<>();
        for (OcrFrame frame : frames) {
            if (frame.getText() != null && frame.getText().toLowerCase(Locale.US).contains(lower)) {
                matches.add(frame);
            }
        }
        return matches;
    }

    public String toFramesJson() {
        if (frames == null || frames.isEmpty()) {
            return "[]";
        }
        JSONArray array = new JSONArray();
        for (OcrFrame f : frames) {
            try {
                array.put(f.toJson());
            } catch (JSONException ignored) {}
        }
        return array.toString();
    }

    public static List<OcrFrame> parseFramesJson(String json) {
        List<OcrFrame> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    OcrFrame frame = OcrFrame.fromJson(obj);
                    if (frame != null) {
                        list.add(frame);
                    }
                }
            }
        } catch (JSONException ignored) {}
        return list;
    }
}
