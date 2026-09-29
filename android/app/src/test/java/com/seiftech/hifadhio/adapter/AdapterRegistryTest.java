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
    public void testDefaultRegistryContainsFacebookAndGenericWebAdapters() {
        List<ContentExtractorAdapter> adapters = registry.getRegisteredAdapters();
        assertEquals(2, adapters.size());
        assertEquals("Facebook", adapters.get(0).getPlatformId());
        assertEquals(GenericWebAdapter.PLATFORM_ID, adapters.get(1).getPlatformId());
    }

    @Test
    public void testRoutingFacebookUrlsToFacebookAdapter() {
        ContentExtractorAdapter adapter1 = registry.getAdapterForUrl("https://facebook.com/share/r/1Qbbym5DGx");
        assertNotNull(adapter1);
        assertEquals("Facebook", adapter1.getPlatformId());

        ContentExtractorAdapter adapter2 = registry.getAdapterForUrl("https://fb.watch/xyz123/");
        assertNotNull(adapter2);
        assertEquals("Facebook", adapter2.getPlatformId());
    }

    @Test
    public void testFallbackToGenericWebAdapterForUnknownUrl() {
        ContentExtractorAdapter adapter = registry.getAdapterForUrl("https://example.com/some/article");
        assertNotNull(adapter);
        assertEquals(GenericWebAdapter.PLATFORM_ID, adapter.getPlatformId());
    }

    @Test
    public void testModularPlatformAdapterRegistrationWithoutCoreChanges() {
        // Mock a specialized adapter (Phase 5 exit criterion)
        ContentExtractorAdapter mockYouTubeAdapter = new ContentExtractorAdapter() {
            @Override
            public String getPlatformId() {
                return "YouTube";
            }

            @Override
            public int getPriority() {
                return 100; // Higher than Facebook (80) and GenericWeb (0)
            }

            @Override
            public boolean canHandle(String url) {
                return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
            }

            @Override
            public ExtractedMetadata extract(String url) {
                ExtractedMetadata meta = new ExtractedMetadata();
                meta.setTitle("YouTube Mock Video");
                meta.setPlatform("YouTube");
                return meta;
            }

            @Override
            public String canonicalize(String url) {
                return url;
            }

            @Override
            public AdapterHealth checkHealth() {
                return AdapterHealth.ok("YouTube adapter online");
            }
        };

        // Register the new adapter dynamically
        registry.registerAdapter(mockYouTubeAdapter);

        // Verify it was added and priority ordering places it first
        List<ContentExtractorAdapter> registered = registry.getRegisteredAdapters();
        assertEquals(3, registered.size());
        assertEquals("YouTube", registered.get(0).getPlatformId());
        assertEquals("Facebook", registered.get(1).getPlatformId());
        assertEquals(GenericWebAdapter.PLATFORM_ID, registered.get(2).getPlatformId());

        // YouTube URLs should route to mock adapter
        ContentExtractorAdapter resolved = registry.getAdapterForUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertEquals("YouTube", resolved.getPlatformId());

        // Facebook URLs should route to Facebook adapter
        ContentExtractorAdapter fbResolved = registry.getAdapterForUrl("https://facebook.com/share/r/12345");
        assertEquals("Facebook", fbResolved.getPlatformId());

        // Generic URLs still route to GenericWebAdapter
        ContentExtractorAdapter genericResolved = registry.getAdapterForUrl("https://techcrunch.com/article");
        assertEquals(GenericWebAdapter.PLATFORM_ID, genericResolved.getPlatformId());
    }

    @Test
    public void testUnregisterAdapter() {
        ContentExtractorAdapter testAdapter = new ContentExtractorAdapter() {
            @Override
            public String getPlatformId() { return "TestAdapter"; }
            @Override
            public int getPriority() { return 50; }
            @Override
            public boolean canHandle(String url) { return url.contains("test.com"); }
            @Override
            public ExtractedMetadata extract(String url) { return new ExtractedMetadata(); }
            @Override
            public String canonicalize(String url) { return url; }
            @Override
            public AdapterHealth checkHealth() { return AdapterHealth.ok("OK"); }
        };

        registry.registerAdapter(testAdapter);
        assertNotNull(registry.getAdapterById("TestAdapter"));

        boolean removed = registry.unregisterAdapter("TestAdapter");
        assertTrue(removed);
        assertNull(registry.getAdapterById("TestAdapter"));
    }

    @Test
    public void testHealthCheckOnAllAdapters() {
        Map<String, AdapterHealth> healthMap = registry.checkAllHealth();
        assertNotNull(healthMap);
        assertTrue(healthMap.containsKey(GenericWebAdapter.PLATFORM_ID));
        assertTrue(healthMap.get(GenericWebAdapter.PLATFORM_ID).isHealthy());
    }
}
