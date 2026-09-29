package com.seiftech.hifadhio.transcription;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Priority 50 provider: Cloud Whisper API client (OpenAI / Groq compliant).
 * Adheres strictly to ADR-007: Provider abstraction and vendor isolation.
 * Computes exact duration and API cost metrics ($0.006 / minute).
 */
public class CloudWhisperProvider implements TranscriptionProvider {
    public static final String PROVIDER_ID = "cloud_whisper";
    public static final double COST_PER_MINUTE_USD = 0.006;

    private String apiKey;
    private String endpointUrl;
    private String modelName;

    public CloudWhisperProvider() {
        this(null, "https://api.openai.com/v1/audio/transcriptions", "whisper-1");
    }

    public CloudWhisperProvider(String apiKey, String endpointUrl, String modelName) {
        this.apiKey = apiKey;
        this.endpointUrl = endpointUrl != null ? endpointUrl : "https://api.openai.com/v1/audio/transcriptions";
        this.modelName = modelName != null ? modelName : "whisper-1";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayName() {
        return "Cloud Whisper API (" + modelName + ")";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public double estimateCost(long audioDurationMs) {
        if (audioDurationMs <= 0) return 0.0;
        double minutes = audioDurationMs / 60000.0;
        return minutes * COST_PER_MINUTE_USD;
    }

    @Override
    public TranscriptionResult transcribe(long contentItemId, File audioFile, TranscriptionOptions options) throws Exception {
        long startProcessing = System.currentTimeMillis();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return TranscriptionResult.failure(TranscriptionResult.ERROR_AUTH_REQUIRED,
                    "Cloud transcription requires an API key in settings");
        }

        if (audioFile == null || !audioFile.exists()) {
            return TranscriptionResult.failure(TranscriptionResult.ERROR_AUDIO_FETCH_FAILED,
                    "Audio file does not exist");
        }

        if (audioFile.length() > options.getMaxFileSizeBytes()) {
            return TranscriptionResult.failure(TranscriptionResult.ERROR_FILE_TOO_LARGE,
                    "Audio file size exceeds limit: " + audioFile.length() + " bytes");
        }

        String boundary = "===" + System.currentTimeMillis() + "===";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        HttpURLConnection conn = (HttpURLConnection) new URL(endpointUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            // Model parameter
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"model\"" + lineEnd + lineEnd);
            dos.writeBytes(modelName + lineEnd);

            // Response format parameter (verbose_json to get true segment timestamps)
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"response_format\"" + lineEnd + lineEnd);
            dos.writeBytes("verbose_json" + lineEnd);

            // Language parameter if specified
            if (options.getLanguage() != null && !options.getLanguage().equalsIgnoreCase("auto")) {
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"language\"" + lineEnd + lineEnd);
                dos.writeBytes(options.getLanguage() + lineEnd);
            }

            // Prompt parameter if specified
            if (options.getPrompt() != null && !options.getPrompt().trim().isEmpty()) {
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"prompt\"" + lineEnd + lineEnd);
                dos.writeBytes(options.getPrompt() + lineEnd);
            }

            // Temperature
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"temperature\"" + lineEnd + lineEnd);
            dos.writeBytes(String.valueOf(options.getTemperature()) + lineEnd);

            // Audio File payload
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + audioFile.getName() + "\"" + lineEnd);
            dos.writeBytes("Content-Type: audio/mpeg" + lineEnd + lineEnd);

            byte[] buffer = new byte[8192];
            try (FileInputStream fis = new FileInputStream(audioFile)) {
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, read);
                }
            }
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
        }

        int statusCode = conn.getResponseCode();
        if (statusCode >= 200 && statusCode < 300) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            return parseApiResponse(contentItemId, response.toString(), startProcessing);
        } else {
            StringBuilder errorResponse = new StringBuilder();
            if (conn.getErrorStream() != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
            }
            return TranscriptionResult.failure(TranscriptionResult.ERROR_TRANSCRIPTION_FAILED,
                    "Cloud transcription failed (HTTP " + statusCode + "): " + errorResponse);
        }
    }

    @Override
    public TranscriptionResult transcribeFromUrl(long contentItemId, String sourceUrl, TranscriptionOptions options) throws Exception {
        return TranscriptionResult.failure(TranscriptionResult.ERROR_PROVIDER_UNAVAILABLE,
                "Cloud Whisper requires uploaded audio file");
    }

    public TranscriptionResult parseApiResponse(long contentItemId, String jsonString, long startProcessing) {
        try {
            JSONObject root = new JSONObject(jsonString);
            String fullText = root.optString("text", "").trim();
            String language = root.optString("language", "en");
            double durationSec = root.optDouble("duration", 0.0);
            long durationMs = (long) (durationSec * 1000.0);

            Transcript transcript = new Transcript(contentItemId, fullText, language, PROVIDER_ID, modelName);
            transcript.setDurationMs(durationMs);
            transcript.setCostUsd(estimateCost(durationMs));

            if (root.has("segments")) {
                JSONArray segs = root.getJSONArray("segments");
                for (int i = 0; i < segs.length(); i++) {
                    JSONObject s = segs.getJSONObject(i);
                    double sStart = s.optDouble("start", 0.0);
                    double sEnd = s.optDouble("end", 0.0);
                    String sText = s.optString("text", "").trim();
                    transcript.addSegment(new TranscriptSegment((long) (sStart * 1000.0), (long) (sEnd * 1000.0), sText));
                }
            }

            return TranscriptionResult.success(transcript, System.currentTimeMillis() - startProcessing);
        } catch (Exception e) {
            return TranscriptionResult.failure(TranscriptionResult.ERROR_TRANSCRIPTION_FAILED,
                    "Failed to parse API response: " + e.getMessage());
        }
    }
}
