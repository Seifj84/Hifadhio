package com.seiftech.hifadhio.transcription;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single time-coded speech segment in a transcript.
 * Adheres strictly to Master Spec §15.3: "Never fabricate timestamps".
 * If timestamps were not genuinely returned by the provider, startMs and endMs are -1.
 */
public class TranscriptSegment {
    private final long startMs;
    private final long endMs;
    private final String text;
    private final double confidence;
    private final String speaker;

    public TranscriptSegment(long startMs, long endMs, String text) {
        this(startMs, endMs, text, 1.0, null);
    }

    public TranscriptSegment(long startMs, long endMs, String text, double confidence) {
        this(startMs, endMs, text, confidence, null);
    }

    public TranscriptSegment(long startMs, long endMs, String text, double confidence, String speaker) {
        this.startMs = startMs;
        this.endMs = endMs;
        this.text = text != null ? text.trim() : "";
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.speaker = speaker;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public String getText() {
        return text;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSpeaker() {
        return speaker;
    }

    public boolean hasTimestamps() {
        return startMs >= 0 && endMs >= startMs;
    }

    /**
     * Formats start timestamp as mm:ss or hh:mm:ss.
     */
    public String getFormattedTimestamp() {
        if (!hasTimestamps()) {
            return "";
        }
        long totalSeconds = startMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("start_ms", startMs);
            obj.put("end_ms", endMs);
            obj.put("text", text);
            obj.put("confidence", confidence);
            if (speaker != null) {
                obj.put("speaker", speaker);
            }
        } catch (JSONException ignored) {}
        return obj;
    }

    public static TranscriptSegment fromJson(JSONObject obj) {
        if (obj == null) return null;
        long start = obj.optLong("start_ms", -1);
        long end = obj.optLong("end_ms", -1);
        String text = obj.optString("text", "");
        double confidence = obj.optDouble("confidence", 1.0);
        String speaker = obj.has("speaker") ? obj.optString("speaker", null) : null;
        return new TranscriptSegment(start, end, text, confidence, speaker);
    }
}
