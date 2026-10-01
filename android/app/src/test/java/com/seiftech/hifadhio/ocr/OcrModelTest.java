package com.seiftech.hifadhio.ocr;

import org.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class OcrModelTest {

    @Test
    public void testOcrFrameTimestampFormatting() {
        OcrFrame frame1 = new OcrFrame(0, 0L, "Intro Slide");
        assertTrue(frame1.hasTimestamp());
        assertEquals("00:00", frame1.getFormattedTimestamp());

        OcrFrame frame2 = new OcrFrame(1, 65000L, "Code Example");
        assertTrue(frame2.hasTimestamp());
        assertEquals("01:05", frame2.getFormattedTimestamp());

        OcrFrame staticFrame = new OcrFrame(0, -1L, "Static Image");
        assertFalse(staticFrame.hasTimestamp());
        assertEquals("", staticFrame.getFormattedTimestamp());
    }

    @Test
    public void testOcrFrameJsonSerialization() throws Exception {
        OcrFrame frame = new OcrFrame(2, 12500L, "Important Architecture Diagram", 0.95f, "/data/media/frames/f2.jpg");
        JSONObject json = frame.toJson();
        assertNotNull(json);
        assertEquals(2, json.getInt("frame_index"));
        assertEquals(12500L, json.getLong("timestamp_ms"));
        assertEquals("Important Architecture Diagram", json.getString("text"));
        assertEquals(0.95, json.getDouble("confidence"), 0.01);
        assertEquals("/data/media/frames/f2.jpg", json.getString("image_path"));

        OcrFrame restored = OcrFrame.fromJson(json);
        assertNotNull(restored);
        assertEquals(2, restored.getFrameIndex());
        assertEquals(12500L, restored.getTimestampMs());
        assertEquals("Important Architecture Diagram", restored.getText());
        assertEquals(0.95f, restored.getConfidence(), 0.01f);
        assertEquals("/data/media/frames/f2.jpg", restored.getImagePath());
    }

    @Test
    public void testOcrRecordSearchFramesOnDetail() {
        OcrRecord record = new OcrRecord(101L, "Full OCR text", "on_device_ocr", "local-mlkit-v1");
        record.addFrame(new OcrFrame(0, 0L, "Welcome to the Machine Learning Tutorial"));
        record.addFrame(new OcrFrame(1, 5000L, "Neural Networks and Deep Architecture"));
        record.addFrame(new OcrFrame(2, 10000L, "Summary and Conclusion"));

        assertEquals(3, record.getFramesCount());

        List<OcrFrame> matches = record.searchFrames("Neural");
        assertEquals(1, matches.size());
        assertEquals("Neural Networks and Deep Architecture", matches.get(0).getText());

        List<OcrFrame> emptyMatches = record.searchFrames("Quantum");
        assertTrue(emptyMatches.isEmpty());

        List<OcrFrame> allMatches = record.searchFrames("");
        assertEquals(3, allMatches.size());
    }

    @Test
    public void testOcrRecordJsonRoundtrip() {
        OcrRecord record = new OcrRecord(42L, "Aggregated OCR content", "on_device_ocr", "local-mlkit-v1");
        record.addFrame(new OcrFrame(0, 1000L, "Frame 1 text"));
        record.addFrame(new OcrFrame(1, 6000L, "Frame 2 text"));

        String json = record.toFramesJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());

        List<OcrFrame> parsed = OcrRecord.parseFramesJson(json);
        assertEquals(2, parsed.size());
        assertEquals("Frame 1 text", parsed.get(0).getText());
        assertEquals("Frame 2 text", parsed.get(1).getText());
        assertEquals(1000L, parsed.get(0).getTimestampMs());
        assertEquals(6000L, parsed.get(1).getTimestampMs());
    }

    @Test
    public void testTextDeduplicatorRemovesConsecutiveDuplicatesAndWatermarks() {
        List<OcrFrame> frames = new ArrayList<>();
        // Watermark "@hifadhio_app" appears in all 4 frames
        frames.add(new OcrFrame(0, 0L, "@hifadhio_app\nIntroduction to Clean Architecture"));
        frames.add(new OcrFrame(1, 5000L, "@hifadhio_app\nIntroduction to Clean Architecture")); // Exact duplicate slide
        frames.add(new OcrFrame(2, 10000L, "@hifadhio_app\nDomain Driven Design Rules")); // New content
        frames.add(new OcrFrame(3, 15000L, "@hifadhio_app\nRepository Pattern")); // New content

        TextDeduplicator.DeduplicationResult result = TextDeduplicator.deduplicateFrames(frames);
        assertNotNull(result);

        String fullText = result.getFullText();
        assertNotNull(fullText);

        // Watermark should only appear once in full text, not 4 times
        int watermarkCount = 0;
        int idx = 0;
        while ((idx = fullText.indexOf("@hifadhio_app", idx)) != -1) {
            watermarkCount++;
            idx += "@hifadhio_app".length();
        }
        assertEquals(1, watermarkCount);

        // Repeated title "Introduction to Clean Architecture" should only appear once
        int introCount = 0;
        idx = 0;
        while ((idx = fullText.indexOf("Introduction to Clean Architecture", idx)) != -1) {
            introCount++;
            idx += "Introduction to Clean Architecture".length();
        }
        assertEquals(1, introCount);

        // All distinct contents must be preserved
        assertTrue(fullText.contains("Domain Driven Design Rules"));
        assertTrue(fullText.contains("Repository Pattern"));
    }
}
