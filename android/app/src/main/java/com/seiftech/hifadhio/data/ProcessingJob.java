package com.seiftech.hifadhio.data;

import java.io.Serializable;

public class ProcessingJob implements Serializable {

    public static final String STATE_QUEUED = "QUEUED";
    public static final String STATE_CLAIMED = "CLAIMED";
    public static final String STATE_RUNNING = "RUNNING";
    public static final String STATE_RETRYING = "RETRYING";
    public static final String STATE_COMPLETED = "COMPLETED";
    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_CANCELLED = "CANCELLED";

    public static final String TYPE_METADATA_FETCH = "METADATA_FETCH";
    public static final String TYPE_EXTRACT_CONTENT = "EXTRACT_CONTENT";
    public static final String TYPE_DUMMY_TEST = "DUMMY_TEST";
    public static final String TYPE_TRANSCRIBE = "TRANSCRIBE";
    public static final String TYPE_OCR = "OCR";

    private long id;
    private long contentItemId;
    private String jobType = TYPE_METADATA_FETCH;
    private String state = STATE_QUEUED;
    private int priority = 0;
    private int attemptCount = 0;
    private int maxAttempts = 3;
    private int progressPercent = 0;
    private String stageMessage = "";
    private String workerId = "";
    private long leaseExpiresAt = 0L;
    private long scheduledAt = System.currentTimeMillis();
    private long startedAt = 0L;
    private long completedAt = 0L;
    private long failedAt = 0L;
    private String errorCode = "";
    private String sanitizedErrorMessage = "";
    private String idempotencyKey = "";
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public ProcessingJob() {}

    public ProcessingJob(long contentItemId, String jobType) {
        this.contentItemId = contentItemId;
        this.jobType = jobType != null ? jobType : TYPE_METADATA_FETCH;
        this.idempotencyKey = this.jobType + ":" + contentItemId;
        this.scheduledAt = System.currentTimeMillis();
        this.createdAt = this.scheduledAt;
        this.updatedAt = this.scheduledAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getContentItemId() { return contentItemId; }
    public void setContentItemId(long contentItemId) { this.contentItemId = contentItemId; }

    public String getJobType() { return jobType == null ? TYPE_METADATA_FETCH : jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getState() { return state == null ? STATE_QUEUED : state; }
    public void setState(String state) { this.state = state; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public int getProgress() { return progressPercent; }
    public void setProgress(int progress) { this.progressPercent = progress; }

    public String getStageMessage() { return stageMessage == null ? "" : stageMessage; }
    public void setStageMessage(String stageMessage) { this.stageMessage = stageMessage; }

    public String getWorkerId() { return workerId == null ? "" : workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public long getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(long leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }

    public long getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(long scheduledAt) { this.scheduledAt = scheduledAt; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public long getFailedAt() { return failedAt; }
    public void setFailedAt(long failedAt) { this.failedAt = failedAt; }

    public String getErrorCode() { return errorCode == null ? "" : errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getSanitizedErrorMessage() { return sanitizedErrorMessage == null ? "" : sanitizedErrorMessage; }
    public void setSanitizedErrorMessage(String sanitizedErrorMessage) { this.sanitizedErrorMessage = sanitizedErrorMessage; }

    public String getIdempotencyKey() { return idempotencyKey == null ? "" : idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isTerminal() {
        return STATE_COMPLETED.equals(state) || STATE_FAILED.equals(state) || STATE_CANCELLED.equals(state);
    }
}
