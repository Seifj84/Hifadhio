package com.seiftech.hifadhio.adapter;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AdapterRegistryTest {

    private ContentAdapterRegistry registry;

    @Before
    public void setUp() {
        registry = ContentAdapterRegistry.getInstance();
        registry.reset();
    }

    @Test
    public void testDefaultRegistryContainsAllPlatformAdaptersInPriorityOrder() {
        List<ContentExtractorAdapter> adapters = registry.getRegisteredAdapters();
        assertEquals(7, adapters.size());

        // Check descending priority order
        for (int i = 0; i < adapters.size() - 1; i++) {
            assertTrue(adapters.get(i).getPriority() >= adapters.get(i + 1).getPriority());
        }

        assertEquals("YouTube", adapters.get(0).getPlatformId());
        assertEquals("TikTok", adapters.get(1).getPlatformId());
        assertEquals(GenericWebAdapter.PLATFORM_ID, adapters.get(6).getPlatformId());
    }

    @Test
    public void testRoutingToSpecializedPlatformAdapters() {
        // YouTube routing
        ContentExtractorAdapter yt = registry.getAdapterForUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertNotNull(yt);
        assertEquals("YouTube", yt.getPlatformId());

        // TikTok routing
        ContentExtractorAdapter tt = registry.getAdapterForUrl("https://www.tiktok.com/@user/video/7106594312292453678");
        assertNotNull(tt);
        assertEquals("TikTok", tt.getPlatformId());

        // Instagram routing
        ContentExtractorAdapter ig = registry.getAdapterForUrl("https://www.instagram.com/reel/C3zY123abcD/");
        assertNotNull(ig);
        assertEquals("Instagram", ig.getPlatformId());

        // Facebook routing
        ContentExtractorAdapter fb = registry.getAdapterForUrl("https://facebook.com/share/r/1Qbbym5DGx");
        assertNotNull(fb);
        assertEquals("Facebook", fb.getPlatformId());

        // Reddit routing
        ContentExtractorAdapter reddit = registry.getAdapterForUrl("https://www.reddit.com/r/androiddev/comments/17xyz/post/");
        assertNotNull(reddit);
        assertEquals("Reddit", reddit.getPlatformId());

        // X / Twitter routing
        ContentExtractorAdapter x = registry.getAdapterForUrl("https://x.com/AndroidDev/status/1765432109876543210");
        assertNotNull(x);
        assertEquals("X", x.getPlatformId());

        // Unknown website routes to GenericWebAdapter
        ContentExtractorAdapter generic = registry.getAdapterForUrl("https://www.bbc.com/news/world-12345");
        assertNotNull(generic);
        assertEquals(GenericWebAdapter.PLATFORM_ID, generic.getPlatformId());
    }

    @Test
    public void testFallbackToGenericWebAdapterForUnknownUrl() {
        ContentExtractorAdapter adapter = registry.getAdapterForUrl("https://example.com/some/article");
        assertNotNull(adapter);
        assertEquals(GenericWebAdapter.PLATFORM_ID, adapter.getPlatformId());
    }

    @Test
    public void testUnregisterAdapter() {
        ContentExtractorAdapter testAdapter = new ContentExtractorAdapter() {
            @Override
            public String getPlatformId() { return "CustomTestAdapter"; }
            @Override
            public int getPriority() { return 95; }
            @Override
            public boolean canHandle(String url) { return url.contains("customtest.com"); }
            @Override
            public ExtractedMetadata extract(String url) { return new ExtractedMetadata(); }
            @Override
            public String canonicalize(String url) { return url; }
            @Override
            public AdapterHealth checkHealth() { return AdapterHealth.ok("OK"); }
        };

        registry.registerAdapter(testAdapter);
        assertNotNull(registry.getAdapterById("CustomTestAdapter"));

        boolean removed = registry.unregisterAdapter("CustomTestAdapter");
        assertTrue(removed);
        assertNull(registry.getAdapterById("CustomTestAdapter"));
    }

    @Test
    public void testHealthCheckOnAllAdapters() {
        Map<String, AdapterHealth> healthMap = registry.checkAllHealth();
        assertNotNull(healthMap);
        assertEquals(7, healthMap.size());
        assertTrue(healthMap.containsKey("YouTube"));
        assertTrue(healthMap.containsKey("TikTok"));
        assertTrue(healthMap.containsKey("Instagram"));
        assertTrue(healthMap.containsKey("Facebook"));
        assertTrue(healthMap.containsKey("Reddit"));
        assertTrue(healthMap.containsKey("X"));
        assertTrue(healthMap.containsKey(GenericWebAdapter.PLATFORM_ID));

        for (AdapterHealth h : healthMap.values()) {
            assertTrue(h.isHealthy());
        }
    }
}
