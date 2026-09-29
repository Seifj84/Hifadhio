# Current Phase

Phase: Phase 04
Name: Processing Job Framework
Status: IN_PROGRESS
Started: 2026-09-29T05:16:00+03:00
Target specification section: Sections 629-662 & 1091-1108 of Master Spec

## Objective
Establish an asynchronous, reliable processing job framework with SQLite persistence (database schema v3), worker claim/lease concurrency control, exponential backoff retries, idempotency key guarantees, stale job recovery, append-only processing event timeline, UI status indicators on content items, and retry actions on failed jobs.

## In scope
- [ ] Database upgrade (v3): Create `processing_jobs` and `processing_events` tables in `ContentDb`.
- [ ] ProcessingJob and ProcessingEvent domain models.
- [ ] Worker claim/lease mechanism with lease expiration & heartbeats.
- [ ] Retry with exponential backoff and max attempt limits.
- [ ] Idempotency keys (`job_type:content_item_id`) preventing duplicate jobs.
- [ ] Stale-job recovery for interrupted or abandoned worker leases.
- [ ] ProcessingJobManager background executor orchestrating jobs and updating content item status.
- [ ] UI visual status indicators on cards and processing event timeline with "Retry" action in Content Detail sheet.
- [ ] Comprehensive unit test suite (`ProcessingJobTest.java`) covering lease, retry, idempotency, recovery, and cancellation.
- [ ] Version bump to `0.4.0-phase4` in `build.gradle` and CI/CD workflow.
- [ ] Verified APK build published to GitHub Releases.

## Out of scope
- Live scraping of third-party platforms (scheduled for Phase 05/06 adapters).
- Whisper / audio transcription and OCR (scheduled for Phase 07+).

## Preconditions
- Phase 03 completed, tested, and verified on physical device.
- Clean GitHub repository state at `Seifj84/Hifadhio` on `main`.

## Work units
- [ ] WU-01 Project state documentation initialization
- [ ] WU-02 ProcessingJob & ProcessingEvent data models
- [ ] WU-03 Database schema upgrade v3 & job/event CRUD operations in ContentDb
- [ ] WU-04 ProcessingJobManager with background execution & worker leasing
- [ ] WU-05 UI integration in ContentAdapter, item cards, and ContentDetailBottomSheet
- [ ] WU-06 Unit test suite ProcessingJobTest
- [ ] WU-07 Version bump to 0.4.0-phase4 and CI/CD automated build
- [ ] WU-08 APK verification and release publishing

## Required tests
- [ ] Worker lease claim & concurrency: PASS
- [ ] Exponential backoff & retry progression: PASS
- [ ] Idempotency key duplicate prevention: PASS
- [ ] Stale job recovery from expired leases: PASS
- [ ] Job cancellation: PASS
- [ ] Processing events timeline recording: PASS
- [ ] Automated CI/CD build: PASS

## Phase acceptance criteria
1. `processing_jobs` and `processing_events` tables created with full indexing.
2. A test job can fail, retry with exponential backoff, recover from worker interruption, and complete without duplication.
3. Content items reflect live processing states (`SAVED`, `PROCESSING`, `READY`, `FAILED`).
4. Content Detail view provides an audit timeline of processing events and retry action for failed jobs.
5. All unit tests pass in CI/CD, and an installable test APK v0.4.0-phase4 is published.

## Blockers
None.
