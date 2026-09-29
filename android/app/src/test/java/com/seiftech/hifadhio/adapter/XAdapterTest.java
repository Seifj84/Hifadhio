package com.seiftech.hifadhio.adapter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class XAdapterTest {

    private XAdapter adapter;

    @Before
    public void setUp() {
        adapter = new XAdapter();
    }

    @Test
    public void testCanHandleXUrls() {
        assertTrue(adapter.canHandle("https://x.com/AndroidDev/status/1765432109876543210"));
        assertTrue(adapter.canHandle("https://twitter.com/SeifTech/status/1834567890123456789"));
        assertTrue(adapter.canHandle("https://x.com/user/status/123456"));

        assertFalse(adapter.canHandle("https://reddit.com/r/technology"));
        assertFalse(adapter.canHandle("https://facebook.com/reel/123"));
        assertFalse(adapter.canHandle(null));
        assertFalse(adapter.canHandle(""));
    }

    @Test
    public void testExtractUsernameAndStatusId() {
        String url = "https://x.com/AndroidDev/status/1765432109876543210";
        assertEquals("AndroidDev", adapter.extractUsername(url));
        assertEquals("1765432109876543210", adapter.extractStatusId(url));
    }

    @Test
    public void testCanonicalizeStatusUrl() {
        String input = "https://x.com/AndroidDev/status/1765432109876543210?s=20&t=abc";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://x.com/AndroidDev/status/1765432109876543210", canonical);
    }

    @Test
    public void testCanonicalizeTwitterToX() {
        String input = "https://twitter.com/SeifTech/status/1834567890123456789";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://x.com/SeifTech/status/1834567890123456789", canonical);
    }

    @Test
    public void testExtractStructuredFallback() throws Exception {
        String url = "https://x.com/AndroidDev/status/1765432109876543210";
        ExtractedMetadata meta = adapter.extract(url);

        assertNotNull(meta);
        assertEquals("X", meta.getPlatform());
        assertEquals("https://x.com/AndroidDev/status/1765432109876543210", meta.getCanonicalUrl());
        assertEquals("post", meta.getContentType());
        assertEquals("@AndroidDev", meta.getCreatorHandle());
        assertTrue(meta.getTitle().contains("AndroidDev") || meta.getTitle().contains("X"));
    }

    @Test
    public void testHealthCheck() {
        AdapterHealth health = adapter.checkHealth();
        assertNotNull(health);
        assertTrue(health.isHealthy());
        assertEquals(XAdapter.PLATFORM_ID, adapter.getPlatformId());
        assertEquals(75, adapter.getPriority());
    }

    @Test(expected = ExtractionException.class)
    public void testExtractInvalidUrlThrows() throws Exception {
        adapter.extract("https://example.com/not-x");
    }
}
