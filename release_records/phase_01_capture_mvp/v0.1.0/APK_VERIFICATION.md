# APK Verification Checklist — Phase 01

Version: 0.1.0-phase1
Package Name: `com.seiftech.hifadhio.debug` / `com.seiftech.hifadhio`
Target SDK: 35
Min SDK: 24

## Functional Verification Steps
1. **Fresh Installation**:
   - Install `Hifadhio-v0.1.0-phase1-debug.apk` via `adb install` or Android Package Installer.
   - Verify launcher icon renders official Hifadhio branding.
2. **App Launch & Orientation**:
   - Launch app. Verify Bottom Navigation with 5 tabs: Home, Inbox, Library, Ask, Profile.
   - Verify stats card shows "0 Saved Content Items" on first launch.
3. **Manual URL Capture**:
   - Tap "Save Link" button.
   - Paste link: `https://www.instagram.com/reel/DCb1234/?igshid=abc123xyz&utm_source=ig_web_copy_link`.
   - Verify platform badge instantly identifies "Detected: Instagram".
   - Add title "Sample Reel" and note "Inspiration for design".
   - Tap "Save to Library".
   - Verify card appears in list with normalized URL (tracking params stripped).
4. **Android Native Share Target**:
   - From Instagram, TikTok, YouTube, or Chrome, tap Share → select Hifadhio.
   - Verify capture bottom sheet automatically appears with prefilled URL and detected platform.
   - Tap Save and verify seamless return to source app.
5. **Search & Filter**:
   - Search by keyword in the search bar.
   - Tap "Instagram" or "YouTube" chips to filter by platform.
6. **Data Retention & Integrity**:
   - Close app and reopen. Confirm all saved items remain intact.
   - Test "Export Library as JSON" from Profile tab.
7. **Deletion**:
   - Open item menu → tap "Delete" → confirm deletion dialog. Verify item is removed.
