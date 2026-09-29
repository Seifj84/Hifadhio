package com.seiftech.hifadhio.adapter;

public class ExtractedMetadata {
    private String title;
    private String originalTitle;
    private String description;
    private String thumbnailUrl;
    private String creatorName;
    private String creatorHandle;
    private String canonicalUrl;
    private String platform;
    private String contentType; // e.g. "article", "video", "website"
    private String rawJson;
    private long extractedAt;

    public ExtractedMetadata() {
        this.extractedAt = System.currentTimeMillis();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorHandle() {
        return creatorHandle;
    }

    public void setCreatorHandle(String creatorHandle) {
        this.creatorHandle = creatorHandle;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public long getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(long extractedAt) {
        this.extractedAt = extractedAt;
    }
}
