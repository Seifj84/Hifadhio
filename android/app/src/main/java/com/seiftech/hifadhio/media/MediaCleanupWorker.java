package com.seiftech.hifadhio.media;

import android.content.Context;
import android.util.Log;
import com.seiftech.hifadhio.data.ContentDb;
import com.seiftech.hifadhio.data.ProcessingEvent;

import java.io.File;
import java.util.*;

/**
 * Automated cleanup worker enforcing retention lifecycles, orphaned file sweeps,
 * and thumbnail cache storage quotas (Master Spec Phase 07, Sections 15.2 & 1165).
 */
public class MediaCleanupWorker {
    private static final String TAG = "MediaCleanupWorker";
    public static final long DEFAULT_CACHE_QUOTA_BYTES = 50 * 1024 * 1024; // 50 MB
    public static final long STALE_TEMP_THRESHOLD_MS = 60 * 60 * 1000; // 1 Hour

    public static class CleanupReport {
        public int expiredArtifactsDeleted = 0;
        public int orphanedFilesDeleted = 0;
        public int staleTempFilesDeleted = 0;
        public int quotaFilesDeleted = 0;
        public long bytesFreed = 0;

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "Freed %d bytes | Expired: %d, Orphans: %d, Stale Temp: %d, Quota Pruned: %d",
                    bytesFreed, expiredArtifactsDeleted, orphanedFilesDeleted, staleTempFilesDeleted, quotaFilesDeleted);
        }
    }

    private final Context context;
    private final MediaStorageManager storageManager;
    private final ContentDb db;
    private final long cacheQuotaBytes;

    public MediaCleanupWorker(Context context, MediaStorageManager storageManager, ContentDb db) {
        this(context, storageManager, db, DEFAULT_CACHE_QUOTA_BYTES);
    }

    public MediaCleanupWorker(Context context, MediaStorageManager storageManager, ContentDb db, long cacheQuotaBytes) {
        this.context = context.getApplicationContext();
        this.storageManager = storageManager != null ? storageManager : MediaStorageManager.getInstance(context);
        this.db = db;
        this.cacheQuotaBytes = cacheQuotaBytes;
    }

    /**
     * Executes a full maintenance sweep:
     * 1. Purges expired artifacts.
     * 2. Cleans stale working files in media/temp/.
     * 3. Sweeps orphaned storage objects on disk.
     * 4. Enforces thumbnail cache storage quota.
     */
    public CleanupReport runCleanupSweep() {
        CleanupReport report = new CleanupReport();
        long now = System.currentTimeMillis();

        // 1. Purge expired artifacts
        if (db != null) {
            List<MediaArtifact> expired = db.getExpiredArtifacts(now);
            for (MediaArtifact art : expired) {
                if (art.getStoragePath() != null) {
                    File f = new File(art.getStoragePath());
                    if (f.exists()) {
                        long size = f.length();
                        if (f.delete()) {
                            report.bytesFreed += size;
                        }
                    }
                }
                db.deleteArtifact(art.getId());
                report.expiredArtifactsDeleted++;
            }
        }

        // 2. Clean stale temporary working files in media/temp/
        File tempDir = storageManager.getTempDir();
        if (tempDir.exists() && tempDir.isDirectory()) {
            File[] tempFiles = tempDir.listFiles();
            if (tempFiles != null) {
                for (File tempFile : tempFiles) {
                    if (now - tempFile.lastModified() > STALE_TEMP_THRESHOLD_MS) {
                        long size = tempFile.length();
                        if (tempFile.delete()) {
                            report.bytesFreed += size;
                            report.staleTempFilesDeleted++;
                        }
                    }
                }
            }
        }

        // 3. Sweep orphaned storage files on disk (files not in db)
        if (db != null) {
            Set<String> trackedPaths = new HashSet<>(db.getAllArtifactStoragePaths());
            sweepDirectoryForOrphans(storageManager.getThumbnailsDir(), trackedPaths, report);
            sweepDirectoryForOrphans(storageManager.getAudioDir(), trackedPaths, report);
            sweepDirectoryForOrphans(storageManager.getArtifactsDir(), trackedPaths, report);
        }

        // 4. Enforce cache quota on thumbnails
        enforceCacheQuota(report);

        // Record cleanup event in audit timeline
        if (db != null && report.bytesFreed > 0) {
            try {
                db.recordEvent(new ProcessingEvent(
                        0,
                        0,
                        "MEDIA_CLEANUP",
                        "MAINTENANCE",
                        "COMPLETED",
                        report.toString()
                ));
            } catch (Exception ignored) {}
        }

        Log.i(TAG, "Cleanup completed: " + report);
        return report;
    }

    private void sweepDirectoryForOrphans(File dir, Set<String> trackedPaths, CleanupReport report) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                String path = file.getAbsolutePath();
                if (!trackedPaths.contains(path)) {
                    long size = file.length();
                    if (file.delete()) {
                        report.bytesFreed += size;
                        report.orphanedFilesDeleted++;
                    }
                }
            }
        }
    }

    private void enforceCacheQuota(CleanupReport report) {
        File thumbsDir = storageManager.getThumbnailsDir();
        if (!thumbsDir.exists()) return;

        long currentSize = MediaStorageManager.getFolderSize(thumbsDir);
        if (currentSize <= cacheQuotaBytes) {
            return;
        }

        File[] files = thumbsDir.listFiles();
        if (files == null || files.length == 0) return;

        // Sort files by lastModified ascending (oldest first)
        List<File> sortedFiles = new ArrayList<>(Arrays.asList(files));
        sortedFiles.sort(Comparator.comparingLong(File::lastModified));

        long targetSize = (long) (cacheQuotaBytes * 0.8); // Prune down to 80% of quota
        for (File file : sortedFiles) {
            if (currentSize <= targetSize) break;
            long size = file.length();
            String path = file.getAbsolutePath();
            if (file.delete()) {
                currentSize -= size;
                report.bytesFreed += size;
                report.quotaFilesDeleted++;
                // Clean from db if present
                if (db != null) {
                    try {
                        db.getWritableDatabase().delete(ContentDb.TABLE_ARTIFACTS, "storage_path=?", new String[]{path});
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
