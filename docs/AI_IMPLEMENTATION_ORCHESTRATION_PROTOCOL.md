---
title: "Reels Links to Content — AI Implementation & Orchestration Protocol"
subtitle: "Master execution instructions for AGY, Codex, and future coding agents"
version: "1.0"
date: "27 September 2026"
status: "MANDATORY IMPLEMENTATION PROTOCOL"
source_of_truth: "docs/Reels_Links_to_Content_Master_Spec_v1.0.md"
---

# 0. Purpose

This document tells an AI coding agent exactly **how to implement the Reels Links to Content application**, how to continue safely across context-window limits or quota exhaustion, how to report work, how to preserve state for another agent, and how to produce a testable Android APK at the end of every completed phase.

This is an **execution protocol**, not a replacement for the product specification.

The product requirements, architecture, UX, data model, security rules, and phase definitions remain governed by:

`docs/Reels_Links_to_Content_Master_Spec_v1.0.md`

Every AI coding agent — including **AGY, Codex, or any replacement agent** — MUST read and obey both documents before changing the project.

---

# 1. Hierarchy of Authority

When instructions conflict, use this priority order:

1. Explicit current instruction from the project owner.
2. `docs/Reels_Links_to_Content_Master_Spec_v1.0.md`
3. This orchestration protocol.
4. Accepted ADRs in `docs/adr/`
5. Existing implementation behavior and tests.
6. Agent assumptions.

Existing code is **not automatically correct** merely because it already exists.

If implementation and the master specification disagree, do not silently alter either side. Record the discrepancy and resolve it explicitly.

---

# 2. Core Operating Rule

The agent MUST NOT attempt to build the entire application in one uncontrolled session.

Implementation MUST proceed as:

```text
MASTER SPEC
    ↓
PHASE
    ↓
WORK UNIT
    ↓
IMPLEMENT
    ↓
TEST
    ↓
CHECKPOINT
    ↓
REPORT
    ↓
NEXT WORK UNIT
    ↓
PHASE ACCEPTANCE TESTS
    ↓
APK BUILD
    ↓
RELEASE RECORD
    ↓
GIT COMMIT/TAG
    ↓
GOOGLE DRIVE ARTIFACT SYNC
    ↓
NEXT PHASE
```

A phase is never marked complete simply because code was written.

---

# 3. Mandatory Agent Startup Procedure

At the beginning of **every new AGY/Codex session**, the agent MUST perform the following steps in order.

## 3.1 Read project authority files

Read:

1. `docs/Reels_Links_to_Content_Master_Spec_v1.0.md`
2. `docs/AI_IMPLEMENTATION_ORCHESTRATION_PROTOCOL.md`  
   or the actual filename containing this protocol.
3. `docs/project_state/PROJECT_STATE.md`
4. `docs/project_state/CURRENT_PHASE.md`
5. `docs/project_state/NEXT_ACTION.md`
6. `docs/project_state/SESSION_HANDOFF.md`
7. `docs/project_state/KNOWN_ISSUES.md`
8. all ADRs relevant to the current phase.

Do not rely on conversation memory.

## 3.2 Inspect repository state

Run or equivalent:

```bash
git status
git branch --show-current
git log -5 --oneline
```

Also inspect:

- latest database migration;
- current app version;
- current backend version;
- uncommitted files;
- latest release record;
- latest phase report.

## 3.3 Verify environment before editing

Check the tools required by the current phase.

Typical checks:

```bash
flutter --version
dart --version
git --version
python --version
```

If the backend uses Node, Docker, Supabase CLI, or another required tool, verify those too.

Do not install or upgrade major dependencies merely because a newer version exists.

## 3.4 Establish baseline

Before modifying code:

- run the fastest relevant lint/type/build check;
- run existing tests relevant to the current phase;
- record failures that already existed.

The agent must distinguish:

```text
PRE-EXISTING FAILURE
```

from:

```text
FAILURE INTRODUCED BY CURRENT WORK
```

---

# 4. Durable Project State — Required Files

The project MUST contain this directory:

```text
docs/project_state/
├── PROJECT_STATE.md
├── CURRENT_PHASE.md
├── NEXT_ACTION.md
├── IMPLEMENTATION_LOG.md
├── SESSION_HANDOFF.md
├── KNOWN_ISSUES.md
├── DECISIONS_PENDING.md
└── BUILD_HISTORY.md
```

These files are the recovery system when:

- the AI context window fills;
- AGY quota expires;
- Codex quota expires;
- the browser/session closes;
- a different coding agent takes over;
- the computer restarts;
- implementation pauses for days or weeks.

Chat history must never be the only source of implementation state.

---

# 5. PROJECT_STATE.md Contract

`PROJECT_STATE.md` MUST summarize the whole project in a compact form.

Required structure:

```markdown
# Project State

Last updated:
Updated by:
Repository:
Branch:
Latest commit:
App version:
Backend version:
Latest migration:

## Completed phases
- Phase 00 — ...
- Phase 01 — ...

## Current phase
Phase XX — Name

## Current status
IN_PROGRESS | BLOCKED | READY_FOR_ACCEPTANCE | COMPLETE

## Current implemented capabilities
- ...

## Current architecture
- Mobile:
- Backend:
- Database:
- Auth:
- Queue:
- Storage:
- AI:
- Search:

## Last verified commands
- command — PASS/FAIL
- command — PASS/FAIL

## Current blockers
- ...

## Exact next action
...

## Latest testable APK
Path:
Build type:
Version:
SHA256:
Phase:
```

Update this file at least:

- after each completed work unit;
- before ending a session;
- when architecture changes;
- when a phase completes.

---

# 6. CURRENT_PHASE.md Contract

This file contains only the phase currently being implemented.

Required structure:

```markdown
# Current Phase

Phase:
Name:
Status:
Started:
Target specification section:

## Objective

## In scope

## Out of scope

## Preconditions

## Work units

- [x] WU-01 ...
- [ ] WU-02 ...
- [ ] WU-03 ...

## Required tests

## Phase acceptance criteria

## Blockers

## Completion evidence

## APK status
NOT_BUILT | BUILD_FAILED | BUILT
```

A future agent should be able to read this file and know exactly where to continue.

---

# 7. NEXT_ACTION.md Contract

This is intentionally small.

It must answer:

> If another agent starts with zero chat history, what exactly should it do next?

Example:

```markdown
# Next Action

1. Open `apps/mobile_flutter/lib/features/capture/...`.
2. Complete WU-02: persist shared URL to ContentItem repository.
3. Do not work on metadata extraction yet.
4. Run:
   - flutter analyze
   - flutter test
5. If both pass, update CURRENT_PHASE.md and proceed to WU-03.
```

Never leave vague text such as:

> Continue working on the app.

---

# 8. IMPLEMENTATION_LOG.md Contract

The agent MUST append one durable entry after every meaningful implementation unit.

Use:

```markdown
## 2026-09-27T16:00+03:00 — AGY/Codex — Phase XX / WU-XX

### Goal
...

### Changes made
- file/path
- file/path

### Database changes
None / migration ...

### Commands run
- `...` → PASS
- `...` → FAIL

### Result
...

### Problems found
...

### Decisions made
...

### Next action
...
```

Do not rewrite history. Append new entries.

---

# 9. SESSION_HANDOFF.md — Context/Quota Survival

This file MUST be refreshed frequently.

A coding agent must update it:

- after every major work unit;
- immediately before a known quota boundary;
- before a long build/migration operation if substantial work has already occurred;
- before ending a session;
- whenever the user says they are switching AGY ↔ Codex.

Use:

```markdown
# Session Handoff

Updated:
Agent:
Phase:
Work unit:

## What I was asked to do

## What is already complete

## What I changed in this session

## Files most relevant now

## Tests already run

## Current errors/failures

## Uncommitted work

## Important constraints

## Exact next command/action

## Do not redo
- ...

## Do not assume
- ...
```

This file exists specifically to prevent context-window hallucination.

---

# 10. Quota / Context Window Protocol

The agent must assume quota or context can disappear at any time.

Therefore:

## 10.1 Never accumulate large invisible state

Do not perform ten major changes and only document them at the end.

After each meaningful unit:

1. save files;
2. run focused validation;
3. update implementation log;
4. update state;
5. make a Git checkpoint when appropriate.

## 10.2 Prefer small work units

A work unit should normally be small enough that another competent agent could understand it from:

- Git diff;
- the implementation log;
- the handoff file.

Good work units:

- add ContentItem model;
- add URL normalization;
- add share intent receiver;
- create one migration;
- add one API endpoint with tests.

Bad work unit:

- build capture, extraction, AI, search, auth, and UI.

## 10.3 When context is becoming large

The agent MUST stop expanding scope and write a checkpoint containing:

```text
COMPLETED
IN PROGRESS
NOT STARTED
FILES CHANGED
TEST STATUS
KNOWN ERROR
NEXT ACTION
```

Then continue only if enough working context remains.

## 10.4 If quota ends unexpectedly

The next agent must:

1. read the durable state files;
2. inspect `git status` and `git diff`;
3. never assume incomplete code works;
4. run targeted checks;
5. resume from `NEXT_ACTION.md`.

---

# 11. AGY ↔ Codex Switching Protocol

AGY and Codex are peers. Neither should trust undocumented work from the other.

When switching agents:

## Outgoing agent

Must update:

- PROJECT_STATE.md
- CURRENT_PHASE.md
- NEXT_ACTION.md
- IMPLEMENTATION_LOG.md
- SESSION_HANDOFF.md
- KNOWN_ISSUES.md if applicable

It should commit a checkpoint if the repository is in a coherent state.

## Incoming agent

Must:

1. read all state files;
2. run `git status`;
3. inspect latest diff/commit;
4. verify relevant tests;
5. continue rather than restart.

## Agent identity in reports

Each implementation log entry must identify the agent:

```text
Agent: AGY
```

or:

```text
Agent: Codex
```

This is for traceability, not competition.

---

# 12. Planning Before Coding

At the beginning of each phase, the agent MUST transform the master phase requirements into explicit work units.

Example:

```text
Phase 02 — Capture MVP

WU-01 URL normalization utility
WU-02 ContentItem persistence
WU-03 manual paste capture
WU-04 Android ACTION_SEND integration
WU-05 duplicate behavior
WU-06 capture status UI
WU-07 capture integration tests
WU-08 phase regression tests
WU-09 APK build and archival
```

Record those units in `CURRENT_PHASE.md`.

Do not start unrelated future-phase features early.

---

# 13. Mandatory Step-by-Step Reporting

The agent must report progress in **implementation steps**, not generic summaries.

For every completed work unit, report:

```text
STEP <number>: <name>

Status: PASS | PARTIAL | BLOCKED | FAIL

Implemented:
- ...

Files changed:
- ...

Validation:
- command → result

Issues:
- ...

Next:
- ...
```

At phase completion, generate a detailed phase report.

---

# 14. Required Phase Report

Create:

```text
release_records/phase_XX_<slug>/<version>/PHASE_REPORT.md
```

Required contents:

```markdown
# Phase XX Completion Report

Phase:
Version:
Date:
Agent(s):
Git commit:
Git tag:

## Objective

## Specification references

## Work units
- WU-01 — PASS
- WU-02 — PASS

## Features implemented

## Files/components changed

## Database migrations

## API changes

## UI/UX changes

## Security considerations

## Tests run

| Command | Result | Notes |
|---|---|---|

## Build results

## APK artifacts

## Known issues

## Deferred items

## Regression status

## Acceptance criteria

| Criterion | PASS/FAIL | Evidence |
|---|---|---|

## Final phase status

COMPLETE / INCOMPLETE

## Next phase readiness

## Exact next action
```

Never mark `COMPLETE` if required acceptance criteria fail.

---

# 15. Source Control Strategy

## 15.1 Git is the source of truth for code

Commit:

- source code;
- migrations;
- tests;
- documentation;
- ADRs;
- project-state files;
- release manifests;
- checksums;
- phase reports.

## 15.2 Do not normally commit APK binaries to normal Git history

APK files are binary artifacts and can make repository history very large.

Preferred design:

```text
Git repository
    └── release records, manifests, reports, checksums

Google Drive synchronized artifact folder
    └── APK binaries
```

If APKs must live in Git, configure **Git LFS** first.

Do not repeatedly commit normal APK binaries without LFS.

---

# 16. Release Record Folder

Create this folder inside the project:

```text
release_records/
```

Recommended structure:

```text
release_records/
├── README.md
├── phase_00_baseline/
│   └── v0.0.1/
│       ├── PHASE_REPORT.md
│       ├── BUILD_MANIFEST.json
│       ├── SHA256SUMS.txt
│       └── android/
│           └── app-debug-v0.0.1-phase00.apk
├── phase_01_auth_shell/
│   └── v0.1.0/
│       ├── PHASE_REPORT.md
│       ├── BUILD_MANIFEST.json
│       ├── SHA256SUMS.txt
│       └── android/
│           └── app-debug-v0.1.0-phase01.apk
└── ...
```

If `release_records/` is directly synchronized by Google Drive, all APK builds become available on the user's Drive automatically.

---

# 17. Recommended Git Ignore Strategy

If Google Drive is preserving APKs, keep the APK binaries out of normal Git:

```gitignore
release_records/**/android/*.apk
release_records/**/android/*.aab
```

Do **not** ignore:

```text
PHASE_REPORT.md
BUILD_MANIFEST.json
SHA256SUMS.txt
```

Those small text records should remain version-controlled.

Alternative:

- use Git LFS for `*.apk`;
- only if explicitly configured and understood.

---

# 18. Google Drive Sync Model

The automation must not pretend Google Drive sync exists merely because a folder is named "Google Drive".

Use one of these real mechanisms:

## Option A — Google Drive for desktop

Configure a local folder that is actually mirrored/synchronized by Google Drive for desktop.

Possible design:

```text
D:\Google Drive\ReelsLinksToContent\release_records\
```

The phase-completion script copies each release artifact there.

## Option B — Repository inside a Drive-synced workspace

This can work, but syncing an active Git repository through Drive may create file-lock/conflict issues.

Not preferred.

## Option C — rclone / Drive API automation

A script uploads completed release folders to a configured Drive destination.

This is more automatable but requires credentials/configuration.

### Rule

The implementation agent MUST make the sync target configurable:

```text
RELEASE_ARCHIVE_PATH=<path>
```

Never hard-code the owner's Windows username or Drive path into source code.

---

# 19. Local Release Archive Configuration

Create a developer-only config such as:

```text
.env.local
```

or:

```text
scripts/local.release.config.ps1
```

with:

```powershell
$env:RELEASE_ARCHIVE_PATH = "D:\Google Drive\ReelsLinksToContent\release_records"
```

This file must not contain cloud secrets and should normally be excluded from Git if machine-specific.

Provide an `.example` file in Git.

---

# 20. APK Policy — Every Completed Phase

At the end of **every phase**, the agent MUST produce an installable Android APK.

During development, use a debug APK unless release signing is fully configured.

Mandatory minimum:

```bash
flutter build apk --debug
```

Expected artifact:

```text
build/app/outputs/flutter-apk/app-debug.apk
```

When production signing exists, also run:

```bash
flutter build apk --release
```

Do not fabricate a release build if signing is not configured.

Record the exact build type.

---

# 21. APK Naming Convention

Copy the generated artifact to:

```text
app-<buildtype>-v<version>-phase<XX>-<YYYYMMDD>.apk
```

Example:

```text
app-debug-v0.2.0-phase02-20260927.apk
```

Later signed production example:

```text
app-release-v1.0.0-phase16-20261201.apk
```

---

# 22. Versioning Policy

Recommended development version mapping:

```text
Phase 00 → 0.0.x
Phase 01 → 0.1.x
Phase 02 → 0.2.x
Phase 03 → 0.3.x
...
Phase 09 → 0.9.x
Phase 10+ → 0.10.x etc.
Production release → 1.0.0
```

Patch increments are used for fixes inside the same phase.

Example:

```text
Phase 02 first completed build → 0.2.0
Phase 02 fix build → 0.2.1
Phase 02 second fix → 0.2.2
```

Do not alter historical release folders.

---

# 23. BUILD_MANIFEST.json

Every archived build must include a machine-readable manifest.

Example:

```json
{
  "project": "Reels Links to Content",
  "phase": "02",
  "phase_name": "Capture MVP",
  "version": "0.2.0",
  "build_type": "debug",
  "platform": "android",
  "artifact": "app-debug-v0.2.0-phase02-20260927.apk",
  "built_at": "2026-09-27T18:30:00+03:00",
  "git_commit": "<commit>",
  "git_tag": "phase-02-v0.2.0",
  "flutter_version": "<version>",
  "dart_version": "<version>",
  "tests": {
    "flutter_analyze": "PASS",
    "flutter_test": "PASS"
  },
  "known_issues": [],
  "sha256": "<hash>"
}
```

Values must come from real commands. Never invent them.

---

# 24. SHA-256 Integrity Record

Generate a SHA-256 checksum for every APK.

Windows PowerShell:

```powershell
Get-FileHash .\app-debug-v0.2.0-phase02-20260927.apk -Algorithm SHA256
```

Linux/macOS:

```bash
sha256sum app-debug-v0.2.0-phase02-20260927.apk
```

Store it in:

```text
SHA256SUMS.txt
```

and in `BUILD_MANIFEST.json`.

---

# 25. Automated Phase-Completion Script

During Phase 00, create:

```text
scripts/phase_complete.ps1
```

and optionally:

```text
scripts/phase_complete.sh
```

The script should automate the mechanical release process.

## Required responsibilities

The script should:

1. verify Git working state;
2. run required lint/analyze checks;
3. run tests;
4. build Android APK;
5. determine version;
6. create release-record directory;
7. copy/rename APK;
8. generate SHA-256;
9. collect Git commit;
10. collect tool versions;
11. generate/update `BUILD_MANIFEST.json`;
12. verify the artifact exists and is non-zero size;
13. copy artifact + release metadata to configured `RELEASE_ARCHIVE_PATH`;
14. report the final local and Drive-sync locations.

The script MUST stop on a failed required test or failed APK build unless explicitly run in a diagnostic mode.

---

# 26. Phase Completion Gate

A phase may be declared complete only if:

```text
[ ] All phase work units complete
[ ] Required migrations applied/tested
[ ] Relevant unit tests pass
[ ] Relevant integration tests pass
[ ] Flutter analyze passes
[ ] Flutter tests pass
[ ] Backend checks pass
[ ] No known critical security regression
[ ] Required documentation updated
[ ] Project state updated
[ ] Phase report generated
[ ] Installable APK built successfully
[ ] APK checksum generated
[ ] APK copied to release_records
[ ] Artifact archive sync attempted
[ ] Git commit recorded
[ ] Git tag created
[ ] NEXT_ACTION updated
```

If any mandatory item fails, phase status is:

```text
INCOMPLETE
```

not `COMPLETE WITH ASSUMPTIONS`.

---

# 27. Suggested Phase Git Tags

Use:

```text
phase-00-v0.0.1
phase-01-v0.1.0
phase-02-v0.2.0
...
```

A tag is created only after the phase completion gate passes.

Example:

```bash
git tag -a phase-02-v0.2.0 -m "Phase 02 Capture MVP complete"
```

Push according to repository policy:

```bash
git push
git push --tags
```

Never force-push shared project history unless explicitly instructed.

---

# 28. Test-on-Phone Feedback Loop

Each completed phase should produce a phone-test cycle.

Workflow:

```text
Agent completes phase
        ↓
APK generated
        ↓
APK archived/synced to Drive
        ↓
User downloads APK
        ↓
User installs on Android phone
        ↓
User performs phase-specific test checklist
        ↓
User reports defects
        ↓
Agent creates patch version
        ↓
APK rebuilt
        ↓
New artifact archived
```

Do not overwrite the previous APK.

---

# 29. PHONE_TEST_CHECKLIST.md

Each release folder should optionally include:

```text
PHONE_TEST_CHECKLIST.md
```

The checklist must only test functionality that exists in that phase.

Example for Capture MVP:

```markdown
# Phone Test — Phase 02

APK:
Version:

- [ ] App installs.
- [ ] App opens.
- [ ] Share a browser URL to the app.
- [ ] Shared URL appears in capture screen.
- [ ] Tap Save.
- [ ] Saved item appears in Inbox.
- [ ] Close/reopen app; item still exists.
- [ ] Share same URL again; duplicate behavior matches spec.
- [ ] Try malformed URL.
- [ ] Test with poor/no connectivity if supported.

## Device
Model:
Android version:

## Results

## Bugs observed
```

---

# 30. Phase 00 Must Build the Automation Foundation

Before normal feature development, Phase 00 should ensure the repository contains:

```text
docs/project_state/
release_records/
scripts/
```

and create:

```text
scripts/check_project.ps1
scripts/phase_complete.ps1
scripts/archive_release.ps1
scripts/build_android.ps1
```

Optional cross-platform equivalents may be added.

The automation itself must be tested.

---

# 31. Build Script Responsibilities

`scripts/build_android.ps1` should:

1. enter the Flutter app directory;
2. run dependency restore;
3. run analyze;
4. run Flutter tests;
5. build debug APK;
6. return artifact path;
7. return non-zero exit code on failure.

Do not bury build failures.

---

# 32. Archive Script Responsibilities

`scripts/archive_release.ps1` should accept parameters similar to:

```powershell
-Phase "02"
-PhaseName "Capture MVP"
-Version "0.2.0"
-BuildType "debug"
-ApkPath "<path>"
```

It should:

- make the version folder;
- copy and rename the APK;
- compute hash;
- generate manifest;
- copy to configured archive path;
- preserve previous versions.

---

# 33. Git Safety

Before automated commit/tag:

- ensure secrets are not staged;
- ensure `.env` files are excluded;
- scan staged filenames;
- inspect `git diff --cached`.

Automation should preferably **prepare** the commit/tag and report it rather than using destructive Git operations.

Never:

```text
git reset --hard
git clean -fd
git push --force
```

unless the owner explicitly instructs it for a understood reason.

---

# 34. Secrets

Never commit:

- API keys;
- Supabase service-role keys;
- signing keystores;
- signing passwords;
- OAuth client secrets;
- Google Drive tokens;
- personal access tokens.

Use environment variables or secure secret stores.

The Android signing keystore must be backed up securely outside the repository.

---

# 35. Database Change Protocol

For every database change:

1. create a migration;
2. do not manually modify production schema without migration record;
3. record migration number in `PROJECT_STATE.md`;
4. test migration;
5. test existing data where applicable;
6. document rollback/forward-fix strategy;
7. never delete user data casually to make a migration pass.

---

# 36. API Change Protocol

When adding/changing an endpoint:

- update shared contracts if applicable;
- update validation;
- update tests;
- update API documentation;
- preserve compatibility where required;
- document breaking changes explicitly.

Do not make the mobile client depend on undocumented response shapes.

---

# 37. UI/UX Implementation Rule

Do not invent major screens or flows outside the master specification.

Every screen must support applicable states:

```text
loading
empty
success
partial
failure
offline/connection issue
```

User-visible processing state must reflect real backend state.

Never display "processed", "transcribed", or "summarized" unless the corresponding artifact actually exists.

---

# 38. External Platform Adapter Rule

Before implementing Instagram, TikTok, YouTube, Facebook, X, Reddit, or another external platform adapter:

1. verify current official documentation;
2. record verification date;
3. document permissions/auth requirements;
4. document accessible fields;
5. document unavailable fields;
6. document quota/rate constraints;
7. add adapter contract tests;
8. retain link-only fallback.

Never make the entire application depend on one unofficial scraper.

---

# 39. AI Feature Rule

For transcript, OCR, summary, tagging, embeddings, or RAG:

- preserve source provenance;
- identify provider/model/version;
- separate source text from generated interpretation;
- validate structured outputs;
- never fabricate missing source content;
- log processing status and failure safely;
- account for cost.

AI output is not canonical source data.

---

# 40. Observability Requirement

New critical operations must provide sufficient logs for debugging.

At minimum log:

- request/job identifier;
- content item identifier;
- processing stage;
- adapter/provider;
- status;
- error class;
- retry count;
- elapsed time where useful.

Never log secrets or unnecessarily log private raw content.

---

# 41. Failure Handling

If something fails:

1. preserve the user's saved URL;
2. preserve already-completed artifacts;
3. record real failure state;
4. provide retry where appropriate;
5. do not pretend the whole item failed when only one enrichment stage failed.

---

# 42. No Hallucination Rules for Coding Agents

The agent MUST NOT:

- invent files it has not inspected;
- invent successful test results;
- invent API endpoints;
- invent platform permissions;
- invent package versions;
- invent environment variables;
- invent database state;
- claim an APK was built if the file does not exist;
- claim Google Drive sync succeeded without evidence from the configured sync mechanism;
- rewrite the master spec to hide implementation deviations.

When information is unknown, write:

```text
UNKNOWN — MUST VERIFY
```

---

# 43. If Blocked

A blocked work unit must produce:

```markdown
## Blocker

What failed:
Evidence:
Commands:
Likely cause:
What was ruled out:
Safe options:
Recommended next action:
Files affected:
```

Do not destroy working architecture to bypass a local error.

---

# 44. Dependency Upgrade Policy

Do not opportunistically upgrade dependencies during feature work.

Upgrade only when:

- required for the phase;
- fixing a confirmed issue;
- addressing security;
- explicitly scheduled.

Record upgrades and regression-test them.

---

# 45. Phase Order

Follow the master specification phase order unless the owner explicitly approves a change:

```text
Phase 00 — Repository, Standards and Product Baseline
Phase 01 — Authentication and Base Application Shell
Phase 02 — Capture MVP
Phase 03 — Collections, Tags and Library
Phase 04 — Processing Job Framework
Phase 05 — Adapter Framework + Generic Web Metadata
Phase 06 — Official Platform Metadata Integrations
Phase 07 — Object Storage and Controlled Media Pipeline
Phase 08 — Transcription
Phase 09 — OCR / Visual Text Extraction
Phase 10 — AI Enrichment
Phase 11 — Full-Text Search
Phase 12 — Semantic Search
Phase 13 — Ask My Library
Phase 14 — Knowledge Connections
Phase 15 — Export, Portability and Recovery
Phase 16 — Performance, Cost and Production Hardening
```

Do not silently jump ahead.

---

# 46. MVP Milestone

The agent must treat the MVP cut line defined in the master specification as more important than flashy AI features.

The first major success is:

> A user can share/save a useful link quickly, close the app, return later, and reliably find the saved item.

Do not delay this milestone in order to build advanced RAG or aggressive scraping.

---

# 47. Automated Master Workflow

The intended automated workflow is:

```text
START
 ↓
Read spec/state
 ↓
Detect current phase
 ↓
Load work-unit checklist
 ↓
Run baseline tests
 ↓
Implement next work unit
 ↓
Run focused tests
 ↓
Update durable state
 ↓
Commit checkpoint when coherent
 ↓
More work units?
 ├─ YES → repeat
 └─ NO
      ↓
Run full phase validation
      ↓
All required checks pass?
 ├─ NO → phase remains INCOMPLETE
 └─ YES
      ↓
Build Android APK
      ↓
Build successful?
 ├─ NO → phase remains INCOMPLETE
 └─ YES
      ↓
Archive APK + checksum + manifest
      ↓
Generate phase report + phone checklist
      ↓
Copy to Google Drive sync target
      ↓
Verify archive copy exists
      ↓
Commit release records
      ↓
Tag phase version
      ↓
Update PROJECT_STATE + NEXT_ACTION
      ↓
PHASE COMPLETE
      ↓
Start next phase only when instructed/allowed
```

---

# 48. What "Automated" Does Not Mean

Automation must not mean:

- blindly proceeding after failed tests;
- inventing architecture decisions;
- accepting API terms automatically;
- creating production secrets;
- changing billing;
- deleting data;
- publishing an app publicly;
- bypassing platform controls.

Automation should remove repetitive engineering steps, not remove safety gates.

---

# 49. Human Approval Gates

Require the project owner before:

- changing the master product scope;
- replacing Flutter/backend/database architecture;
- introducing a paid service with material recurring cost;
- enabling production billing;
- deleting or migrating irreversible user data;
- publishing Play Store releases;
- changing app signing identity;
- adding unsupported scraping/bypass techniques;
- changing privacy/retention behavior materially.

Normal work inside an approved phase does not need repeated approval.

---

# 50. Minimum End-of-Session Report to User

At the end of each agent session, report:

```text
CURRENT PHASE:
WORK UNIT COMPLETED:
FILES CHANGED:
TESTS:
APK:
GIT STATUS:
BLOCKERS:
NEXT ACTION:
```

If an APK was not expected yet because the phase is still in progress, state:

```text
APK: Not built — phase not yet complete.
```

Do not build misleading "phase complete" APKs from half-completed phases unless explicitly requested as a diagnostic snapshot.

---

# 51. End-of-Phase Report to User

At successful completion:

```text
PHASE XX — <NAME>: COMPLETE

Implemented:
- ...

Validation:
- flutter analyze — PASS
- flutter test — PASS
- backend tests — PASS
- integration tests — PASS

APK:
<filename>

APK SHA256:
<hash>

Release record:
<path>

Google Drive archive:
<verified local sync/archive path or upload result>

Git:
Commit: <hash>
Tag: <tag>

Known issues:
- ...

Phone tests to perform:
1. ...
2. ...

Next phase:
Phase XX+1 — ...
```

Only report actual results.

---

# 52. Recommended Repository Layout Additions

The master specification's repository layout should be extended with:

```text
reels-links-to-content/
├── apps/
├── services/
├── packages/
├── database/
├── docs/
│   ├── Reels_Links_to_Content_Master_Spec_v1.0.md
│   ├── AI_IMPLEMENTATION_ORCHESTRATION_PROTOCOL.md
│   ├── adr/
│   ├── adapters/
│   └── project_state/
│       ├── PROJECT_STATE.md
│       ├── CURRENT_PHASE.md
│       ├── NEXT_ACTION.md
│       ├── IMPLEMENTATION_LOG.md
│       ├── SESSION_HANDOFF.md
│       ├── KNOWN_ISSUES.md
│       ├── DECISIONS_PENDING.md
│       └── BUILD_HISTORY.md
├── release_records/
├── scripts/
│   ├── check_project.ps1
│   ├── build_android.ps1
│   ├── archive_release.ps1
│   └── phase_complete.ps1
├── tests/
├── .env.example
└── README.md
```

---

# 53. Initial Files the First Agent Must Create

If they do not yet exist, the first implementation agent must create:

```text
docs/project_state/PROJECT_STATE.md
docs/project_state/CURRENT_PHASE.md
docs/project_state/NEXT_ACTION.md
docs/project_state/IMPLEMENTATION_LOG.md
docs/project_state/SESSION_HANDOFF.md
docs/project_state/KNOWN_ISSUES.md
docs/project_state/DECISIONS_PENDING.md
docs/project_state/BUILD_HISTORY.md
release_records/README.md
scripts/release.config.example.ps1
```

Then populate them using actual repository facts.

---

# 54. Build History

`BUILD_HISTORY.md` should append:

```markdown
| Date | Phase | Version | Type | APK | SHA256 | Git commit | Result |
|---|---|---|---|---|---|---|---|
```

This becomes the quick index of every phone-testable build.

---

# 55. Bugfixes After a Phase

If phone testing reveals a bug:

1. do not modify the historical artifact;
2. reopen the same phase as a patch;
3. increment patch version;
4. fix;
5. rerun required regression checks;
6. build a new APK;
7. create new release folder;
8. archive;
9. update build history;
10. tag patch release.

Example:

```text
0.2.0 → first Phase 02 completion
0.2.1 → duplicate-save bug fix
0.2.2 → Android share-intent fix
```

---

# 56. Recovery After Broken Experimental Work

If an experiment fails:

- keep a record of what was tried;
- revert only the experiment changes, not unrelated work;
- prefer Git revert/checkpoint recovery;
- never wipe the repository casually;
- update KNOWN_ISSUES if the underlying problem remains.

---

# 57. Definition of Done — Work Unit

A work unit is done only when:

- intended behavior implemented;
- focused tests pass;
- code builds/type-checks as applicable;
- documentation/state updated;
- no unexplained errors remain;
- next action is explicit.

---

# 58. Definition of Done — Phase

A phase is done only when:

- all master-spec phase criteria pass;
- all work units are done;
- regression tests pass;
- phase report exists;
- installable Android APK exists;
- SHA-256 exists;
- build manifest exists;
- release record exists;
- archive/sync step completed or explicitly reported failed;
- Git commit/tag exists;
- project state points to the next phase.

---

# 59. Master Prompt for AGY or Codex

Copy the following instruction to a new coding agent:

```text
You are implementing the project "Reels Links to Content".

Before changing any code, read these files completely:
1. docs/Reels_Links_to_Content_Master_Spec_v1.0.md
2. docs/AI_IMPLEMENTATION_ORCHESTRATION_PROTOCOL.md
3. docs/project_state/PROJECT_STATE.md
4. docs/project_state/CURRENT_PHASE.md
5. docs/project_state/NEXT_ACTION.md
6. docs/project_state/SESSION_HANDOFF.md
7. docs/project_state/KNOWN_ISSUES.md
8. relevant ADRs.

Then inspect the repository with git status, current branch, recent commits, latest migration, and existing tests.

Do not rely on chat history. Do not invent requirements, APIs, test results, data, permissions, or completed work.

Continue only the current phase and current work unit unless the project state or owner explicitly says otherwise.

Work in small durable units. After each meaningful work unit:
- run focused validation;
- update IMPLEMENTATION_LOG.md;
- update CURRENT_PHASE.md;
- update PROJECT_STATE.md where needed;
- refresh NEXT_ACTION.md and SESSION_HANDOFF.md;
- create a coherent Git checkpoint when appropriate.

Assume the context window or quota may end unexpectedly. The repository documentation must always contain enough state for AGY, Codex, or another agent to resume without chat history.

At the end of a phase:
- run all required tests/checks;
- do not mark the phase complete if mandatory checks fail;
- build an installable Android APK;
- use debug APK until release signing is deliberately configured;
- archive the APK under release_records using the defined naming/version policy;
- generate SHA256SUMS.txt;
- generate BUILD_MANIFEST.json from real command results;
- generate PHASE_REPORT.md;
- generate a phase-specific PHONE_TEST_CHECKLIST.md;
- copy the release artifact to the configured Google Drive synchronized archive path;
- verify that the archive copy exists;
- record build history;
- commit the phase records;
- create the phase Git tag;
- update the exact next phase/action.

Do not commit secrets. Do not normally commit APK binaries to normal Git history unless Git LFS has been intentionally configured. Git stores source and release metadata; Google Drive stores testable APK artifacts.

Report every implementation step with:
STEP, status, changes, files, validation, issues, and next action.

If something is unknown, write UNKNOWN — MUST VERIFY instead of guessing.

If blocked, preserve working functionality and document the blocker rather than bypassing architecture.

Start by telling me:
- the current phase,
- the current work unit,
- baseline repository status,
- baseline test/build status,
- the exact next implementation action.

Then proceed with implementation.
```

---

# 60. Master Instruction for Fully Automated Continuation

When the project owner says:

> Continue automatically.

Interpret this as:

1. continue the current approved phase;
2. complete work units sequentially;
3. run validations after each;
4. maintain durable state;
5. fix ordinary implementation/test defects within phase scope;
6. perform phase completion gate;
7. generate/archive APK;
8. report completion.

It does **not** authorize:

- changing product scope;
- spending money;
- publishing externally;
- deleting production data;
- bypassing platform restrictions;
- changing architecture without ADR/approval.

After a completed phase, the agent may prepare the next phase state but should follow the owner's chosen autonomy policy for whether to begin it immediately.

---

# 61. Recommended Autonomy Mode

For this project, use:

```text
AUTONOMY_MODE=PHASE_AUTOMATIC
```

Meaning:

- agent may finish all work units inside the current phase without repeatedly asking permission;
- agent may fix defects necessary to satisfy that phase;
- agent must stop on architecture/product/compliance/payment decisions;
- agent must complete APK + release record automatically;
- after phase completion it reports results and prepares the next phase.

If the owner later wants uninterrupted multi-phase execution, change to:

```text
AUTONOMY_MODE=MULTI_PHASE
```

but retain all quality gates.

---

# 62. Recommended First Action

Before feature implementation continues, bootstrap the orchestration system:

1. put the master specification into `/docs`;
2. put this protocol into `/docs`;
3. inspect current repository state;
4. create all project-state files;
5. create release-record structure;
6. create build/archive/phase-completion scripts;
7. configure the local Google Drive archive path;
8. test the automation using a baseline Android APK;
9. create a Phase 00 release record;
10. only then continue Phase 01.

This prevents later implementation from becoming dependent on AI memory.

---

# 63. Final Non-Negotiable Rule

> **No AI session is the project memory. The repository is the project memory. Every meaningful change must leave enough code, tests, logs, state, and release evidence for the next agent to continue safely.**
