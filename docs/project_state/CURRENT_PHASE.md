# Current Phase

Phase: Phase 07
Name: Object Storage and Controlled Media Pipeline
Status: COMPLETED
Started: 2026-09-29T20:07:00+03:00
Completed: 2026-09-29T20:41:00+03:00
Target specification section: Sections 15.2, 19.1, 19.2 & 1155-1170 of Master Spec

## Objective
Establish a controlled, local-first object storage architecture and media pipeline for Hifadhio. Ensure all media artifacts (thumbnails, temporary processing files, future audio streams) are stored strictly as managed files in private app storage rather than database blobs. Implement deterministic object naming, per-item scoped access via Android `FileProvider`, retention lifecycles (`NO_RETENTION`, `TEMPORARY_PROCESSING`, `CACHE`, `LONG_TERM`), and an automated cleanup worker that prevents orphaned files and enforces storage quotas.

## In scope
- [x] Database Schema v4 migration in `ContentDb.java`: add `artifacts` table storing metadata, storage path, content URI, MIME type, size, SHA256, retention policy, and expiration.
- [x] Domain models: `MediaArtifact.java` and `MediaRetentionPolicy.java`.
- [x] `MediaStorageManager.java`: manages partitioned directories (`media/thumbnails/`, `media/temp/`, `media/audio/`, `media/artifacts/`), deterministic object naming, SHA256 hashing, thumbnail caching, and item-scoped file deletion.
- [x] Secure access: `res/xml/file_paths.xml` and `FileProvider` declaration in `AndroidManifest.xml` for scoped, temporary URI access.
- [x] `MediaCleanupWorker.java`: automated cleanup engine sweeping expired temporary files, orphaned disk objects, and enforcing cache quotas.
- [x] Pipeline integration in `ProcessingJobManager.java`: automatically caches remote thumbnails to object storage upon metadata extraction, links artifacts, and purges media on item deletion.
- [x] UI integration: offline thumbnail loading in cards and detail sheet, storage usage inspection in Profile.
- [x] Unit test suites: `MediaStorageManagerTest.java` and `MediaCleanupWorkerTest.java`.
- [x] Version bump to `0.7.0-phase7` (versionCode 7), automated CI/CD build, and verified APK release.

## Out of scope
- Cloud S3/R2 multi-tenant cloud bucket sync (deferred to server sync phase).
- Audio transcription with Whisper (scheduled for Phase 08).
- Full video frame OCR (scheduled for Phase 09).

## Preconditions
- Phase 06 completed, verified, and published as `v0.6.0-phase6`.
- Clean working directory on `main`.

## Work units
- [x] WU-01 Phase 07 state initialization & Database Schema v4 migration (`artifacts` table)
- [x] WU-02 `MediaArtifact` model, `MediaRetentionPolicy`, and `MediaStorageManager`
- [x] WU-03 `FileProvider` setup, `file_paths.xml`, and scoped media access
- [x] WU-04 `MediaCleanupWorker` lifecycle engine (expired file cleanup, orphan detection, quota limit)
- [x] WU-05 Pipeline integration (`ProcessingJobManager` thumbnail caching, delete cascade, offline display)
- [x] WU-06 Unit test suites `MediaStorageManagerTest` and `MediaCleanupWorkerTest`
- [x] WU-07 Version bump to `0.7.0-phase7` (versionCode 7), CI/CD verification, and APK release

## Phase acceptance criteria
1. No binary media blobs are written into SQLite tables: all media resides in partitioned object storage.
2. Saving items with thumbnails downloads and caches images into private object storage with SHA256 integrity verification.
3. Content cards and detail bottom sheet load cached thumbnails offline.
4. Deleting a content item cascades to remove all associated media files from storage.
5. Cleanup worker successfully detects and removes orphaned and expired storage objects without data loss.
6. All unit tests pass in CI/CD, and an installable test APK v0.7.0-phase7 is published.
