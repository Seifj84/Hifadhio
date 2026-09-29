# Release Record: v0.4.0-phase4

**App Name:** Hifadhio  
**Brand:** NAS Digital Solutions • engineered by SeifTech  
**Phase:** Phase 04 - Processing Job Framework  
**Version:** 0.4.0-phase4 (versionCode 4)  
**Date:** 2026-09-29  
**Git Commit:** `78a71fe`  
**GitHub Action Run:** [#36512899916](https://github.com/Seifj84/Hifadhio/actions/runs/36512899916)  
**GitHub Release:** [v0.4.0-phase4](https://github.com/Seifj84/Hifadhio/releases/tag/v0.4.0-phase4)  

---

## 1. Summary of Changes
- **SQLite Database Schema v3:**
  - Added `processing_jobs` table with columns for `idempotency_key`, `worker_id`, `lease_expires_at`, `attempt_count`, `max_attempts`, `priority`, and error sanitization.
  - Added `processing_events` append-only audit trail table for tracing job state changes.
  - Indexed critical query paths (`state, scheduled_at`, `content_item_id`, `idempotency_key`).
- **Processing Job Engine:**
  - Implemented `ProcessingJobManager` orchestrator running background jobs with worker concurrency lease locking.
  - Added exponential backoff retry engine (`2s * 2^(attempt - 1)`, capped at 60s).
  - Stale-job recovery mechanism automatically reclaiming abandoned worker leases.
  - Idempotency key `METADATA_FETCH:<content_item_id>` prevents duplicated concurrent jobs.
- **UI Enhancements:**
  - Added live status chips on content cards (`QUEUED`, `RUNNING`, `FAILED`, `READY`).
  - Added real-time processing event audit timeline to `ContentDetailBottomSheet`.
  - Added "Retry Processing" button inside `ContentDetailBottomSheet` to trigger immediate manual retries for failed jobs.
  - Enqueue job automatically when saving a new link in `SaveLinkBottomSheet`.
- **Unit Testing:**
  - `ProcessingJobTest.java` verifying lease expiration, exponential backoff, idempotency, state transitions, and timeline event recording.

---

## 2. Verification & Test Evidence
- **Unit Tests:** `ProcessingJobTest`, `CollectionsAndTagsTest`, `UrlNormalizerTest` passed on GitHub Actions CI.
- **CI Build:** Clean execution in Gradle 8.9 / OpenJDK 17.
- **APK Size:** 6,276,301 bytes.

---

## 3. Artifact Download
- Direct APK: [Hifadhio-v0.4.0-phase4-debug.apk](https://github.com/Seifj84/Hifadhio/releases/download/v0.4.0-phase4/Hifadhio-v0.4.0-phase4-debug.apk)
- Local Path: `G:\Other computers\My PC (1)\Coding Project\Hifadhio\Hifadhio-v0.4.0-phase4-debug.apk`
