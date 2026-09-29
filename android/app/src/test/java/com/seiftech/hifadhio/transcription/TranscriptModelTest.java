package com.seiftech.hifadhio.transcription;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranscriptModelTest {

    @Test
    public void testTranscriptSegmentTimestamps() {
        TranscriptSegment seg = new TranscriptSegment(15000, 18500, "Hello world", 0.98);
        assertTrue(seg.hasTimestamps());
        assertEquals("00:15", seg.getFormattedTimestamp());
        assertEquals("Hello world", seg.getText());
        assertEquals(0.98, seg.getConfidence(), 0.001);

        TranscriptSegment longSeg = new TranscriptSegment(3665000, 3670000, "Hour mark");
        assertTrue(longSeg.hasTimestamps());
        assertEquals("01:01:05", longSeg.getFormattedTimestamp());

        // Master Spec §15.3: Never fabricate timestamps
        TranscriptSegment noTimeSeg = new TranscriptSegment(-1, -1, "Untimed text");
        assertFalse(noTimeSeg.hasTimestamps());
        assertEquals("", noTimeSeg.getFormattedTimestamp());
    }

    @Test
    public void testTranscriptSegmentJsonRoundtrip() {
        TranscriptSegment seg = new TranscriptSegment(12000, 16000, "Testing JSON serialization", 0.95, "Speaker A");
        JSONObject json = seg.toJson();
        assertNotNull(json);

        TranscriptSegment parsed = TranscriptSegment.fromJson(json);
        assertNotNull(parsed);
        assertEquals(12000, parsed.getStartMs());
        assertEquals(16000, parsed.getEndMs());
        assertEquals("Testing JSON serialization", parsed.getText());
        assertEquals("Speaker A", parsed.getSpeaker());
        assertEquals(0.95, parsed.getConfidence(), 0.001);
    }

    @Test
    public void testTranscriptDurationFormatting() {
        Transcript t = new Transcript();
        t.setDurationMs(75000); // 1 min 15 sec
        assertEquals("01:15", t.getFormattedDuration());

        t.setDurationMs(3690000); // 1 hour, 1 min, 30 sec
        assertEquals("01:01:30", t.getFormattedDuration());

        t.setDurationMs(0);
        assertEquals("00:00", t.getFormattedDuration());
    }

    @Test
    public void testSearchSegmentsOnDetail() {
        Transcript t = new Transcript(1, "Full speech text", "en", "offline", "test");
        t.addSegment(new TranscriptSegment(0, 5000, "Welcome to the podcast"));
        t.addSegment(new TranscriptSegment(5000, 10000, "Today we discuss artificial intelligence"));
        t.addSegment(new TranscriptSegment(10000, 15000, "Specifically on-device machine learning"));

        assertTrue(t.hasTimestamps());

        List<TranscriptSegment> matches = t.searchSegments("intelligence");
        assertEquals(1, matches.size());
        assertEquals("Today we discuss artificial intelligence", matches.get(0).getText());

        List<TranscriptSegment> multiMatches = t.searchSegments("to");
        // Matches "Welcome to the podcast" and "Today we discuss..."
        assertEquals(2, multiMatches.size());

        List<TranscriptSegment> noMatches = t.searchSegments("blockchain");
        assertTrue(noMatches.isEmpty());
    }

    @Test
    public void testSegmentsJsonRoundtrip() {
        Transcript t = new Transcript(42, "Full transcript text", "en", "subtitles", "test");
        t.addSegment(new TranscriptSegment(1000, 3000, "Segment one"));
        t.addSegment(new TranscriptSegment(3000, 6000, "Segment two"));

        String json = t.toSegmentsJson();
        assertNotNull(json);

        List<TranscriptSegment> parsed = Transcript.parseSegmentsJson(json);
        assertEquals(2, parsed.size());
        assertEquals("Segment one", parsed.get(0).getText());
        assertEquals("Segment two", parsed.get(1).getText());
    }

    @Test
    public void testGetPreview() {
        Transcript t = new Transcript(1, "The quick brown fox jumps over the lazy dog", "en", "test", "test");
        assertEquals("The quick brown fox...", t.getPreview(20));
        assertEquals("The quick brown fox jumps over the lazy dog", t.getPreview(100));
    }
}
