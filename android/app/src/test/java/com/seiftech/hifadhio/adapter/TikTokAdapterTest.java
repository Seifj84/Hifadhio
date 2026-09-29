package com.seiftech.hifadhio.adapter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TikTokAdapterTest {

    private TikTokAdapter adapter;

    @Before
    public void setUp() {
        adapter = new TikTokAdapter();
    }

    @Test
    public void testCanHandleTikTokUrls() {
        assertTrue(adapter.canHandle("https://www.tiktok.com/@tiktok/video/7106594312292453678"));
        assertTrue(adapter.canHandle("https://tiktok.com/@user.name/video/1234567890123456789"));
        assertTrue(adapter.canHandle("https://vt.tiktok.com/ZS234567/"));
        assertTrue(adapter.canHandle("https://vm.tiktok.com/ZM876543/"));

        assertFalse(adapter.canHandle("https://youtube.com/watch?v=123"));
        assertFalse(adapter.canHandle("https://instagram.com/reel/123"));
        assertFalse(adapter.canHandle(null));
        assertFalse(adapter.canHandle(""));
    }

    @Test
    public void testExtractUsernameAndVideoId() {
        String url = "https://www.tiktok.com/@creative_user/video/7106594312292453678";
        assertEquals("creative_user", adapter.extractUsername(url));
        assertEquals("7106594312292453678", adapter.extractVideoId(url));
    }

    @Test
    public void testCanonicalizeDirectVideoUrl() {
        String url = "https://www.tiktok.com/@creative_user/video/7106594312292453678?is_from_webapp=1&sender_device=pc";
        String canonical = adapter.canonicalize(url);
        assertEquals("https://www.tiktok.com/@creative_user/video/7106594312292453678", canonical);
    }

    @Test
    public void testCanonicalizeShortLinks() {
        assertEquals("https://vt.tiktok.com/ZS234567", adapter.canonicalize("https://vt.tiktok.com/ZS234567/"));
        assertEquals("https://vm.tiktok.com/ZM876543", adapter.canonicalize("https://vm.tiktok.com/ZM876543/"));
    }

    @Test
    public void testExtractStructuredFallback() throws Exception {
        String url = "https://www.tiktok.com/@chef_sarah/video/7106594312292453678";
        ExtractedMetadata meta = adapter.extract(url);

        assertNotNull(meta);
        assertEquals("TikTok", meta.getPlatform());
        assertEquals("https://www.tiktok.com/@chef_sarah/video/7106594312292453678", meta.getCanonicalUrl());
        assertEquals("video", meta.getContentType());
        assertEquals("@chef_sarah", meta.getCreatorHandle());
        assertTrue(meta.getTitle().contains("chef_sarah") || meta.getTitle().contains("TikTok"));
    }

    @Test
    public void testHealthCheck() {
        AdapterHealth health = adapter.checkHealth();
        assertNotNull(health);
        assertTrue(health.isHealthy());
        assertEquals(TikTokAdapter.PLATFORM_ID, adapter.getPlatformId());
        assertEquals(85, adapter.getPriority());
    }

    @Test(expected = ExtractionException.class)
    public void testExtractInvalidUrlThrows() throws Exception {
        adapter.extract("https://example.com/not-tiktok");
    }
}
