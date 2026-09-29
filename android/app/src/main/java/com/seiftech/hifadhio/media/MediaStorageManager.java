package com.seiftech.hifadhio.media;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.FileProvider;
import com.seiftech.hifadhio.adapter.HttpFetchHelper;
import com.seiftech.hifadhio.data.ContentDb;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Controlled object storage manager for media artifacts, thumbnails, and working files
 * (Master Spec Section 15.2, Section 19.1, and Phase 07).
 *
 * Guarantees that:
 * 1. Media is stored strictly in private sandboxed storage, never as SQLite BLOBs.
 * 2. Deterministic, collision-free object naming is enforced.
 * 3. Scoped URI access is provided via FileProvider.
 * 4. Item deletion safely cascades to eliminate disk files.
 */
public class MediaStorageManager {
    private static final String TAG = "MediaStorageManager";
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 5 MB max thumbnail size
    private static final int TIMEOUT_CONNECT_MS = 8000;
    private static final int TIMEOUT_READ_MS = 10000;

    private static volatile MediaStorageManager instance;
    private final Context context;
    private final File baseMediaDir;
    private final File thumbnailsDir;
    private final File tempDir;
    private final File audioDir;
    private final File artifactsDir;

    private MediaStorageManager(Context context) {
        this.context = context.getApplicationContext();
        this.baseMediaDir = new File(this.context.getFilesDir(), "media");
        this.thumbnailsDir = new File(baseMediaDir, "thumbnails");
        this.tempDir = new File(baseMediaDir, "temp");
        this.audioDir = new File(baseMediaDir, "audio");
        this.artifactsDir = new File(baseMediaDir, "artifacts");

        ensureDirectories();
    }

    public static MediaStorageManager getInstance(Context context) {
        if (instance == null) {
            synchronized (MediaStorageManager.class) {
                if (instance == null) {
                    instance = new MediaStorageManager(context);
                }
            }
        }
        return instance;
    }

    private void ensureDirectories() {
        if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs();
        if (!tempDir.exists()) tempDir.mkdirs();
        if (!audioDir.exists()) audioDir.mkdirs();
        if (!artifactsDir.exists()) artifactsDir.mkdirs();
    }

    public File getBaseMediaDir() { return baseMediaDir; }
    public File getThumbnailsDir() { return thumbnailsDir; }
    public File getTempDir() { return tempDir; }
    public File getAudioDir() { return audioDir; }
    public File getArtifactsDir() { return artifactsDir; }

    /**
     * Downloads and caches a remote thumbnail into private object storage.
     * Records the artifact into SQLite with SHA256 integrity hash.
     */
    public MediaArtifact cacheThumbnail(long itemId, String remoteUrl, ContentDb db) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty() || itemId <= 0) {
            return null;
        }

        // Check if valid cached thumbnail artifact already exists
        if (db != null) {
            MediaArtifact existing = db.getArtifact(itemId, MediaArtifact.TYPE_THUMBNAIL);
            if (existing != null && existing.getStoragePath() != null) {
                File f = new File(existing.getStoragePath());
                if (f.exists() && f.length() > 0) {
                    return existing;
                }
            }
        }

        String safeUrl = HttpFetchHelper.upgradeHttpToHttps(remoteUrl.trim());
        String urlHash = hashString(safeUrl).substring(0, 12);
        String filename = String.format(Locale.US, "item_%d_thumb_%s.jpg", itemId, urlHash);
        File targetFile = new File(thumbnailsDir, filename);

        File tempDownload = new File(tempDir, "temp_" + System.currentTimeMillis() + "_" + filename);

        try {
            URL url = new URL(safeUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", HttpFetchHelper.DEFAULT_USER_AGENT);
            conn.setRequestProperty("Accept", "image/webp,image/jpeg,image/png,*/*");
            conn.setConnectTimeout(TIMEOUT_CONNECT_MS);
            conn.setReadTimeout(TIMEOUT_READ_MS);
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                conn.disconnect();
                Log.w(TAG, "Failed to download thumbnail, HTTP " + status + " for " + safeUrl);
                return null;
            }

            String contentType = conn.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "image/jpeg";
            }

            InputStream is = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(tempDownload);
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                totalBytes += read;
                if (totalBytes > MAX_IMAGE_BYTES) {
                    fos.close();
                    is.close();
                    conn.disconnect();
                    tempDownload.delete();
                    Log.w(TAG, "Thumbnail exceeds maximum allowable size (> 5MB): " + safeUrl);
                    return null;
                }
            }
            fos.flush();
            fos.close();
            is.close();
            conn.disconnect();

            if (totalBytes == 0) {
                tempDownload.delete();
                return null;
            }

            // Move temp file to final location
            if (targetFile.exists()) targetFile.delete();
            if (!tempDownload.renameTo(targetFile)) {
                // Fallback copy if renameTo fails
                copyFile(tempDownload, targetFile);
                tempDownload.delete();
            }

            String sha256 = computeSha256(targetFile);
            long fileSizeBytes = targetFile.length();
            String contentUri = getSafeContentUri(targetFile);

            MediaArtifact artifact = new MediaArtifact(
                    itemId,
                    MediaArtifact.TYPE_THUMBNAIL,
                    targetFile.getAbsolutePath(),
                    contentUri,
                    contentType,
                    fileSizeBytes,
                    sha256,
                    MediaRetentionPolicy.CACHE,
                    0 // Thumbnails do not expire by default; pruned on LRU/quota
            );

            if (db != null) {
                db.insertArtifact(artifact);
            }

            Log.i(TAG, String.format(Locale.US, "Cached thumbnail for Item #%d (%d bytes, SHA256: %s)",
                    itemId, fileSizeBytes, sha256.substring(0, 10)));
            return artifact;

        } catch (Exception e) {
            Log.w(TAG, "Exception caching thumbnail: " + e.getMessage());
            if (tempDownload.exists()) tempDownload.delete();
            return null;
        }
    }

    /**
     * Returns local cached thumbnail File if available on disk.
     */
    public File getThumbnailFile(long itemId, ContentDb db) {
        if (itemId <= 0 || db == null) return null;
        MediaArtifact artifact = db.getArtifact(itemId, MediaArtifact.TYPE_THUMBNAIL);
        if (artifact != null && artifact.getStoragePath() != null) {
            File file = new File(artifact.getStoragePath());
            if (file.exists() && file.length() > 0) {
                return file;
            }
        }
        return null;
    }

    /**
     * Creates a unique temporary working file in media/temp/ with collision-free naming.
     */
    public File createTempWorkingFile(long itemId, String suffix) {
        String safeSuffix = (suffix != null && !suffix.isEmpty()) ? (suffix.startsWith(".") ? suffix : "." + suffix) : ".tmp";
        String name = String.format(Locale.US, "work_item_%d_%d%s", itemId, System.currentTimeMillis(), safeSuffix);
        return new File(tempDir, name);
    }

    /**
     * Generates a safe, scoped FileProvider URI for external view/sharing.
     */
    public String getSafeContentUri(File file) {
        if (file == null || !file.exists()) return null;
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            return uri.toString();
        } catch (Exception e) {
            return Uri.fromFile(file).toString();
        }
    }

    /**
     * Cascades item deletion to remove all associated media files from storage.
     */
    public void deleteItemMedia(long itemId, ContentDb db) {
        if (itemId <= 0 || db == null) return;
        try {
            for (MediaArtifact art : db.getArtifactsForItem(itemId)) {
                if (art.getStoragePath() != null) {
                    File f = new File(art.getStoragePath());
                    if (f.exists()) {
                        f.delete();
                    }
                }
            }
            db.deleteArtifactsForItem(itemId);
        } catch (Exception e) {
            Log.w(TAG, "Error deleting media for item " + itemId + ": " + e.getMessage());
        }
    }

    /**
     * Computes hexadecimal SHA256 checksum of a file.
     */
    public static String computeSha256(File file) {
        if (file == null || !file.exists()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            fis.close();
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String hashString(String input) {
        if (input == null) return "000000000000";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * Registers a generic media file as a managed artifact.
     */
    public MediaArtifact registerArtifact(long itemId, String artifactType, String storagePath,
                                         String mimeType, long fileSizeBytes, String retentionPolicy) {
        File file = new File(storagePath);
        String sha256 = computeSha256(file);
        String contentUri = getSafeContentUri(file);
        long expiresAt = 0L;
        if (MediaRetentionPolicy.TEMPORARY_PROCESSING.equalsIgnoreCase(retentionPolicy)) {
            expiresAt = System.currentTimeMillis() + (60 * 60 * 1000L); // 1 hour
        } else if (MediaRetentionPolicy.CACHE.equalsIgnoreCase(retentionPolicy)) {
            expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L); // 30 days
        }
        MediaArtifact artifact = new MediaArtifact(
                itemId,
                artifactType,
                storagePath,
                contentUri,
                mimeType,
                fileSizeBytes,
                sha256,
                retentionPolicy,
                expiresAt
        );
        ContentDb db = new ContentDb(context);
        try {
            long id = db.insertArtifact(artifact);
            artifact.setId(id);
        } finally {
            db.close();
        }
        return artifact;
    }

    /**
     * Persists a transcript as an immutable text artifact in media/artifacts/ with SHA256 integrity.
     */
    public MediaArtifact saveTranscriptArtifact(long itemId, String transcriptText, ContentDb db) {
        if (transcriptText == null || transcriptText.trim().isEmpty() || itemId <= 0) {
            return null;
        }
        try {
            String text = transcriptText.trim();
            byte[] bytes = text.getBytes("UTF-8");
            String textHash = hashString(text).substring(0, 10);
            String filename = String.format(Locale.US, "item_%d_transcript_%s.txt", itemId, textHash);
            File targetFile = new File(artifactsDir, filename);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(bytes);
                fos.flush();
            }

            String sha256 = computeSha256(targetFile);
            String contentUri = getSafeContentUri(targetFile);
            MediaArtifact artifact = new MediaArtifact(
                    itemId,
                    MediaArtifact.TYPE_TRANSCRIPT,
                    targetFile.getAbsolutePath(),
                    contentUri,
                    "text/plain",
                    bytes.length,
                    sha256,
                    MediaRetentionPolicy.LONG_TERM,
                    0L
            );
            if (db != null) {
                long id = db.insertArtifact(artifact);
                artifact.setId(id);
            }
            return artifact;
        } catch (Exception e) {
            Log.w(TAG, "Failed to save transcript artifact: " + e.getMessage());
            return null;
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public long getTotalMediaStorageBytes() {
        return getFolderSize(baseMediaDir);
    }

    public long getThumbnailsStorageBytes() {
        return getFolderSize(thumbnailsDir);
    }

    public static long getFolderSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    size += getFolderSize(f);
                } else {
                    size += f.length();
                }
            }
        }
        return size;
    }
}
