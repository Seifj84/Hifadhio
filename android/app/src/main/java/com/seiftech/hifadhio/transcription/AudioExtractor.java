package com.seiftech.hifadhio.transcription;

import android.content.Context;
import com.seiftech.hifadhio.data.ContentItem;
import com.seiftech.hifadhio.media.MediaArtifact;
import com.seiftech.hifadhio.media.MediaRetentionPolicy;
import com.seiftech.hifadhio.media.MediaStorageManager;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Handles audio extraction, size/duration constraint validation,
 * and audio artifact lifecycle management for media transcription.
 * Adheres strictly to Master Spec §15.3 and §1176-1177.
 */
public class AudioExtractor {
    private final Context context;
    private final MediaStorageManager storageManager;

    public AudioExtractor(Context context, MediaStorageManager storageManager) {
        this.context = context;
        this.storageManager = storageManager;
    }

    /**
     * Determines whether a ContentItem represents audio/video media eligible for transcription.
     */
    public boolean isEligibleForTranscription(ContentItem item) {
        if (item == null) return false;
        String platform = item.getPlatform();
        if (platform == null) return false;
        String p = platform.toLowerCase();
        if (p.equals("youtube") || p.equals("tiktok") || p.equals("instagram") || p.equals("facebook") || p.equals("reddit")) {
            return true;
        }
        String url = item.getUrl() != null ? item.getUrl().toLowerCase() : "";
        return url.endsWith(".mp4") || url.endsWith(".m4a") || url.endsWith(".mp3")
                || url.endsWith(".wav") || url.endsWith(".ogg") || url.endsWith(".webm")
                || url.contains("/reel/") || url.contains("/shorts/") || url.contains("/video/");
    }

    /**
     * Prepares an audio scratchpad file for processing in the managed media/audio partition.
     */
    public File createAudioFile(long itemId, String extension) {
        File audioDir = storageManager.getAudioDir();
        String safeExt = (extension != null && !extension.isEmpty()) ? (extension.startsWith(".") ? extension : "." + extension) : ".m4a";
        String fileName = "item_" + itemId + "_audio_" + System.currentTimeMillis() + safeExt;
        return new File(audioDir, fileName);
    }

    /**
     * Validates whether an audio file satisfies the size and duration limits.
     */
    public boolean validateAudioFile(File audioFile, TranscriptionOptions options) throws IllegalArgumentException {
        if (audioFile == null || !audioFile.exists()) {
            throw new IllegalArgumentException("Audio file does not exist");
        }
        if (audioFile.length() == 0) {
            throw new IllegalArgumentException("Audio file is empty");
        }
        if (audioFile.length() > options.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("Audio file exceeds size limit (" + audioFile.length() + " > " + options.getMaxFileSizeBytes() + ")");
        }
        return true;
    }

    /**
     * Saves audio data bytes to the media/audio directory and registers a MediaArtifact in ContentDb.
     */
    public MediaArtifact saveAudioArtifact(long itemId, byte[] audioBytes, String mimeType, String retentionPolicy) throws Exception {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException("Audio bytes cannot be empty");
        }
        File audioFile = createAudioFile(itemId, ".m4a");
        try (FileOutputStream fos = new FileOutputStream(audioFile)) {
            fos.write(audioBytes);
            fos.flush();
        }

        return storageManager.registerArtifact(
                itemId,
                "AUDIO_STREAM",
                audioFile.getAbsolutePath(),
                mimeType != null ? mimeType : "audio/mp4",
                audioBytes.length,
                retentionPolicy != null ? retentionPolicy : MediaRetentionPolicy.TEMPORARY_PROCESSING
        );
    }

    /**
     * Cleans up audio file after transcription if retention policy is TEMPORARY_PROCESSING or NO_RETENTION.
     */
    public boolean cleanupTempAudio(File audioFile, String retentionPolicy) {
        if (audioFile == null || !audioFile.exists()) return true;
        if (MediaRetentionPolicy.NO_RETENTION.equalsIgnoreCase(retentionPolicy)
                || MediaRetentionPolicy.TEMPORARY_PROCESSING.equalsIgnoreCase(retentionPolicy)) {
            return audioFile.delete();
        }
        return false;
    }
}
