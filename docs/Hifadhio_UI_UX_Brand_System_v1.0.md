---
title: "Hifadhio UI/UX + Brand System"
version: "1.0"
date: "27 September 2026"
status: "Implementation Baseline"
---

# Hifadhio

**Tagline:** Save once. Find it when it matters.

**Brand hierarchy:** Hifadhio is the primary product brand. NAS Digital Solutions is the business/product brand. SeifTech is the technology/engineering brand.

**About wording:** `A NAS Digital Solutions product - engineered by SeifTech.`

> Working name only. A preliminary collision scan was performed, but formal trademark, app-store, domain and legal clearance is required before public launch.

# Implementation authority

This file is the machine-readable companion to `Hifadhio_UI_UX_Brand_System_v1.0.pdf`. Coding agents must use the PDF for visual reference and this file for explicit rules. Neither replaces the master product specification.

# Design principles

1. Capture first, process later.
2. Never lose a valid source URL because enrichment failed.
3. Truthful state labels: Saved, Processing, Ready, Needs attention.
4. Source text, AI-generated interpretation and user-authored notes are separate data/visual classes.
5. Home is not an infinite content feed.
6. Ask is not enabled until grounded retrieval exists.
7. Connections are derived from identifiable saved items.
8. User controls retention, export and deletion.
9. Accessibility and dark mode are baseline requirements.
10. Product brand first; parent brands appear subtly except About/legal.

# Navigation

- Home
- Inbox
- Library
- Ask
- Profile

Universal capture/Add is accessible from Home, Inbox and Library.

# Screen inventory

## Core
- Splash
- Onboarding 1-3
- Sign in
- Create account
- Home
- Home empty
- Manual URL paste
- Capture preview/details
- Android share quick save
- Android share detailed save
- Saved confirmation

## Inbox / organization
- Inbox
- Processing details
- Offline queued saves
- Extraction error / retry
- Library - all
- Library - collections
- Library - tags
- Collection detail
- Search results
- Search filters

## Content
- Content overview
- Source/provenance
- Transcript
- OCR/visual text
- User notes / why saved

## Intelligence
- Ask empty
- Ask grounded answer with source cards
- Connections/clusters
- Cluster detail

## Control
- Settings
- Privacy & security
- Storage & retention
- Connected platforms
- Export/backup
- About
- Delete account

# Brand tokens

```json
{
  "product": "Hifadhio",
  "version": "1.0",
  "tagline": "Save once. Find it when it matters.",
  "brand_architecture": {
    "product": "Hifadhio",
    "business_brand": "NAS Digital Solutions",
    "technology_brand": "SeifTech",
    "about_line": "A NAS Digital Solutions product - engineered by SeifTech."
  },
  "colors": {
    "primary_navy": "#0B1020",
    "surface_navy": "#121A2F",
    "mint": "#32D5A4",
    "mint_soft": "#DDF9F0",
    "purple": "#7257E8",
    "blue": "#3B82F6",
    "amber": "#F4B740",
    "danger": "#EF5B5B",
    "success": "#20B486",
    "background": "#F6F8FB",
    "surface": "#FFFFFF",
    "text": "#111827",
    "muted_text": "#667085",
    "divider": "#E4E7EC"
  },
  "radius": {
    "xs": 8,
    "sm": 12,
    "md": 16,
    "lg": 24,
    "xl": 32,
    "pill": 999
  },
  "spacing": [
    4,
    8,
    12,
    16,
    20,
    24,
    32,
    40,
    48
  ],
  "typography": {
    "ui_family": "Inter or system sans-serif fallback",
    "display": "700",
    "heading": "700",
    "body": "400/500",
    "caption": "500"
  },
  "navigation": [
    "Home",
    "Inbox",
    "Library",
    "Ask",
    "Profile"
  ]
}
```

# Brand usage

- App launcher and app icon: Hifadhio only.
- Main app header: Hifadhio/product UI only.
- Splash: Hifadhio primary; small `NAS Digital Solutions x SeifTech` endorsement permitted.
- About: `A NAS Digital Solutions product - engineered by SeifTech.`
- Do not create three equal mastheads/logos inside normal product screens.

# State requirements

Every major feature must implement empty, loading, partial, success and failure states. No coding agent may declare a screen complete when only the happy path exists.

# Visual anti-hallucination rules

- Do not show a transcript unless an actual transcript artifact exists.
- Do not label caption text as transcript.
- Do not show a platform as connected without real authorization state.
- Do not show AI summary/key points as source text.
- Do not display a processing percentage that is not backed by a real job/progress model.
- Do not fabricate thumbnails, creators or publish dates to fill empty cards.
- Do not enable Ask until answers are grounded in the user library and return source references.

# Phase mapping

- Phase 00: theme, logo/icon, shell tokens, component primitives.
- Phase 01: auth, base navigation, profile/About.
- Phase 02: capture/share flow, saved confirmation, basic Inbox.
- Phase 03: Library, collections, tags, favorites, notes.
- Phase 04: processing/retry/offline/error states.
- Phase 05-06: metadata previews and provenance.
- Phase 07-09: media/transcript/OCR + retention controls.
- Phase 10: AI summary/key points/entities with labels.
- Phase 11: full-text search.
- Phase 12: semantic search through same search surface.
- Phase 13: grounded Ask.
- Phase 14: connections/clusters.
- Phase 15: portability/export/import.
- Phase 16: accessibility, diagnostics, performance/hardening.

# Required agent behavior

At the end of each phase, compare the implemented APK with the relevant pages of the PDF. Any intentional divergence must be recorded in the implementation log/ADR and the visual source-of-truth must be updated if the change is approved.
