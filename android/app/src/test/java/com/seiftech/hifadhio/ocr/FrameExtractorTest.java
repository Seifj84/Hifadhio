package com.seiftech.hifadhio.ocr;

import com.seiftech.hifadhio.model.ContentItem;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static org.junit.Assert.*;

public class FrameExtractorTest {

    private FrameExtractor extractor;
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = File.createTempFile("frame_test_", "");
        tempDir.delete();
        tempDir.mkdirs();
        extractor = new FrameExtractor(null);
    }

    @Test
    public void testEligibilityDetection() {
        ContentItem yt = new ContentItem("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        yt.setPlatform("YouTube");
        assertTrue(FrameExtractor.isEligibleForOcr(yt));

        ContentItem tiktok = new ContentItem("https://www.tiktok.com/@user/video/12345");
        tiktok.setPlatform("TikTok");
        assertTrue(FrameExtractor.isEligibleForOcr(tiktok));

        ContentItem insta = new ContentItem("https://www.instagram.com/reel/xyz123/");
        insta.setPlatform("Instagram");
        assertTrue(FrameExtractor.isEligibleForOcr(insta));

        ContentItem directMp4 = new ContentItem("https://example.com/lecture_video.mp4");
        directMp4.setPlatform("Web");
        assertTrue(FrameExtractor.isEligibleForOcr(directMp4));

        ContentItem directPng = new ContentItem("https://example.com/infographic_chart.png");
        directPng.setPlatform("Web");
        assertTrue(FrameExtractor.isEligibleForOcr(directPng));

        ContentItem webWithThumb = new ContentItem("https://blog.example.com/article");
        webWithThumb.setPlatform("Web");
        webWithThumb.setThumbnailUrl("https://blog.example.com/thumb.jpg");
        assertTrue(FrameExtractor.isEligibleForOcr(webWithThumb));

        ContentItem plainWeb = new ContentItem("https://textonly.example.com/readme.txt");
        plainWeb.setPlatform("Web");
        assertFalse(FrameExtractor.isEligibleForOcr(plainWeb));
    }

    @Test
    public void testSampleVideoFramesRespectsIntervalAndMaxLimit() throws Exception {
        File dummyVideo = new File(tempDir, "sample.mp4");
        try (FileOutputStream fos = new FileOutputStream(dummyVideo)) {
            fos.write(new byte[1024]);
        }

        OcrOptions options = OcrOptions.createDefault();
        options.setSampleIntervalSeconds(5);
        options.setMaxFrames(15);

        // 30-second video: 30 / 5 = 6 frames
        List<OcrFrame> frames30s = extractor.sampleVideoFrames(dummyVideo, 30000L, options);
        assertEquals(6, frames30s.size());
        assertEquals(0L, frames30s.get(0).getTimestampMs());
        assertEquals(5000L, frames30s.get(1).getTimestampMs());
        assertEquals(25000L, frames30s.get(5).getTimestampMs());

        // Long video (600 seconds): 600 / 5 = 120 potential slots, but maxFrames = 15 must cap it
        List<OcrFrame> cappedFrames = extractor.sampleVideoFrames(dummyVideo, 600000L, options);
        assertEquals(15, cappedFrames.size());
    }

    @Test
    public void testSampleImageFrameNeverFabricatesTimestamp() throws Exception {
        File dummyImage = new File(tempDir, "screenshot.png");
        try (FileOutputStream fos = new FileOutputStream(dummyImage)) {
            fos.write(new byte[512]);
        }

        List<OcrFrame> imageFrames = extractor.sampleImageFrame(dummyImage);
        assertEquals(1, imageFrames.size());
        OcrFrame frame = imageFrames.get(0);
        // Master Spec §15.3 & §15.4: Never fabricate timestamps for static images
        assertEquals(-1L, frame.getTimestampMs());
        assertFalse(frame.hasTimestamp());
        assertEquals("", frame.getFormattedTimestamp());
    }

    @Test
    public void testCleanSampledFramesRemovesFiles() throws Exception {
        File f1 = new File(tempDir, "test_f1.jpg");
        try (FileOutputStream fos = new FileOutputStream(f1)) {
            fos.write(new byte[100]);
        }
        assertTrue(f1.exists());

        OcrFrame frame = new OcrFrame(0, 0L, "Sample", 1.0f, f1.getAbsolutePath());
        extractor.cleanSampledFrames(java.util.Collections.singletonList(frame));
        assertFalse(f1.exists());
    }
}
