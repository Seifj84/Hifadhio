package com.seiftech.hifadhio.adapter;

import com.seiftech.hifadhio.data.UrlNormalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized platform adapter for Instagram content (Reels, Posts, IGTV).
 * Canonicalizes Instagram reel and post identifiers, strips tracking parameters,
 * and extracts metadata with bot-block fallback protection.
 */
public class InstagramAdapter implements ContentExtractorAdapter {
    public static final String PLATFORM_ID = "Instagram";

    private static final Pattern REEL_PATTERN =
            Pattern.compile("instagram\\.com/(?:reel|reels|share/reel|share/r)/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern POST_PATTERN =
            Pattern.compile("instagram\\.com/(?:p|share/p)/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TV_PATTERN =
            Pattern.compile("instagram\\.com/tv/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);

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
        return lower.contains("instagram.com") || lower.contains("instagr.am");
    }

    public String extractShortcode(String url) {
        if (url == null) return null;
        Matcher reelMatcher = REEL_PATTERN.matcher(url);
        if (reelMatcher.find()) return reelMatcher.group(1);

        Matcher postMatcher = POST_PATTERN.matcher(url);
        if (postMatcher.find()) return postMatcher.group(1);

        Matcher tvMatcher = TV_PATTERN.matcher(url);
        if (tvMatcher.find()) return tvMatcher.group(1);

        return null;
    }

    @Override
    public String canonicalize(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        String clean = UrlNormalizer.normalize(url);

        Matcher reelMatcher = REEL_PATTERN.matcher(clean);
        if (reelMatcher.find()) {
            return "https://www.instagram.com/reel/" + reelMatcher.group(1) + "/";
        }

        Matcher postMatcher = POST_PATTERN.matcher(clean);
        if (postMatcher.find()) {
            return "https://www.instagram.com/p/" + postMatcher.group(1) + "/";
        }

        Matcher tvMatcher = TV_PATTERN.matcher(clean);
        if (tvMatcher.find()) {
            return "https://www.instagram.com/tv/" + tvMatcher.group(1) + "/";
        }

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
        return AdapterHealth.ok("Instagram adapter active (Reel/Post canonicalizer & metadata fallback ready)");
    }

    @Override
    public ExtractedMetadata extract(String url) throws ExtractionException {
        if (!canHandle(url)) {
            throw new ExtractionException("ERR_INVALID_URL", "Not a valid Instagram URL: " + url);
        }

        String canonical = canonicalize(url);
        ExtractedMetadata meta = new ExtractedMetadata();
        meta.setPlatform(PLATFORM_ID);
        meta.setCanonicalUrl(canonical);

        // Classify content type and set structured fallback
        if (canonical.contains("/reel/")) {
            meta.setContentType("video");
            meta.setTitle("Instagram Reel");
            meta.setOriginalTitle("Instagram Reel");
            meta.setDescription("Instagram Reel • Saved for viewing");
        } else if (canonical.contains("/tv/")) {
            meta.setContentType("video");
            meta.setTitle("Instagram Video");
            meta.setOriginalTitle("Instagram Video");
            meta.setDescription("Instagram Video • Saved for viewing");
        } else {
            meta.setContentType("image");
            meta.setTitle("Instagram Post");
            meta.setOriginalTitle("Instagram Post");
            meta.setDescription("Instagram Post • Saved for viewing");
        }

        meta.setCreatorName("Instagram");

        // Attempt lightweight OpenGraph extraction if permitted by endpoint
        try {
            HttpFetchHelper.FetchResult result = HttpFetchHelper.get(canonical, "text/html");
            if (result.isSuccess() && result.body != null) {
                // Parse og:title or og:description if returned without login wall
                String html = result.body;
                int ogTitleIdx = html.indexOf("property=\"og:title\"");
                if (ogTitleIdx != -1) {
                    int contentIdx = html.indexOf("content=\"", Math.max(0, ogTitleIdx - 100));
                    if (contentIdx != -1 && contentIdx < ogTitleIdx + 200) {
                        int start = contentIdx + 9;
                        int end = html.indexOf("\"", start);
                        if (end > start) {
                            String parsedTitle = html.substring(start, end).trim();
                            if (!parsedTitle.isEmpty() && !parsedTitle.equalsIgnoreCase("Instagram")) {
                                meta.setTitle(parsedTitle);
                                meta.setOriginalTitle(parsedTitle);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Instagram login wall or bot challenge: gracefully rely on structured fallback
        }

        return meta;
    }
}
