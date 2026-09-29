# Next Action

Phase: Phase 04 - Processing Job Framework

1. Implement `ProcessingJob.java` and `ProcessingEvent.java` domain models.
2. Upgrade `ContentDb.java` to schema version 3 with `processing_jobs` and `processing_events` tables and indexes.
3. Implement `ProcessingJobManager.java` with worker lease/claim, exponential backoff retry, stale recovery, and event logging.
4. Integrate processing status indicators in `ContentAdapter` and the event audit timeline + retry action in `ContentDetailBottomSheet`.
5. Write unit tests in `ProcessingJobTest.java`.
6. Bump version to `0.4.0-phase4`, push to GitHub, and build release APK.
