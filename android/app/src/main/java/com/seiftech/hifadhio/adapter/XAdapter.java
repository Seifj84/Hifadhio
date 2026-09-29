package com.seiftech.hifadhio.adapter;

import com.seiftech.hifadhio.data.UrlNormalizer;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized platform adapter for X / Twitter content (Posts/Tweets, Threads).
 * Extracts official tweet metadata using Twitter's public publish oEmbed service
 * with author-aware fallback metadata.
 */
public class XAdapter implements ContentExtractorAdapter {
    public static final String PLATFORM_ID = "X";

    private static final Pattern STATUS_PATTERN =
            Pattern.compile("(?:x\\.com|twitter\\.com)/([a-zA-Z0-9_]+)/status/([0-9]+)", Pattern.CASE_INSENSITIVE);

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
        return lower.contains("x.com") || lower.contains("twitter.com");
    }

    public String extractUsername(String url) {
        if (url == null) return null;
        Matcher m = STATUS_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public String extractStatusId(String url) {
        if (url == null) return null;
        Matcher m = STATUS_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(2);
        }
        return null;
    }

    @Override
    public String canonicalize(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        String clean = UrlNormalizer.normalize(url);

        Matcher statusMatcher = STATUS_PATTERN.matcher(clean);
        if (statusMatcher.find()) {
            String user = statusMatcher.group(1);
            String id = statusMatcher.group(2);
            return "https://x.com/" + user + "/status/" + id;
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
        return AdapterHealth.ok("X/Twitter adapter active (publish oEmbed service & status parser ready)");
    }

    @Override
    public ExtractedMetadata extract(String url) throws ExtractionException {
        if (!canHandle(url)) {
            throw new ExtractionException("ERR_INVALID_URL", "Not a valid X/Twitter URL: " + url);
        }

        String canonical = canonicalize(url);
        String username = extractUsername(canonical);

        ExtractedMetadata meta = new ExtractedMetadata();
        meta.setPlatform(PLATFORM_ID);
        meta.setCanonicalUrl(canonical);
        meta.setContentType("post");

        if (username != null) {
            meta.setCreatorHandle("@" + username);
            meta.setCreatorName("@" + username);
        } else {
            meta.setCreatorName("X");
        }

        // Attempt live extraction via official Twitter publish oEmbed service
        try {
            String encoded = URLEncoder.encode(canonical, StandardCharsets.UTF_8.name());
            String oembedUrl = "https://publish.twitter.com/oembed?url=" + encoded;

            HttpFetchHelper.FetchResult result = HttpFetchHelper.get(oembedUrl, "application/json");
            if (result.isSuccess() && result.body != null && !result.body.trim().isEmpty()) {
                JSONObject json = new JSONObject(result.body);
                if (json.has("author_name")) {
                    String author = json.getString("author_name");
                    meta.setCreatorName(author);
                    String title = "Post by " + author + " on X";
                    meta.setTitle(title);
                    meta.setOriginalTitle(title);
                }
                if (json.has("html")) {
                    // Extract text snippet from blockquote if available
                    String html = json.getString("html");
                    int pStart = html.indexOf("<p ");
                    if (pStart != -1) {
                        int pContent = html.indexOf(">", pStart);
                        int pEnd = html.indexOf("</p>", pContent);
                        if (pContent != -1 && pEnd != -1) {
                            String tweetText = html.substring(pContent + 1, pEnd).replaceAll("<[^>]+>", "").trim();
                            if (!tweetText.isEmpty()) {
                                meta.setDescription(tweetText);
                                if (meta.getTitle() == null) {
                                    meta.setTitle(tweetText);
                                    meta.setOriginalTitle(tweetText);
                                }
                            }
                        }
                    }
                }
                meta.setRawJson(json.toString());
                return meta;
            }
        } catch (Exception ignored) {
            // Non-fatal: if oEmbed fails or is offline, continue to fallback metadata
        }

        // Graceful deterministic fallback
        if (meta.getTitle() == null || meta.getTitle().isEmpty()) {
            meta.setTitle(username != null ? "Post by @" + username + " on X" : "Post on X");
            meta.setOriginalTitle(meta.getTitle());
        }
        if (meta.getDescription() == null || meta.getDescription().isEmpty()) {
            meta.setDescription("Post on X • Saved for viewing");
        }

        return meta;
    }
}
