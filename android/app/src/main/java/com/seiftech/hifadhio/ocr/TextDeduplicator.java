package com.seiftech.hifadhio.ocr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Intelligent text deduplication engine for video frame OCR per Master Spec §1195.
 * Suppresses persistent on-screen watermarks, static title headers, and repeated channel logos
 * across consecutive sampled frames without losing unique content or timestamp references.
 */
public class TextDeduplicator {

    /**
     * Deduplicates extracted text across sampled video frames.
     * 1. Detects persistent global watermarks/titles appearing across >= 60% of all frames.
     * 2. Eliminates identical or near-identical adjacent lines across consecutive frames.
     * 3. Constructs a clean, chronological full-text transcript of on-screen visual content.
     */
    public static DeduplicationResult deduplicateFrames(List<OcrFrame> frames) {
        if (frames == null || frames.isEmpty()) {
            return new DeduplicationResult("", new ArrayList<OcrFrame>());
        }

        // Count line frequencies across frames to identify static watermarks
        Map<String, Integer> lineFrequency = new HashMap<>();
        List<List<String>> frameLines = new ArrayList<>();

        for (OcrFrame frame : frames) {
            List<String> lines = extractCleanLines(frame.getText());
            frameLines.add(lines);
            Set<String> uniqueInFrame = new HashSet<>(lines);
            for (String line : uniqueInFrame) {
                String norm = normalizeForComparison(line);
                if (!norm.isEmpty()) {
                    lineFrequency.put(norm, lineFrequency.containsKey(norm) ? lineFrequency.get(norm) + 1 : 1);
                }
            }
        }

        int frameCount = frames.size();
        // Lines appearing in >= 60% of frames (minimum 3 frames) are treated as persistent watermarks/channel branding
        Set<String> persistentWatermarks = new HashSet<>();
        if (frameCount >= 3) {
            int threshold = (int) Math.ceil(frameCount * 0.60);
            for (Map.Entry<String, Integer> entry : lineFrequency.entrySet()) {
                if (entry.getValue() >= threshold) {
                    persistentWatermarks.add(entry.getKey());
                }
            }
        }

        List<OcrFrame> deduplicatedFrames = new ArrayList<>();
        StringBuilder aggregatedFullText = new StringBuilder();
        Set<String> globallySeenNonWatermarks = new HashSet<>();

        String prevLineNorm = "";
        for (int i = 0; i < frames.size(); i++) {
            OcrFrame orig = frames.get(i);
            List<String> rawLines = frameLines.get(i);
            List<String> keptLines = new ArrayList<>();

            for (String line : rawLines) {
                String norm = normalizeForComparison(line);
                if (norm.isEmpty()) continue;

                // If it's a persistent watermark, only keep it in the first frame it appeared in
                if (persistentWatermarks.contains(norm)) {
                    if (!globallySeenNonWatermarks.contains(norm)) {
                        keptLines.add(line);
                        globallySeenNonWatermarks.add(norm);
                    }
                    continue;
                }

                // If identical to immediately preceding line from previous or current frame, skip
                if (norm.equals(prevLineNorm)) {
                    continue;
                }

                // Check similarity with previous line
                if (isHighlySimilar(norm, prevLineNorm)) {
                    continue;
                }

                keptLines.add(line);
                prevLineNorm = norm;
                globallySeenNonWatermarks.add(norm);
            }

            if (!keptLines.isEmpty()) {
                String frameCleanText = joinLines(keptLines);
                OcrFrame cleanFrame = new OcrFrame(
                        orig.getFrameIndex(),
                        orig.getTimestampMs(),
                        frameCleanText,
                        orig.getConfidence(),
                        orig.getImagePath()
                );
                deduplicatedFrames.add(cleanFrame);

                if (aggregatedFullText.length() > 0) {
                    aggregatedFullText.append("\n");
                }
                aggregatedFullText.append(frameCleanText);
            }
        }

        return new DeduplicationResult(aggregatedFullText.toString(), deduplicatedFrames);
    }

    private static List<String> extractCleanLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return lines;
        String[] split = text.split("\r?\n");
        for (String s : split) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private static String normalizeForComparison(String line) {
        if (line == null) return "";
        return line.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
    }

    private static boolean isHighlySimilar(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) return false;
        if (s1.equals(s2)) return true;
        // Substring match if long enough
        if (s1.length() >= 6 && s2.length() >= 6) {
            if (s1.contains(s2) || s2.contains(s1)) return true;
        }
        return false;
    }

    private static String joinLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    public static class DeduplicationResult {
        private final String fullText;
        private final List<OcrFrame> deduplicatedFrames;

        public DeduplicationResult(String fullText, List<OcrFrame> deduplicatedFrames) {
            this.fullText = fullText;
            this.deduplicatedFrames = deduplicatedFrames;
        }

        public String getFullText() { return fullText; }
        public List<OcrFrame> getDeduplicatedFrames() { return deduplicatedFrames; }
    }
}
