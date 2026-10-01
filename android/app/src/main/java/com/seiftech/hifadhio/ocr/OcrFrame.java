package com.seiftech.hifadhio.ocr;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Domain model representing a single sampled frame and its extracted visual text.
 * Strictly maintains true timestamps (or -1 for static images) per Master Spec §15.4.
 */
public class OcrFrame {
    private int frameIndex;
    private long timestampMs; // -1 if not a timecoded video frame
    private String text;
    private float confidence;
    private String imagePath;

    public OcrFrame() {
        this.frameIndex = 0;
        this.timestampMs = -1L;
        this.text = "";
        this.confidence = 1.0f;
    }

    public OcrFrame(int frameIndex, long timestampMs, String text) {
        this.frameIndex = frameIndex;
        this.timestampMs = timestampMs;
        this.text = text != null ? text.trim() : "";
        this.confidence = 1.0f;
    }

    public OcrFrame(int frameIndex, long timestampMs, String text, float confidence, String imagePath) {
        this.frameIndex = frameIndex;
        this.timestampMs = timestampMs;
        this.text = text != null ? text.trim() : "";
        this.confidence = confidence;
        this.imagePath = imagePath;
    }

    public int getFrameIndex() { return frameIndex; }
    public void setFrameIndex(int frameIndex) { this.frameIndex = frameIndex; }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }

    public String getText() { return text != null ? text : ""; }
    public void setText(String text) { this.text = text != null ? text.trim() : ""; }

    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean hasTimestamp() {
        return timestampMs >= 0;
    }

    public String getFormattedTimestamp() {
        if (!hasTimestamp()) return "";
        long totalSeconds = timestampMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("frame_index", frameIndex);
        obj.put("timestamp_ms", timestampMs);
        obj.put("text", text);
        obj.put("confidence", (double) confidence);
        if (imagePath != null && !imagePath.isEmpty()) {
            obj.put("image_path", imagePath);
        }
        return obj;
    }

    public static OcrFrame fromJson(JSONObject obj) {
        if (obj == null) return null;
        int index = obj.optInt("frame_index", 0);
        long ts = obj.optLong("timestamp_ms", -1L);
        String text = obj.optString("text", "");
        float conf = (float) obj.optDouble("confidence", 1.0);
        String path = obj.optString("image_path", null);
        return new OcrFrame(index, ts, text, conf, path);
    }
}
