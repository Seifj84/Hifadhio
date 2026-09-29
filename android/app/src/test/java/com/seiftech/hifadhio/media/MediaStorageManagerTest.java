package com.seiftech.hifadhio.media;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class MediaStorageManagerTest {

    private File testRootDir;

    @Before
    public void setUp() throws Exception {
        testRootDir = new File(System.getProperty("java.io.tmpdir"), "hifadhio_test_media_" + System.currentTimeMillis());
        testRootDir.mkdirs();
    }

    @After
    public void tearDown() {
        deleteRecursive(testRootDir);
    }

    private void deleteRecursive(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteRecursive(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }

    @Test
    public void testComputeSha256OfFile() throws IOException {
        File file = new File(testRootDir, "sample.txt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("Hifadhio Media Pipeline Test".getBytes(StandardCharsets.UTF_8));
        }

        String sha256 = MediaStorageManager.computeSha256(file);
        assertNotNull(sha256);
        assertEquals(64, sha256.length()); // Standard 256-bit hex length

        // Determinism test: same content yields exact same hash
        String sha256Again = MediaStorageManager.computeSha256(file);
        assertEquals(sha256, sha256Again);
    }

    @Test
    public void testHashString() {
        String hash1 = MediaStorageManager.hashString("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertNotNull(hash1);
        assertEquals(64, hash1.length());

        String hash2 = MediaStorageManager.hashString("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertEquals(hash1, hash2);

        String hashDiff = MediaStorageManager.hashString("https://example.com/other");
        assertNotEquals(hash1, hashDiff);
    }

    @Test
    public void testFolderSizeCalculation() throws IOException {
        File subDir = new File(testRootDir, "thumbnails");
        subDir.mkdirs();

        File f1 = new File(subDir, "thumb1.jpg");
        try (FileOutputStream fos = new FileOutputStream(f1)) {
            fos.write(new byte[1024]); // 1 KB
        }

        File f2 = new File(subDir, "thumb2.jpg");
        try (FileOutputStream fos = new FileOutputStream(f2)) {
            fos.write(new byte[2048]); // 2 KB
        }

        long totalSize = MediaStorageManager.getFolderSize(testRootDir);
        assertEquals(3072, totalSize);
    }

    @Test
    public void testMediaArtifactModel() {
        MediaArtifact artifact = new MediaArtifact(
                101,
                MediaArtifact.TYPE_THUMBNAIL,
                "/data/media/item_101.jpg",
                "content://media/item_101.jpg",
                "image/jpeg",
                45000,
                "abc123sha",
                MediaRetentionPolicy.CACHE,
                System.currentTimeMillis() + 60000
        );

        assertEquals(101, artifact.getContentItemId());
        assertEquals(MediaArtifact.TYPE_THUMBNAIL, artifact.getArtifactType());
        assertEquals("/data/media/item_101.jpg", artifact.getStoragePath());
        assertEquals("content://media/item_101.jpg", artifact.getContentUri());
        assertEquals("image/jpeg", artifact.getMimeType());
        assertEquals(45000, artifact.getFileSizeBytes());
        assertEquals("abc123sha", artifact.getSha256());
        assertEquals(MediaRetentionPolicy.CACHE, artifact.getRetentionPolicy());
        assertFalse(artifact.isExpired(System.currentTimeMillis()));
        assertTrue(artifact.isExpired(System.currentTimeMillis() + 100000));
    }
}
