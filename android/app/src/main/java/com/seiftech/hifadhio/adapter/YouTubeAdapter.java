package com.seiftech.hifadhio.adapter;

import com.seiftech.hifadhio.data.UrlNormalizer;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized platform adapter for YouTube content (Standard Videos, Shorts, Live, youtu.be).
 * Extracts official video metadata using YouTube's public oEmbed service
 * with deterministic fallback to direct video ID thumbnails and titles.
 */
public class YouTubeAdapter implements ContentExtractorAdapter {
    public static final String PLATFORM_ID = "YouTube";

    // Pattern matching YouTube 11-char video ID across watch, shorts, live, embed, and youtu.be
    private static final Pattern WATCH_V_PATTERN = Pattern.compile("[?&]v=([a-zA-Z0-9_-]{11})");
    private static final Pattern SHORTS_PATTERN = Pattern.compile("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIVE_PATTERN = Pattern.compile("youtube\\.com/live/([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMBED_PATTERN = Pattern.compile("youtube\\.com/embed/([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE);
    private static final Pattern YOUTUBE_SHORT_DOMAIN_PATTERN = Pattern.compile("youtu\\.be/([a-zA-Z0-9_-]{11})", Pattern.CASE_INSENSITIVE);

    @Override
    public String getPlatformId() {
        return PLATFORM_ID;
    }

    @Override
    public int getPriority() {
        return 90; // Higher than GenericWeb (0) and social platforms
    }

    @Override
    public boolean canHandle(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.toLowerCase();
        return lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    /**
     * Extracts the 11-character YouTube video ID if present in the URL.
     */
    public String extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) return null;

        Matcher shortsMatcher = SHORTS_PATTERN.matcher(url);
        if (shortsMatcher.find()) return shortsMatcher.group(1);

        Matcher youtuBeMatcher = YOUTUBE_SHORT_DOMAIN_PATTERN.matcher(url);
        if (youtuBeMatcher.find()) return youtuBeMatcher.group(1);

        Matcher watchMatcher = WATCH_V_PATTERN.matcher(url);
        if (watchMatcher.find()) return watchMatcher.group(1);

        Matcher liveMatcher = LIVE_PATTERN.matcher(url);
        if (liveMatcher.find()) return liveMatcher.group(1);

        Matcher embedMatcher = EMBED_PATTERN.matcher(url);
        if (embedMatcher.find()) return embedMatcher.group(1);

        return null;
    }

    public boolean isShorts(String url) {
        if (url == null) return false;
        return url.toLowerCase().contains("/shorts/");
    }

    @Override
    public String canonicalize(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        String clean = UrlNormalizer.normalize(url);
        String videoId = extractVideoId(clean);

        if (videoId != null) {
            if (isShorts(clean)) {
                return "https://www.youtube.com/shorts/" + videoId;
            }
            return "https://www.youtube.com/watch?v=" + videoId;
        }

        // Channels, playlists, or non-video URLs
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
        return AdapterHealth.ok("YouTube adapter active (oEmbed service & video ID parser ready)");
    }

    @Override
    public ExtractedMetadata extract(String url) throws ExtractionException {
        if (!canHandle(url)) {
            throw new ExtractionException("ERR_INVALID_URL", "Not a valid YouTube URL: " + url);
        }

        String canonical = canonicalize(url);
        String videoId = extractVideoId(canonical);
        boolean isShort = isShorts(canonical);

        ExtractedMetadata meta = new ExtractedMetadata();
        meta.setPlatform(PLATFORM_ID);
        meta.setCanonicalUrl(canonical);
        meta.setContentType("video");

        // Set default deterministic thumbnail from video ID if available
        if (videoId != null) {
            meta.setThumbnailUrl("https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg");
        }

        // Attempt live metadata extraction via official YouTube oEmbed endpoint
        try {
            String encodedUrl = URLEncoder.encode(canonical, StandardCharsets.UTF_8.name());
            String oembedUrl = "https://www.youtube.com/oembed?url=" + encodedUrl + "&format=json";

            HttpFetchHelper.FetchResult result = HttpFetchHelper.get(oembedUrl, "application/json");
            if (result.isSuccess() && result.body != null && !result.body.trim().isEmpty()) {
                JSONObject json = new JSONObject(result.body);
                if (json.has("title")) {
                    String title = json.getString("title");
                    meta.setTitle(title);
                    meta.setOriginalTitle(title);
                }
                if (json.has("author_name")) {
                    String author = json.getString("author_name");
                    meta.setCreatorName(author);
                    meta.setDescription("YouTube video by " + author);
                }
                if (json.has("thumbnail_url")) {
                    meta.setThumbnailUrl(json.getString("thumbnail_url"));
                }
                meta.setRawJson(json.toString());
                return meta;
            }
        } catch (Exception e) {
            // Non-fatal: if oEmbed fails or is offline, continue to fallback metadata
        }

        // Graceful deterministic fallback
        if (meta.getTitle() == null || meta.getTitle().isEmpty()) {
            String title = isShort ? "YouTube Short" : "YouTube Video";
            meta.setTitle(title);
            meta.setOriginalTitle(title);
        }
        if (meta.getCreatorName() == null || meta.getCreatorName().isEmpty()) {
            meta.setCreatorName("YouTube");
        }
        if (meta.getDescription() == null || meta.getDescription().isEmpty()) {
            meta.setDescription(isShort ? "YouTube Short • Saved for viewing" : "YouTube Video • Saved for viewing");
        }

        return meta;
    }
}
