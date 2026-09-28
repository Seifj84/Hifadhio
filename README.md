# Hifadhio 📱💡

> **Save once. Find it when it matters.**

A personal AI content library that captures links shared from social media and the web, extracts and understands the available content, automatically organizes it, and makes the resulting knowledge searchable and conversational later.

A **NAS Digital Solutions** product — engineered by **SeifTech**.

---

## 🌟 Overview

Useful information is increasingly discovered inside Instagram Reels, TikTok videos, YouTube Shorts, Facebook posts/reels, X posts, Reddit threads, articles, and other web content. Native "Save" or bookmark features preserve items but fail at retrieval, cross-platform organization, transcript search, synthesis, personal notes, and long-term knowledge reuse.

**Hifadhio** solves this by turning a shared link into a durable **Content Item** in a personal knowledge library.

---

## 🚀 Phase 1: Capture MVP & Local Library

Phase 1 establishes the rock-solid, local-first capture and organization baseline:

- 📥 **Android Native Share Target**: Direct capture via Android's `ACTION_SEND` intent from Instagram, TikTok, YouTube, Facebook, X, browser, etc.
- ⚡ **Instant URL Normalization**: Automatically extracts URLs from noisy share text and strips tracking parameters (`igshid`, `fbclid`, `si`, `utm_*`).
- 🏷️ **Smart Platform Recognition**: Detects source platforms (Instagram, TikTok, YouTube, Facebook, X, Reddit, LinkedIn, Web) with custom brand badges and colors.
- 💾 **Local-First SQLite Storage**: Fast, offline-first persistence storing URL, title, snippet, user notes ("Why I saved this"), collections, tags, and favorite status.
- 🔍 **Live Search & Filter**: Real-time filtering across titles, URLs, platforms, notes, collections, and tags with platform chip selectors.
- 🗂️ **Collections & Triage**: Organized views across Home, Inbox, and Library.
- 🎨 **Material 3 & Brand Design System**: Strictly adheres to the Hifadhio UI/UX Design System with Navy, Mint, and Cream surface palettes.

---

## 📐 Brand & UI System

| Token | Hex Value | Role |
|-------|-----------|------|
| `primary_navy` | `#0B1020` | Main brand and high-contrast surfaces |
| `surface_navy` | `#121A2F` | Top app bars, dark cards |
| `mint` | `#32D5A4` | Primary brand accent and interactive elements |
| `mint_soft` | `#DDF9F0` | Selected states, tag badges |
| `background` | `#F6F8FB` | Main application background |
| `surface` | `#FFFFFF` | Cards, sheets, dialogs |
| `text` | `#111827` | Primary typography |
| `muted_text` | `#667085` | Secondary captions, timestamps |

---

## 📱 Navigation Structure

- **Home**: Search bar, library stats, quick platform filters, recent saves.
- **Inbox**: Capture triage, status indicators (Saved / Ready), unsorted items.
- **Library**: Collections overview, item counters, tag clouds.
- **Ask**: Grounded retrieval preview (unlocks in Phase 13).
- **Profile**: Storage metrics, export options, and official About attributions.

---

## 🛠️ Build & Installation

### Android Debug APK
Requirements: Android SDK 35, Java 17+, Gradle 8.9+.

```bash
cd android
./gradlew assembleDebug
```

Output APK will be generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License & Attribution

Copyright © 2026 **NAS Digital Solutions** & **SeifTech**. All rights reserved.
