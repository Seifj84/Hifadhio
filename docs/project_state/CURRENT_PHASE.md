# Current Phase

Phase: Phase 04
Name: Processing Job Framework
Status: COMPLETED
Started: 2026-09-29T05:16:00+03:00
Completed: 2026-09-29T05:33:00+03:00
Target specification section: Sections 629-662 & 1091-1108 of Master Spec

## Objective
Establish an asynchronous, reliable processing job framework with SQLite persistence (database schema v3), worker claim/lease concurrency control, exponential backoff retries, idempotency key guarantees, stale job recovery, append-only processing event timeline, UI status indicators on content items, and retry actions on failed jobs.

## In scope
- [x] Database upgrade (v3): Create `processing_jobs` and `processing_events` tables in `ContentDb`.
- [x] ProcessingJob and ProcessingEvent domain models.
- [x] Worker claim/lease mechanism with lease expiration & heartbeats.
- [x] Retry with exponential backoff and max attempt limits.
- [x] Idempotency keys (`job_type:content_item_id`) preventing duplicate jobs.
- [x] Stale-job recovery for interrupted or abandoned worker leases.
- [x] ProcessingJobManager background executor orchestrating jobs and updating content item status.
- [x] UI visual status indicators on cards and processing event timeline with "Retry" action in Content Detail sheet.
- [x] Comprehensive unit test suite (`ProcessingJobTest.java`) covering lease, retry, idempotency, recovery, and cancellation.
- [x] Version bump to `0.4.0-phase4` in `build.gradle` and CI/CD workflow.
- [x] Verified APK build published to GitHub Releases.

## Out of scope
- Live scraping of third-party platforms (scheduled for Phase 05/06 adapters).
- Whisper / audio transcription and OCR (scheduled for Phase 07+).

## Preconditions
- Phase 03 completed, tested, and verified on physical device.
- Clean GitHub repository state at `Seifj84/Hifadhio` on `main`.

## Work units
- [x] WU-01 Project state documentation initialization
- [x] WU-02 ProcessingJob & ProcessingEvent data models
- [x] WU-03 Database schema upgrade v3 & job/event CRUD operations in ContentDb
- [x] WU-04 ProcessingJobManager with background execution & worker leasing
- [x] WU-05 UI integration in ContentAdapter, item cards, and ContentDetailBottomSheet
- [x] WU-06 Unit test suite ProcessingJobTest
- [x] WU-07 Version bump to 0.4.0-phase4 and CI/CD automated build
- [x] WU-08 APK verification and release publishing

## Required tests
- [x] Worker lease claim & concurrency: PASS
- [x] Exponential backoff & retry progression: PASS
- [x] Idempotency key duplicate prevention: PASS
- [x] Stale job recovery from expired leases: PASS
- [x] Job cancellation: PASS
- [x] Processing events timeline recording: PASS
- [x] Automated CI/CD build: PASS

## Phase acceptance criteria
1. `processing_jobs` and `processing_events` tables created with full indexing.
2. A test job can fail, retry with exponential backoff, recover from worker interruption, and complete without duplication.
3. Content items reflect live processing states (`SAVED`, `PROCESSING`, `READY`, `FAILED`).
4. Content Detail view provides an audit timeline of processing events and retry action for failed jobs.
5. All unit tests pass in CI/CD, and an installable test APK v0.4.0-phase4 is published.

## Blockers
None.

## Completion evidence
- GitHub Actions CI/CD Build Run #36512899916 succeeded.
- GitHub Release `v0.4.0-phase4` published.
- Unit test suites (`ProcessingJobTest`, `CollectionsAndTagsTest`, `UrlNormalizerTest`) executed and passed in CI.
- Downloaded APK verified (6,276,301 bytes).

## APK status
AVAILABLE
- Direct Download: https://github.com/Seifj84/Hifadhio/releases/download/v0.4.0-phase4/Hifadhio-v0.4.0-phase4-debug.apk
- Release Page: https://github.com/Seifj84/Hifadhio/releases/tag/v0.4.0-phase4
- Local Workspace Path: `Hifadhio-v0.4.0-phase4-debug.apk` and `release_records/phase_04_processing_framework/v0.4.0/Hifadhio-v0.4.0-phase4-debug.apk`
