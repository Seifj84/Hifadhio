package com.seiftech.hifadhio.data;

import java.io.Serializable;

public class ProcessingEvent implements Serializable {

    public static final String EVENT_ENQUEUED = "JOB_ENQUEUED";
    public static final String EVENT_CLAIMED = "JOB_CLAIMED";
    public static final String EVENT_PROGRESS = "PROGRESS_UPDATED";
    public static final String EVENT_RETRY = "RETRY_SCHEDULED";
    public static final String EVENT_COMPLETED = "JOB_COMPLETED";
    public static final String EVENT_FAILED = "JOB_FAILED";
    public static final String EVENT_CANCELLED = "JOB_CANCELLED";
    public static final String EVENT_STALE_RECOVERED = "STALE_RECOVERED";

    private long id;
    private long jobId;
    private long contentItemId;
    private String eventType = EVENT_ENQUEUED;
    private String stage = "INIT";
    private String status = "QUEUED";
    private String message = "";
    private String metadataJson = "{}";
    private long createdAt = System.currentTimeMillis();

    public ProcessingEvent() {}

    public ProcessingEvent(long jobId, long contentItemId, String eventType, String stage, String status, String message) {
        this.jobId = jobId;
        this.contentItemId = contentItemId;
        this.eventType = eventType;
        this.stage = stage;
        this.status = status;
        this.message = message;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getJobId() { return jobId; }
    public void setJobId(long jobId) { this.jobId = jobId; }

    public long getContentItemId() { return contentItemId; }
    public void setContentItemId(long contentItemId) { this.contentItemId = contentItemId; }

    public String getEventType() { return eventType == null ? EVENT_ENQUEUED : eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStage() { return stage == null ? "" : stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getStatus() { return status == null ? "" : status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message == null ? "" : message; }
    public void setMessage(String message) { this.message = message; }

    public String getMetadataJson() { return metadataJson == null ? "{}" : metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
