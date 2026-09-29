package com.seiftech.hifadhio.adapter;

import com.seiftech.hifadhio.data.UrlNormalizer;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized platform adapter for Reddit content (Subreddits, Posts, Comments, redd.it shortlinks).
 * Extracts official post metadata using Reddit's public oEmbed service
 * with structured subreddit-aware fallback metadata.
 */
public class RedditAdapter implements ContentExtractorAdapter {
    public static final String PLATFORM_ID = "Reddit";

    private static final Pattern POST_PATTERN =
            Pattern.compile("reddit\\.com/r/([a-zA-Z0-9_]+)/comments/([a-zA-Z0-9_]+)(?:/([a-zA-Z0-9_]+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_PATTERN =
            Pattern.compile("redd\\.it/([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBREDDIT_PATTERN =
            Pattern.compile("reddit\\.com/r/([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String getPlatformId() {
        return PLATFORM_ID;
    }

    @Override
    public int getPriority() {
        return 75; // Specialized platform priority
    }

    @Override
    public boolean canHandle(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.toLowerCase();
        return lower.contains("reddit.com") || lower.contains("redd.it");
    }

    public String extractSubreddit(String url) {
        if (url == null) return null;
        Matcher m = SUBREDDIT_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    @Override
    public String canonicalize(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        String clean = UrlNormalizer.normalize(url);

        Matcher postMatcher = POST_PATTERN.matcher(clean);
        if (postMatcher.find()) {
            String sub = postMatcher.group(1);
            String id = postMatcher.group(2);
            String slug = postMatcher.group(3);
            if (slug != null && !slug.isEmpty()) {
                return "https://www.reddit.com/r/" + sub + "/comments/" + id + "/" + slug + "/";
            }
            return "https://www.reddit.com/r/" + sub + "/comments/" + id + "/";
        }

        Matcher shortMatcher = SHORT_PATTERN.matcher(clean);
        if (shortMatcher.find()) {
            return "https://redd.it/" + shortMatcher.group(1);
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
        return AdapterHealth.ok("Reddit adapter active (oEmbed service & subreddit parser ready)");
    }

    @Override
    public ExtractedMetadata extract(String url) throws ExtractionException {
        if (!canHandle(url)) {
            throw new ExtractionException("ERR_INVALID_URL", "Not a valid Reddit URL: " + url);
        }

        String canonical = canonicalize(url);
        String subreddit = extractSubreddit(canonical);

        ExtractedMetadata meta = new ExtractedMetadata();
        meta.setPlatform(PLATFORM_ID);
        meta.setCanonicalUrl(canonical);
        meta.setContentType("post");

        if (subreddit != null) {
            meta.setCreatorHandle("r/" + subreddit);
            meta.setCreatorName("r/" + subreddit);
        } else {
            meta.setCreatorName("Reddit");
        }

        // Attempt live extraction via official Reddit oEmbed service
        try {
            String encoded = URLEncoder.encode(canonical, StandardCharsets.UTF_8.name());
            String oembedUrl = "https://www.reddit.com/oembed?url=" + encoded;

            HttpFetchHelper.FetchResult result = HttpFetchHelper.get(oembedUrl, "application/json");
            if (result.isSuccess() && result.body != null && !result.body.trim().isEmpty()) {
                JSONObject json = new JSONObject(result.body);
                if (json.has("title")) {
                    String title = json.getString("title");
                    meta.setTitle(title);
                    meta.setOriginalTitle(title);
                    meta.setDescription(title);
                }
                if (json.has("author_name")) {
                    meta.setCreatorName("u/" + json.getString("author_name"));
                }
                meta.setRawJson(json.toString());
                return meta;
            }
        } catch (Exception ignored) {
            // Non-fatal: if oEmbed fails or is offline, continue to fallback metadata
        }

        // Graceful deterministic fallback
        if (meta.getTitle() == null || meta.getTitle().isEmpty()) {
            meta.setTitle(subreddit != null ? "Reddit post in r/" + subreddit : "Reddit Post");
            meta.setOriginalTitle(meta.getTitle());
        }
        if (meta.getDescription() == null || meta.getDescription().isEmpty()) {
            meta.setDescription(subreddit != null ? "Reddit discussion from r/" + subreddit : "Reddit discussion • Saved for viewing");
        }

        return meta;
    }
}
