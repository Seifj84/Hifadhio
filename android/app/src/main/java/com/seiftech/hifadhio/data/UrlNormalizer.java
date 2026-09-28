package com.seiftech.hifadhio.data;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlNormalizer {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            "igshid", "fbclid", "si", "utm_source", "utm_medium", "utm_campaign",
            "utm_term", "utm_content", "ref", "ref_src", "feature", "gclid",
            "msclkid", "dclid", "twclid", "aff_platform", "aff_trace_key"
    ));

    public static String extractUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            String found = matcher.group();
            while (found.endsWith(".") || found.endsWith(",") || found.endsWith(")") || found.endsWith("]") || found.endsWith(">")) {
                found = found.substring(0, found.length() - 1);
            }
            return found;
        }
        return text.trim();
    }

    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }

    public static String normalize(String inputUrl) {
        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            return "";
        }
        String raw = inputUrl.trim();
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "https";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            String path = uri.getPath();
            if (path == null) path = "";
            while (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            String query = uri.getRawQuery();
            String cleanQuery = "";
            if (query != null && !query.trim().isEmpty()) {
                String[] pairs = query.split("&");
                List<String> retained = new ArrayList<>();
                for (String pair : pairs) {
                    if (pair.isEmpty()) continue;
                    int idx = pair.indexOf("=");
                    String key = idx > 0 ? pair.substring(0, idx) : pair;
                    String val = idx > 0 ? pair.substring(idx + 1) : "";
                    if (!TRACKING_PARAMS.contains(key.toLowerCase(Locale.ROOT))) {
                        retained.add(key + (idx > 0 ? "=" + val : ""));
                    }
                }
                if (!retained.isEmpty()) {
                    Collections.sort(retained);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < retained.size(); i++) {
                        if (i > 0) sb.append("&");
                        sb.append(retained.get(i));
                    }
                    cleanQuery = sb.toString();
                }
            }

            StringBuilder norm = new StringBuilder();
            norm.append(scheme).append("://").append(host).append(path);
            if (!cleanQuery.isEmpty()) {
                norm.append("?").append(cleanQuery);
            }
            return norm.toString();
        } catch (Exception e) {
            // Fallback for non-standard URIs
            return raw;
        }
    }
}
