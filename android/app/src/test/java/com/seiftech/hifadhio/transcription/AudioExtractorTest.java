package com.seiftech.hifadhio.transcription;

import com.seiftech.hifadhio.data.ContentItem;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AudioExtractorTest {

    @Test
    public void testEligibilityForTranscription() {
        AudioExtractor extractor = new AudioExtractor(null, null);

        ContentItem youtube = new ContentItem("https://www.youtube.com/watch?v=dQw4w9WgXcQ", "YouTube");
        assertTrue(extractor.isEligibleForTranscription(youtube));

        ContentItem tiktok = new ContentItem("https://www.tiktok.com/@user/video/1234567890", "TikTok");
        assertTrue(extractor.isEligibleForTranscription(tiktok));

        ContentItem instaReel = new ContentItem("https://www.instagram.com/reel/C7xyz123/", "Instagram");
        assertTrue(extractor.isEligibleForTranscription(instaReel));

        ContentItem fb = new ContentItem("https://www.facebook.com/reel/123456", "Facebook");
        assertTrue(extractor.isEligibleForTranscription(fb));

        ContentItem mp4Link = new ContentItem("https://example.com/podcast/episode42.mp4", "Web");
        assertTrue(extractor.isEligibleForTranscription(mp4Link));

        ContentItem textArticle = new ContentItem("https://techcrunch.com/2026/09/28/ai-news/", "Web");
        assertFalse(extractor.isEligibleForTranscription(textArticle));

        assertFalse(extractor.isEligibleForTranscription(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateMissingFile() {
        AudioExtractor extractor = new AudioExtractor(null, null);
        extractor.validateAudioFile(new File("non_existent_path.m4a"), TranscriptionOptions.defaults());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateEmptyFile() throws Exception {
        AudioExtractor extractor = new AudioExtractor(null, null);
        File temp = File.createTempFile("empty_", ".m4a");
        try {
            extractor.validateAudioFile(temp, TranscriptionOptions.defaults());
        } finally {
            temp.delete();
        }
    }

    @Test
    public void testValidateValidFile() throws Exception {
        AudioExtractor extractor = new AudioExtractor(null, null);
        File temp = File.createTempFile("valid_", ".m4a");
        try {
            try (FileWriter fw = new FileWriter(temp)) {
                fw.write("audio stream payload simulation");
            }
            boolean valid = extractor.validateAudioFile(temp, TranscriptionOptions.defaults());
            assertTrue(valid);
        } finally {
            temp.delete();
        }
    }
}
