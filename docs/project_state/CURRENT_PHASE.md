# Current Phase

Phase: Phase 02
Name: Capture MVP & Inbox Triage
Status: IN_PROGRESS
Started: 2026-09-28T21:20:00+03:00
Target specification section: Sections 1052-1074 of Master Spec & UI/UX Brand System

## Objective
Elevate the capture experience to full production quality by implementing intelligent duplicate detection (canonical URL matching and pre-fill updates), instant saved confirmation feedback, bottom navigation inbox badge counts, and an intuitive inbox triage workflow to organize recently saved links into collections.

## In scope
- [x] Duplicate detection by canonical URL in `ContentDb` (`findByCanonicalUrl`).
- [x] Non-intrusive duplicate warning banner in `SaveLinkBottomSheet` with pre-filled existing data and "Update Existing" action.
- [x] Saved confirmation feedback (Material 3 Snackbar with "View in Inbox" direct action).
- [x] Bottom navigation Inbox badge counter showing unorganized items count in real-time.
- [x] Fast "Move to Collection" triage dialog from content cards and popup menus.
- [x] Inbox triage counter header and enhanced empty state ("Inbox Zero").
- [x] Unit tests for duplicate detection and canonical URL resolution.
- [x] Version bump to `0.2.0-phase2` in `build.gradle` and CI/CD workflow.

## Out of scope
- Server-side background scrapers, video downloading, Whisper transcription, Frame OCR, RAG chat (scheduled for subsequent phases).

## Preconditions
- Phase 01 completed, tested on device, and accepted by user.
- Clean GitHub repository state at `Seifj84/Hifadhio` on `main`.

## Work units
- [x] WU-01 Update project state documents for Phase 02
- [x] WU-02 Implement canonical duplicate query in ContentDb
- [x] WU-03 Implement duplicate detection banner & prefill in SaveLinkBottomSheet
- [x] WU-04 Implement Move-to-Collection quick triage in ContentAdapter
- [x] WU-05 Implement Inbox badge counter & confirmation snackbar in MainActivity
- [x] WU-06 Enhance InboxFragment with triage header & empty state
- [x] WU-07 Add unit tests for duplicate detection & canonical consistency
- [x] WU-08 Bump version to 0.2.0-phase2 and update CI/CD workflow
- [ ] WU-09 Build and verify APK via GitHub Actions, produce release record v0.2.0-phase2

## Required tests
- [x] Canonical URL duplicate matching: PASS
- [x] Duplicate pre-fill and update flow: PASS
- [x] Quick move to collection updates database and removes from Inbox: PASS
- [x] Inbox badge reflects real unorganized item count: PASS
- [ ] CI/CD automated build and test passes: PENDING

## Phase acceptance criteria
1. Sharing an already-saved link detects the duplicate and offers to update notes/collection.
2. Saving a new link shows instant confirmation with a direct shortcut to view it.
3. Bottom navigation Inbox tab displays a badge counter of unorganized items.
4. Moving an item out of Inbox decrements the badge and updates the collection.
5. All unit tests pass, and an installable test APK v0.2.0-phase2 is compiled and published.

## Blockers
None.

## Completion evidence
Pending build execution.

## APK status
PENDING
