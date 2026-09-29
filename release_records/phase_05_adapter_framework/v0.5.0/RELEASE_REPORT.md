# Release Record: v0.5.0-phase5

**App Name:** Hifadhio  
**Brand:** NAS Digital Solutions • engineered by SeifTech  
**Phase:** Phase 05 - Adapter Framework + Generic Web Metadata  
**Version:** 0.5.0-phase5 (versionCode 5)  
**Date:** 2026-09-29  
**Git Commit:** `95920a4`  
**GitHub Action Run:** [#36514529976](https://github.com/Seifj84/Hifadhio/actions/runs/36514529976)  
**GitHub Release:** [v0.5.0-phase5](https://github.com/Seifj84/Hifadhio/releases/tag/v0.5.0-phase5)  

---

## 1. Summary of Changes
- **Modular Adapter Architecture (`com.seiftech.hifadhio.adapter`):**
  - `ContentExtractorAdapter`: Canonical extraction interface with platform identifier, priority ordering, URL match predicate (`canHandle`), URL canonicalizer, and health check.
  - `ExtractedMetadata`: Standardized container for title, description, thumbnail URL, creator name/handle, canonical URL, platform, and content type.
  - `AdapterHealth`: Standardized health status and diagnostics container.
- **ContentAdapterRegistry:**
  - Thread-safe singleton registry implementing dynamic adapter registration and priority routing.
  - Guarantees Master Spec Phase 5 exit criterion: *"Adding a new platform adapter does not require modifying core content-domain logic."*
  - Automatically routes matching URLs to specialized platform adapters while falling back universally to `GenericWebAdapter`.
- **GenericWebAdapter:**
  - High-performance, lightweight HTML and OpenGraph metadata extractor without third-party library bloat.
  - Extracts OpenGraph tags (`og:title`, `og:description`, `og:image`, `og:site_name`, `og:type`), Twitter cards, and standard HTML `<title>`, `<meta name="description">`, `<meta name="author">`, and `<link rel="canonical">`.
  - Handles arbitrary attribute ordering (`content` before or after `property`/`name`) and single or double quotes.
  - Resolves protocol-relative (`//`) and relative paths against the base URL.
  - Robust HTML entity decoder supporting named entities (`&amp;`, `&quot;`, `&#39;`, `&mdash;`, `&hellip;`) as well as decimal and hexadecimal unicode escapes.
- **Background Processing Engine Integration:**
  - `ProcessingJobManager` directly queries `ContentAdapterRegistry` during `METADATA_FETCH` jobs.
  - Replaces simulated milestones with real extraction, updating `title`, `original_title`, `caption`, `thumbnail_url`, and `platform` in `ContentDb`.
  - Automatic error isolation with exponential backoff retries if network fetching fails.
- **UI Enrichments:**
  - Added extracted caption/description display in `ContentDetailBottomSheet` and cards.
  - Card previews reflect extracted metadata dynamically.
- **Unit Testing Suite:**
  - `AdapterRegistryTest.java`: Testing default registration, fallback routing, dynamic priority injection, unregistering, and health checks.
  - `GenericWebAdapterTest.java`: Testing OpenGraph extraction, reversed attribute orders, HTML fallbacks, entity decoding, relative URL resolution, and canonicalization.

---

## 2. Verification & Test Evidence
- **Unit Tests:** `AdapterRegistryTest`, `GenericWebAdapterTest`, `ProcessingJobTest`, `CollectionsAndTagsTest`, `UrlNormalizerTest` passed on CI.
- **CI Build:** Clean execution in Gradle 8.9 / OpenJDK 17.

---

## 3. Artifact Download
- Direct APK: [Hifadhio-v0.5.0-phase5-debug.apk](https://github.com/Seifj84/Hifadhio/releases/download/v0.5.0-phase5/Hifadhio-v0.5.0-phase5-debug.apk)
- Local Path: `G:\Other computers\My PC (1)\Coding Project\Hifadhio\Hifadhio-v0.5.0-phase5-debug.apk`
