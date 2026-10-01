# Current Phase

Phase: Phase 09
Name: Visual Frame Extraction and OCR Engine
Status: IN_PROGRESS
Started: 2026-10-01T05:25:00+03:00
Completed: 
Target specification section: Sections 15.4, 20.2, 35 (ADR-007/ADR-010) & 1189-1202 of Master Spec

## Objective
Implement a resource-efficient visual frame sampling and optical character recognition (OCR) engine for Hifadhio. Provide an extensible provider abstraction (`OcrProvider`), selective keyframe sampling policy to avoid compute/storage bloat, intelligent text deduplication of persistent on-screen titles/watermarks, deterministic SQLite Schema v6 (`ocr_records` table) persistence, search-on-detail UI with live filtering and frame timestamp chips, and seamless integration with the background processing queue.

## In scope
- [ ] Database Schema v6 migration in `ContentDb.java`: add `ocr_records` table (`id`, `content_item_id UNIQUE`, `full_text`, `provider_id`, `model`, `frames_count`, `frames_json`, `cost_usd`, timestamps), index on `content_item_id`, and search integration.
- [ ] Domain models: `OcrFrame.java`, `OcrRecord.java`, `OcrOptions.java`, `OcrResult.java`.
- [ ] Provider architecture: `OcrProvider` interface, `OcrRegistry`, `OnDeviceOcrProvider` (on-device local OCR engine), and `CloudVisionOcrProvider` (cloud vision OCR abstraction with cost tracking).
- [ ] Frame sampling & text deduplication: `FrameExtractor.java` (selective periodic/keyframe sampling, max frame limit, scratchpad management) and `TextDeduplicator.java` (token/phrase overlap reduction across consecutive frames).
- [ ] Processing pipeline integration: `ProcessingJob.TYPE_OCR = "OCR"` in `ProcessingJobManager.java`, automated chaining on visual media, artifact registration in `media/artifacts/`, and scratchpad eviction.
- [ ] UI integration: `ContentDetailBottomSheet` Visual Text / OCR card with search-on-detail live filter, frame timestamp chips, copy action, and manual trigger.
- [ ] Unit test suites: `OcrModelTest.java`, `OcrProviderTest.java`, `FrameExtractorTest.java`.
- [ ] Version bump to `0.9.0-phase9` (versionCode 9), CI/CD verification, and APK release.

## Out of scope
- AI summaries and RAG embeddings (scheduled for Phase 10 & 12).
- Real-time video playback syncing (scheduled for Phase 14).
- Direct screen recording capture.

## Preconditions
- Phase 08 completed, verified, and published as `v0.8.0-phase8`.
- Clean working directory on `main`.

## Work units
- [ ] WU-01 Database Schema v6 migration (`ocr_records` table, foreign keys, cascade deletion, queries)
- [ ] WU-02 OCR domain models (`OcrFrame`, `OcrRecord`, `OcrOptions`, `OcrResult`)
- [ ] WU-03 `OcrProvider` interface, `OcrRegistry`, and providers (`OnDeviceOcrProvider`, `CloudVisionOcrProvider`)
- [ ] WU-04 `FrameExtractor` sampling policy & `TextDeduplicator` algorithm
- [ ] WU-05 `ProcessingJobManager` OCR pipeline integration & artifact archiving
- [ ] WU-06 UI OCR rendering with search-on-detail & frame timestamp chips in `ContentDetailBottomSheet`
- [ ] WU-07 Unit test suites (`OcrModelTest`, `OcrProviderTest`, `FrameExtractorTest`)
- [ ] WU-08 Version bump to `0.9.0-phase9` (versionCode 9), CI/CD verification, and release packaging

## Phase acceptance criteria
1. Database Schema v6 seamlessly migrates existing v5 databases with clean foreign keys and indexes.
2. OCR provider abstraction allows runtime provider registration without hardcoded engines.
3. Frame sampling selectively samples frames (max 15 frames) without exploding compute or storage.
4. Consecutive on-screen text duplicates (watermarks, titles) are deduplicated while preserving frame references.
5. Extracted visual text is stored in SQLite, registered in object storage as an immutable artifact, and indexed in search.
6. Content detail sheet provides seamless reading, searching within visual text, and copying.
7. All unit tests pass in CI/CD, and an installable debug APK `v0.9.0-phase9` is produced.
