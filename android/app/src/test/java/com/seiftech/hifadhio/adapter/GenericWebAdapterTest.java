package com.seiftech.hifadhio.adapter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GenericWebAdapterTest {

    private GenericWebAdapter adapter;

    @Before
    public void setUp() {
        adapter = new GenericWebAdapter();
    }

    @Test
    public void testOpenGraphFullExtraction() {
        String html = "<!DOCTYPE html><html><head>"
                + "<meta property=\"og:title\" content=\"Understanding Quantum Computing\" />"
                + "<meta property=\"og:description\" content=\"A deep dive into qubits and superposition.\" />"
                + "<meta property=\"og:image\" content=\"https://cdn.example.com/images/quantum.jpg\" />"
                + "<meta property=\"og:site_name\" content=\"Tech Insights\" />"
                + "<meta property=\"og:type\" content=\"article\" />"
                + "<meta property=\"og:url\" content=\"https://example.com/quantum-computing\" />"
                + "<link rel=\"canonical\" href=\"https://example.com/quantum-computing\" />"
                + "<title>Old HTML Title</title>"
                + "</head><body><h1>Content</h1></body></html>";

        ExtractedMetadata meta = adapter.extractFromHtml(html, "https://example.com/quantum-computing?utm_source=twitter");

        assertNotNull(meta);
        assertEquals("Understanding Quantum Computing", meta.getTitle());
        assertEquals("Understanding Quantum Computing", meta.getOriginalTitle());
        assertEquals("A deep dive into qubits and superposition.", meta.getDescription());
        assertEquals("https://cdn.example.com/images/quantum.jpg", meta.getThumbnailUrl());
        assertEquals("Tech Insights", meta.getCreatorName());
        assertEquals("article", meta.getContentType());
        assertEquals("https://example.com/quantum-computing", meta.getCanonicalUrl());
    }

    @Test
    public void testAttributeOrderingAndSingleQuotes() {
        // Test HTML where 'content' comes before 'property' and single quotes are used
        String html = "<html><head>"
                + "<meta content='Reversed Attribute Title' property='og:title'>"
                + "<meta content='Single quoted reversed description' property='og:description'>"
                + "<meta content='https://example.com/cover.png' property='og:image'>"
                + "</head><body></body></html>";

        ExtractedMetadata meta = adapter.extractFromHtml(html, "https://example.com/post/1");

        assertNotNull(meta);
        assertEquals("Reversed Attribute Title", meta.getTitle());
        assertEquals("Single quoted reversed description", meta.getDescription());
        assertEquals("https://example.com/cover.png", meta.getThumbnailUrl());
    }

    @Test
    public void testHtmlFallbacksWhenOpenGraphMissing() {
        // Standard HTML page without OpenGraph tags
        String html = "<html><head>"
                + "<title>Classic Blog Post &amp; News</title>"
                + "<meta name=\"description\" content=\"Standard meta description of the article.\" />"
                + "<meta name=\"author\" content=\"Jane Doe\" />"
                + "<link rel=\"canonical\" href=\"https://blog.example.com/standard-post\" />"
                + "</head><body></body></html>";

        ExtractedMetadata meta = adapter.extractFromHtml(html, "https://blog.example.com/standard-post?fbclid=12345");

        assertNotNull(meta);
        assertEquals("Classic Blog Post & News", meta.getTitle());
        assertEquals("Standard meta description of the article.", meta.getDescription());
        assertEquals("Jane Doe", meta.getCreatorName());
        assertEquals("https://blog.example.com/standard-post", meta.getCanonicalUrl());
    }

    @Test
    public void testHtmlEntityDecoding() {
        String html = "<html><head>"
                + "<meta property=\"og:title\" content=\"Tom &amp; Jerry&#39;s &quot;Epic&quot; Adventure &mdash; 2026\" />"
                + "<meta property=\"og:description\" content=\"An exciting tale &hellip; save &lt;100%&gt; today!\" />"
                + "</head><body></body></html>";

        ExtractedMetadata meta = adapter.extractFromHtml(html, "https://example.com/movie");

        assertNotNull(meta);
        assertEquals("Tom & Jerry's \"Epic\" Adventure — 2026", meta.getTitle());
        assertEquals("An exciting tale … save <100%> today!", meta.getDescription());
    }

    @Test
    public void testRelativeUrlResolution() {
        String html = "<html><head>"
                + "<meta property=\"og:title\" content=\"Relative Asset Test\" />"
                + "<meta property=\"og:image\" content=\"/assets/images/thumb.jpg\" />"
                + "<link rel=\"canonical\" href=\"/articles/relative-path\" />"
                + "</head><body></body></html>";

        ExtractedMetadata meta = adapter.extractFromHtml(html, "https://developer.mozilla.org/en-US/docs/Web");

        assertNotNull(meta);
        assertEquals("https://developer.mozilla.org/assets/images/thumb.jpg", meta.getThumbnailUrl());
        assertEquals("https://developer.mozilla.org/articles/relative-path", meta.getCanonicalUrl());
    }

    @Test
    public void testCanHandleAndCanonicalize() {
        assertTrue(adapter.canHandle("https://google.com"));
        assertTrue(adapter.canHandle("http://sub.domain.co.uk/page"));
        assertFalse(adapter.canHandle("ftp://files.example.com"));
        assertFalse(adapter.canHandle(""));
        assertFalse(adapter.canHandle(null));

        String raw = "https://example.com/page?utm_source=fb&utm_medium=social&igshid=abcdef";
        assertEquals("https://example.com/page", adapter.canonicalize(raw));
    }

    @Test
    public void testUpgradeHttpToHttps() {
        assertEquals("https://www.fao.org/home/en", GenericWebAdapter.upgradeHttpToHttps("http://www.fao.org/home/en"));
        assertEquals("https://example.com", GenericWebAdapter.upgradeHttpToHttps("https://example.com"));
        assertEquals("https://insecure.site/path?arg=1", GenericWebAdapter.upgradeHttpToHttps("http://insecure.site/path?arg=1"));
        assertNull(GenericWebAdapter.upgradeHttpToHttps(null));
    }

    @Test
    public void testExtractDomain() {
        assertEquals("fao.org", GenericWebAdapter.extractDomain("https://www.fao.org/home/en"));
        assertEquals("fao.org", GenericWebAdapter.extractDomain("http://fao.org"));
        assertEquals("news.ycombinator.com", GenericWebAdapter.extractDomain("https://news.ycombinator.com/item?id=123"));
        assertEquals("Web", GenericWebAdapter.extractDomain(null));
    }

    @Test
    public void testFallbackMetadataOnClientError() {
        String testUrl = "https://www.fao.org/home/en";
        ExtractedMetadata fallback = adapter.createFallbackMetadata(testUrl, "Cleartext HTTP traffic not permitted");

        assertNotNull(fallback);
        assertEquals("fao.org Page", fallback.getTitle());
        assertEquals("fao.org Page", fallback.getOriginalTitle());
        assertEquals(testUrl, fallback.getCanonicalUrl());
        assertEquals("Web", fallback.getPlatform());
        assertTrue(fallback.getDescription().contains("Saved from fao.org"));
    }
}
