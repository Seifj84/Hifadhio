package com.seiftech.hifadhio.adapter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RedditAdapterTest {

    private RedditAdapter adapter;

    @Before
    public void setUp() {
        adapter = new RedditAdapter();
    }

    @Test
    public void testCanHandleRedditUrls() {
        assertTrue(adapter.canHandle("https://www.reddit.com/r/androiddev/comments/17xyz/hifadhio/"));
        assertTrue(adapter.canHandle("https://reddit.com/r/technology/comments/abc123/"));
        assertTrue(adapter.canHandle("https://redd.it/17xyz"));

        assertFalse(adapter.canHandle("https://x.com/status/123"));
        assertFalse(adapter.canHandle("https://youtube.com/watch?v=123"));
        assertFalse(adapter.canHandle(null));
        assertFalse(adapter.canHandle(""));
    }

    @Test
    public void testExtractSubreddit() {
        assertEquals("androiddev", adapter.extractSubreddit("https://www.reddit.com/r/androiddev/comments/17xyz/hifadhio/"));
        assertEquals("technology", adapter.extractSubreddit("https://reddit.com/r/technology/"));
    }

    @Test
    public void testCanonicalizePostUrl() {
        String input = "https://www.reddit.com/r/androiddev/comments/17xyz/hifadhio_offline_first/?utm_source=share&utm_medium=web2x";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.reddit.com/r/androiddev/comments/17xyz/hifadhio_offline_first/", canonical);
    }

    @Test
    public void testCanonicalizeShortUrl() {
        String input = "https://redd.it/17xyz";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://redd.it/17xyz", canonical);
    }

    @Test
    public void testExtractStructuredFallback() throws Exception {
        String url = "https://www.reddit.com/r/androiddev/comments/17xyz/hifadhio/";
        ExtractedMetadata meta = adapter.extract(url);

        assertNotNull(meta);
        assertEquals("Reddit", meta.getPlatform());
        assertEquals("https://www.reddit.com/r/androiddev/comments/17xyz/hifadhio/", meta.getCanonicalUrl());
        assertEquals("post", meta.getContentType());
        assertEquals("r/androiddev", meta.getCreatorHandle());
        assertTrue(meta.getTitle().contains("androiddev") || meta.getTitle().contains("Reddit"));
    }

    @Test
    public void testHealthCheck() {
        AdapterHealth health = adapter.checkHealth();
        assertNotNull(health);
        assertTrue(health.isHealthy());
        assertEquals(RedditAdapter.PLATFORM_ID, adapter.getPlatformId());
        assertEquals(75, adapter.getPriority());
    }

    @Test(expected = ExtractionException.class)
    public void testExtractInvalidUrlThrows() throws Exception {
        adapter.extract("https://example.com/not-reddit");
    }
}
