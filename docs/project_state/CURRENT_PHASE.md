# Current Phase

Phase: Phase 01
Name: Capture MVP & Local Library
Status: IN_PROGRESS
Started: 2026-09-28T16:40:00+03:00
Target specification section: Sections 6, 7, 8, 9 of Master Spec & UI/UX Brand System

## Objective
Deliver a rock-solid, production-grade Android capture MVP that enables seamless URL capture from native share intents or manual paste, normalizes URLs, identifies platforms with custom branding, stores records in local SQLite, provides live search, collection filtering, and adheres faithfully to the Hifadhio UI/UX brand tokens.

## In scope
- Android ACTION_SEND intent receiver for text/plain links.
- Manual link capture with BottomSheet dialog.
- URL cleaning & tracking parameter removal (igshid, fbclid, si, utm_*).
- Automatic platform detection (Instagram, TikTok, YouTube, Facebook, X, Reddit, Web).
- Local SQLite database `ContentDb` with complete CRUD and search.
- Material Design 3 UI with 5-tab BottomNavigationView: Home, Inbox, Library, Ask, Profile.
- Quick Stats, Platform Chips, Recent Cards, Collection Badges.
- Item detail bottom sheet with Open in Browser, Share, Edit, and Delete actions.
- Official brand assets & launcher icons integrated from design tokens.
- Automated CI/CD pipeline for APK compilation and test artifact verification.

## Out of scope
- Server-side background scrapers, video downloading, Whisper transcription, Frame OCR, RAG chat (scheduled for subsequent phases).

## Preconditions
- Brand tokens and documentation in `docs/`.
- GitHub repository established at `Seifj84/Hifadhio`.

## Work units
- [x] WU-01 Initialize docs and durable project state
- [x] WU-02 Create GitHub repository Seifj84/Hifadhio and configure remote
- [ ] WU-03 Implement URL normalization & platform detection domain logic
- [ ] WU-04 Implement local SQLite storage & ContentItem entity
- [ ] WU-05 Design & implement Material 3 layout, Navigation, and Fragments
- [ ] WU-06 Integrate official brand assets and generate launcher mipmaps
- [ ] WU-07 Configure Gradle build scripts & CI/CD workflow
- [ ] WU-08 Execute unit tests and compile Android APK
- [ ] WU-09 Archive APK, produce release record, and push to GitHub

## Required tests
- URL extraction from messy social share text
- Query param stripping (igshid, fbclid, utm_*)
- Platform detection accuracy
- SQLite CRUD & search query verification

## Phase acceptance criteria
1. Sharing from any app sends text to Hifadhio without crashing.
2. Manual paste extracts clean URL and identifies platform.
3. Items persist across app restarts.
4. Live search filters items instantly.
5. All 5 navigation tabs render appropriate screens without error.
6. Installable test APK compiled and verified.

## Blockers
None.

## Completion evidence
Pending build artifact and phase report.

## APK status
NOT_BUILT
