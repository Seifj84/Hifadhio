# Project State

Last updated: 2026-09-29T19:05:00+03:00
Updated by: Antigravity Agent
Repository: https://github.com/Seifj84/Hifadhio.git
Branch: main
Latest commit: f65a0c8
App version: 0.6.0-phase6
Backend version: Local-first SQLite MVP (Schema v3)
Latest migration: 003_processing_jobs_and_events

## Completed phases
- Phase 00 — Foundation: Theme tokens, Brand Assets, Architecture Specification, Repository Creation on Seifj84 GitHub.
- Phase 01 — Capture MVP & Local Library: Native Android Material 3 app, local SQLite database, URL normalization, platform detection, share target intent, live search, and verified debug APK compilation.
- Phase 02 — Inbox Triaging & Responsive Navigation: Fixed bottom nav icon/label overlap, responsive edge-to-edge layout, quick actions (move to collection, star, copy, delete), and empty state illustrations.
- Phase 03 — Collections, Tags & Library: Collection management (create, rename, delete with move-to-inbox), tag indexing and filtering, favorites filtering, and ContentDetailBottomSheet.
- Phase 04 — Processing Job Framework: Asynchronous job queue (`processing_jobs`), append-only event timeline (`processing_events`), worker lease concurrency, exponential backoff retries, idempotency keys, stale job recovery, UI status indicators on cards, and retry action in Content Detail.
- Phase 05 — Adapter Framework + Generic Web Metadata: Standardized `ContentExtractorAdapter` interface, `ContentAdapterRegistry` with prioritized routing and health checks, and `GenericWebAdapter` with OpenGraph & HTML metadata extraction, entity decoding, and relative URL resolution.
- Phase 06 — Official Platform Metadata Integrations: Native platform adapters for YouTube (`YouTubeAdapter`, Priority 90), TikTok (`TikTokAdapter`, Priority 85), Instagram (`InstagramAdapter`, Priority 80), Facebook (`FacebookAdapter`, Priority 80), Reddit (`RedditAdapter`, Priority 75), and X/Twitter (`XAdapter`, Priority 75) with official oEmbed integration, video ID thumbnail parsing, and guaranteed fallback metadata.

## Current phase
Phase 06 — Official Platform Metadata Integrations (COMPLETED)
Next: Phase 07 — Object Storage and Controlled Media Pipeline

## Current status
READY_FOR_ACCEPTANCE

## Current implemented capabilities
- Android native share target for `ACTION_SEND` (`text/plain`)
- URL normalization & telemetry stripping (`igshid`, `fbclid`, `si`, `utm_*`)
- Automatic platform recognition: YouTube, TikTok, Instagram, Facebook, Reddit, X, LinkedIn, Web
- Rich Material 3 UI with 5-tab Bottom Navigation: Home, Inbox, Library, Ask, Profile
- Live full-text search and platform filter chips
- Full Collection CRUD (Create, Rename, Delete with item preservation to Inbox)
- Tag indexing, extraction, and tag-based filtering chips
- Favorites / Starred items filtering
- ContentDetailBottomSheet: Full item inspection, notes editing, tag editing, processing audit timeline, extracted caption display, and retry trigger
- Processing Job Queue (`processing_jobs`): SQLite persistence, worker lease locking, heartbeat lease renewal, and stale job recovery
- Retry Engine: Scheduled executor with exponential backoff (2s, 4s, 8s capped at 60s) with max attempts limit (default 3)
- Idempotency Guarantee: `job_type:content_item_id` prevents duplicate concurrent or queued processing jobs
- Processing Events Audit Log (`processing_events`): Append-only recording of job transitions
- Modular Adapter Architecture: `ContentExtractorAdapter` interface, `ExtractedMetadata` model, `AdapterHealth` model
- ContentAdapterRegistry: Centralized priority-based adapter routing and health checks
- YouTube Adapter: Official oEmbed metadata extraction, 11-char video ID parsing across watch, shorts, live, and youtu.be, high-resolution thumbnail generation (`hqdefault.jpg`)
- TikTok Adapter: Official oEmbed metadata extraction, creator handle parsing, and cover thumbnail extraction
- Instagram Adapter: Reel/Post canonicalization, tracking query stripping, and bot-block fallback protection
- Facebook Adapter: Reel/Share canonicalization, tracking parameter stripping, and structured fallback metadata
- Reddit Adapter: Subreddit comment thread canonicalization, Reddit oEmbed metadata extraction
- X / Twitter Adapter: Status link canonicalization, Twitter publish oEmbed snippet extraction
- GenericWebAdapter: OpenGraph (`og:title`, `og:description`, `og:image`, `og:site_name`, `og:type`), Twitter cards, HTML `<title>`, `<meta name="description">`, `<link rel="canonical">`, and automatic HTTPS redirect upgrade
- Official Hifadhio brand system: NAS Digital Solutions • engineered by SeifTech
- Automated GitHub Actions CI/CD with 13 unit test suites and GitHub Release publishing

## Current architecture
- Mobile: Android Native (Java, Material 3, AndroidX, SDK 35)
- Extraction Framework: Extensible `ContentExtractorAdapter` hierarchy and `ContentAdapterRegistry`
- Network: `HttpFetchHelper` with HTTPS upgrade, mobile User-Agent, and bounded memory reads
- Database: Local-first SQLite (`hifadhio.db`, Schema v3)
- Storage: Local private app storage
- Brand: Hifadhio (NAS Digital Solutions • engineered by SeifTech)

## Last verified commands
- `git commit -m "feat(adapters): implement Phase 06 official platform metadata integrations..."` — PASS
- `powershell push_repo.ps1` — PASS (Remote: https://github.com/Seifj84/Hifadhio)
- GitHub Actions CI/CD Build #36594437363 — PASS (Status: completed, Conclusion: success)
- Unit tests (13 test suites) — ALL PASS
- APK Package Structure & Signature Verification — PASS

## Current blockers
- None.

## Exact next action
Present Phase 06 completion report, test instructions, and verified APK links to the user for device verification.

## Latest testable APK
- Path: `release_records/phase_06_platform_adapters/v0.6.0/Hifadhio-v0.6.0-phase6-debug.apk`
- Workspace Root: `Hifadhio-v0.6.0-phase6-debug.apk`
- Direct Download: https://github.com/Seifj84/Hifadhio/releases/download/v0.6.0-phase6/Hifadhio-v0.6.0-phase6-debug.apk
- GitHub Release: https://github.com/Seifj84/Hifadhio/releases/tag/v0.6.0-phase6
- Build type: debug
- Version: 0.6.0-phase6 (versionCode 6)
- Size: 6,300,571 bytes (6.0 MB)
- SHA256: 36C970D4FDC5BA0168A1F0E456138AE1AE81C4DACA827F86B10E6CADC471B337
- Phase: Phase 06 Official Platform Metadata Integrations
