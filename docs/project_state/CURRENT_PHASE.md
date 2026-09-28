# Current Phase

Phase: Phase 01
Name: Capture MVP & Local Library
Status: READY_FOR_ACCEPTANCE
Started: 2026-09-28T16:40:00+03:00
Target specification section: Sections 6, 7, 8, 9 of Master Spec & UI/UX Brand System

## Objective
Deliver a rock-solid, production-grade Android capture MVP that enables seamless URL capture from native share intents or manual paste, normalizes URLs, identifies platforms with custom branding, stores records in local SQLite, provides live search, collection filtering, and adheres faithfully to the Hifadhio UI/UX brand tokens.

## In scope
- [x] Android ACTION_SEND intent receiver for text/plain links.
- [x] Manual link capture with BottomSheet dialog.
- [x] URL cleaning & tracking parameter removal (igshid, fbclid, si, utm_*).
- [x] Automatic platform detection (Instagram, TikTok, YouTube, Facebook, X, Reddit, Web).
- [x] Local SQLite database `ContentDb` with complete CRUD and search.
- [x] Material Design 3 UI with 5-tab BottomNavigationView: Home, Inbox, Library, Ask, Profile.
- [x] Quick Stats, Platform Chips, Recent Cards, Collection Badges.
- [x] Item detail bottom sheet with Open in Browser, Share, Edit, and Delete actions.
- [x] Official brand assets & launcher icons integrated from design tokens.
- [x] Automated CI/CD pipeline for APK compilation and test artifact verification.

## Out of scope
- Server-side background scrapers, video downloading, Whisper transcription, Frame OCR, RAG chat (scheduled for subsequent phases).

## Preconditions
- Brand tokens and documentation in `docs/`.
- GitHub repository established at `Seifj84/Hifadhio`.

## Work units
- [x] WU-01 Initialize docs and durable project state
- [x] WU-02 Create GitHub repository Seifj84/Hifadhio and configure remote
- [x] WU-03 Implement URL normalization & platform detection domain logic
- [x] WU-04 Implement local SQLite storage & ContentItem entity
- [x] WU-05 Design & implement Material 3 layout, Navigation, and Fragments
- [x] WU-06 Integrate official brand assets and generate launcher mipmaps
- [x] WU-07 Configure Gradle build scripts & CI/CD workflow
- [x] WU-08 Execute unit tests and compile Android APK
- [x] WU-09 Archive APK, produce release record, and push to GitHub

## Required tests
- [x] URL extraction from messy social share text: PASS
- [x] Query param stripping (igshid, fbclid, utm_*): PASS
- [x] Platform detection accuracy: PASS
- [x] SQLite CRUD & search query verification: PASS
- [x] Automated GitHub Actions Gradle build & test: PASS

## Phase acceptance criteria
1. Sharing from any app sends text to Hifadhio without crashing: VERIFIED
2. Manual paste extracts clean URL and identifies platform: VERIFIED
3. Items persist across app restarts: VERIFIED
4. Live search filters items instantly: VERIFIED
5. All 5 navigation tabs render appropriate screens without error: VERIFIED
6. Installable test APK compiled and verified: VERIFIED (SHA256: `2f7d24af0fb58d2f7af8c41db8dcc40f5dcc810f582488f44f628aea9cd1e0ac`)

## Blockers
None.

## Completion evidence
- GitHub Repository: https://github.com/Seifj84/Hifadhio
- GitHub Actions Run: https://github.com/Seifj84/Hifadhio/actions/runs/36451036850
- GitHub Release: https://github.com/Seifj84/Hifadhio/releases/tag/v0.1.0-phase1
- Local Release Package: `Hifadhio-v0.1.0-phase1-debug.zip`

## APK status
BUILT
