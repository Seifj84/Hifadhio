# Phase 07 Completion Report: Object Storage & Controlled Media Pipeline

> **Save once. Find it when it matters.**
> A NAS Digital Solutions product • engineered by SeifTech.

- **Phase**: Phase 07 — Object Storage and Controlled Media Pipeline
- **Status**: COMPLETED
- **Target Specification**: Sections 550-590, 750-785 & 1155-1190 of Master Spec
- **Version**: `0.7.0-phase7` (versionCode 7)
- **GitHub Release**: [`v0.7.0-phase7`](https://github.com/Seifj84/Hifadhio/releases/tag/v0.7.0-phase7)
- **CI Build Run**: [#36606195301](https://github.com/Seifj84/Hifadhio/actions/runs/36606195301) (Conclusion: `success`)
- **Direct APK**: `Hifadhio-v0.7.0-phase7-debug.apk` (6,315,409 bytes)
- **SHA256**: `A9B38BEC9EA443949710A98EC2DFE1B81500738039283894D3C38ED87BC0C94A`

---

## 1. Executive Summary

Phase 07 establishes Hifadhio's private, controlled local object storage architecture and media lifecycle management. In strict adherence to the Master Specification, media files (thumbnails, temporary extraction scratchpads, future audio streams, and export bundles) are **never** stored as SQLite database BLOBs. Instead, media artifacts are partitioned into dedicated local disk directories with deterministic naming and SHA256 integrity verification, backed by a SQLite Schema v4 metadata registry (`artifacts` table).

All 15 project unit test suites execute cleanly on GitHub Actions CI/CD with 0 failures, and verified release APK `Hifadhio-v0.7.0-phase7-debug.apk` has been published to GitHub Releases and archived in the workspace repository.

---

## 2. Core Architecture & Storage Hierarchy

### 2.1 Partitioned Directory Structure
Internal application storage (`Context.getFilesDir()`) is partitioned into isolated media directories:
- `media/thumbnails/`: High-resolution and preview cover images for captured links and platform videos.
- `media/temp/`: Scratchpad space for active processing jobs, intermediate downloads, and payload transformations. Automatically evicted after 1 hour or upon job termination.
- `media/audio/`: Extracted or transcoded audio tracks (reserved for future transcription/media streams).
- `media/artifacts/`: Export packages, search index dumps, and persistent media bundles.

### 2.2 Deterministic Naming & Integrity
Object file names follow deterministic patterns to eliminate collisions and facilitate direct disk identification:
- Format: `item_{itemId}_{type}_{sha256Prefix8}.{ext}`
- Example: `item_42_thumb_a1b2c3d4.jpg`
- SHA256 hashes are computed dynamically upon creation and verified before indexing.

---

## 3. SQLite Database Migration: Schema v4

`ContentDb.java` bumped database version to `4` and introduced `TABLE_ARTIFACTS`:

```sql
CREATE TABLE artifacts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    storage_path TEXT NOT NULL,
    content_uri TEXT,
    mime_type TEXT NOT NULL,
    file_size INTEGER NOT NULL,
    sha256 TEXT NOT NULL,
    retention_policy TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER,
    FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE INDEX idx_artifacts_item_id ON artifacts(item_id);
CREATE INDEX idx_artifacts_expires ON artifacts(expires_at);
```

### Key Migration Capabilities:
- **Foreign Key Cascade**: Items cascade to artifacts in the database schema.
- **Physical File Deletion**: In `deleteArtifactsForItem(long itemId)` and `delete(long id)`, the system queries all registered physical files from SQLite and immediately deletes them from disk prior to dropping the database records, eliminating orphaned storage.
- **Quota & Diagnostics Queries**: Includes `getAllArtifactStoragePaths()`, `getExpiredArtifacts(long now)`, and `getTotalArtifactsSize()`.

---

## 4. Domain Models & Retention Lifecycle

### 4.1 `MediaRetentionPolicy.java`
Controls lifecycle handling for all media artifacts:
- `NO_RETENTION`: Discarded immediately after processing task completes.
- `TEMPORARY_PROCESSING`: Scratchpad files with short-lived expiration (default: 1 hour).
- `CACHE`: Re-fetchable assets (e.g. thumbnails) subject to LRU / size quota eviction (default: 30 days).
- `LONG_TERM`: Permanent user assets preserved until explicitly deleted by the user.

### 4.2 `MediaArtifact.java`
Thread-safe immutable domain entity capturing artifact metadata:
- Fields: `id`, `itemId`, `storagePath`, `contentUri`, `mimeType`, `fileSize`, `sha256`, `retentionPolicy`, `createdAt`, `expiresAt`.
- Helper: `isExpired(long currentTimeMillis)` for automated cleanup sweeps.

---

## 5. Storage Engine (`MediaStorageManager.java`)

Central manager responsible for physical I/O and object lifecycle:
- `saveThumbnail(long itemId, Bitmap bitmap)`: Encodes bitmap to JPEG into `media/thumbnails/` with deterministic hash and registers artifact in `ContentDb`.
- `cacheRemoteThumbnail(long itemId, String remoteUrl)`: Downloads remote HTTP thumbnail streams with 10s timeouts, safe stream buffering, and automatic database indexing.
- `createTempFile(long itemId, String prefix, String suffix)`: Generates scoped scratchpad files in `media/temp/`.
- `getArtifactFile(MediaArtifact artifact)`: Validates and returns physical `File` reference.
- `getShareableUri(MediaArtifact artifact)`: Produces scoped `content://` URIs via Android `FileProvider`.
- `deleteArtifact(long artifactId)`: Atomically deletes physical file and removes database record.
- `deleteItemMedia(long itemId)`: Sweeps all physical media files for an item across all partitions.

---

## 6. Lifecycle & Quota Cleanup Worker (`MediaCleanupWorker.java`)

Self-contained background maintenance worker implementing periodic sweeping rules:
1. **Expired Artifacts Sweep**: Queries `ContentDb.getExpiredArtifacts(now)` and deletes expired physical files and database rows.
2. **Stale Temp Files Sweep**: Scans `media/temp/` and purges any file older than 1 hour, even if unregistered in SQLite.
3. **Orphaned Files Detection**: Reconciles files on disk in `media/` with registered paths in `ContentDb.getAllArtifactStoragePaths()`. Unregistered files older than 1 hour are removed.
4. **Cache Quota Enforcement**: If `media/thumbnails/` exceeds 50 MB (`DEFAULT_MAX_CACHE_BYTES`), prunes oldest thumbnail files by modification date until usage is within quota.

---

## 7. Android FileProvider Scoped Access

Registered `androidx.core.content.FileProvider` in `AndroidManifest.xml` with authority `com.seiftech.hifadhio.fileprovider` and path configuration `res/xml/file_paths.xml`:

```xml
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <files-path name="media" path="media/" />
    <files-path name="thumbnails" path="media/thumbnails/" />
    <files-path name="temp" path="media/temp/" />
    <files-path name="audio" path="media/audio/" />
    <files-path name="artifacts" path="media/artifacts/" />
</paths>
```

This guarantees that external intents (sharing, viewing) access media strictly via temporary permission grants without ever exposing raw filesystem paths.

---

## 8. Pipeline & UI Integrations

### 8.1 Automated Thumbnail Caching in `ProcessingJobManager`
Upon metadata extraction from YouTube, TikTok, Reddit, OpenGraph, or platform adapters:
- If a valid thumbnail URL is discovered, `ProcessingJobManager` immediately triggers `MediaStorageManager.cacheRemoteThumbnail(item.id, thumbnailUrl)`.
- Updates `item.previewUrl` with the cached local storage path or preserves original URL with local backing.

### 8.2 Content Detail Bottom Sheet (`ContentDetailBottomSheet.java`)
- **Offline Thumbnail Rendering**: Reads cached thumbnail from `MediaStorageManager` or local storage path using efficient in-memory `BitmapFactory` decode.
- **Diagnostic Storage Info**: Displays real-time storage info badge (`tv_tech_storage_info`) indicating local file size, resolution, and caching status.

### 8.3 Profile Tab Media Maintenance (`ProfileFragment.java`)
- **Live Cache Utilization**: Computes and displays real-time disk consumption for media thumbnails and temp storage.
- **Maintenance Action**: One-tap "Clean Cache" triggers `MediaCleanupWorker.runCleanup()`, reclaiming storage and updating UI dynamically.

---

## 9. Verification & Test Suites

The test suite expanded to **15 comprehensive unit test suites** with 100% passing tests:

| Test Suite | Coverage Area | Status |
|:---|:---|:---:|
| `MediaStorageManagerTest.java` | Partition management, deterministic naming, hash calculation, bitmap saving, remote caching, cascade deletion | PASS |
| `MediaCleanupWorkerTest.java` | Expired artifact sweeping, stale temp file purging, orphaned file detection, quota pruning | PASS |
| `PlatformAdaptersTest.java` | YouTube, TikTok, Instagram, Facebook, Reddit, X adapters | PASS |
| `ContentExtractorAdapterTest.java` | Adapter registry, priority sorting, fallback dispatch | PASS |
| `UrlCanonicalizerTest.java` | Tracking parameter stripping, URL normalization | PASS |
| `ContentDbTest.java` | Schema v4 migrations, CRUD operations, transactions | PASS |
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

## 10. Deliverables & Build Artifacts

- **Debug APK**: `Hifadhio-v0.7.0-phase7-debug.apk` (6,315,409 bytes)
- **SHA256**: `A9B38BEC9EA443949710A98EC2DFE1B81500738039283894D3C38ED87BC0C94A`
- **GitHub Release Tag**: `v0.7.0-phase7`
- **Release Records Location**: `release_records/phase_07_object_storage/v0.7.0/`

---

## 11. Master Specification Compliance Matrix

| Section | Master Spec Requirement | Phase 07 Status |
|:---|:---|:---:|
| §550-555 | Private internal app storage for media assets | COMPLIED (`MediaStorageManager`) |
| §556-560 | Strict prohibition of SQLite BLOB media storage | COMPLIED (Schema v4 `artifacts` table) |
| §561-570 | Deterministic naming (`item_{id}_{type}_{hash}`) | COMPLIED |
| §571-580 | SHA256 integrity checks on stored artifacts | COMPLIED |
| §581-590 | Cascade deletion of physical files on item drop | COMPLIED (`ContentDb.delete` & `deleteArtifactsForItem`) |
| §750-760 | Automated remote thumbnail caching pipeline | COMPLIED (`ProcessingJobManager`) |
| §761-770 | Android `FileProvider` scoped URI access | COMPLIED (`com.seiftech.hifadhio.fileprovider`) |
| §771-785 | Cache quota enforcement & orphaned file sweeping | COMPLIED (`MediaCleanupWorker`) |
| §1155-1190| User storage diagnostics & manual maintenance | COMPLIED (`ProfileFragment` & `ContentDetailBottomSheet`) |
