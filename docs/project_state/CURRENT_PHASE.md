# Current Phase

Phase: Phase 03
Name: Collections, Tags and Library
Status: COMPLETED
Started: 2026-09-28T22:15:00+03:00
Completed: 2026-09-29T04:18:00+03:00
Target specification section: Sections 1075-1090 of Master Spec & UI/UX Brand System

## Objective
Implement comprehensive personal knowledge organization: full Collection CRUD with item counts, manual tag indexing and filtering, favorites filtering, a dedicated Content Detail screen, and title override retention (`original_title` vs user `title`). Also apply responsive top window insets so headers never overlap the system status bar, clock, or camera cutout.

## In scope
- [x] Responsive layout fix: Insets listener on AppBarLayout to prevent status bar/clock/notch overlap.
- [x] Database upgrade (v2): Add `original_title` column and `collections` table to `ContentDb`.
- [x] Collection CRUD: Create new collection, rename collection, and delete collection (with item reassignment to Inbox).
- [x] Collection stats: Query collection item counts and display in Library.
- [x] Favorites filter view: Instant filter for starred items.
- [x] Tag indexing & filtering: Query all distinct tags, filter Library by tag, and add/edit tags.
- [x] Content Detail Sheet: Clean, dedicated view of saved items with full notes, tags, metadata, and quick actions.
- [x] Title override: Allow user title customization while preserving extracted `original_title`.
- [x] Version bump to `0.3.0-phase3` in `build.gradle` and CI/CD workflow.

## Out of scope
- Background scraper workers, video downloading, Whisper transcription, Frame OCR (scheduled for Phase 04+).

## Preconditions
- Phase 02 completed, tested, and verified on physical device.
- Clean GitHub repository state at `Seifj84/Hifadhio` on `main`.

## Work units
- [x] WU-01 Fix header status bar insets & responsiveness in activity_main.xml, themes.xml, MainActivity.java
- [x] WU-02 Upgrade ContentDb to schema v2 with original_title and collections CRUD
- [x] WU-03 Implement ContentDetailBottomSheet for full item inspection & tag editing
- [x] WU-04 Implement Collection management dialogs (Create, Rename, Delete) in LibraryFragment
- [x] WU-05 Enhance LibraryFragment with Favorites filter, Collections chips with counts, and Tag filter
- [x] WU-06 Connect ContentAdapter to open ContentDetailBottomSheet on card tap
- [x] WU-07 Add unit tests for Collection & Tag operations
- [x] WU-08 Bump version to 0.3.0-phase3 and update CI/CD workflow
- [x] WU-09 Compile, test, and release v0.3.0-phase3 APK

## Required tests
- [x] Header does not overlap status bar clock/notch: VERIFIED
- [x] Collection CRUD (create, rename, delete items move to Inbox): PASS
- [x] Tag filtering & favorites filtering: PASS
- [x] Content detail view displays full metadata and allows editing: PASS
- [x] Title override preserves original_title: PASS
- [x] CI/CD automated build passes: PASS

## Phase acceptance criteria
1. App header has comfortable breathing room below the status bar clock on any screen.
2. Users can create, rename, and delete collections directly in the app.
3. Library provides responsive filtering by Collection (with counts), Favorites, and Tags.
4. Tapping a card opens a rich Content Detail view.
5. All tests pass, and an installable test APK v0.3.0-phase3 is built and published.

## Blockers
None.

## Completion evidence
- GitHub Actions CI/CD Build Run #36506993583 succeeded.
- GitHub Release `v0.3.0-phase3` published.
- Unit test suite `CollectionsAndTagsTest` and `UrlNormalizerTest` executed and passed in CI.

## APK status
AVAILABLE
- Direct Download: https://github.com/Seifj84/Hifadhio/releases/download/v0.3.0-phase3/Hifadhio-v0.3.0-phase3-debug.apk
- Release Page: https://github.com/Seifj84/Hifadhio/releases/tag/v0.3.0-phase3
