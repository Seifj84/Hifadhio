# Phase 09 Completion Report: Visual Frame Extraction and OCR Engine

> **Save once. Find it when it matters.**  
> A NAS Digital Solutions product • engineered by SeifTech.

- **Phase**: Phase 09 — Visual Frame Extraction and OCR Engine
- **Status**: COMPLETED
- **Target Specification**: Sections 15.4, 20.2, 35 (ADR-007/ADR-010) & 1189-1202 of Master Spec
- **Version**: `0.9.0-phase9` (versionCode 9)
- **GitHub Release**: [`v0.9.0-phase9`](https://github.com/Seifj84/Hifadhio/releases/tag/v0.9.0-phase9)
- **Direct APK**: `Hifadhio-v0.9.0-phase9-debug.apk`

---

## 1. Executive Summary

Phase 09 introduces Hifadhio's resource-conscious visual frame extraction and optical character recognition (OCR) engine. Designed in strict compliance with Master Spec §15.4 and §1189–§1202, the system extracts visible on-screen text from text-heavy videos, creator slides, screenshots, infographics, and saved media without exploding compute or storage costs.

The architecture isolates OCR vendors behind an extensible provider abstraction (`OcrProvider`), enforces an intelligent periodic keyframe sampling policy (max 15 frames per video, zero timestamp fabrication for static images), suppresses persistent on-screen watermarks and repeated slide headers through algorithmic text deduplication (`TextDeduplicator`), persists structured OCR records into SQLite database Schema v6 (`ocr_records` table), and mirrors full visual transcripts into private object storage (`media/artifacts/`) as immutable, checksum-verified text artifacts.

In the user interface, `ContentDetailBottomSheet` features a dedicated Visual Text (OCR) card with provider/frame badges, live within-visual-text search ("search-on-detail" §1181), clickable frame timestamp chips, and one-tap clipboard copying. Furthermore, all extracted visual text is automatically indexed into the main library search query (`ContentDb.search()`), making any on-screen text or slide headline in saved videos instantly searchable across the entire library.

All 21 unit test suites pass cleanly in CI/CD, and the verified debug APK `Hifadhio-v0.9.0-phase9-debug.apk` is released and published.

---

## 2. Architecture & Provider Abstraction (§1194)

### 2.1 Provider Interface (`OcrProvider`)
Defines the clean vendor-neutral contract for optical character recognition:
- `getProviderId()`: Unique string identifier (e.g. `on_device_ocr`, `cloud_vision`).
- `getDisplayName()`: Human-readable provider label.
- `getPriority()`: Integer rank for automated selection (higher = preferred).
- `isAvailable()`: Health and prerequisite check.
- `isOffline()`: Denotes whether execution is strictly local.
- `estimateCost(int frameCount)`: Computes anticipated financial cost in USD.
- `processFrames(long contentItemId, List<OcrFrame> frames, OcrOptions options)`: Executes text extraction across sampled frames.

### 2.2 Provider Registry (`OcrRegistry`)
Thread-safe singleton managing prioritized provider dispatch:
1. **`OnDeviceOcrProvider` (Priority 100)**: First attempted for all requests; performs on-device local optical character recognition at zero financial cost ($0.00) without network dependencies.
2. **`CloudVisionOcrProvider` (Priority 50)**: Standard cloud vision API client abstraction (Google Cloud Vision / OpenAI Vision compatible) with precise usage tracking ($0.0015 per frame / $1.50 per 1,000 frames).

---

## 3. Database Migration: Schema v6 (`ocr_records` table)

`ContentDb.java` bumped database version to `6` and introduced `TABLE_OCR_RECORDS`:

```sql
CREATE TABLE ocr_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    content_item_id INTEGER NOT NULL UNIQUE,
    full_text TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    model TEXT,
    frames_count INTEGER DEFAULT 0,
    frames_json TEXT,
    cost_usd REAL DEFAULT 0.0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(content_item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE INDEX idx_ocr_item ON ocr_records(content_item_id);
CREATE INDEX idx_ocr_created ON ocr_records(created_at);
```

### Key Migration Capabilities:
- **Foreign Key Cascade Deletion**: Deleting a `ContentItem` automatically cascades to remove its OCR records and temporary scratchpad frames.
- **Library-wide Visual Search**: Updated `ContentDb.search()` with:
  ```sql
  OR id IN (SELECT content_item_id FROM ocr_records WHERE full_text LIKE ?)
  ```
  Enables instant full-text searching across all on-screen video text and infographic content from the main search bar.

---

## 4. Intelligent Text Deduplication Engine (`TextDeduplicator`, §1195)

Videos and presentations frequently exhibit persistent creator watermarks, channel logos, and static title banners that remain on screen for minutes across dozens of frames. Without deduplication, visual text extraction would fill storage and search indices with noisy, repetitive lines.

`TextDeduplicator` applies a multi-stage filtering algorithm:
1. **Persistent Global Watermark Detection**: Measures line frequencies across all sampled frames; lines appearing in >= 60% of frames are identified as static branding and retained only once in the aggregated full text.
2. **Adjacent Slide Suppression**: Eliminates identical or near-identical consecutive lines across adjacent sampled frames (token overlap > 85%).
3. **Clean Provenance Preservation**: Retains true frame timestamps while presenting a readable, deduplicated chronological transcript of visual content.

---

## 5. Selective Frame Sampling Subsystem (`FrameExtractor`, §15.4 & §1193)

Located in `com.seiftech.hifadhio.ocr.FrameExtractor`:
- **Eligibility Verification**: Identifies visual media platforms (YouTube, TikTok, Instagram, Facebook, Reddit, X) or direct video/image URLs (`.mp4`, `.png`, `.jpg`, `.webp`) or items with cached thumbnails.
- **Selective Sampling Policy**:
  - Sample interval: 1 frame every 5 seconds (configurable via `OcrOptions`).
  - Max frames limit: Hard cap of 15 frames per video to prevent compute, memory, and battery exhaustion (§1434).
- **True Timestamps Rule**:
  - Video frames preserve genuine millisecond offsets (`timestampMs`).
  - Static images and screenshots strictly preserve `-1L` (`hasTimestamp() == false`) without fabricating timestamps.
- **Scratchpad Lifecycle**: Allocates temporary frames in `media/frames/` and cleans them up after OCR processing completes.

---

## 6. Pipeline & Background Queue Integration

`ProcessingJobManager` was enhanced with:
- `ProcessingJob.TYPE_OCR = "OCR"`: Dedicated asynchronous processing job.
- **Automated Chaining**: When metadata extraction completes successfully, `ProcessingJobManager` evaluates `FrameExtractor.isEligibleForOcr(item)`. If eligible, it enqueues `TYPE_OCR`.
- **Traceable Object Storage Mirroring**: Every extracted OCR result is stored in SQLite and written to `media/artifacts/item_{id}_ocr_{hash}.txt` with SHA256 integrity hashing and `MediaRetentionPolicy.LONG_TERM`.
- **Manual Trigger API**: `enqueueOcr(contentItemId)` allows users or UI buttons to request visual text extraction on demand.

---

## 7. UI Integration & Search-on-Detail (§1181)

`ContentDetailBottomSheet.java` and `dialog_content_detail.xml` feature a dedicated Visual Text (OCR) Card:
1. **Header & Badges**:
   - Provider badge (e.g. `on_device_ocr` or `Eligible`).
   - Sampled frames count badge (e.g. `4 frames`).
   - One-tap "Copy Visual Text" button.
2. **Search-on-Detail Filter Bar (§1181)**:
   - Live search input dynamically filtering visual text and frame segments in real time.
3. **Frame Timestamp Chips**:
   - Displays timecoded frame chips (e.g. `[00:05] Slide: Clean Architecture`, `[00:10] Code Sample...`) or `[Frame X]` for images.
4. **On-Demand Action**:
   - "Extract Visual Text (OCR)" action button displayed for eligible items lacking OCR records.

---

## 8. Verification & Test Suites

The test suite expanded to **21 comprehensive unit test suites** with 100% passing tests:

| Test Suite | Coverage Area | Status |
|:---|:---|:---:|
| `OcrModelTest.java` | Frame timestamp formatting, JSON roundtrip, search-on-detail live filter, text deduplication of repeated watermarks/slides | PASS |
| `OcrProviderTest.java` | Provider registry, priority ordering, on-device local OCR, cloud vision cost estimation ($0.0015/frame), offline checks | PASS |
| `FrameExtractorTest.java` | Platform visual eligibility, video interval sampling, max frames limit enforcement, true timestamps on static images | PASS |
| `TranscriptModelTest.java` | Segment timestamp formatting, JSON roundtrip, no-fabricated-timestamps guarantee, duration formatting, search-on-detail | PASS |
| `TranscriptionProviderTest.java` | Provider registry, priority sorting, SRT parsing, VTT parsing, offline speech handling, cloud cost estimation | PASS |
| `AudioExtractorTest.java` | Platform media eligibility, missing file validation, empty file validation, valid file validation | PASS |
| `MediaStorageManagerTest.java` | Partition management, deterministic naming, hash calculation, bitmap saving, remote caching, cascade deletion | PASS |
| `MediaCleanupWorkerTest.java` | Expired artifact sweeping, stale temp file purging, orphaned file detection, quota pruning | PASS |
| `PlatformAdaptersTest.java` | YouTube, TikTok, Instagram, Facebook, Reddit, X adapters | PASS |
| `ContentExtractorAdapterTest.java` | Adapter registry, priority sorting, fallback dispatch | PASS |
| `UrlCanonicalizerTest.java` | Tracking parameter stripping, URL normalization | PASS |
| `ContentDbTest.java` | Schema v6 migrations, CRUD operations, transactions | PASS |
| `ContentRepositoryTest.java` | Cache invalidation, query filtering, repository layer | PASS |
| `ProcessingJobManagerTest.java` | Extraction queue, retry handling, status updates | PASS |
| `NetworkClientTest.java` | HTTP timeouts, headers, redirect handling | PASS |
| `OpenGraphExtractorTest.java` | HTML tag parsing, fallback metadata | PASS |
| `PlatformTypeTest.java` | Platform classification and regex matching | PASS |
| `SearchEngineTest.java` | FTS search queries, match scoring | PASS |
| `TagEngineTest.java` | Tag extraction, association, indexing | PASS |
| `ExportEngineTest.java` | JSON/HTML/ZIP bundle generation | PASS |
| `ImportEngineTest.java` | Backup restore, schema validation | PASS |

---

## 9. Deliverables & Build Artifacts

- **Debug APK**: `Hifadhio-v0.9.0-phase9-debug.apk`
- **Version**: `0.9.0-phase9` (versionCode 9)
- **GitHub Release Tag**: `v0.9.0-phase9`
- **Release Records Location**: `release_records/phase_09_ocr/v0.9.0/`

---

## 10. Master Specification Compliance Matrix

| Section | Master Spec Requirement | Phase 09 Status |
|:---|:---|:---:|
| §15.4 | Use selectively, not on every video frame | COMPLIED (`FrameExtractor` periodic keyframe sampling) |
| §15.4 | Candidate approaches: periodic key frames, scene changes, slides | COMPLIED (Periodic sampling with 5s intervals and slide detection) |
| §15.4 | Store OCR output separately from transcript | COMPLIED (Dedicated `ocr_records` table & `MediaArtifact.TYPE_OCR`) |
| §1193 | Frame sampling policy (<15 frames cap) | COMPLIED (`OcrOptions.maxFrames = 15`) |
| §1194 | OCR provider interface and registry | COMPLIED (`OcrProvider`, `OcrRegistry`) |
| §1195 | Deduplication of repeated on-screen text | COMPLIED (`TextDeduplicator` watermark and adjacent line suppression) |
| §1196 | Artifact provenance and object storage mirroring | COMPLIED (`media/artifacts/item_{id}_ocr_{hash}.txt` with SHA256) |
| §1197 | Optional timestamp / frame references | COMPLIED (`OcrFrame` with `timestampMs` and `frameIndex`) |
| §1181 | Search-on-detail live filtering in UI | COMPLIED (`ContentDetailBottomSheet.filterOcr`) |
| §1434 | Sample OCR frames intelligently | COMPLIED |
| §920 | Normalized error taxonomy (`OCR_FAILED`, `NO_FRAMES_EXTRACTED`) | COMPLIED (`OcrResult`) |
| §1201 | OCR improves text-heavy videos without exploding compute/storage cost | COMPLIED |
