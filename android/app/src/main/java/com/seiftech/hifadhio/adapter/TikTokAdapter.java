package com.seiftech.hifadhio.adapter;

import com.seiftech.hifadhio.data.UrlNormalizer;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized platform adapter for TikTok content (Videos, Shares, Shorts, vt.tiktok / vm.tiktok).
 * Extracts official video metadata using TikTok's public oEmbed service
 * with structured fallback metadata guaranteeing capture resilience.
 */
public class TikTokAdapter implements ContentExtractorAdapter {
    public static final String PLATFORM_ID = "TikTok";

    private static final Pattern VIDEO_URL_PATTERN =
            Pattern.compile("tiktok\\.com/@([a-zA-Z0-9_.-]+)/video/([0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_DOMAIN_PATTERN =
            Pattern.compile("(?:vt|vm)\\.tiktok\\.com/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String getPlatformId() {
        return PLATFORM_ID;
    }

    @Override
    public int getPriority() {
        return 85; // Higher than Facebook/Instagram (80)
    }

    @Override
    public boolean canHandle(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.toLowerCase();
        return lower.contains("tiktok.com");
    }

    public String extractUsername(String url) {
        if (url == null) return null;
        Matcher m = VIDEO_URL_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public String extractVideoId(String url) {
        if (url == null) return null;
        Matcher m = VIDEO_URL_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(2);
        }
        return null;
    }

    @Override
    public String canonicalize(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        String clean = UrlNormalizer.normalize(url);

        Matcher videoMatcher = VIDEO_URL_PATTERN.matcher(clean);
        if (videoMatcher.find()) {
            String user = videoMatcher.group(1);
            String videoId = videoMatcher.group(2);
            return "https://www.tiktok.com/@" + user + "/video/" + videoId;
        }

        Matcher shortMatcher = SHORT_DOMAIN_PATTERN.matcher(clean);
        if (shortMatcher.find()) {
            String code = shortMatcher.group(1);
            String domain = clean.toLowerCase().contains("vm.tiktok.com") ? "vm.tiktok.com" : "vt.tiktok.com";
            return "https://" + domain + "/" + code;
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
        return AdapterHealth.ok("TikTok adapter active (oEmbed service & URL normalizer ready)");
    }

    @Override
    public ExtractedMetadata extract(String url) throws ExtractionException {
        if (!canHandle(url)) {
            throw new ExtractionException("ERR_INVALID_URL", "Not a valid TikTok URL: " + url);
        }

        String canonical = canonicalize(url);
        String username = extractUsername(canonical);

        ExtractedMetadata meta = new ExtractedMetadata();
        meta.setPlatform(PLATFORM_ID);
        meta.setCanonicalUrl(canonical);
        meta.setContentType("video");

        if (username != null) {
            meta.setCreatorHandle("@" + username);
            meta.setCreatorName("@" + username);
        }

        // Attempt live metadata extraction via TikTok's public oEmbed service
        try {
            String encodedUrl = URLEncoder.encode(canonical, StandardCharsets.UTF_8.name());
            String oembedUrl = "https://www.tiktok.com/oembed?url=" + encodedUrl;

            HttpFetchHelper.FetchResult result = HttpFetchHelper.get(oembedUrl, "application/json");
            if (result.isSuccess() && result.body != null && !result.body.trim().isEmpty()) {
                JSONObject json = new JSONObject(result.body);
                if (json.has("title")) {
                    String title = json.getString("title");
                    if (!title.trim().isEmpty()) {
                        meta.setTitle(title);
                        meta.setOriginalTitle(title);
                        meta.setDescription(title);
                    }
                }
                if (json.has("author_name")) {
                    String author = json.getString("author_name");
                    meta.setCreatorName(author);
                }
                if (json.has("author_unique_id")) {
                    meta.setCreatorHandle("@" + json.getString("author_unique_id"));
                }
                if (json.has("thumbnail_url")) {
                    meta.setThumbnailUrl(json.getString("thumbnail_url"));
                }
                meta.setRawJson(json.toString());
                return meta;
            }
        } catch (Exception e) {
            // Non-fatal: if oEmbed fails or network is offline, continue to fallback metadata
        }

        // Graceful deterministic fallback
        if (meta.getTitle() == null || meta.getTitle().isEmpty()) {
            meta.setTitle(username != null ? "TikTok Video by @" + username : "TikTok Video");
            meta.setOriginalTitle(meta.getTitle());
        }
        if (meta.getDescription() == null || meta.getDescription().isEmpty()) {
            meta.setDescription("TikTok video • Saved for viewing");
        }
        if (meta.getCreatorName() == null || meta.getCreatorName().isEmpty()) {
            meta.setCreatorName("TikTok");
        }

        return meta;
    }
}
