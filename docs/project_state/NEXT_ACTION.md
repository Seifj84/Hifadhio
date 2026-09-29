# Next Action

Phase: Phase 05 - Adapter Framework + Generic Web Metadata

1. Implement `ContentExtractorAdapter.java` interface, `ExtractedMetadata.java`, and `AdapterHealth.java` in package `com.seiftech.hifadhio.adapter`.
2. Implement `ContentAdapterRegistry.java` with priority ordering, dynamic registration, and health monitoring.
3. Implement `GenericWebAdapter.java` with OpenGraph & HTML metadata extraction, entity decoding, and relative URL resolution.
4. Integrate `ContentAdapterRegistry` into `ProcessingJobManager.java` for real metadata extraction and database updates.
5. Enhance UI in `ContentAdapter`, `item_content_card.xml`, and `ContentDetailBottomSheet` to display extracted thumbnails, captions, and creator details.
6. Write unit tests in `AdapterRegistryTest.java` and `GenericWebAdapterTest.java`.
7. Bump version to `0.5.0-phase5` (versionCode 5), push to GitHub, and build release APK.
