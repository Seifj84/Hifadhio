package com.seiftech.hifadhio.transcription;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranscriptionProviderTest {

    @Test
    public void testRegistryPriorityAndDefaultProviders() {
        TranscriptionRegistry registry = TranscriptionRegistry.getInstance();
        assertNotNull(registry);

        List<TranscriptionProvider> all = registry.getAllProviders();
        assertTrue(all.size() >= 3);

        // Highest priority should be SubtitlesExtractorProvider (100)
        assertEquals(SubtitlesExtractorProvider.PROVIDER_ID, all.get(0).getProviderId());
        assertEquals(100, all.get(0).getPriority());

        // Second should be OfflineSpeechProvider (80)
        assertEquals(OfflineSpeechProvider.PROVIDER_ID, all.get(1).getProviderId());
        assertEquals(80, all.get(1).getPriority());

        // Offline provider selection
        TranscriptionProvider offline = registry.getBestProvider(true);
        assertNotNull(offline);
        assertTrue(offline.isOffline());
    }

    @Test
    public void testSubtitlesExtractorSrtParsing() {
        SubtitlesExtractorProvider provider = new SubtitlesExtractorProvider();
        String srt = "1\n" +
                "00:00:01,500 --> 00:00:04,200\n" +
                "Welcome to Hifadhio knowledge repository.\n\n" +
                "2\n" +
                "00:00:04,500 --> 00:00:08,000\n" +
                "Save once. Find it when it matters.\n";

        TranscriptionResult result = provider.parseSubtitleContent(10, srt, System.currentTimeMillis());
        assertTrue(result.isSuccess());
        assertNotNull(result.getTranscript());

        Transcript transcript = result.getTranscript();
        assertEquals("Welcome to Hifadhio knowledge repository. Save once. Find it when it matters.", transcript.getFullText());
        assertEquals(2, transcript.getSegments().size());

        TranscriptSegment s1 = transcript.getSegments().get(0);
        assertEquals(1500, s1.getStartMs());
        assertEquals(4200, s1.getEndMs());
        assertEquals("Welcome to Hifadhio knowledge repository.", s1.getText());
        assertEquals("00:01", s1.getFormattedTimestamp());

        TranscriptSegment s2 = transcript.getSegments().get(1);
        assertEquals(4500, s2.getStartMs());
        assertEquals(8000, s2.getEndMs());
        assertEquals("Save once. Find it when it matters.", s2.getText());
        assertEquals(0.0, transcript.getCostUsd(), 0.0001);
    }

    @Test
    public void testSubtitlesExtractorVttParsing() {
        SubtitlesExtractorProvider provider = new SubtitlesExtractorProvider();
        String vtt = "WEBVTT\n\n" +
                "01:10.000 --> 01:15.500\n" +
                "<v Speaker1>This is a <i>WebVTT</i> subtitle test.\n\n" +
                "01:16.000 --> 01:20.000\n" +
                "Tags are stripped cleanly.\n";

        TranscriptionResult result = provider.parseSubtitleContent(20, vtt, System.currentTimeMillis());
        assertTrue(result.isSuccess());

        Transcript transcript = result.getTranscript();
        assertEquals("This is a WebVTT subtitle test. Tags are stripped cleanly.", transcript.getFullText());
        assertEquals(2, transcript.getSegments().size());
        assertEquals(70000, transcript.getSegments().get(0).getStartMs()); // 1m 10s = 70000ms
        assertEquals(75500, transcript.getSegments().get(0).getEndMs());
        assertEquals("01:10", transcript.getSegments().get(0).getFormattedTimestamp());
    }

    @Test
    public void testSubtitlesExtractorEmptyContent() {
        SubtitlesExtractorProvider provider = new SubtitlesExtractorProvider();
        TranscriptionResult result = provider.parseSubtitleContent(30, "   ", System.currentTimeMillis());
        assertFalse(result.isSuccess());
        assertEquals(TranscriptionResult.ERROR_NO_SPEECH_DETECTED, result.getErrorCode());
    }

    @Test
    public void testOfflineSpeechProviderTextFile() throws Exception {
        OfflineSpeechProvider provider = new OfflineSpeechProvider();

        File tempFile = File.createTempFile("test_audio_", ".txt");
        try {
            try (FileWriter fw = new FileWriter(tempFile)) {
                fw.write("Transcribed text content directly from audio stream.");
            }

            TranscriptionOptions options = TranscriptionOptions.defaults();
            TranscriptionResult result = provider.transcribe(40, tempFile, options);

            assertTrue(result.isSuccess());
            Transcript t = result.getTranscript();
            assertEquals("Transcribed text content directly from audio stream.", t.getFullText());
            assertEquals(OfflineSpeechProvider.PROVIDER_ID, t.getProviderId());
            assertEquals(0.0, t.getCostUsd(), 0.0001);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testOfflineSpeechProviderFileLimits() throws Exception {
        OfflineSpeechProvider provider = new OfflineSpeechProvider();

        // Non-existent file
        File missing = new File("non_existent_audio.wav");
        TranscriptionResult resMissing = provider.transcribe(50, missing, TranscriptionOptions.defaults());
        assertFalse(resMissing.isSuccess());
        assertEquals(TranscriptionResult.ERROR_AUDIO_FETCH_FAILED, resMissing.getErrorCode());

        // File too large limit
        File temp = File.createTempFile("large_", ".wav");
        try {
            try (FileWriter fw = new FileWriter(temp)) {
                fw.write("Some dummy bytes");
            }
            TranscriptionOptions smallLimit = TranscriptionOptions.defaults().setMaxFileSizeBytes(5); // 5 bytes limit
            TranscriptionResult resTooLarge = provider.transcribe(51, temp, smallLimit);
            assertFalse(resTooLarge.isSuccess());
            assertEquals(TranscriptionResult.ERROR_FILE_TOO_LARGE, resTooLarge.getErrorCode());
        } finally {
            temp.delete();
        }
    }

    @Test
    public void testCloudWhisperProviderCostAndValidation() {
        CloudWhisperProvider cloud = new CloudWhisperProvider(null, null, "whisper-1");
        assertFalse(cloud.isAvailable()); // No API key configured
        assertFalse(cloud.isOffline());

        // 1 minute (60,000 ms) = $0.006
        assertEquals(0.006, cloud.estimateCost(60000), 0.0001);
        // 5 minutes (300,000 ms) = $0.03
        assertEquals(0.03, cloud.estimateCost(300000), 0.0001);

        // Parsing mock verbose_json response
        String json = "{\n" +
                "  \"text\": \"Welcome to the AI revolution.\",\n" +
                "  \"language\": \"english\",\n" +
                "  \"duration\": 12.5,\n" +
                "  \"segments\": [\n" +
                "    {\"start\": 0.0, \"end\": 6.0, \"text\": \"Welcome to the\"},\n" +
                "    {\"start\": 6.0, \"end\": 12.5, \"text\": \"AI revolution.\"}\n" +
                "  ]\n" +
                "}";

        TranscriptionResult parsed = cloud.parseApiResponse(60, json, System.currentTimeMillis());
        assertTrue(parsed.isSuccess());
        Transcript t = parsed.getTranscript();
        assertEquals("Welcome to the AI revolution.", t.getFullText());
        assertEquals("english", t.getLanguage());
        assertEquals(12500, t.getDurationMs());
        assertEquals(2, t.getSegments().size());
        assertEquals(0, t.getSegments().get(0).getStartMs());
        assertEquals(6000, t.getSegments().get(0).getEndMs());
        assertEquals("Welcome to the", t.getSegments().get(0).getText());
        assertEquals(6000, t.getSegments().get(1).getStartMs());
        assertEquals(12500, t.getSegments().get(1).getEndMs());
        assertEquals("AI revolution.", t.getSegments().get(1).getText());
    }
}
