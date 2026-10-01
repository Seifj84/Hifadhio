# Current Phase

Phase: Phase 10
Name: AI Enrichment Engine
Status: IN_PROGRESS
Started: 2026-10-02T00:20:00+03:00
Completed: 
Target specification section: Sections 15.5, 18, 19, 20.2, 35 (ADR-008/ADR-010) & 1203–1221 of Master Spec

## Objective
Implement a multi-modal AI Enrichment Engine providing vendor-neutral AI provider abstractions (`AiProvider`, `AiRegistry`), strict JSON schema validation, prompt versioning (`PromptManager`), dual-tier summaries (concise and detailed), key points extraction, topic classification, entity recognition, tag suggestions, collection recommendations, SQLite Schema v7 persistence (`ai_enrichments` table), immutable object storage artifact mirroring (`media/artifacts/`), background job pipeline integration (`TYPE_AI_ENRICHMENT`), library-wide search integration, and interactive UI in `ContentDetailBottomSheet`.

## In scope
- [x] Database Schema v7 migration in `ContentDb.java`: add `ai_enrichments` table with cascade deletion and search index integration.
- [x] Domain models: `AiEntity.java`, `AiEnrichment.java`, `AiOptions.java`, `AiResult.java`.
- [x] Prompt management: `PromptManager.java` for prompt versioning (`v1.0.0`), context assembly (item + transcript + OCR), and strict JSON schema validation.
- [x] Provider architecture: `AiProvider.java` interface, `AiRegistry.java`, `LocalHeuristicAiProvider.java` (Priority 100, $0.00 cost, offline), and `CloudLlmAiProvider.java` (Priority 50).
- [x] Media storage artifact persistence: `saveAiEnrichmentArtifact` in `MediaStorageManager.java` with SHA256 integrity and `TYPE_AI_ENRICHMENT`.
- [x] Background queue integration: `ProcessingJob.TYPE_AI_ENRICHMENT` in `ProcessingJobManager.java`, automated pipeline chaining, and reprocess support.
- [x] UI integration: AI Enrichment Card in `dialog_content_detail.xml` and `ContentDetailBottomSheet.java` with "AI GENERATED" label, summaries, key takeaways, entities, interactive tags, and collection move.
- [x] Unit test suites: `AiModelTest.java`, `AiProviderTest.java`, `PromptManagerTest.java`.
- [x] App version bump to `0.10.0-phase10` (versionCode 10), CI/CD verification, and release packaging.

## Out of scope
- Full-text search chunks with BM25 / vector rankings (scheduled for Phase 11 & 12).
- Semantic RAG Q&A chat streaming (scheduled for Phase 13).
- Real-time video player synchronization (scheduled for Phase 14).

## Preconditions
- Phase 09 completed, verified, and published as `v0.9.0-phase9`.
- Clean working directory on `main`.

## Work units
- [x] WU-01 Database Schema v7 migration (`ai_enrichments` table, foreign keys, cascade deletion, queries)
- [x] WU-02 AI domain models (`AiEntity`, `AiEnrichment`, `AiOptions`, `AiResult`)
- [x] WU-03 `PromptManager` context assembly, prompt building, and strict schema validation
- [x] WU-04 `AiProvider` interface, `AiRegistry`, and providers (`LocalHeuristicAiProvider`, `CloudLlmAiProvider`)
- [x] WU-05 Storage manager artifact persistence (`MediaStorageManager.saveAiEnrichmentArtifact`)
- [x] WU-06 `ProcessingJobManager` AI pipeline integration, auto-chaining, and reprocess support
- [x] WU-07 UI AI Enrichment Card in `dialog_content_detail.xml` and `ContentDetailBottomSheet`
- [x] WU-08 Unit test suites (`AiModelTest`, `AiProviderTest`, `PromptManagerTest`)
- [x] WU-09 App version bump to `0.10.0-phase10` (versionCode 10), workflow updates, CI/CD verification, and release packaging

## Phase acceptance criteria
1. Database Schema v7 seamlessly migrates existing databases with clean foreign keys and indexes.
2. AI provider abstraction allows runtime provider registration and fallback without hardcoded vendor locks.
3. Strict JSON schema is enforced; invalid outputs are rejected rather than patched with invented defaults.
4. Prompt versioning is recorded and reproducible, supporting on-demand reprocessing.
5. Extracted enrichment data is stored in SQLite, mirrored into object storage as immutable JSON, and indexed in search.
6. Content detail sheet provides clear "AI GENERATED" attribution, dual-tier summaries, key takeaways, and interactive tag/collection actions.
7. All 24 unit test suites pass in CI/CD, and an installable debug APK `v0.10.0-phase10` is produced.
