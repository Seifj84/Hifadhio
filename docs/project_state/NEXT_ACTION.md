# Next Action

Phase: Transitioning to Phase 05 - Adapter Framework + Generic Web Metadata

1. User testing and device verification of `Hifadhio-v0.4.0-phase4-debug.apk`:
   - Verify job creation on link capture.
   - Verify processing status indicator display on cards (`QUEUED`, `RUNNING`, `READY`, `FAILED`).
   - Verify audit event timeline and "Retry Processing" action inside Content Detail bottom sheet.
2. Prepare Phase 05 plan per Master Spec Section 1109-1124:
   - Design `ContentAdapterRegistry` and `ContentExtractorAdapter` interface.
   - Implement `GenericWebAdapter` with OpenGraph metadata extraction (`og:title`, `og:description`, `og:image`, canonical URL).
   - Hook adapter extraction into `ProcessingJobManager` worker loop.
   - Add unit tests with HTML/OpenGraph fixtures.
