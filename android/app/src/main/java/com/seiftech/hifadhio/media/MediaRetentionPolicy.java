package com.seiftech.hifadhio.media;

/**
 * Retention policies for controlled media artifacts as defined in Master Spec Section 15.2.
 */
public class MediaRetentionPolicy {
    /**
     * Do not retain media on disk. Discarded immediately after processing stream.
     */
    public static final String NO_RETENTION = "NO_RETENTION";

    /**
     * Retained temporarily for processing pipeline (e.g. video file while extracting audio/frames).
     * Deleted automatically upon job completion or failure.
     */
    public static final String TEMPORARY_PROCESSING = "TEMPORARY_PROCESSING";

    /**
     * Cached media (e.g. thumbnails) retained with quota and LRU cache management.
     */
    public static final String CACHE = "CACHE";

    /**
     * User-selected long-term retention when permitted by platform policy.
     */
    public static final String LONG_TERM = "LONG_TERM";
}
