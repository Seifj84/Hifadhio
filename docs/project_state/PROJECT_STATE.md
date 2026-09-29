# Project State

Last updated: 2026-09-29T20:42:00+03:00
Updated by: Antigravity Agent
Repository: https://github.com/Seifj84/Hifadhio.git
Branch: main
Latest commit: 7f1c981
App version: 0.7.0-phase7
Backend version: Local-first SQLite MVP (Schema v4)
Latest migration: 004_artifacts_and_media_storage

## Completed phases
- Phase 00 — Foundation: Theme tokens, Brand Assets, Architecture Specification, Repository Creation on Seifj84 GitHub.
- Phase 01 — Capture MVP & Local Library: Native Android Material 3 app, local SQLite database, URL normalization, platform detection, share target intent, live search, and verified debug APK compilation.
- Phase 02 — Inbox Triaging & Responsive Navigation: Fixed bottom nav icon/label overlap, responsive edge-to-edge layout, quick actions (move to collection, star, copy, delete), and empty state illustrations.
- Phase 03 — Collections, Tags & Library: Collection management (create, rename, delete with move-to-inbox), tag indexing and filtering, favorites filtering, and ContentDetailBottomSheet.
- Phase 04 — Processing Job Framework: Asynchronous job queue (`processing_jobs`), append-only event timeline (`processing_events`), worker lease concurrency, exponential backoff retries, idempotency keys, stale job recovery, UI status indicators on cards, and retry action in Content Detail.
- Phase 05 — Adapter Framework + Generic Web Metadata: Standardized `ContentExtractorAdapter` interface, `ContentAdapterRegistry` with prioritized routing and health checks, and `GenericWebAdapter` with OpenGraph & HTML metadata extraction, entity decoding, and relative URL resolution.
- Phase 06 — Official Platform Metadata Integrations: Native platform adapters for YouTube (`YouTubeAdapter`, Priority 90), TikTok (`TikTokAdapter`, Priority 85), Instagram (`InstagramAdapter`, Priority 80), Facebook (`FacebookAdapter`, Priority 80), Reddit (`RedditAdapter`, Priority 75), and X/Twitter (`XAdapter`, Priority 75) with official oEmbed integration, video ID thumbnail parsing, and guaranteed fallback metadata.
- Phase 07 — Object Storage and Controlled Media Pipeline: Partitioned internal app storage (`media/thumbnails/`, `media/temp/`, `media/audio/`, `media/artifacts/`), strict prohibition of database BLOBs via SQLite Schema v4 `artifacts` table, deterministic collision-free naming with SHA256 integrity, Android `FileProvider` scoped URI access (`com.seiftech.hifadhio.fileprovider`), automated remote thumbnail caching in `ProcessingJobManager`, `MediaCleanupWorker` for expired artifacts, orphaned disk files, and quota enforcement (50 MB quota), item cascade deletion, and offline image decoding in UI.

## Current phase
Phase 07 — Object Storage and Controlled Media Pipeline (COMPLETED)
Next: Phase 08 — Audio Extraction and Transcription Pipeline

## Current status
COMPLETED

## Current implemented capabilities
- Android native share target for `ACTION_SEND` (`text/plain`)
- URL normalization & telemetry stripping (`igshid`, `fbclid`, `si`, `utm_*`)
- Automatic platform recognition: YouTube, TikTok, Instagram, Facebook, Reddit, X, LinkedIn, Web
- Rich Material 3 UI with 5-tab Bottom Navigation: Home, Inbox, Library, Ask, Profile
- Live full-text search and platform filter chips
- Full Collection CRUD (Create, Rename, Delete with item preservation to Inbox)
- Tag indexing, extraction, and tag-based filtering chips
- Favorites / Starred items filtering
- ContentDetailBottomSheet: Full item inspection, notes editing, tag editing, processing audit timeline, extracted caption display, offline cached thumbnail rendering, diagnostic storage badge, and retry trigger
- Processing Job Queue (`processing_jobs`): SQLite persistence, worker lease locking, heartbeat lease renewal, and stale job recovery
- Retry Engine: Scheduled executor with exponential backoff (2s, 4s, 8s capped at 60s) with max attempts limit (default 3)
- Idempotency Guarantee: `job_type:content_item_id` prevents duplicate concurrent or queued processing jobs
- Processing Events Audit Log (`processing_events`): Append-only recording of job transitions
- Modular Adapter Architecture: `ContentExtractorAdapter` interface, `ExtractedMetadata` model, `AdapterHealth` model
- ContentAdapterRegistry: Centralized priority-based adapter routing and health checks
- YouTube Adapter: Official oEmbed metadata extraction, 11-char video ID parsing, high-resolution thumbnail caching
- TikTok Adapter: Official oEmbed metadata extraction, creator handle parsing, and cover thumbnail caching
- Instagram Adapter: Reel/Post canonicalization, tracking query stripping, and bot-block fallback protection
- Facebook Adapter: Reel/Share canonicalization, tracking parameter stripping, and structured fallback metadata
- Reddit Adapter: Subreddit comment thread canonicalization, Reddit oEmbed metadata extraction
- X / Twitter Adapter: Status link canonicalization, Twitter publish oEmbed snippet extraction
- GenericWebAdapter: OpenGraph and HTML metadata extraction with automatic HTTPS redirect upgrade
- Private Controlled Object Storage: Partitioned app files (`media/thumbnails/`, `media/temp/`, `media/audio/`, `media/artifacts/`)
- Database Schema v4 Migration: Added `artifacts` table indexing storage paths, mime types, file sizes, SHA256 checksums, retention policies, and expiration timestamps
- Retention Policies: `NO_RETENTION`, `TEMPORARY_PROCESSING`, `CACHE`, `LONG_TERM`
- Media Cleanup Worker (`MediaCleanupWorker`): Expired artifact purging, stale temp file removal, orphaned file detection, and 50 MB cache quota pruning
- Android FileProvider Scoped Access: `com.seiftech.hifadhio.fileprovider` for secure temporary URI sharing
- Cascade Deletion: Deleting content item sweeps all physical media files from disk before deleting database records
- Storage Management UI: Profile tab shows live media storage usage and provides a one-tap "Clean Cache" maintenance action
- Official Hifadhio brand system: NAS Digital Solutions • engineered by SeifTech
- Automated GitHub Actions CI/CD with 15 unit test suites and GitHub Release publishing

## Current architecture
- Mobile: Android Native (Java, Material 3, AndroidX, SDK 35)
- Extraction Framework: Extensible `ContentExtractorAdapter` hierarchy and `ContentAdapterRegistry`
- Storage: Controlled local object storage (`MediaStorageManager`) + SQLite Schema v4 (`artifacts` table)
- Network: `HttpFetchHelper` with HTTPS upgrade, mobile User-Agent, and bounded memory reads
- Database: Local-first SQLite (`hifadhio.db`, Schema v4)
- Brand: Hifadhio (NAS Digital Solutions • engineered by SeifTech)

## Last verified commands
- `git commit -m "feat(media): implement Phase 07 object storage and controlled media pipeline"` — PASS
- `powershell push_repo.ps1` — PASS (Remote: https://github.com/Seifj84/Hifadhio)
- GitHub Actions CI/CD Build #36606195301 — PASS (Status: completed, Conclusion: success)
- Unit tests (15 test suites) — ALL PASS
- APK Package Structure & Signature Verification — PASS

## Current blockers
- None.

## Exact next action
Present Phase 07 completion report, test instructions, and verified APK links to the user for device verification.

## Latest testable APK
- Path: `release_records/phase_07_object_storage/v0.7.0/Hifadhio-v0.7.0-phase7-debug.apk`
- Workspace Root: `Hifadhio-v0.7.0-phase7-debug.apk`
- Direct Download: https://github.com/Seifj84/Hifadhio/releases/download/v0.7.0-phase7/Hifadhio-v0.7.0-phase7-debug.apk
- GitHub Release: https://github.com/Seifj84/Hifadhio/releases/tag/v0.7.0-phase7
- Build type: debug
- Version: 0.7.0-phase7 (versionCode 7)
- Size: 6,315,409 bytes (6.02 MB)
- SHA256: A9B38BEC9EA443949710A98EC2DFE1B81500738039283894D3C38ED87BC0C94A
- Phase: Phase 07 Object Storage and Controlled Media Pipeline
