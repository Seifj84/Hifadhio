package com.seiftech.hifadhio.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class ContentItemFallbackTest {

    @Test
    public void testFacebookLinkFallbackHierarchy() {
        ContentItem item = new ContentItem();
        item.setPlatform("Facebook");

        // 1. If title is provided, use it
        item.setTitle("My Facebook Post Title");
        assertEquals("My Facebook Post Title", item.getDisplayTitle());

        // 2. If title is empty, check fallback for Reel
        item.setTitle("");
        item.setUrl("https://www.facebook.com/reel/1234567890/?s=share");
        assertEquals("Facebook Reel", item.getDisplayTitle());
        assertEquals("facebook.com", item.getDomainPreview());

        // 3. Fallback for Video
        item.setUrl("https://www.facebook.com/watch/?v=987654321");
        assertEquals("Facebook video", item.getDisplayTitle());

        // 4. Fallback for generic post
        item.setUrl("https://www.facebook.com/user/posts/11223344");
        assertEquals("Facebook post", item.getDisplayTitle());

        // 5. Generic Facebook link
        item.setUrl("https://facebook.com/share/abcxyz123/?mibextid=wwXIfr");
        assertEquals("Facebook link", item.getDisplayTitle());
        assertEquals("facebook.com", item.getDomainPreview());
    }

    @Test
    public void testOtherPlatformsFallbackHierarchy() {
        ContentItem yt = new ContentItem();
        yt.setPlatform("YouTube");
        yt.setUrl("https://youtube.com/shorts/abcd1234efg?feature=share");
        assertEquals("YouTube Short", yt.getDisplayTitle());
        assertEquals("youtube.com", yt.getDomainPreview());

        ContentItem ig = new ContentItem();
        ig.setPlatform("Instagram");
        ig.setUrl("https://www.instagram.com/reel/DCb1234/?igshid=abc");
        assertEquals("Instagram Reel", ig.getDisplayTitle());
        assertEquals("instagram.com", ig.getDomainPreview());

        ContentItem tt = new ContentItem();
        tt.setPlatform("TikTok");
        tt.setUrl("https://www.tiktok.com/@user/video/71234567890");
        assertEquals("TikTok video", tt.getDisplayTitle());
        assertEquals("tiktok.com", tt.getDomainPreview());

        ContentItem blog = new ContentItem();
        blog.setPlatform("Web");
        blog.setUrl("https://blog.google/technology/ai/new-gemini-features/");
        assertEquals("blog.google link", blog.getDisplayTitle());
        assertEquals("blog.google", blog.getDomainPreview());
    }

    @Test
    public void testDomainPreviewWithComplexUrls() {
        ContentItem item = new ContentItem();
        item.setUrl("https://subdomain.example.com:8080/path/to/page?utm_source=foo&bar=baz#frag");
        assertEquals("subdomain.example.com", item.getDomainPreview());

        ContentItem item2 = new ContentItem();
        item2.setUrl("http://www.nytimes.com/2026/09/28/technology/ai.html");
        assertEquals("nytimes.com", item2.getDomainPreview());
    }
}
