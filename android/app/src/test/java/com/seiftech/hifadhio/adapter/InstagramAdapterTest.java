package com.seiftech.hifadhio.adapter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InstagramAdapterTest {

    private InstagramAdapter adapter;

    @Before
    public void setUp() {
        adapter = new InstagramAdapter();
    }

    @Test
    public void testCanHandleInstagramUrls() {
        assertTrue(adapter.canHandle("https://www.instagram.com/reel/C3zY123abcD/"));
        assertTrue(adapter.canHandle("https://instagram.com/p/C9x8765uvwX/"));
        assertTrue(adapter.canHandle("https://instagr.am/p/xyz123"));
        assertTrue(adapter.canHandle("https://www.instagram.com/share/reel/C3zY123abcD/"));

        assertFalse(adapter.canHandle("https://tiktok.com/@user/video/123"));
        assertFalse(adapter.canHandle("https://youtube.com/watch?v=123"));
        assertFalse(adapter.canHandle(null));
        assertFalse(adapter.canHandle(""));
    }

    @Test
    public void testExtractShortcode() {
        assertEquals("C3zY123abcD", adapter.extractShortcode("https://www.instagram.com/reel/C3zY123abcD/"));
        assertEquals("C9x8765uvwX", adapter.extractShortcode("https://www.instagram.com/p/C9x8765uvwX/"));
        assertEquals("B5mno987xyz", adapter.extractShortcode("https://www.instagram.com/tv/B5mno987xyz/"));
    }

    @Test
    public void testCanonicalizeReelUrl() {
        String input = "https://www.instagram.com/share/reel/C3zY123abcD/?igshid=MzRlODBiNWFlZA==";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.instagram.com/reel/C3zY123abcD/", canonical);
    }

    @Test
    public void testCanonicalizePostUrl() {
        String input = "https://www.instagram.com/p/C9x8765uvwX/?utm_source=ig_web_copy_link";
        String canonical = adapter.canonicalize(input);
        assertEquals("https://www.instagram.com/p/C9x8765uvwX/", canonical);
    }

    @Test
    public void testExtractReelFallbackMetadata() throws Exception {
        String url = "https://www.instagram.com/reel/C3zY123abcD/";
        ExtractedMetadata meta = adapter.extract(url);

        assertNotNull(meta);
        assertEquals("Instagram", meta.getPlatform());
        assertEquals("https://www.instagram.com/reel/C3zY123abcD/", meta.getCanonicalUrl());
        assertEquals("video", meta.getContentType());
        assertEquals("Instagram Reel", meta.getTitle());
    }

    @Test
    public void testExtractPostFallbackMetadata() throws Exception {
        String url = "https://www.instagram.com/p/C9x8765uvwX/";
        ExtractedMetadata meta = adapter.extract(url);

        assertNotNull(meta);
        assertEquals("Instagram", meta.getPlatform());
        assertEquals("https://www.instagram.com/p/C9x8765uvwX/", meta.getCanonicalUrl());
        assertEquals("image", meta.getContentType());
        assertEquals("Instagram Post", meta.getTitle());
    }

    @Test
    public void testHealthCheck() {
        AdapterHealth health = adapter.checkHealth();
        assertNotNull(health);
        assertTrue(health.isHealthy());
        assertEquals(InstagramAdapter.PLATFORM_ID, adapter.getPlatformId());
        assertEquals(80, adapter.getPriority());
    }

    @Test(expected = ExtractionException.class)
    public void testExtractInvalidUrlThrows() throws Exception {
        adapter.extract("https://example.com/not-instagram");
    }
}
