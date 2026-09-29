package com.seiftech.hifadhio.media;

import org.junit.Test;
import static org.junit.Assert.*;

public class MediaCleanupWorkerTest {

    @Test
    public void testCleanupReportFormatting() {
        MediaCleanupWorker.CleanupReport report = new MediaCleanupWorker.CleanupReport();
        report.bytesFreed = 1048576; // 1 MB
        report.expiredArtifactsDeleted = 5;
        report.orphanedFilesDeleted = 3;
        report.staleTempFilesDeleted = 2;
        report.quotaFilesDeleted = 4;

        String str = report.toString();
        assertNotNull(str);
        assertTrue(str.contains("1048576 bytes"));
        assertTrue(str.contains("Expired: 5"));
        assertTrue(str.contains("Orphans: 3"));
        assertTrue(str.contains("Stale Temp: 2"));
        assertTrue(str.contains("Quota Pruned: 4"));
    }

    @Test
    public void testMediaRetentionPolicies() {
        assertEquals("NO_RETENTION", MediaRetentionPolicy.NO_RETENTION);
        assertEquals("TEMPORARY_PROCESSING", MediaRetentionPolicy.TEMPORARY_PROCESSING);
        assertEquals("CACHE", MediaRetentionPolicy.CACHE);
        assertEquals("LONG_TERM", MediaRetentionPolicy.LONG_TERM);
    }
}
