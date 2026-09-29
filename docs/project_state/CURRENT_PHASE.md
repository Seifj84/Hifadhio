# Current Phase

Phase: Phase 05
Name: Adapter Framework + Generic Web Metadata
Status: IN_PROGRESS
Started: 2026-09-29T05:46:00+03:00
Target specification section: Sections 500-545, 690-740 & 1109-1124 of Master Spec

## Objective
Establish an extensible adapter framework with a centralized adapter registry, a clean `ContentExtractorAdapter` interface, adapter health checks, platform-specific URL canonicalizers, and a robust `GenericWebAdapter` capable of extracting OpenGraph and HTML meta tags (title, description, image, creator, canonical URL). Connect the adapter framework to the `ProcessingJobManager` background worker pipeline so saved items are enriched with real metadata.

## In scope
- [ ] `ContentExtractorAdapter` interface defining contract for all platform extractors.
- [ ] `ExtractedMetadata` and `AdapterHealth` domain models.
- [ ] `ContentAdapterRegistry` singleton providing adapter lookup, prioritization, dynamic registration, and health monitoring.
- [ ] `GenericWebAdapter` implementation extracting OpenGraph (`og:title`, `og:description`, `og:image`, `og:site_name`, `og:type`), Twitter cards, and standard HTML `<title>`, `<meta name="description">`, and `<link rel="canonical">`.
- [ ] Relative-to-absolute URL resolution and HTML entity decoding for extracted strings.
- [ ] Platform-specific URL canonicalization in `UrlNormalizer` and adapters.
- [ ] Integration with `ProcessingJobManager` worker loop: replace dummy milestones with real extraction, updating `ContentItem` title, original_title, caption, thumbnail_url, and status.
- [ ] Card and detail sheet display of extracted metadata and thumbnail preview.
- [ ] Comprehensive unit test suites (`AdapterRegistryTest.java`, `GenericWebAdapterTest.java`) with HTML/OpenGraph fixtures.
- [ ] Version bump to `0.5.0-phase5` in `build.gradle` and CI/CD workflow.
- [ ] Verified APK build published to GitHub Releases.

## Out of scope
- Official authenticated APIs for YouTube Data API, TikTok Display API, Instagram Graph API (scheduled for Phase 06).
- Video binary downloading, audio transcription with Whisper, OCR (scheduled for Phase 07).

## Preconditions
- Phase 04 completed, tested, and verified on physical device.
- Clean GitHub repository state at `Seifj84/Hifadhio` on `main`.

## Work units
- [ ] WU-01 Project state documentation initialization
- [ ] WU-02 `ContentExtractorAdapter` interface, `ExtractedMetadata`, and `AdapterHealth` models
- [ ] WU-03 `ContentAdapterRegistry` with prioritized lookup and health check API
- [ ] WU-04 `GenericWebAdapter` with OpenGraph/HTML parser and entity decoder
- [ ] WU-05 Connect `ProcessingJobManager` to extract real metadata and persist to `ContentDb`
- [ ] WU-06 UI updates to display extracted thumbnails, captions, and creator information
- [ ] WU-07 Unit test suites `AdapterRegistryTest` and `GenericWebAdapterTest` with HTML test fixtures
- [ ] WU-08 Version bump to `0.5.0-phase5` (versionCode 5) and CI/CD automated build
- [ ] WU-09 APK verification and release publishing

## Required tests
- [ ] Adapter registry registration and lookup: PASS
- [ ] Registry fallback to GenericWebAdapter: PASS
- [ ] OpenGraph extraction (title, description, image, site_name): PASS
- [ ] HTML fallback extraction (`<title>`, `<meta name="description">`): PASS
- [ ] HTML entity decoding and relative URL resolution: PASS
- [ ] Extraction error handling with sanitized messages: PASS
- [ ] Automated CI/CD build: PASS

## Phase acceptance criteria
1. Adding a new platform adapter does not require modifying core content-domain logic (Registry pattern).
2. Saving any standard web link or article extracts real title, description, and thumbnail without user manual input.
3. Content cards and detail bottom sheet display the extracted metadata and preview.
4. All unit tests pass in CI/CD, and an installable test APK v0.5.0-phase5 is published.

## Blockers
None.
