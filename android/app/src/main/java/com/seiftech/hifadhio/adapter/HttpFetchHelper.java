package com.seiftech.hifadhio.adapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Shared network fetch utility for metadata extraction adapters.
 * Enforces HTTPS upgrades, standard mobile User-Agent, safe redirect limits,
 * and bounded read sizes.
 */
public class HttpFetchHelper {
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 Hifadhio/0.6.0";
    private static final int MAX_BYTES = 512 * 1024; // 512 KB
    private static final int TIMEOUT_CONNECT_MS = 8000;
    private static final int TIMEOUT_READ_MS = 10000;

    public static class FetchResult {
        public final int statusCode;
        public final String body;
        public final String finalUrl;

        public FetchResult(int statusCode, String body, String finalUrl) {
            this.statusCode = statusCode;
            this.body = body;
            this.finalUrl = finalUrl;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    public static String upgradeHttpToHttps(String url) {
        if (url == null) return null;
        if (url.startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }

    public static FetchResult get(String targetUrl, String acceptHeader) throws Exception {
        String currentUrl = upgradeHttpToHttps(targetUrl);
        int redirects = 0;

        while (redirects < 5) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
            if (acceptHeader != null && !acceptHeader.trim().isEmpty()) {
                conn.setRequestProperty("Accept", acceptHeader);
            }
            conn.setConnectTimeout(TIMEOUT_CONNECT_MS);
            conn.setReadTimeout(TIMEOUT_READ_MS);

            int status = conn.getResponseCode();

            if (status >= 300 && status < 400) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new Exception("Redirect with no Location header from " + currentUrl);
                }
                if (!location.startsWith("http://") && !location.startsWith("https://")) {
                    URL base = new URL(currentUrl);
                    location = new URL(base, location).toString();
                }
                currentUrl = upgradeHttpToHttps(location);
                redirects++;
                continue;
            }

            if (status < 200 || status >= 300) {
                conn.disconnect();
                throw new Exception("HTTP response error: " + status + " for " + currentUrl);
            }

            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int totalChars = 0;
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
                totalChars += read;
                if (totalChars > MAX_BYTES) break;
            }
            reader.close();
            conn.disconnect();

            return new FetchResult(status, sb.toString(), currentUrl);
        }

        throw new Exception("Too many redirects (> 5) for URL: " + targetUrl);
    }
}
