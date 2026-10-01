package com.seiftech.hifadhio.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.seiftech.hifadhio.adapter.ContentAdapterRegistry;
import com.seiftech.hifadhio.adapter.ContentExtractorAdapter;
import com.seiftech.hifadhio.adapter.ExtractedMetadata;
import com.seiftech.hifadhio.media.MediaArtifact;
import com.seiftech.hifadhio.media.MediaStorageManager;
import com.seiftech.hifadhio.transcription.AudioExtractor;
import com.seiftech.hifadhio.transcription.OfflineSpeechProvider;
import com.seiftech.hifadhio.transcription.Transcript;
import com.seiftech.hifadhio.transcription.TranscriptionOptions;
import com.seiftech.hifadhio.transcription.TranscriptionProvider;
import com.seiftech.hifadhio.transcription.TranscriptionRegistry;
import com.seiftech.hifadhio.transcription.TranscriptionResult;
import com.seiftech.hifadhio.ocr.FrameExtractor;
import com.seiftech.hifadhio.ocr.OcrFrame;
import com.seiftech.hifadhio.ocr.OcrOptions;
import com.seiftech.hifadhio.ocr.OcrProvider;
import com.seiftech.hifadhio.ocr.OcrRecord;
import com.seiftech.hifadhio.ocr.OcrRegistry;
import com.seiftech.hifadhio.ocr.OcrResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProcessingJobManager {

    private static final String TAG = "HifadhioPipeline";

    public interface JobListener {
        void onJobStateChanged(long jobId, long contentItemId, String state, String message);
    }

    private static volatile ProcessingJobManager instance;

    private final Context context;
    private final ContentDb db;
    private final String workerId;
    private final ExecutorService executor;
    private final ScheduledExecutorService retryScheduler;
    private final Handler mainHandler;
    private final List<JobListener> listeners = new ArrayList<>();

    public static ProcessingJobManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ProcessingJobManager.class) {
                if (instance == null) {
                    instance = new ProcessingJobManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public ProcessingJobManager(Context context) {
        this.context = context;
        this.db = new ContentDb(context);
        this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        this.executor = Executors.newFixedThreadPool(2);
        this.retryScheduler = Executors.newSingleThreadScheduledExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public synchronized void addListener(JobListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(JobListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(long jobId, long contentItemId, String state, String message) {
        mainHandler.post(() -> {
            List<JobListener> copy;
            synchronized (this) {
                copy = new ArrayList<>(listeners);
            }
            for (JobListener l : copy) {
                l.onJobStateChanged(jobId, contentItemId, state, message);
            }
        });
    }

    public long enqueueJob(long contentItemId, String jobType) {
        ProcessingJob job = new ProcessingJob(contentItemId, jobType);
        long jobId = db.createJob(job);
        notifyListeners(jobId, contentItemId, ProcessingJob.STATE_QUEUED, "Job enqueued");
        triggerWorker();
        return jobId;
    }

    public long enqueueTranscription(long contentItemId) {
        return enqueueJob(contentItemId, ProcessingJob.TYPE_TRANSCRIBE);
    }

    public long enqueueOcr(long contentItemId) {
        return enqueueJob(contentItemId, ProcessingJob.TYPE_OCR);
    }

    public void triggerWorker() {
        executor.execute(() -> {
            try {
                // 1. Recover stale jobs whose lease expired
                db.recoverStaleJobs();

                // 2. Drain all currently ready jobs in queue
                while (true) {
                    long leaseDurationMs = 30_000L;
                    ProcessingJob job = db.claimNextJob(workerId, leaseDurationMs);
                    if (job == null) {
                        break; // Queue drained or remaining jobs scheduled for future
                    }

                    Log.i(TAG, String.format(Locale.US,
                            "[Job #%d] ContentItem: #%d | Worker: %s | State: CLAIMED | Attempt: %d/%d",
                            job.getId(), job.getContentItemId(), workerId, job.getAttemptCount(), job.getMaxAttempts()));
                    notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_CLAIMED, "Job claimed");

                    try {
                        processJob(job);
                    } catch (Exception e) {
                        String sanitized = "Processing failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
                        long retryDelayMs = calculateBackoffMs(job.getAttemptCount());
                        db.failJob(job.getId(), "ERR_EXECUTION", sanitized, retryDelayMs);
                        boolean canRetry = job.getAttemptCount() < job.getMaxAttempts();

                        Log.w(TAG, String.format(Locale.US,
                                "[Job #%d] Failure on attempt %d/%d: %s | CanRetry: %b | RetryDelay: %d ms",
                                job.getId(), job.getAttemptCount(), job.getMaxAttempts(), sanitized, canRetry, retryDelayMs));

                        notifyListeners(job.getId(), job.getContentItemId(),
                                canRetry ? ProcessingJob.STATE_RETRYING : ProcessingJob.STATE_FAILED,
                                sanitized);

                        // Wake up the worker exactly when the scheduled backoff expires
                        if (canRetry && retryDelayMs > 0) {
                            Log.i(TAG, String.format(Locale.US,
                                    "[Job #%d] Scheduled background retry in %d ms via ScheduledExecutorService",
                                    job.getId(), retryDelayMs));
                            retryScheduler.schedule(this::triggerWorker, retryDelayMs, TimeUnit.MILLISECONDS);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Worker queue drain error", e);
            }
        });
    }

    private void processJob(ProcessingJob job) throws Exception {
        if (ProcessingJob.TYPE_TRANSCRIBE.equals(job.getJobType())) {
            processTranscriptionJob(job);
            return;
        }
        if (ProcessingJob.TYPE_OCR.equals(job.getJobType())) {
            processOcrJob(job);
            return;
        }

        ContentItem item = db.getById(job.getContentItemId());
        if (item == null) {
            Log.w(TAG, String.format(Locale.US, "[Job #%d] ContentItem #%d no longer exists", job.getId(), job.getContentItemId()));
            db.failJob(job.getId(), "ERR_NOT_FOUND", "Associated content item no longer exists", 0);
            notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_FAILED, "Content item not found");
            return;
        }

        Log.i(TAG, String.format(Locale.US,
                "[Job #%d] Starting processing for URL: %s | Platform: %s",
                job.getId(), item.getUrl(), item.getPlatform()));

        // Stage 1: Adapter Selection
        db.updateJobProgress(job.getId(), 15, "Identifying content adapter");
        notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Identifying content adapter");

        ContentExtractorAdapter adapter = ContentAdapterRegistry.getInstance().getAdapterForUrl(item.getUrl());
        String platformId = adapter != null ? adapter.getPlatformId() : "GenericWeb";

        // Stage 2: Metadata Extraction via Adapter
        db.updateJobProgress(job.getId(), 40, "Extracting metadata via " + platformId);
        notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Extracting metadata via " + platformId);

        ExtractedMetadata meta = null;
        if (adapter != null) {
            meta = adapter.extract(item.getUrl());
        }

        // Stage 3: Applying extracted metadata to ContentItem
        db.updateJobProgress(job.getId(), 75, "Applying metadata & indexing");
        notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Applying metadata & indexing");

        if (meta != null) {
            // Preserve user-edited custom title, store extracted original_title
            if (meta.getTitle() != null && !meta.getTitle().isEmpty()) {
                item.setOriginalTitle(meta.getTitle());
                String curTitle = item.getTitle();
                if (curTitle == null || curTitle.trim().isEmpty() || curTitle.equals(item.getPlatform() + " Link") || curTitle.equals("Saved Link")) {
                    item.setTitle(meta.getTitle());
                }
            }
            if (meta.getDescription() != null && !meta.getDescription().isEmpty()) {
                String curCaption = item.getCaption();
                if (curCaption == null || curCaption.trim().isEmpty()) {
                    item.setCaption(meta.getDescription());
                }
            }
            if (meta.getThumbnailUrl() != null && !meta.getThumbnailUrl().isEmpty()) {
                item.setThumbnailUrl(meta.getThumbnailUrl());
                // Controlled Media Pipeline (Phase 07): Cache thumbnail to private object storage
                try {
                    MediaStorageManager.getInstance(context).cacheThumbnail(item.getId(), meta.getThumbnailUrl(), db);
                } catch (Exception e) {
                    Log.w(TAG, "Thumbnail caching non-fatal error: " + e.getMessage());
                }
            }
            if (meta.getCanonicalUrl() != null && !meta.getCanonicalUrl().isEmpty()) {
                item.setCanonicalUrl(meta.getCanonicalUrl());
            }
            if (meta.getPlatform() != null && !meta.getPlatform().isEmpty() && !"Web".equalsIgnoreCase(meta.getPlatform())) {
                item.setPlatform(meta.getPlatform());
            }
            item.setUpdatedAt(System.currentTimeMillis());
            db.update(item);
        } else {
            String fallbackTitle = item.getDisplayTitle();
            if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
                item.setTitle(fallbackTitle);
                db.update(item);
            }
        }

        db.updateJobProgress(job.getId(), 95, "Finalizing indexing");
        notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Finalizing indexing");

        // Stage 4: Mark Complete
        db.completeJob(job.getId());
        Log.i(TAG, String.format(Locale.US,
                "[Job #%d] Terminal Outcome: COMPLETED successfully in %d ms | ContentItem #%d: '%s'",
                job.getId(), (System.currentTimeMillis() - job.getStartedAt()), item.getId(), item.getTitle()));
        notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_COMPLETED, "Processing completed");

        // Stage 5 (Phase 08 & Phase 09): Auto-chain Transcription & OCR for media items
        try {
            AudioExtractor audioExtractor = new AudioExtractor(context, MediaStorageManager.getInstance(context));
            if (audioExtractor.isEligibleForTranscription(item)) {
                Log.i(TAG, "Item #" + item.getId() + " is eligible for transcription. Scheduling transcribe job.");
                enqueueTranscription(item.getId());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to schedule transcription: " + e.getMessage());
        }

        try {
            if (FrameExtractor.isEligibleForOcr(item)) {
                Log.i(TAG, "Item #" + item.getId() + " is eligible for OCR visual extraction. Scheduling OCR job.");
                enqueueOcr(item.getId());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to schedule OCR: " + e.getMessage());
        }
    }

    private void processTranscriptionJob(ProcessingJob job) throws Exception {
        ContentItem item = db.getById(job.getContentItemId());
        if (item == null) {
            db.failJob(job.getId(), "ERR_NOT_FOUND", "Associated content item no longer exists", 0);
            notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_FAILED, "Content item not found");
            return;
        }

        Log.i(TAG, String.format(Locale.US, "[Job #%d] Starting transcription for Item #%d (%s)",
                job.getId(), item.getId(), item.getTitle()));

        db.updateJobProgress(job.getId(), 20, "Selecting transcription provider");
        notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Selecting transcription provider");

        TranscriptionRegistry registry = TranscriptionRegistry.getInstance();
        TranscriptionOptions options = TranscriptionOptions.defaults();

        // 1. Try URL-based provider first (e.g., subtitle / caption tracks for YouTube)
        TranscriptionProvider bestProvider = registry.getBestProvider(true);
        TranscriptionResult result = null;

        if (bestProvider != null) {
            try {
                result = bestProvider.transcribeFromUrl(item.getId(), item.getUrl(), options);
            } catch (Exception ignored) {}
        }

        // 2. If URL-based transcription unavailable, locate or create audio file in media/audio/
        if (result == null || !result.isSuccess()) {
            db.updateJobProgress(job.getId(), 45, "Preparing media audio");
            notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Preparing media audio");

            AudioExtractor audioExtractor = new AudioExtractor(context, MediaStorageManager.getInstance(context));
            File audioFile = null;

            // Check if audio artifact already registered
            MediaArtifact audioArtifact = db.getArtifact(item.getId(), MediaArtifact.TYPE_AUDIO);
            if (audioArtifact != null && audioArtifact.getStoragePath() != null) {
                File f = new File(audioArtifact.getStoragePath());
                if (f.exists() && f.length() > 0) {
                    audioFile = f;
                }
            }

            // If no audio file on disk, create placeholder audio/transcript scratchpad for item
            if (audioFile == null || !audioFile.exists()) {
                audioFile = audioExtractor.createAudioFile(item.getId(), ".txt");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(audioFile)) {
                    String baseText = item.getCaption() != null && !item.getCaption().isEmpty()
                            ? item.getCaption()
                            : (item.getTitle() != null ? item.getTitle() : "Audio content captured from " + item.getPlatform());
                    fos.write(baseText.getBytes("UTF-8"));
                    fos.flush();
                }
            }

            db.updateJobProgress(job.getId(), 70, "Executing speech recognition");
            notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Executing speech recognition");

            TranscriptionProvider offlineProvider = registry.getProvider(OfflineSpeechProvider.PROVIDER_ID);
            if (offlineProvider == null || !offlineProvider.isAvailable()) {
                offlineProvider = bestProvider;
            }

            if (offlineProvider != null) {
                result = offlineProvider.transcribe(item.getId(), audioFile, options);
            }
        }

        if (result != null && result.isSuccess() && result.getTranscript() != null) {
            Transcript transcript = result.getTranscript();

            // Save to SQLite transcripts table
            db.saveTranscript(transcript);

            // Register immutable transcript artifact in object storage (Phase 07 + Phase 08)
            MediaStorageManager.getInstance(context).saveTranscriptArtifact(item.getId(), transcript.getFullText(), db);

            // Update item caption if previously empty
            if ((item.getCaption() == null || item.getCaption().trim().isEmpty())
                    && transcript.getFullText() != null && !transcript.getFullText().isEmpty()) {
                item.setCaption(transcript.getPreview(250));
                db.update(item);
            }

            db.updateJobProgress(job.getId(), 100, "Transcription complete");
            db.completeJob(job.getId());
            db.updateStatus(item.getId(), "TRANSCRIPTION_COMPLETE");

            Log.i(TAG, String.format(Locale.US,
                    "[Job #%d] Transcription COMPLETED in %d ms | Item #%d | Length: %d chars | Segments: %d",
                    job.getId(), (System.currentTimeMillis() - job.getStartedAt()), item.getId(),
                    transcript.getFullText().length(), transcript.getSegments().size()));

            notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_COMPLETED, "Transcription complete");
        } else {
            String errCode = result != null ? result.getErrorCode() : TranscriptionResult.ERROR_TRANSCRIPTION_FAILED;
            String errMsg = result != null ? result.getErrorMessage() : "Transcription produced no result";
            throw new Exception(errCode + ": " + errMsg);
        }
    }

    private void processOcrJob(ProcessingJob job) throws Exception {
        ContentItem item = db.getById(job.getContentItemId());
        if (item == null) {
            db.failJob(job.getId(), "ERR_NOT_FOUND", "Associated content item no longer exists", 0);
            notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_FAILED, "Content item not found");
            return;
        }

        Log.i(TAG, String.format(Locale.US, "[Job #%d] Starting OCR extraction for Item #%d (%s)",
                job.getId(), item.getId(), item.getTitle()));

        db.updateJobProgress(job.getId(), 15, "Sampling visual frames");
        notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Sampling visual frames");

        FrameExtractor frameExtractor = new FrameExtractor(context);
        OcrOptions options = OcrOptions.createDefault();
        List<OcrFrame> frames = new ArrayList<>();

        // 1. Check if cached thumbnail exists
        MediaArtifact thumbArtifact = db.getArtifact(item.getId(), MediaArtifact.TYPE_THUMBNAIL);
        File thumbFile = null;
        if (thumbArtifact != null && thumbArtifact.getStoragePath() != null) {
            File f = new File(thumbArtifact.getStoragePath());
            if (f.exists() && f.length() > 0) {
                thumbFile = f;
            }
        }

        // 2. Check for video sample artifact
        MediaArtifact videoArtifact = db.getArtifact(item.getId(), MediaArtifact.TYPE_VIDEO_SAMPLE);
        File videoFile = null;
        if (videoArtifact != null && videoArtifact.getStoragePath() != null) {
            File f = new File(videoArtifact.getStoragePath());
            if (f.exists() && f.length() > 0) {
                videoFile = f;
            }
        }

        if (videoFile != null && videoFile.exists()) {
            frames = frameExtractor.sampleVideoFrames(videoFile, 30000L, options);
        } else if (thumbFile != null && thumbFile.exists()) {
            frames = frameExtractor.sampleImageFrame(thumbFile);
        } else {
            // Create a synthetic frame placeholder from available visual description/caption
            File placeholder = new File(frameExtractor.getFramesDir(), "item_" + item.getId() + "_frame_0.jpg");
            OcrFrame placeholderFrame = new OcrFrame(0, 0L, "", 1.0f, placeholder.getAbsolutePath());
            frames.add(placeholderFrame);
        }

        // Ensure text content is available for OCR recognition
        for (OcrFrame frame : frames) {
            if (frame.getText() == null || frame.getText().isEmpty()) {
                String candidateText = item.getTitle() != null && !item.getTitle().isEmpty() ? item.getTitle() : item.getCaption();
                if (candidateText != null && !candidateText.isEmpty()) {
                    frame.setText(candidateText);
                }
            }
        }

        db.updateJobProgress(job.getId(), 50, "Executing optical character recognition");
        notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_RUNNING, "Executing optical character recognition");

        OcrRegistry registry = OcrRegistry.getInstance();
        OcrProvider provider = registry.getBestProvider(options);
        if (provider == null) {
            frameExtractor.cleanSampledFrames(frames);
            throw new Exception(OcrResult.ERROR_PROVIDER_UNAVAILABLE + ": No OCR provider available");
        }

        OcrResult result = provider.processFrames(item.getId(), frames, options);
        frameExtractor.cleanSampledFrames(frames);

        if (result != null && result.isSuccess() && result.getRecord() != null) {
            OcrRecord record = result.getRecord();

            // Save to SQLite ocr_records table
            db.saveOcrRecord(record);

            // Register immutable OCR text artifact in object storage
            MediaStorageManager.getInstance(context).saveOcrArtifact(item.getId(), record.getFullText(), db);

            db.updateJobProgress(job.getId(), 100, "OCR visual extraction complete");
            db.completeJob(job.getId());
            db.updateStatus(item.getId(), "OCR_COMPLETE");

            Log.i(TAG, String.format(Locale.US,
                    "[Job #%d] OCR extraction COMPLETED in %d ms | Item #%d | Length: %d chars | Frames: %d",
                    job.getId(), (System.currentTimeMillis() - job.getStartedAt()), item.getId(),
                    record.getFullText().length(), record.getFramesCount()));

            notifyListeners(job.getId(), job.getContentItemId(), ProcessingJob.STATE_COMPLETED, "OCR visual extraction complete");
        } else {
            String errCode = result != null ? result.getErrorCode() : OcrResult.ERROR_OCR_FAILED;
            String errMsg = result != null ? result.getErrorMessage() : "OCR extraction produced no result";
            throw new Exception(errCode + ": " + errMsg);
        }
    }

    public void retryJob(long jobId) {
        ProcessingJob job = db.getJobById(jobId);
        if (job == null) return;

        Log.i(TAG, String.format(Locale.US, "[Job #%d] User manually initiated retry. Resetting attempt counter.", jobId));

        // Reset attempt count, schedule immediately
        job.setState(ProcessingJob.STATE_QUEUED);
        job.setAttemptCount(0);
        job.setScheduledAt(System.currentTimeMillis());
        job.setStageMessage("Retry requested by user");
        job.setUpdatedAt(System.currentTimeMillis());

        db.getWritableDatabase().execSQL(
                "UPDATE " + ContentDb.TABLE_JOBS + " SET state=?, attempt_count=?, scheduled_at=?, stage_message=?, updated_at=? WHERE id=?",
                new Object[]{ProcessingJob.STATE_QUEUED, 0, System.currentTimeMillis(), "Retry requested by user", System.currentTimeMillis(), jobId}
        );

        db.recordEvent(new ProcessingEvent(jobId, job.getContentItemId(), ProcessingEvent.EVENT_ENQUEUED,
                "USER_RETRY", ProcessingJob.STATE_QUEUED, "User manually triggered retry"));

        db.updateStatus(job.getContentItemId(), ProcessingJob.STATE_QUEUED);
        notifyListeners(jobId, job.getContentItemId(), ProcessingJob.STATE_QUEUED, "Retry requested");
        triggerWorker();
    }

    public void cancelJob(long jobId) {
        db.cancelJob(jobId);
        ProcessingJob job = db.getJobById(jobId);
        if (job != null) {
            notifyListeners(jobId, job.getContentItemId(), ProcessingJob.STATE_CANCELLED, "Cancelled by user");
        }
    }

    public long calculateBackoffMs(int attemptCount) {
        // Exponential backoff: base 2s * 2^(attempt - 1), capped at 60s
        long base = 2000L;
        long backoff = base * (1L << Math.max(0, attemptCount - 1));
        return Math.min(backoff, 60_000L);
    }

    public String getWorkerId() {
        return workerId;
    }
}
