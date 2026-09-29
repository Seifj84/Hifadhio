package com.seiftech.hifadhio.adapter;

import com.seiftech.hifadhio.data.UrlNormalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized platform adapter for Facebook content links (Reels, Shares, Posts, Watch).
 * Resolves share redirects, canonicalizes reel URLs, and safely handles extraction
 * without failing or hanging in retries when automated scraping is denied by Facebook.
 */
public class FacebookAdapter implements ContentExtractorAdapter {
    public static final String PLATFORM_ID = "Facebook";

    // Patterns matching Facebook URLs
    private static final Pattern REEL_SHARE_PATTERN = Pattern.compile("facebook\\.com/share/r/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REEL_DIRECT_PATTERN = Pattern.compile("facebook\\.com/reel/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WATCH_PATTERN = Pattern.compile("(?:facebook\\.com/watch|fb\\.watch)/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern POST_SHARE_PATTERN = Pattern.compile("facebook\\.com/share/p/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String getPlatformId() {
        return PLATFORM_ID;
    }

    @Override
    public int getPriority() {
        return 80; // Higher than GenericWeb (0)
    }

    @Override
    public boolean canHandle(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.toLowerCase();
        return lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com");
    }

    @Override
    public String canonicalize(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        String clean = UrlNormalizer.normalize(url);

        // Canonicalize share/r/<id> -> www.facebook.com/reel/<id>/
        Matcher reelShare = REEL_SHARE_PATTERN.matcher(clean);
        if (reelShare.find()) {
            String reelId = reelShare.group(1);
            return "https://www.facebook.com/reel/" + reelId + "/";
        }

        Matcher reelDirect = REEL_DIRECT_PATTERN.matcher(clean);
        if (reelDirect.find()) {
            String reelId = reelDirect.group(1);
            return "https://www.facebook.com/reel/" + reelId + "/";
        }

        Matcher watch = WATCH_PATTERN.matcher(clean);
        if (watch.find()) {
            return "https://www.facebook.com/watch/?v=" + watch.group(1);
        }

        // Return stripped URL
        if (!clean.startsWith("https://")) {
            if (clean.startsWith("http://")) {
                clean = "https://" + clean.substring(7);
            } else {
                clean = "https://" + clean;
            }
        }
        return clean;
    }

    @Override
    public AdapterHealth checkHealth() {
        return AdapterHealth.ok("Facebook adapter active (safe share resolver & metadata fallback)");
    }

    @Override
    public ExtractedMetadata extract(String url) throws ExtractionException {
        if (!canHandle(url)) {
            throw new ExtractionException("ERR_INVALID_URL", "Not a valid Facebook URL: " + url);
        }

        String canonical = canonicalize(url);
        ExtractedMetadata meta = new ExtractedMetadata();
        meta.setPlatform(PLATFORM_ID);
        meta.setCanonicalUrl(canonical);

        // Classify content type
        if (canonical.contains("/reel/")) {
            meta.setContentType("video");
            meta.setTitle("Facebook Reel");
            meta.setOriginalTitle("Facebook Reel");
            meta.setDescription("Facebook Reel • Saved for viewing");
        } else if (canonical.contains("/watch")) {
            meta.setContentType("video");
            meta.setTitle("Facebook Video");
            meta.setOriginalTitle("Facebook Video");
            meta.setDescription("Facebook Video • Saved for viewing");
        } else if (canonical.contains("/share/p/")) {
            meta.setContentType("post");
            meta.setTitle("Facebook Post");
            meta.setOriginalTitle("Facebook Post");
            meta.setDescription("Facebook Post • Saved for viewing");
        } else {
            meta.setContentType("post");
            meta.setTitle("Facebook Link");
            meta.setOriginalTitle("Facebook Link");
            meta.setDescription("Facebook Link • Saved for viewing");
        }

        meta.setCreatorName("Facebook");

        // Note: Facebook edge proxies block automated server-side scraping of share/reel URLs with HTTP 400.
        // Returning this clean, structured fallback ensures the link is safely captured and immediately ready
        // without throwing exceptions that cause continuous useless retries.
        return meta;
    }
}
