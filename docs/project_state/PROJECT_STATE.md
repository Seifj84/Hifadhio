# Project State

Last updated: 2026-10-02T01:37:00+03:00
Updated by: Antigravity Agent
Repository: https://github.com/Seifj84/Hifadhio.git
Branch: main
Latest commit: cbfdf47
App version: 0.10.0-phase10
Backend version: Local-first SQLite MVP (Schema v7)
Latest migration: 007_ai_enrichments_and_semantic_synthesis

## Completed phases
- Phase 00 — Foundation: Theme tokens, Brand Assets, Architecture Specification, Repository Creation on Seifj84 GitHub.
- Phase 01 — Capture MVP & Local Library: Native Android Material 3 app, local SQLite database, URL normalization, platform detection, share target intent, live search, and verified debug APK compilation.
- Phase 02 — Inbox Triaging & Responsive Navigation: Fixed bottom nav icon/label overlap, responsive edge-to-edge layout, quick actions (move to collection, star, copy, delete), and empty state illustrations.
- Phase 03 — Collections, Tags & Library: Collection management (create, rename, delete with move-to-inbox), tag indexing and filtering, favorites filtering, and ContentDetailBottomSheet.
- Phase 04 — Processing Job Framework: Asynchronous job queue (`processing_jobs`), append-only event timeline (`processing_events`), worker lease concurrency, exponential backoff retries, idempotency keys, stale job recovery, UI status indicators on cards, and retry action in Content Detail.
- Phase 05 — Adapter Framework + Generic Web Metadata: Standardized `ContentExtractorAdapter` interface, `ContentAdapterRegistry` with prioritized routing and health checks, and `GenericWebAdapter` with OpenGraph & HTML metadata extraction, entity decoding, and relative URL resolution.
- Phase 06 — Official Platform Metadata Integrations: Native platform adapters for YouTube (`YouTubeAdapter`, Priority 90), TikTok (`TikTokAdapter`, Priority 85), Instagram (`InstagramAdapter`, Priority 80), Facebook (`FacebookAdapter`, Priority 80), Reddit (`RedditAdapter`, Priority 75), and X/Twitter (`XAdapter`, Priority 75) with official oEmbed integration, video ID thumbnail parsing, and guaranteed fallback metadata.
- Phase 07 — Object Storage and Controlled Media Pipeline: Partitioned internal app storage (`media/thumbnails/`, `media/temp/`, `media/audio/`, `media/artifacts/`), strict prohibition of database BLOBs via SQLite Schema v4 `artifacts` table, deterministic collision-free naming with SHA256 integrity, Android `FileProvider` scoped URI access (`com.seiftech.hifadhio.fileprovider`), automated remote thumbnail caching in `ProcessingJobManager`, `MediaCleanupWorker` for expired artifacts, orphaned disk files, and quota enforcement (50 MB quota), item cascade deletion, and offline image decoding in UI.
- Phase 08 — Audio Extraction and Transcription Pipeline: Vendor-neutral transcription provider architecture (`TranscriptionProvider`, `TranscriptionRegistry`, ADR-007), database migration to SQLite Schema v5 (`transcripts` table), strict "never fabricate timestamps" rule (§15.3), zero-cost native subtitle/caption parsing (`SubtitlesExtractorProvider`, Priority 100), on-device offline speech recognition (`OfflineSpeechProvider`, Priority 80), cloud Whisper integration (`CloudWhisperProvider`, Priority 50), audio management subsystem (`AudioExtractor`), automatic pipeline job chaining (`TYPE_TRANSCRIBE`) in `ProcessingJobManager`, search-on-detail UI with live transcript filtering, timestamp navigation chips, and one-tap copy in `ContentDetailBottomSheet`.
- Phase 09 — Visual Frame Extraction and OCR Engine: Vendor-neutral OCR provider architecture (`OcrProvider`, `OcrRegistry`, ADR-007/010), selective periodic frame sampling policy (`FrameExtractor`, 15 frame cap, zero fabricated timestamps), intelligent text deduplication (`TextDeduplicator`, watermark and consecutive slide suppression), SQLite Schema v6 migration (`ocr_records` table with cascade deletion and search index integration), immutable object storage artifact mirroring (`media/artifacts/`), automated background queue chaining (`TYPE_OCR`), and search-on-detail UI with live filter, frame chips, and copy action in `ContentDetailBottomSheet`.
- Phase 10 — AI Enrichment Engine: Vendor-neutral AI provider architecture (`AiProvider`, `AiRegistry`, ADR-008/010), strict JSON schema validation (§18), prompt versioning (`PromptManager`, `v1.0.0`), zero-cost deterministic on-device NLP engine (`LocalHeuristicAiProvider`, Priority 100, $0.00 cost) and cloud LLM client abstraction (`CloudLlmAiProvider`, Priority 50), SQLite Schema v7 migration (`ai_enrichments` table with cascade deletion and search index integration), immutable JSON artifact mirroring (`media/artifacts/item_{id}_ai_{hash}.json`), background queue integration (`TYPE_AI_ENRICHMENT`), and UI integration in `ContentDetailBottomSheet` with "AI GENERATED" label, dual-tier summaries, key takeaways, entities, interactive suggested tags, and one-tap collection reassignment.

## Current phase
Phase 10 — AI Enrichment Engine (COMPLETED)
Next: Phase 11 — Local Search & Retrieval Engine (BM25 Full-Text Search, Hybrid Ranking, Search Operators)

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
- ContentDetailBottomSheet: Full item inspection, notes editing, tag editing, processing audit timeline, extracted caption display, offline cached thumbnail rendering, diagnostic storage badge, retry trigger, speech transcript inspection, timestamp chips, and live search-on-detail filter
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
- Database Schema v4 & v5 Migrations: `artifacts` table and `transcripts` table with full cascade deletion
- Retention Policies: `NO_RETENTION`, `TEMPORARY_PROCESSING`, `CACHE`, `LONG_TERM`
- Media Cleanup Worker (`MediaCleanupWorker`): Expired artifact purging, stale temp file removal, orphaned file detection, and 50 MB cache quota pruning
- Android FileProvider Scoped Access: `com.seiftech.hifadhio.fileprovider` for secure temporary URI sharing
- Cascade Deletion: Deleting content item sweeps physical media files and transcript database records
- Vendor-Neutral Transcription Provider Architecture (`TranscriptionRegistry`): ADR-007 compliant provider registry isolating speech engines from domain logic
- Subtitle & Caption Extraction (`SubtitlesExtractorProvider`): Zero-cost parsing of WebVTT and SubRip (SRT) tracks with milliseconds accuracy
- On-Device Offline Speech Processing (`OfflineSpeechProvider`): Local offline transcription respecting file duration and size limits
- Cloud Whisper Integration (`CloudWhisperProvider`): OpenAI/Groq compatible Whisper client with cost estimation ($0.006/min)
- Strict Timestamps Rule: Timestamps recorded only when genuinely provided; never fabricated
- Audio Subsystem (`AudioExtractor`): Media eligibility checks, file size (<25 MB) and duration (<30 min) validation, and scratchpad management
- Pipeline Job Chaining: Automatic chaining of transcription jobs and OCR jobs on media items upon metadata extraction completion
- Search-on-Detail: Live dynamic text filtering across transcript segments and visual OCR frames within ContentDetailBottomSheet
- Library-wide Transcript & OCR Search: Main search bar indexes inside full transcript texts and on-screen visual texts
- Vendor-Neutral OCR Engine (`OcrRegistry`): Priority-ordered provider architecture (`OnDeviceOcrProvider`, `CloudVisionOcrProvider`)
- Selective Keyframe Sampling Policy (`FrameExtractor`): Periodic sampling cap (15 frames) with genuine timestamp preservation and image `-1L` guarantees
- Intelligent Text Deduplication (`TextDeduplicator`): Detects >=60% watermark frequencies and adjacent slide redundancies
- Database Schema v6 Migration: `ocr_records` table with cascade deletion and search index
- Official Hifadhio brand system: NAS Digital Solutions • engineered by SeifTech
- Automated GitHub Actions CI/CD with 21 unit test suites and GitHub Release publishing

## Current architecture
- Mobile: Android Native (Java, Material 3, AndroidX, SDK 35)
- Speech & Transcription: Vendor-neutral `TranscriptionProvider` registry, true-timestamp segment model, VTT/SRT parsers
- Visual Frame & OCR: Vendor-neutral `OcrProvider` registry, `FrameExtractor` sampling, `TextDeduplicator` watermark suppression
- Extraction Framework: Extensible `ContentExtractorAdapter` hierarchy and `ContentAdapterRegistry`
- Storage: Controlled local object storage (`MediaStorageManager`) + SQLite Schema v6 (`artifacts`, `transcripts`, and `ocr_records` tables)
- Network: `HttpFetchHelper` with HTTPS upgrade, mobile User-Agent, and bounded memory reads
- Database: Local-first SQLite (`hifadhio.db`, Schema v6)
- Brand: Hifadhio (NAS Digital Solutions • engineered by SeifTech)

## Last verified commands
- `git commit -m "fix(ocr): correct ContentItem package import in FrameExtractor and tests"` — PASS
- `powershell push_repo.ps1` — PASS (Remote: https://github.com/Seifj84/Hifadhio)
- GitHub Actions CI/CD Build #36806949533 — PASS (Status: completed, Conclusion: success)
- Unit tests (21 test suites) — ALL PASS
- APK Package Structure & Signature Verification — PASS

## Current blockers
- None.

## Exact next action
Present Phase 09 completion report, test instructions, and verified APK links to the user for device verification.

## Latest testable APK
- Path: `release_records/phase_09_ocr/v0.9.0/Hifadhio-v0.9.0-phase9-debug.apk`
- Workspace Root: `Hifadhio-v0.9.0-phase9-debug.apk`
- Direct Download: https://github.com/Seifj84/Hifadhio/releases/download/v0.9.0-phase9/Hifadhio-v0.9.0-phase9-debug.apk
- GitHub Release: https://github.com/Seifj84/Hifadhio/releases/tag/v0.9.0-phase9
- Build type: debug
- Version: 0.9.0-phase9 (versionCode 9)
- Size: 6,355,376 bytes (6.06 MB)
- SHA256: 3A2E1AC274906B79244B32655EB22EF9291C798070C04F1358229F2D8954D433
- Phase: Phase 09 Visual Frame Extraction and OCR Engine
