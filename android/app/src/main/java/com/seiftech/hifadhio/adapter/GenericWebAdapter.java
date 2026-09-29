package com.seiftech.hifadhio.adapter;

import com.seiftech.hifadhio.data.UrlNormalizer;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic web metadata extractor extracting OpenGraph, Twitter Card, and standard HTML meta tags.
 * Serves as the universal fallback adapter for all web links.
 */
public class GenericWebAdapter implements ContentExtractorAdapter {
    public static final String PLATFORM_ID = "GenericWeb";
    private static final int MAX_HTML_BYTES = 512 * 1024; // Read max 512 KB (head is always near top)
    private static final int TIMEOUT_CONNECT_MS = 8000;
    private static final int TIMEOUT_READ_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 Hifadhio/0.5.0";

    @Override
    public String getPlatformId() {
        return PLATFORM_ID;
    }

    @Override
    public int getPriority() {
        return 0; // Universal fallback priority
    }

    @Override
    public boolean canHandle(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    @Override
    public String canonicalize(String url) {
        return UrlNormalizer.normalize(url);
    }

    @Override
    public AdapterHealth checkHealth() {
        return AdapterHealth.ok("Generic web scraper ready (OpenGraph / HTML / Meta)");
    }

    @Override
    public ExtractedMetadata extract(String targetUrl) throws ExtractionException {
        if (!canHandle(targetUrl)) {
            throw new ExtractionException("ERR_INVALID_URL", "URL protocol must be http or https: " + targetUrl);
        }

        String canonical = canonicalize(targetUrl);
        String html;
        String finalUrl = canonical;

        try {
            HttpResult result = fetchHtml(canonical);
            html = result.content;
            if (result.finalUrl != null && !result.finalUrl.isEmpty()) {
                finalUrl = result.finalUrl;
            }
            return extractFromHtml(html, finalUrl);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // Check for cleartext downgrade or permanent client/blocking errors (400, 401, 403, 404, too many redirects)
            if (msg.contains("Cleartext HTTP traffic")
                    || msg.contains("HTTP response error: 400")
                    || msg.contains("HTTP response error: 401")
                    || msg.contains("HTTP response error: 403")
                    || msg.contains("HTTP response error: 404")
                    || msg.contains("Too many redirects")) {
                ExtractedMetadata fallback = new ExtractedMetadata();
                fallback.setPlatform(detectPlatform(canonical));
                fallback.setCanonicalUrl(canonical);
                String domain = extractDomain(canonical);
                fallback.setTitle(domain + " Page");
                fallback.setOriginalTitle(domain + " Page");
                if (msg.contains("Cleartext HTTP traffic")) {
                    fallback.setDescription("Page saved securely (destination attempted cleartext HTTP redirect)");
                } else if (msg.contains("403") || msg.contains("401") || msg.contains("400")) {
                    fallback.setDescription("Page saved (content protected or requires browser session)");
                } else if (msg.contains("404")) {
                    fallback.setDescription("Link saved (page returned 404 Not Found)");
                } else {
                    fallback.setDescription("Link saved");
                }
                return fallback;
            }
            // For real network timeouts or connection drops, throw so retry engine can retry!
            throw new ExtractionException("ERR_FETCH_FAILED", "Failed to fetch content from URL: " + msg, e);
        }
    }

    /**
     * Parse HTML string and extract metadata tags. Exposed for unit testing without network.
     */
    public ExtractedMetadata extractFromHtml(String html, String baseUrl) {
        ExtractedMetadata meta = new ExtractedMetadata();
        meta.setPlatform(detectPlatform(baseUrl));
        meta.setCanonicalUrl(canonicalize(baseUrl));

        if (html == null || html.trim().isEmpty()) {
            return meta;
        }

        // 1. Extract all meta tags into key-value map
        Map<String, String> metaMap = extractMetaTags(html);

        // 2. Extract <title>
        String htmlTitle = extractTagContent(html, "title");

        // 3. Extract canonical link tag: <link rel="canonical" href="...">
        String linkCanonical = extractCanonicalLink(html);

        // Resolve title (Priority: og:title -> twitter:title -> <title>)
        String title = metaMap.get("og:title");
        if (title == null || title.isEmpty()) {
            title = metaMap.get("twitter:title");
        }
        if (title == null || title.isEmpty()) {
            title = htmlTitle;
        }
        if (title != null) {
            String decodedTitle = decodeHtmlEntities(title);
            meta.setTitle(decodedTitle);
            meta.setOriginalTitle(decodedTitle);
        }

        // Resolve description (Priority: og:description -> twitter:description -> description)
        String desc = metaMap.get("og:description");
        if (desc == null || desc.isEmpty()) {
            desc = metaMap.get("twitter:description");
        }
        if (desc == null || desc.isEmpty()) {
            desc = metaMap.get("description");
        }
        if (desc != null) {
            meta.setDescription(decodeHtmlEntities(desc));
        }

        // Resolve thumbnail (Priority: og:image -> twitter:image -> twitter:image:src)
        String img = metaMap.get("og:image");
        if (img == null || img.isEmpty()) {
            img = metaMap.get("twitter:image");
        }
        if (img == null || img.isEmpty()) {
            img = metaMap.get("twitter:image:src");
        }
        if (img != null) {
            meta.setThumbnailUrl(resolveUrl(baseUrl, decodeHtmlEntities(img)));
        }

        // Resolve creator/site (og:site_name -> author)
        String siteName = metaMap.get("og:site_name");
        String author = metaMap.get("author");
        if (author != null && !author.isEmpty()) {
            meta.setCreatorName(decodeHtmlEntities(author));
        } else if (siteName != null && !siteName.isEmpty()) {
            meta.setCreatorName(decodeHtmlEntities(siteName));
        }

        // Resolve content type (og:type)
        String ogType = metaMap.get("og:type");
        if (ogType != null && !ogType.isEmpty()) {
            meta.setContentType(ogType.toLowerCase());
        } else {
            meta.setContentType("article");
        }

        // Canonical URL override from page tags if valid
        String pageCanonical = metaMap.get("og:url");
        if (pageCanonical == null || pageCanonical.isEmpty()) {
            pageCanonical = linkCanonical;
        }
        if (pageCanonical != null && !pageCanonical.isEmpty()) {
            String resolved = resolveUrl(baseUrl, pageCanonical);
            if (resolved != null && (resolved.startsWith("http://") || resolved.startsWith("https://"))) {
                meta.setCanonicalUrl(canonicalize(resolved));
            }
        }

        // Raw JSON representation
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> entry : metaMap.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            if (htmlTitle != null) json.put("html:title", htmlTitle);
            if (linkCanonical != null) json.put("html:canonical", linkCanonical);
            meta.setRawJson(json.toString());
        } catch (Exception ignored) {}

        return meta;
    }

    private String detectPlatform(String url) {
        if (url == null) return "Web";
        String lower = url.toLowerCase();
        if (lower.contains("instagram.com")) return "Instagram";
        if (lower.contains("tiktok.com")) return "TikTok";
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) return "YouTube";
        if (lower.contains("facebook.com") || lower.contains("fb.watch")) return "Facebook";
        if (lower.contains("twitter.com") || lower.contains("x.com")) return "X";
        if (lower.contains("reddit.com") || lower.contains("redd.it")) return "Reddit";
        if (lower.contains("linkedin.com")) return "LinkedIn";
        return "Web";
    }

    private Map<String, String> extractMetaTags(String html) {
        Map<String, String> map = new HashMap<>();
        Pattern metaPattern = Pattern.compile("<meta\\s+([^>]*?)>", Pattern.CASE_INSENSITIVE);
        Pattern propPattern = Pattern.compile("(?:property|name)\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Pattern contentPattern = Pattern.compile("content\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);

        Matcher m = metaPattern.matcher(html);
        while (m.find()) {
            String attributes = m.group(1);
            if (attributes == null) continue;

            String key = null;
            String val = null;

            Matcher propMatcher = propPattern.matcher(attributes);
            if (propMatcher.find()) {
                key = propMatcher.group(1).trim().toLowerCase();
            }

            Matcher contentMatcher = contentPattern.matcher(attributes);
            if (contentMatcher.find()) {
                val = contentMatcher.group(1).trim();
            }

            if (key != null && val != null && !key.isEmpty()) {
                map.put(key, val);
            }
        }
        return map;
    }

    private String extractTagContent(String html, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + "[^>]*>(.*?)</" + tagName + ">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = pattern.matcher(html);
        if (m.find()) {
            String content = m.group(1);
            if (content != null) {
                return content.replaceAll("\\s+", " ").trim();
            }
        }
        return null;
    }

    private String extractCanonicalLink(String html) {
        Pattern linkPattern = Pattern.compile("<link\\s+([^>]*?)>", Pattern.CASE_INSENSITIVE);
        Pattern relPattern = Pattern.compile("rel\\s*=\\s*[\"']canonical[\"']", Pattern.CASE_INSENSITIVE);
        Pattern hrefPattern = Pattern.compile("href\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

        Matcher m = linkPattern.matcher(html);
        while (m.find()) {
            String attrs = m.group(1);
            if (attrs != null && relPattern.matcher(attrs).find()) {
                Matcher hrefMatcher = hrefPattern.matcher(attrs);
                if (hrefMatcher.find()) {
                    return hrefMatcher.group(1).trim();
                }
            }
        }
        return null;
    }

    public static String decodeHtmlEntities(String input) {
        if (input == null) return null;
        String text = input
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&#x27;", "'")
                .replace("&#x2F;", "/")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .replace("&hellip;", "…");

        // Numeric entities &#123;
        Pattern numPattern = Pattern.compile("&#(\\d+);");
        Matcher m = numPattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try {
                int code = Integer.parseInt(m.group(1));
                m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(code))));
            } catch (Exception e) {
                m.appendReplacement(sb, m.group(0));
            }
        }
        m.appendTail(sb);
        text = sb.toString();

        // Hex entities &#x1F600;
        Pattern hexPattern = Pattern.compile("&#x([0-9a-fA-F]+);");
        m = hexPattern.matcher(text);
        sb = new StringBuffer();
        while (m.find()) {
            try {
                int code = Integer.parseInt(m.group(1), 16);
                m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(code))));
            } catch (Exception e) {
                m.appendReplacement(sb, m.group(0));
            }
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }

    public static String resolveUrl(String baseUrl, String target) {
        if (target == null || target.trim().isEmpty()) return null;
        String trimmed = target.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("//")) {
            String proto = (baseUrl != null && baseUrl.startsWith("https://")) ? "https:" : "http:";
            return proto + trimmed;
        }
        if (baseUrl != null) {
            try {
                URI base = new URI(baseUrl);
                return base.resolve(trimmed).toString();
            } catch (Exception ignored) {}
        }
        return trimmed;
    }

    private static class HttpResult {
        final String content;
        final String finalUrl;

        HttpResult(String content, String finalUrl) {
            this.content = content;
            this.finalUrl = finalUrl;
        }
    }

    public static String upgradeHttpToHttps(String url) {
        if (url == null) return null;
        if (url.startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }

    private HttpResult fetchHtml(String targetUrl) throws Exception {
        String currentUrl = upgradeHttpToHttps(targetUrl);
        int redirects = 0;

        while (redirects < 5) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setConnectTimeout(TIMEOUT_CONNECT_MS);
            conn.setReadTimeout(TIMEOUT_READ_MS);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                    || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                if (location != null && !location.isEmpty()) {
                    String resolved = resolveUrl(currentUrl, location);
                    // Prevent insecure redirect downgrade to cleartext HTTP (e.g. FAO redirecting to http://)
                    resolved = upgradeHttpToHttps(resolved);
                    currentUrl = resolved;
                    redirects++;
                    conn.disconnect();
                    continue;
                }
            }

            if (status != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP response error: " + status + " " + conn.getResponseMessage());
            }

            try (InputStream in = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[4096];
                int read;
                int total = 0;
                while ((read = reader.read(buffer)) != -1 && total < MAX_HTML_BYTES) {
                    sb.append(buffer, 0, read);
                    total += read;
                }
                return new HttpResult(sb.toString(), currentUrl);
            } finally {
                conn.disconnect();
            }
        }
        throw new Exception("Too many redirects: " + targetUrl);
    }

    public static String extractDomain(String url) {
        if (url == null) return "Web";
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null) {
                if (host.startsWith("www.")) host = host.substring(4);
                return host;
            }
        } catch (Exception ignored) {}
        return "Web";
    }
}
