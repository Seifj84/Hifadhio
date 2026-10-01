package com.seiftech.hifadhio.media;

/**
 * Domain model representing a managed media artifact stored on disk (Master Spec Section 13.1).
 */
public class MediaArtifact {
    public static final String TYPE_THUMBNAIL = "thumbnail";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_VIDEO_SAMPLE = "video_sample";
    public static final String TYPE_TRANSCRIPT = "transcript";
    public static final String TYPE_OCR = "ocr";
    public static final String TYPE_SUMMARY = "summary";

    private long id;
    private long contentItemId;
    private String artifactType;
    private String storagePath;
    private String contentUri;
    private String mimeType;
    private long fileSizeBytes;
    private String sha256;
    private String retentionPolicy;
    private long expiresAt;
    private long createdAt;

    public MediaArtifact() {
        this.retentionPolicy = MediaRetentionPolicy.CACHE;
        this.createdAt = System.currentTimeMillis();
    }

    public MediaArtifact(long contentItemId, String artifactType, String storagePath,
                         String contentUri, String mimeType, long fileSizeBytes,
                         String sha256, String retentionPolicy, long expiresAt) {
        this.contentItemId = contentItemId;
        this.artifactType = artifactType;
        this.storagePath = storagePath;
        this.contentUri = contentUri;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256 = sha256;
        this.retentionPolicy = retentionPolicy != null ? retentionPolicy : MediaRetentionPolicy.CACHE;
        this.expiresAt = expiresAt;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getContentItemId() { return contentItemId; }
    public void setContentItemId(long contentItemId) { this.contentItemId = contentItemId; }

    public String getArtifactType() { return artifactType; }
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getContentUri() { return contentUri; }
    public void setContentUri(String contentUri) { this.contentUri = contentUri; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getRetentionPolicy() { return retentionPolicy; }
    public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isExpired(long now) {
        return expiresAt > 0 && expiresAt <= now;
    }
}
