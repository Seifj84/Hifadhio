# Current Phase

Phase: Phase 06
Name: Official Platform Metadata Integrations
Status: IN_PROGRESS
Started: 2026-09-29T18:55:00+03:00
Completed: 
Target specification section: Sections 500-545 & 1125-1154 of Master Spec

## Objective
Implement specialized, deterministic platform metadata adapters for major social content platforms (YouTube, TikTok, Instagram, Reddit, X/Twitter) integrated into the `ContentAdapterRegistry`. Each adapter provides platform-specific URL canonicalization, official public oEmbed/metadata extraction, creator attribution, and high-fidelity fallback metadata to guarantee that no extraction failure or rate limit ever jeopardizes content capture.

## In scope
- [ ] Lightweight shared HTTP/oEmbed utility (`HttpFetchHelper.java`) with redirect following, HTTPS upgrade, and timeouts.
- [ ] `YouTubeAdapter.java` (Priority 90): canonical video/shorts/live ID parsing, high-resolution thumbnail generation (`hqdefault.jpg`), official YouTube oEmbed JSON extraction, structured fallback.
- [ ] `TikTokAdapter.java` (Priority 85): video & short URL canonicalization, official TikTok oEmbed metadata extraction (`title`, `author_name`, `thumbnail_url`), structured fallback.
- [ ] `InstagramAdapter.java` (Priority 80): Reel/Post/TV URL canonicalization, tracking parameter stripping, graceful structured metadata without blocking capture.
- [ ] `RedditAdapter.java` (Priority 75): subreddit & post canonicalization, Reddit oEmbed metadata extraction.
- [ ] `XAdapter.java` (Priority 75): status link canonicalization, Twitter publish oEmbed metadata extraction.
- [ ] Comprehensive adapter specifications in `docs/adapters/`: `ADAPTER_YOUTUBE.md`, `ADAPTER_TIKTOK.md`, `ADAPTER_INSTAGRAM.md`, `ADAPTER_REDDIT.md`, `ADAPTER_X.md`.
- [ ] Unit test suites for each adapter verifying URL recognition, canonicalization, metadata extraction, health checks, and fallback resilience.
- [ ] Centralized registration in `ContentAdapterRegistry` maintaining clean priority hierarchy.
- [ ] Version bump to `0.6.0-phase6` (versionCode 6) and CI/CD automated build + release publishing.

## Out of scope
- Media binary downloading, audio extraction, local Whisper transcription (scheduled for Phase 07 & 08).
- Paid third-party scraping APIs requiring commercial proxy networks.

## Preconditions
- Phase 05 & Phase 05 Corrective completed, tested, and published as `v0.5.1-phase5`.
- Clean repository state on `main`.

## Work units
- [ ] WU-01 Phase 06 state documentation & `HttpFetchHelper` utility
- [ ] WU-02 `YouTubeAdapter`, `ADAPTER_YOUTUBE.md`, and `YouTubeAdapterTest`
- [ ] WU-03 `TikTokAdapter`, `ADAPTER_TIKTOK.md`, and `TikTokAdapterTest`
- [ ] WU-04 `InstagramAdapter`, `ADAPTER_INSTAGRAM.md`, and `InstagramAdapterTest`
- [ ] WU-05 `RedditAdapter` & `XAdapter`, documentation, and tests
- [ ] WU-06 Registry wiring & routing tests in `AdapterRegistryTest`
- [ ] WU-07 Version bump to `0.6.0-phase6` (versionCode 6), CI/CD verification, and release packaging

## Required tests
- [ ] YouTube URL recognition, canonicalization (watch, short, youtu.be), and oEmbed extraction: PASS
- [ ] TikTok URL recognition, canonicalization, and oEmbed extraction: PASS
- [ ] Instagram URL recognition, canonicalization, and metadata fallback: PASS
- [ ] Reddit and X/Twitter URL recognition and oEmbed parsing: PASS
- [ ] Priority order resolution in ContentAdapterRegistry: PASS
- [ ] Failure isolation (failed HTTP/bot-block yields valid fallback without failing capture): PASS
- [ ] All CI/CD automated unit tests and APK build: PASS

## Phase acceptance criteria
1. Saving YouTube (standard, shorts, youtu.be) extracts official video title, author/channel, and thumbnail.
2. Saving TikTok links extracts title/caption and creator name without manual input.
3. Saving Instagram, Facebook, Reddit, and X links canonicalizes cleanly and captures metadata reliably.
4. If an external platform blocks or returns error, structured fallback metadata is stored and capture completes cleanly.
5. All unit tests pass in CI/CD, and an installable test APK v0.6.0-phase6 is published.
