# Phase 5 Corrective Root-Cause Diagnosis Report

**Date:** 2026-09-29  
**Investigator:** Antigravity AI Engine  
**Target:** Hifadhio Android Processing Pipeline & UX  

---

## 1. Facebook Failure Analysis

* **Example Target:** `https://facebook.com/share/r/1Qbbym5DGx`
* **Detected Adapter:** `GenericWebAdapter`
* **Normalized URL:** `https://facebook.com/share/r/1Qbbym5DGx`
* **Redirect Chain:**
  1. `https://facebook.com/share/r/1Qbbym5DGx` (HTTP 301 Moved Permanently)
  2. `Location: https://www.facebook.com/share/r/1Qbbym5DGx` (HTTP 400 Bad Request)
* **Request Method:** `GET`
* **Headers Sent:** User-Agent `Mozilla/5.0 (Linux; Android 14) ... Mobile Safari/537.36 Hifadhio/0.5.0`
* **Returned Status:** `HTTP/1.1 400 Bad Request`
* **Server Header:** `Proxy-Status: http_request_error; e_proxy="AcQIYOJ78ihD6FDTJkmLNlCB2EJH9L54Y0HRyW0gLMAl5mHg7LgHltI4VJZw8TKKN_iN16C2m7LyeBPz8sXC"`
* **Root Cause:**
  Facebook proxy edge gateways intentionally reject server-side / headless HTTP scraping requests to `facebook.com/share/r/...` with HTTP 400 Bad Request to protect against automated scrapers. Routing Facebook links to `GenericWebAdapter` causes guaranteed HTTP 400 exceptions. Because this was treated as a transient network exception, `failJob` scheduled retries, trapping the job in repeated `RETRYING` loops.
* **Resolution Strategy:**
  1. Implement a dedicated `FacebookAdapter` registered in `ContentAdapterRegistry` with priority 80.
  2. Canonicalize Facebook reel share URLs: `/share/r/<id>` ➔ `https://www.facebook.com/reel/<id>/`.
  3. Recognize platform specifically as `"Facebook"` and type as `"video"` (Reel) or `"post"`.
  4. Perform safe metadata extraction with graceful non-blocking fallback: when Facebook blocks automated scraping (HTTP 400/403/redirect to login), cleanly generate a standardized fallback metadata record (`title = "Facebook Reel"`, `platform = "Facebook"`, `canonicalUrl = canonicalReelUrl`).
  5. The job reaches `COMPLETED` (`READY`), preserving the saved link, correct platform badge, and one-tap "Open in Browser" / native app action.

---

## 2. FAO Cleartext HTTP Failure Analysis

* **Input URL:** `https://fao.org` or `https://fao.org/...`
* **Component Making Request:** `GenericWebAdapter.fetchHtml(String targetUrl)`
* **Redirect Chain Verified by Live Probe:**
  1. Request: `https://fao.org` (HTTP 301 Moved Permanently)
  2. Server Header: `Location: http://www.fao.org/home/en` **(Note: Scheme is http://, NOT https://!)**
* **Exact Point Where HTTPS Becomes HTTP:**
  Step 1 response header from FAO's edge server sends an insecure cleartext `http://` redirect target: `Location: http://www.fao.org/home/en`.
* **Reason Android Reports Cleartext Traffic:**
  `GenericWebAdapter` faithfully followed the `Location` header to `http://www.fao.org/home/en`. Android (API 28+) enforces `NetworkSecurityPolicy.isCleartextTrafficPermitted() == false` by default for all HTTP requests, throwing `java.io.IOException: Cleartext HTTP traffic to www.fao.org not permitted`.
* **Resolution Strategy:**
  1. Enforce HTTPS redirect preservation: If any redirect `Location` header contains an `http://` scheme, or if the original request was HTTPS, automatically promote the URL to `https://` (`http://` ➔ `https://`).
  2. In `GenericWebAdapter.fetchHtml()`, catch any `IOException` containing `"Cleartext HTTP traffic"`. Do NOT crash or endlessly retry: treat it as a controlled extraction fallback, preserve the user's link intact, and complete the job.
  3. Do NOT globally allow cleartext HTTP in `AndroidManifest.xml` (preserves network security).

---

## 3. Retry System Stagnation Analysis

* **Scheduled Retry Calculation:**
  In `ProcessingJobManager.java`: `retryDelayMs = calculateBackoffMs(attempt)` (e.g. 2,000ms, 4,000ms).
* **Database State:**
  `db.failJob()` correctly updated `state = 'RETRYING'` and `scheduled_at = now + retryDelayMs`.
* **Why the 2s/4s Retry Appeared Minutes Later:**
  1. **Missing Scheduler:** In `ProcessingJobManager.triggerWorker()`, when `processJob()` threw an exception and `failJob()` was called, **no delayed task was ever scheduled** to wake up the worker after `retryDelayMs`!
  2. **Single-Job Worker Trigger:** `triggerWorker()` executed a single `claimNextJob()` and terminated.
  3. Because no timer or delayed handler was invoked, the retrying job remained parked in SQLite indefinitely until a user manually saved another link or restarted the app.
* **Resolution Strategy:**
  1. Implement a `ScheduledExecutorService` in `ProcessingJobManager`: when a retryable failure occurs, schedule `triggerWorker()` after `retryDelayMs` using `scheduler.schedule(() -> triggerWorker(), retryDelayMs, TimeUnit.MILLISECONDS)`.
  2. Implement an active queue drain loop in `triggerWorker()`: after finishing a job, check if more jobs are ready (`scheduled_at <= now AND state IN ('QUEUED', 'RETRYING')`) and process them.
  3. Call `triggerWorker()` on app startup and on resume.

---

## 4. UX & Diagnostic Separation Analysis

* **Current Defect:** The audit event timeline in `ContentDetailBottomSheet` exposed raw internal strings: `worker-12146bc5`, `Extracting metadata via GenericWeb`, and raw Java HTTP stack traces.
* **Resolution Strategy:**
  1. Map internal job events to user-friendly status copy:
     - `Preparing link`
     - `Reading page details`
     - `Extracting details`
     - `Ready`
     - `Could not analyze this link` (with friendly subtitle: "Link saved safely. Tap to retry or open in browser.")
     - `Retrying automatically`
  2. Move technical worker IDs, HTTP response codes, and raw JSON behind an optional, collapsed "Technical Details" debug section or log files only.

---

## 5. Dialog Theme Defect Analysis

* **Current Defect:** The delete confirmation dialog in `LibraryFragment` and `ContentDetailBottomSheet` inherited default Material3 DayNight styles that produced dark surfaces with poor contrast in light mode.
* **Resolution Strategy:**
  1. Define `@style/ThemeOverlay.Hifadhio.Dialog` with explicit `@color/surface` (white in light mode), bold primary text contrast (`@color/text_primary`), secondary text (`@color/text_secondary`), and rounded corners.
  2. Apply `materialAlertDialogTheme` and `alertDialogTheme` in `Theme.Hifadhio`.
  3. Specifically style the destructive button with `@color/danger` and Cancel with `@color/text_secondary`.
