package com.seiftech.hifadhio.adapter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class YouTubeAdapterTest {

    private YouTubeAdapter adapter;

    @Before
    public void setUp() {
        adapter = new YouTubeAdapter();
    }

    @Test
    public void testCanHandleYouTubeUrls() {
        assertTrue(adapter.canHandle("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertTrue(adapter.canHandle("https://youtu.be/dQw4w9WgXcQ"));
        assertTrue(adapter.canHandle("https://www.youtube.com/shorts/kJQP7kiw5Fk"));
        assertTrue(adapter.canHandle("https://m.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertTrue(adapter.canHandle("https://youtube.com/live/jfKfPfyJRdk"));

        assertFalse(adapter.canHandle("https://facebook.com/share/r/12345"));
        assertFalse(adapter.canHandle("https://example.com/article"));
        assertFalse(adapter.canHandle(null));
        assertFalse(adapter.canHandle(""));
    }

    @Test
    public void testExtractVideoId() {
        assertEquals("dQw4w9WgXcQ", adapter.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertEquals("dQw4w9WgXcQ", adapter.extractVideoId("https://youtu.be/dQw4w9WgXcQ"));
        assertEquals("kJQP7kiw5Fk", adapter.extractVideoId("https://www.youtube.com/shorts/kJQP7kiw5Fk"));
        assertEquals("jfKfPfyJRdk", adapter.extractVideoId("https://www.youtube.com/live/jfKfPfyJRdk"));
    }

    @Test
    public void testCanonicalizeWatchUrl() {
        String input = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", canonical);
    }

    @Test
    public void testCanonicalizeShortUrl() {
        String input = "https://youtu.be/dQw4w9WgXcQ?si=tracking123";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", canonical);
    }

    @Test
    public void testCanonicalizeShortsUrl() {
        String input = "https://www.youtube.com/shorts/kJQP7kiw5Fk?si=123";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.youtube.com/shorts/kJQP7kiw5Fk", canonical);
    }

    @Test
    public void testExtractStructuredFallback() throws Exception {
        String url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        ExtractedMetadata meta = adapter.extract(url);

        assertNotNull(meta);
        assertEquals("YouTube", meta.getPlatform());
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", meta.getCanonicalUrl());
        assertEquals("video", meta.getContentType());
        assertNotNull(meta.getTitle());
        assertNotNull(meta.getThumbnailUrl());
        assertTrue(meta.getThumbnailUrl().contains("dQw4w9WgXcQ"));
    }

    @Test
    public void testExtractShortsFallback() throws Exception {
        String url = "https://www.youtube.com/shorts/kJQP7kiw5Fk";
        ExtractedMetadata meta = adapter.extract(url);

        assertNotNull(meta);
        assertEquals("YouTube", meta.getPlatform());
        assertEquals("https://www.youtube.com/shorts/kJQP7kiw5Fk", meta.getCanonicalUrl());
        assertEquals("video", meta.getContentType());
        assertNotNull(meta.getTitle());
        assertTrue(meta.getThumbnailUrl().contains("kJQP7kiw5Fk"));
    }

    @Test
    public void testHealthCheck() {
        AdapterHealth health = adapter.checkHealth();
        assertNotNull(health);
        assertTrue(health.isHealthy());
        assertEquals(YouTubeAdapter.PLATFORM_ID, adapter.getPlatformId());
        assertEquals(90, adapter.getPriority());
    }

    @Test(expected = ExtractionException.class)
    public void testExtractInvalidUrlThrows() throws Exception {
        adapter.extract("https://example.com/test");
    }
}
