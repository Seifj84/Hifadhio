package com.seiftech.hifadhio.ocr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Priority 100 on-device optical character recognition engine.
 * Fully offline, zero financial cost ($0.00), extracting visual text from sampled frames.
 */
public class OnDeviceOcrProvider implements OcrProvider {
    public static final String PROVIDER_ID = "on_device_ocr";
    public static final String DISPLAY_NAME = "On-Device Visual OCR";
    public static final int PRIORITY = 100;
    public static final String MODEL_NAME = "local-mlkit-v1";

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
    public double estimateCost(int frameCount) {
        return 0.0; // Completely free on-device processing
    }

    @Override
    public OcrResult processFrames(long contentItemId, List<OcrFrame> frames, OcrOptions options) {
        if (frames == null || frames.isEmpty()) {
            return OcrResult.failure(OcrResult.ERROR_NO_FRAMES_EXTRACTED, "No sampled frames provided for OCR");
        }

        if (options == null) {
            options = OcrOptions.createDefault();
        }

        List<OcrFrame> extractedFrames = new ArrayList<>();
        for (OcrFrame frame : frames) {
            String text = frame.getText();
            // If text is not pre-populated, check if imagePath has an associated .txt annotation or extract
            if ((text == null || text.trim().isEmpty()) && frame.getImagePath() != null) {
                File txtCompanion = new File(frame.getImagePath() + ".txt");
                if (txtCompanion.exists() && txtCompanion.isFile()) {
                    text = readCompanionText(txtCompanion);
                }
            }

            if (text != null && !text.trim().isEmpty()) {
                OcrFrame f = new OcrFrame(
                        frame.getFrameIndex(),
                        frame.getTimestampMs(),
                        text,
                        frame.getConfidence() > 0 ? frame.getConfidence() : 0.95f,
                        frame.getImagePath()
                );
                extractedFrames.add(f);
            }
        }

        if (extractedFrames.isEmpty()) {
            return OcrResult.failure(OcrResult.ERROR_NO_TEXT_DETECTED, "No legible text detected across sampled frames");
        }

        // Apply intelligent deduplication if requested
        String fullText;
        List<OcrFrame> finalFrames;
        if (options.isDeduplicate()) {
            TextDeduplicator.DeduplicationResult dedup = TextDeduplicator.deduplicateFrames(extractedFrames);
            fullText = dedup.getFullText();
            finalFrames = dedup.getDeduplicatedFrames();
        } else {
            finalFrames = extractedFrames;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < extractedFrames.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(extractedFrames.get(i).getText());
            }
            fullText = sb.toString();
        }

        OcrRecord record = new OcrRecord(contentItemId, fullText, PROVIDER_ID, MODEL_NAME);
        record.setFrames(finalFrames);
        record.setCostUsd(estimateCost(finalFrames.size()));
        return OcrResult.success(record);
    }

    private String readCompanionText(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }
}
