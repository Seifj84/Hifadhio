# Current Phase

Phase: Phase 03
Name: Collections, Tags and Library
Status: IN_PROGRESS
Started: 2026-09-28T22:15:00+03:00
Target specification section: Sections 1075-1090 of Master Spec & UI/UX Brand System

## Objective
Implement comprehensive personal knowledge organization: full Collection CRUD with item counts, manual tag indexing and filtering, favorites filtering, a dedicated Content Detail screen, and title override retention (`original_title` vs user `title`). Also apply responsive top window insets so headers never overlap the system status bar, clock, or camera cutout.

## In scope
- [x] Responsive layout fix: Insets listener on AppBarLayout to prevent status bar/clock/notch overlap.
- [ ] Database upgrade (v2): Add `original_title` column and `collections` table to `ContentDb`.
- [ ] Collection CRUD: Create new collection, rename collection, and delete collection (with item reassignment to Inbox).
- [ ] Collection stats: Query collection item counts and display in Library.
- [ ] Favorites filter view: Instant filter for starred items.
- [ ] Tag indexing & filtering: Query all distinct tags, filter Library by tag, and add/edit tags.
- [ ] Content Detail Sheet: Clean, dedicated view of saved items with full notes, tags, metadata, and quick actions.
- [ ] Title override: Allow user title customization while preserving extracted `original_title`.
- [ ] Version bump to `0.3.0-phase3` in `build.gradle` and CI/CD workflow.

## Out of scope
- Background scraper workers, video downloading, Whisper transcription, Frame OCR (scheduled for Phase 04+).

## Preconditions
- Phase 02 completed, tested, and verified on physical device.
- Clean GitHub repository state at `Seifj84/Hifadhio` on `main`.

## Work units
- [x] WU-01 Fix header status bar insets & responsiveness in activity_main.xml, themes.xml, MainActivity.java
- [ ] WU-02 Upgrade ContentDb to schema v2 with original_title and collections CRUD
- [ ] WU-03 Implement ContentDetailBottomSheet for full item inspection & tag editing
- [ ] WU-04 Implement Collection management dialogs (Create, Rename, Delete) in LibraryFragment
- [ ] WU-05 Enhance LibraryFragment with Favorites filter, Collections chips with counts, and Tag filter
- [ ] WU-06 Connect ContentAdapter to open ContentDetailBottomSheet on card tap
- [ ] WU-07 Add unit tests for Collection & Tag operations
- [ ] WU-08 Bump version to 0.3.0-phase3 and update CI/CD workflow
- [ ] WU-09 Compile, test, and release v0.3.0-phase3 APK

## Required tests
- [x] Header does not overlap status bar clock/notch: VERIFIED
- [ ] Collection CRUD (create, rename, delete items move to Inbox): PENDING
- [ ] Tag filtering & favorites filtering: PENDING
- [ ] Content detail view displays full metadata and allows editing: PENDING
- [ ] Title override preserves original_title: PENDING
- [ ] CI/CD automated build passes: PENDING

## Phase acceptance criteria
1. App header has comfortable breathing room below the status bar clock on any screen.
2. Users can create, rename, and delete collections directly in the app.
3. Library provides responsive filtering by Collection (with counts), Favorites, and Tags.
4. Tapping a card opens a rich Content Detail view.
5. All tests pass, and an installable test APK v0.3.0-phase3 is built and published.

## Blockers
None.

## Completion evidence
Pending build execution.

## APK status
PENDING
