package com.seiftech.hifadhio.data;

import java.io.Serializable;

public class ContentItem implements Serializable {
    private long id;
    private String url = "";
    private String canonicalUrl = "";
    private String platform = "Web";
    private String title = "";
    private String originalTitle = "";
    private String caption = "";
    private String thumbnailUrl = "";
    private String notes = "";
    private String collectionName = "Inbox";
    private String tags = "";
    private boolean isFavorite = false;
    private String status = "SAVED";
    private long savedAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public ContentItem() {}

    public ContentItem(String url) {
        this(url, "Web");
    }

    public ContentItem(String url, String platform) {
        this.url = url;
        this.platform = platform;
        this.savedAt = System.currentTimeMillis();
        this.updatedAt = this.savedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUrl() { return url == null ? "" : url; }
    public void setUrl(String url) { this.url = url; }

    public String getCanonicalUrl() { return canonicalUrl == null ? "" : canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }

    public String getPlatform() { return platform == null ? "Web" : platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getTitle() { return title == null ? "" : title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalTitle() { return originalTitle == null ? "" : originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }

    public String getCaption() { return caption == null ? "" : caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getThumbnailUrl() { return thumbnailUrl == null ? "" : thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getNotes() { return notes == null ? "" : notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCollectionName() { return (collectionName == null || collectionName.trim().isEmpty()) ? "Inbox" : collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }

    public String getTags() { return tags == null ? "" : tags; }
    public void setTags(String tags) { this.tags = tags; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getStatus() { return status == null ? "SAVED" : status; }
    public void setStatus(String status) { this.status = status; }

    public long getSavedAt() { return savedAt; }
    public void setSavedAt(long savedAt) { this.savedAt = savedAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        if (originalTitle != null && !originalTitle.trim().isEmpty()) {
            return originalTitle.trim();
        }
        return getPlatformTypeFallback();
    }

    public String getPlatformTypeFallback() {
        String lowerUrl = url != null ? url.toLowerCase(java.util.Locale.ROOT) : "";
        String p = getPlatform();

        if ("Facebook".equalsIgnoreCase(p)) {
            if (lowerUrl.contains("/reel/") || lowerUrl.contains("/reels/") || lowerUrl.contains("/r/")) {
                return "Facebook Reel";
            } else if (lowerUrl.contains("/watch/") || lowerUrl.contains("fb.watch")) {
                return "Facebook video";
            } else if (lowerUrl.contains("/posts/") || lowerUrl.contains("/post/") || lowerUrl.contains("/photo/")) {
                return "Facebook post";
            }
            return "Facebook link";
        } else if ("Instagram".equalsIgnoreCase(p)) {
            if (lowerUrl.contains("/reel/") || lowerUrl.contains("/reels/")) {
                return "Instagram Reel";
            } else if (lowerUrl.contains("/p/")) {
                return "Instagram post";
            }
            return "Instagram link";
        } else if ("YouTube".equalsIgnoreCase(p)) {
            if (lowerUrl.contains("/shorts/")) {
                return "YouTube Short";
            } else if (lowerUrl.contains("/watch") || lowerUrl.contains("youtu.be")) {
                return "YouTube video";
            }
            return "YouTube link";
        } else if ("TikTok".equalsIgnoreCase(p)) {
            if (lowerUrl.contains("/video/") || lowerUrl.contains("/v/")) {
                return "TikTok video";
            }
            return "TikTok link";
        } else if ("X".equalsIgnoreCase(p)) {
            if (lowerUrl.contains("/status/")) {
                return "X post";
            }
            return "X link";
        }

        String domain = getDomainPreview();
        if (!domain.isEmpty()) {
            return domain + " link";
        }
        return "Saved link";
    }

    public String getDomainPreview() {
        if (url == null || url.trim().isEmpty()) return "";
        try {
            java.net.URI uri = new java.net.URI(url.trim());
            String host = uri.getHost();
            if (host != null) {
                if (host.startsWith("www.")) host = host.substring(4);
                return host.toLowerCase(java.util.Locale.ROOT);
            }
        } catch (Exception ignored) {}

        String clean = url.replace("https://", "").replace("http://", "").replace("www.", "");
        int slash = clean.indexOf("/");
        if (slash > 0) {
            clean = clean.substring(0, slash);
        }
        return clean.toLowerCase(java.util.Locale.ROOT);
    }
}
