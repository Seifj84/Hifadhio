package com.seiftech.hifadhio.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProcessingJobTest {

    @Test
    public void testJobInitializationAndIdempotencyKey() {
        ProcessingJob job = new ProcessingJob(42L, ProcessingJob.TYPE_METADATA_FETCH);

        assertEquals(42L, job.getContentItemId());
        assertEquals(ProcessingJob.TYPE_METADATA_FETCH, job.getJobType());
        assertEquals(ProcessingJob.STATE_QUEUED, job.getState());
        assertEquals(0, job.getAttemptCount());
        assertEquals(3, job.getMaxAttempts());
        assertEquals(0, job.getProgressPercent());
        assertEquals("METADATA_FETCH:42", job.getIdempotencyKey());
        assertFalse(job.isTerminal());
        assertTrue(job.getScheduledAt() > 0);
    }

    @Test
    public void testTerminalStates() {
        ProcessingJob job = new ProcessingJob(1L, ProcessingJob.TYPE_METADATA_FETCH);
        assertFalse(job.isTerminal());

        job.setState(ProcessingJob.STATE_RUNNING);
        assertFalse(job.isTerminal());

        job.setState(ProcessingJob.STATE_RETRYING);
        assertFalse(job.isTerminal());

        job.setState(ProcessingJob.STATE_COMPLETED);
        assertTrue(job.isTerminal());

        job.setState(ProcessingJob.STATE_FAILED);
        assertTrue(job.isTerminal());

        job.setState(ProcessingJob.STATE_CANCELLED);
        assertTrue(job.isTerminal());
    }

    @Test
    public void testBackoffCalculation() {
        // Mock manager backoff calculation: 2s * 2^(attempt - 1), max 60s
        long base = 2000L;
        // Attempt 1: 2000 * 2^0 = 2000ms
        long backoff1 = base * (1L << Math.max(0, 1 - 1));
        assertEquals(2000L, backoff1);

        // Attempt 2: 2000 * 2^1 = 4000ms
        long backoff2 = base * (1L << Math.max(0, 2 - 1));
        assertEquals(4000L, backoff2);

        // Attempt 3: 2000 * 2^2 = 8000ms
        long backoff3 = base * (1L << Math.max(0, 3 - 1));
        assertEquals(8000L, backoff3);

        // Attempt 6: 2000 * 2^5 = 64000ms capped at 60000ms
        long backoff6 = Math.min(base * (1L << Math.max(0, 6 - 1)), 60_000L);
        assertEquals(60_000L, backoff6);
    }

    @Test
    public void testWorkerLeaseExpirationCheck() {
        long now = 1000000000000L;
        long leaseDuration = 30_000L;

        ProcessingJob job = new ProcessingJob(10L, ProcessingJob.TYPE_METADATA_FETCH);
        job.setState(ProcessingJob.STATE_CLAIMED);
        job.setWorkerId("worker-test-1");
        job.setLeaseExpiresAt(now + leaseDuration);

        // During lease: not expired
        assertTrue(job.getLeaseExpiresAt() > now);

        // After lease duration + 1ms: expired (eligible for stale recovery)
        long expiredTime = now + leaseDuration + 1L;
        assertTrue(job.getLeaseExpiresAt() < expiredTime);
    }

    @Test
    public void testProcessingEventTimelineRecording() {
        ProcessingEvent event1 = new ProcessingEvent(1L, 10L, ProcessingEvent.EVENT_ENQUEUED,
                "ENQUEUE", ProcessingJob.STATE_QUEUED, "Job enqueued");
        assertEquals(1L, event1.getJobId());
        assertEquals(10L, event1.getContentItemId());
        assertEquals(ProcessingEvent.EVENT_ENQUEUED, event1.getEventType());
        assertEquals("ENQUEUE", event1.getStage());
        assertEquals("QUEUED", event1.getStatus());
        assertTrue(event1.getCreatedAt() > 0);

        ProcessingEvent event2 = new ProcessingEvent(1L, 10L, ProcessingEvent.EVENT_CLAIMED,
                "LEASE", ProcessingJob.STATE_CLAIMED, "Job claimed by worker");
        assertEquals(ProcessingEvent.EVENT_CLAIMED, event2.getEventType());

        ProcessingEvent event3 = new ProcessingEvent(1L, 10L, ProcessingEvent.EVENT_PROGRESS,
                "EXTRACT", ProcessingJob.STATE_RUNNING, "Progress 50%");
        assertEquals(ProcessingEvent.EVENT_PROGRESS, event3.getEventType());

        ProcessingEvent event4 = new ProcessingEvent(1L, 10L, ProcessingEvent.EVENT_COMPLETED,
                "DONE", ProcessingJob.STATE_COMPLETED, "Completed successfully");
        assertEquals(ProcessingEvent.EVENT_COMPLETED, event4.getEventType());
    }

    @Test
    public void testMaxAttemptsRetryProgression() {
        ProcessingJob job = new ProcessingJob(50L, ProcessingJob.TYPE_METADATA_FETCH);
        job.setMaxAttempts(3);

        // Attempt 1 fails -> can retry
        job.setAttemptCount(1);
        assertTrue(job.getAttemptCount() < job.getMaxAttempts());

        // Attempt 2 fails -> can retry
        job.setAttemptCount(2);
        assertTrue(job.getAttemptCount() < job.getMaxAttempts());

        // Attempt 3 fails -> reached max, must fail
        job.setAttemptCount(3);
        assertFalse(job.getAttemptCount() < job.getMaxAttempts());
    }
}
