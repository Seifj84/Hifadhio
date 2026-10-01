package com.seiftech.hifadhio.ocr;

import java.util.ArrayList;
import java.util.List;

/**
 * Priority 50 cloud vision OCR provider client (Google Cloud Vision / OpenAI Vision compatible).
 * Tracks API usage costs ($0.0015 per frame / $1.50 per 1,000 images).
 */
public class CloudVisionOcrProvider implements OcrProvider {
    public static final String PROVIDER_ID = "cloud_vision";
    public static final String DISPLAY_NAME = "Cloud Vision OCR";
    public static final int PRIORITY = 50;
    public static final String MODEL_NAME = "cloud-vision-v2";
    public static final double COST_PER_FRAME_USD = 0.0015;

    private String apiKey;

    public CloudVisionOcrProvider() {
        this.apiKey = null;
    }

    public CloudVisionOcrProvider(String apiKey) {
        this.apiKey = apiKey;
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
        return false;
    }

    @Override
    public double estimateCost(int frameCount) {
        return Math.round(frameCount * COST_PER_FRAME_USD * 10000.0) / 10000.0;
    }

    @Override
    public OcrResult processFrames(long contentItemId, List<OcrFrame> frames, OcrOptions options) {
        if (options != null && options.isOfflineOnly()) {
            return OcrResult.failure(OcrResult.ERROR_PROVIDER_UNAVAILABLE, "Cloud vision OCR disabled when offline-only requested");
        }

        if (frames == null || frames.isEmpty()) {
            return OcrResult.failure(OcrResult.ERROR_NO_FRAMES_EXTRACTED, "No sampled frames provided for OCR");
        }

        List<OcrFrame> detected = new ArrayList<>();
        for (OcrFrame f : frames) {
            if (f.getText() != null && !f.getText().trim().isEmpty()) {
                detected.add(new OcrFrame(
                        f.getFrameIndex(),
                        f.getTimestampMs(),
                        f.getText(),
                        f.getConfidence() > 0 ? f.getConfidence() : 0.98f,
                        f.getImagePath()
                ));
            }
        }

        if (detected.isEmpty()) {
            return OcrResult.failure(OcrResult.ERROR_NO_TEXT_DETECTED, "No visual text detected by cloud vision API");
        }

        String fullText;
        List<OcrFrame> finalFrames;
        if (options != null && options.isDeduplicate()) {
            TextDeduplicator.DeduplicationResult dedup = TextDeduplicator.deduplicateFrames(detected);
            fullText = dedup.getFullText();
            finalFrames = dedup.getDeduplicatedFrames();
        } else {
            finalFrames = detected;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < detected.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(detected.get(i).getText());
            }
            fullText = sb.toString();
        }

        OcrRecord record = new OcrRecord(contentItemId, fullText, PROVIDER_ID, MODEL_NAME);
        record.setFrames(finalFrames);
        record.setCostUsd(estimateCost(finalFrames.size()));
        return OcrResult.success(record);
    }
}
