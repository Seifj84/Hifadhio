package com.seiftech.hifadhio.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class UrlNormalizerTest {

    @Test
    public void testExtractUrlFromMessyShareText() {
        String shared1 = "Look at this amazing recipe https://www.instagram.com/reel/DCb1234/?igshid=abc123xyz it looks delicious!";
        assertEquals("https://www.instagram.com/reel/DCb1234/?igshid=abc123xyz", UrlNormalizer.extractUrl(shared1));

        String shared2 = "https://youtube.com/shorts/xyz123?si=trackingsi";
        assertEquals("https://youtube.com/shorts/xyz123?si=trackingsi", UrlNormalizer.extractUrl(shared2));
    }

    @Test
    public void testNormalizeStripsTrackingParameters() {
        String raw = "https://www.instagram.com/reel/DCb1234/?igshid=abc123xyz&utm_source=ig_web_copy_link";
        String normalized = UrlNormalizer.normalize(raw);
        assertEquals("https://instagram.com/reel/DCb1234", normalized);

        String ytRaw = "https://youtu.be/dQw4w9WgXcQ?si=abcdef&utm_medium=share";
        String ytNormalized = UrlNormalizer.normalize(ytRaw);
        assertEquals("https://youtu.be/dQw4w9WgXcQ", ytNormalized);
    }

    @Test
    public void testCanonicalUrlDuplicateDetectionMatching() {
        // Link with tracking parameters and www prefix vs clean link
        String link1 = "https://www.instagram.com/reel/DCb1234/?igshid=abc123xyz&utm_source=ig_web_copy_link";
        String link2 = "https://instagram.com/reel/DCb1234/";
        String link3 = "https://instagram.com/reel/DCb1234";

        assertEquals(UrlNormalizer.normalize(link1), UrlNormalizer.normalize(link2));
        assertEquals(UrlNormalizer.normalize(link2), UrlNormalizer.normalize(link3));

        // YouTube variant matching
        String ytShare = "https://youtu.be/dQw4w9WgXcQ?si=tracker123&utm_medium=share";
        String ytClean = "https://youtu.be/dQw4w9WgXcQ";
        assertEquals(UrlNormalizer.normalize(ytShare), UrlNormalizer.normalize(ytClean));

        // Query param order determinism
        String queryA = "https://example.com/article?b=2&a=1";
        String queryB = "https://example.com/article?a=1&b=2";
        assertEquals(UrlNormalizer.normalize(queryA), UrlNormalizer.normalize(queryB));
    }

    @Test
    public void testIsValidUrl() {
        assertTrue(UrlNormalizer.isValidUrl("https://example.com"));
        assertTrue(UrlNormalizer.isValidUrl("http://example.com/path"));
        assertFalse(UrlNormalizer.isValidUrl("not-a-url"));
        assertFalse(UrlNormalizer.isValidUrl("ftp://fileserver"));
        assertFalse(UrlNormalizer.isValidUrl(""));
        assertFalse(UrlNormalizer.isValidUrl(null));
    }

    @Test
    public void testPlatformDetection() {
        assertEquals("Instagram", PlatformDetector.detect("https://www.instagram.com/p/C123"));
        assertEquals("TikTok", PlatformDetector.detect("https://www.tiktok.com/@creator/video/12345"));
        assertEquals("YouTube", PlatformDetector.detect("https://www.youtube.com/watch?v=123"));
        assertEquals("YouTube", PlatformDetector.detect("https://youtu.be/123"));
        assertEquals("Facebook", PlatformDetector.detect("https://fb.watch/12345/"));
        assertEquals("X", PlatformDetector.detect("https://x.com/OpenAI/status/12345"));
        assertEquals("X", PlatformDetector.detect("https://twitter.com/OpenAI/status/12345"));
        assertEquals("Web", PlatformDetector.detect("https://news.ycombinator.com/item?id=123"));
    }
}
