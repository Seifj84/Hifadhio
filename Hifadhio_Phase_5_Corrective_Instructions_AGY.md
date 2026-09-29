# Hifadhio — Phase 5 Corrective Instruction for AGY

## Context

Phase 5 is currently **not behaving correctly in real-device testing**.

The screenshots show that saving links works at the UI/database level, but the **background processing pipeline is failing repeatedly** for both Facebook links and ordinary web links.

Do **not** treat Phase 5 as complete until these failures are diagnosed, fixed, regression-tested, and verified on a real Android build.

---

# 1. What is happening

## A. Facebook Reel processing is failing

Example saved links:

- `https://facebook.com/share/r/1Qbbym5DGx`
- `https://facebook.com/share/r/1GwGTBKiUP`

The item is successfully created and appears in the Library, but the processing job reaches approximately:

- 15% — Identifying content adapter
- 40% — Extracting metadata via `GenericWeb`

Then it fails with:

> `Failed to fetch content from URL: HTTP response error: 400 Bad Request`

The job then enters `RETRYING`.

### Important observation

A Facebook URL should **not simply fall through to GenericWeb if Facebook requires special handling**.

AGY must inspect:

- adapter selection logic;
- URL normalization;
- Facebook share/reel redirect handling;
- HTTP request headers;
- redirect behavior;
- cookies/session assumptions;
- whether Facebook blocks ordinary server-side scraping;
- whether the URL must first be resolved to its canonical destination;
- whether Facebook needs a dedicated adapter or safe fallback strategy.

Do not blindly increase retry counts. Retrying the same invalid request three times is not a fix.

---

# 2. Ordinary web links are also failing

A normal FAO link was saved successfully, but processing failed with:

> `Cleartext HTTP traffic to www.fao.org not permitted`

The user supplied an HTTPS URL.

This strongly suggests that somewhere during URL resolution or redirect handling, the pipeline is ending up on an `http://` URL and Android/network security rejects it.

AGY must trace the full redirect chain.

Investigate:

- original URL;
- normalized URL;
- every redirect target;
- whether the HTTP client automatically follows redirects;
- whether an HTTPS → HTTP redirect occurs;
- whether the application rewrites URLs incorrectly;
- whether the request is being executed in Android rather than the backend;
- whether a malformed URL loses the `https` scheme.

### Required behavior

Prefer secure HTTPS.

Do **not** globally enable cleartext traffic merely to hide this bug.

If a site redirects to HTTP:

1. attempt a safe HTTPS equivalent where valid;
2. otherwise report a controlled extraction failure;
3. preserve the saved URL;
4. allow the user to open the original URL normally.

---

# 3. Retry behavior is suspicious

The UI reports messages such as:

- `Next retry in 2s`
- `Next retry in 4s`

but screenshots taken several minutes later still show the job retrying.

AGY must verify that the retry scheduler actually executes at the intended time.

Audit:

- retry queue;
- worker polling interval;
- app foreground/background behavior;
- WorkManager/background-job configuration if Android-side;
- backend worker lifecycle if server-side;
- retry timestamp calculations;
- exponential backoff implementation;
- persistence of queued jobs after app restarts;
- job claiming/locking;
- stale job recovery.

The displayed retry countdown must match actual execution.

---

# 4. Processing status UX is exposing internal implementation details

The current user-facing `Audit Timeline` contains technical details such as:

- `worker-12146bc5`
- `GenericWeb`
- raw HTTP errors;
- internal pipeline stages;
- retry internals.

This is useful for developers, but **not appropriate as the default end-user interface**.

## Change the user-facing UI

Normal users should see simple states such as:

- Preparing link
- Reading page
- Extracting details
- Ready
- Could not analyze this link
- Retrying automatically
- Tap to retry

Detailed worker IDs, adapters, stack/error details, HTTP codes, and internal diagnostics should be available only in:

- development/debug mode;
- admin diagnostics;
- backend logs.

Do not delete diagnostic information from the backend. Separate **user UX** from **developer diagnostics**.

---

# 5. Failed processing must not make the saved item useless

The core Hifadhio promise is:

> Save once. Find it when it matters.

Therefore a metadata-processing failure must **never mean the link itself is lost or unusable**.

If extraction fails:

- keep the original URL;
- keep source/platform detection if known;
- keep saved timestamp;
- keep collection;
- keep user notes;
- keep tags;
- keep favorite state;
- allow `Open in Browser`;
- allow editing;
- allow manual title;
- provide manual retry.

Processing enrichment is secondary to reliable saving.

---

# 6. Facebook handling requires a deliberate strategy

AGY must determine what Phase 5 is realistically expected to extract from Facebook share/reel links.

Do not pretend generic scraping will reliably work if Facebook blocks it.

Implement the best legally and technically sustainable approach.

At minimum:

1. recognize Facebook URLs correctly;
2. resolve share URLs where possible;
3. identify the platform as Facebook;
4. store the canonical/original URL;
5. attempt public metadata extraction safely;
6. fail gracefully when Facebook denies access;
7. never leave the item permanently stuck in `RETRYING`.

If reliable title/thumbnail/description extraction is not possible without authentication or an official API, document that limitation instead of hacking around it.

---

# 7. Fix the retry state machine

The processing state machine must have explicit terminal states.

Recommended states:

- `queued`
- `processing`
- `retry_scheduled`
- `completed`
- `failed`
- `cancelled`

A job must not remain in `RETRYING` indefinitely.

After the configured maximum attempts:

- set the job to `failed`;
- store a sanitized failure reason;
- keep the saved item;
- expose a `Retry` action;
- optionally expose `Open in Browser`.

Ensure duplicate workers cannot process the same job simultaneously.

---

# 8. Delete confirmation dialog has a UI defect

The Library screenshot shows the delete confirmation dialog with an extremely dark surface and poor text contrast.

Fix the dialog theme.

Required:

- readable background;
- strong title contrast;
- readable body copy;
- destructive `Delete` action visually distinguished;
- `Cancel` clearly visible;
- correct appearance in light and dark themes;
- no accidental inherited Material theme causing a nearly-black dialog in the light UI.

Also review all dialogs for consistent Hifadhio styling.

---

# 9. Perform root-cause diagnosis before coding patches

Before changing code, produce a short diagnosis report containing:

## Facebook failure

- detected adapter;
- normalized URL;
- redirect chain;
- request method;
- relevant headers;
- returned status;
- final failure source.

## FAO failure

- input URL;
- redirect chain;
- exact point where HTTPS becomes HTTP, if it does;
- component making the request;
- reason Android reports cleartext traffic.

## Retry system

- scheduled retry timestamp;
- actual worker execution timestamp;
- why a supposed 2s/4s retry appears minutes later.

Do not guess.

Instrument the pipeline temporarily if necessary.

---

# 10. Logging requirements

Add structured backend/debug logging for every processing job.

Include:

- job ID;
- content item ID;
- original URL;
- normalized URL;
- adapter selected;
- redirect chain;
- attempt number;
- state transitions;
- start/end timestamps;
- HTTP status;
- sanitized exception;
- retry scheduled time;
- terminal outcome.

Never log:

- passwords;
- auth tokens;
- cookies;
- private user credentials;
- secrets.

The user-facing application must not display raw logs.

---

# 11. Tests that must be added

Create automated tests for at least:

### URL classification

- Facebook Reel URL
- Facebook share URL
- normal HTTPS webpage
- redirecting webpage
- malformed URL

### Processing

- successful generic page extraction
- HTTP 400
- HTTP 403
- HTTP 404
- timeout
- redirect loop
- HTTPS → HTTP redirect
- unsupported site
- adapter extraction failure

### Retries

- attempt 1 failure → retry
- retry executes
- attempt counter increments correctly
- max attempts → terminal failed state
- no duplicate simultaneous processing
- manual retry after failure

### Persistence

Verify saved content remains available even when enrichment fails.

---

# 12. Real-device acceptance tests

After fixes, build and install a fresh Android APK and test at least:

1. one normal public HTTPS article;
2. one FAO link;
3. one Facebook Reel/share link;
4. one intentionally invalid URL;
5. app restart while a job is queued;
6. network unavailable → network restored;
7. manual retry after terminal failure;
8. delete confirmation dialog.

Record the outcome of each test.

---

# 13. Expected UI behavior after the fix

A newly saved link should quickly show:

`Queued → Processing → Ready`

If extraction temporarily fails:

`Processing → Retrying`

If all allowed attempts fail:

`Could not analyze this link`

with:

- Retry
- Open in Browser
- Edit Details

The item itself must remain safely stored.

---

# 14. Do not do these shortcuts

Do NOT:

- mark Phase 5 complete because the queue technically runs;
- endlessly retry permanent HTTP errors;
- enable global cleartext HTTP just to suppress the FAO error;
- expose worker IDs/raw backend errors to normal users;
- delete saved content when metadata extraction fails;
- use GenericWeb for every platform without validating whether it is appropriate;
- hide failures without logging them;
- patch only the visible error message while leaving the pipeline broken.

---

# 15. Deliverables

When complete, report:

1. root cause of each observed failure;
2. files changed;
3. architecture/logic changes;
4. retry state-machine changes;
5. Facebook handling changes;
6. redirect/network changes;
7. UI changes;
8. tests added;
9. automated test results;
10. real-device test results;
11. remaining known limitations;
12. final Phase 5 status: `PASS` or `NOT READY`.

If any important failure remains, Phase 5 must be reported as `NOT READY`.

---

## Main conclusion

The current problem is **not that links fail to save**.

The save layer appears to work.

The failure is in the **post-save enrichment/processing pipeline**:

- Facebook requests are reaching `GenericWeb` and returning HTTP 400;
- a normal HTTPS FAO link is ending up in a cleartext HTTP rejection;
- retry scheduling appears inconsistent with the UI message;
- the app exposes internal processing diagnostics directly to end users;
- the delete dialog theme also has a visual defect.

Fix the pipeline architecture and failure handling, not just the visible symptoms.
