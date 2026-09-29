package com.seiftech.hifadhio.adapter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FacebookAdapterTest {

    private FacebookAdapter adapter;

    @Before
    public void setUp() {
        adapter = new FacebookAdapter();
    }

    @Test
    public void testCanHandleFacebookUrls() {
        assertTrue(adapter.canHandle("https://facebook.com/share/r/1Qbbym5DGx"));
        assertTrue(adapter.canHandle("https://www.facebook.com/share/r/1GwGTBKiUP/?mibextid=wwXIfr"));
        assertTrue(adapter.canHandle("https://facebook.com/reel/123456789"));
        assertTrue(adapter.canHandle("https://fb.watch/12345/"));
        assertTrue(adapter.canHandle("https://facebook.com/share/p/987654"));
        assertTrue(adapter.canHandle("http://fb.com/xyz"));

        assertFalse(adapter.canHandle("https://example.com/article"));
        assertFalse(adapter.canHandle("https://twitter.com/post/123"));
        assertFalse(adapter.canHandle(null));
        assertFalse(adapter.canHandle(""));
    }

    @Test
    public void testCanonicalizeReelShareUrl() {
        String input = "https://facebook.com/share/r/1Qbbym5DGx";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.facebook.com/reel/1Qbbym5DGx/", canonical);
    }

    @Test
    public void testCanonicalizeReelWithTrackingParams() {
        String input = "https://www.facebook.com/share/r/1GwGTBKiUP/?mibextid=wwXIfr";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.facebook.com/reel/1GwGTBKiUP/", canonical);
    }

    @Test
    public void testCanonicalizeWatchUrl() {
        String input = "https://fb.watch/xyz987/";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.facebook.com/watch/?v=xyz987", canonical);
    }

    @Test
    public void testExtractionGracefulFallback() throws Exception {
        String url = "https://facebook.com/share/r/1Qbbym5DGx";
        ExtractedMetadata meta = adapter.extract(url);

        assertNotNull(meta);
        assertEquals("Facebook", meta.getPlatform());
        assertEquals("Facebook Reel", meta.getTitle());
        assertEquals("https://www.facebook.com/reel/1Qbbym5DGx/", meta.getCanonicalUrl());
        assertEquals("video", meta.getContentType());
        assertTrue(meta.getDescription().contains("Facebook Reel"));
    }

    @Test
    public void testHealthCheck() {
        AdapterHealth health = adapter.checkHealth();
        assertNotNull(health);
        assertTrue(health.isHealthy());
        assertEquals(FacebookAdapter.PLATFORM_ID, adapter.getPlatformId());
        assertEquals(80, adapter.getPriority());
    }

    @Test(expected = ExtractionException.class)
    public void testExtractInvalidUrlThrows() throws Exception {
        adapter.extract("https://google.com/search");
    }
}
