package com.seiftech.hifadhio.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.seiftech.hifadhio.adapter.ContentAdapterRegistry;
import com.seiftech.hifadhio.adapter.ContentExtractorAdapter;
import com.seiftech.hifadhio.adapter.ExtractedMetadata;
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
