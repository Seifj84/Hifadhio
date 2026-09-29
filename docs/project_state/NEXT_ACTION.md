# Next Action

Phase: Phase 07 — Object Storage and Controlled Media Pipeline

Current Execution:
1. WU-01: Update `ContentDb.java` to Schema v4 (add `artifacts` table and query methods).
2. WU-02: Implement `MediaArtifact.java`, `MediaRetentionPolicy.java`, and `MediaStorageManager.java`.
3. WU-03: Create `res/xml/file_paths.xml` and register `FileProvider` in `AndroidManifest.xml`.
4. WU-04: Implement `MediaCleanupWorker.java` for lifecycle management, quota enforcement, and orphan sweeps.
5. WU-05: Integrate thumbnail caching into `ProcessingJobManager.java` and UI.
6. WU-06: Create `MediaStorageManagerTest.java` and `MediaCleanupWorkerTest.java`.
7. WU-07: Version bump to `0.7.0-phase7` (versionCode 7), push to GitHub, verify CI/CD, and package release.
