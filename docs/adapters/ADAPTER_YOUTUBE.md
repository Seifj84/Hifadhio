# YouTube Platform Adapter Specification (`ADAPTER_YOUTUBE.md`)

## 1. Official Documentation Links
- **oEmbed Endpoint Specification**: https://oembed.com/ and https://developers.google.com/youtube/oembed
- **Endpoint URL**: `https://www.youtube.com/oembed?url={URL}&format=json`
- **YouTube Data API v3 Reference**: https://developers.google.com/youtube/v3/docs/videos

## 2. Date Verified
- **Verification Date**: 2026-09-29
- **Verified by**: Antigravity Agent (Phase 06)

## 3. Authentication & Scopes
- **Authentication**: None required for the public oEmbed endpoint.
- **API Key**: Not required for oEmbed requests.
- **Scopes**: Public read access only.

## 4. Exact Fields Used
| Field Name | Source | Hifadhio Target | Description |
|---|---|---|---|
| `title` | oEmbed JSON | `title`, `original_title` | Official title of the YouTube video |
| `author_name` | oEmbed JSON | `creator_name` | Channel display name |
| `author_url` | oEmbed JSON | `creator_handle` | Channel page link |
| `thumbnail_url` | oEmbed JSON | `thumbnail_url` | Video preview thumbnail |
| `type` | oEmbed JSON | `content_type` | Content type identifier ("video") |
| `videoId` | URL Parser | Used for fallback thumbnail | 11-character video ID |

## 5. Quotas & Rate Limits
- Public oEmbed endpoints do not enforce restrictive per-day quotas on standard mobile device requests.
- Standard client-side HTTP timeouts: 8000ms connect, 10000ms read.

## 6. Storage & Retention Rules
- Metadata (title, author, thumbnail URL, canonical URL) is stored locally in `content_items` SQLite table.
- Raw JSON response is optionally stored in `raw_json` for debugging.
- No copyrighted video binary data is captured or retained in Phase 06.

## 7. Test Fixture URLs
- Standard Video: `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- Shortened URL: `https://youtu.be/dQw4w9WgXcQ?si=abcdef123456`
- YouTube Short: `https://www.youtube.com/shorts/kJQP7kiw5Fk`
- YouTube Live: `https://www.youtube.com/live/jfKfPfyJRdk`
- Embed Link: `https://www.youtube.com/embed/dQw4w9WgXcQ`

## 8. Known Failure Modes
1. **Network Disconnection / Offline Mode**: Device is in airplane mode or lacks connectivity.
2. **Private / Removed Videos**: Video is set to private, deleted, or geoblocked.
3. **Age-Restricted Content**: oEmbed may return HTTP 401 or restricted payload.

## 9. Fallback Behavior
- If network connection fails or oEmbed returns non-200:
  - Canonical URL is deterministically synthesized: `https://www.youtube.com/watch?v={videoId}` or `https://www.youtube.com/shorts/{videoId}`.
  - Video thumbnail is deterministically constructed: `https://i.ytimg.com/vi/{videoId}/hqdefault.jpg`.
  - Title defaults to `"YouTube Video"` (or `"YouTube Short"`).
  - Creator defaults to `"YouTube"`.
  - Capture pipeline succeeds immediately with `READY` status without failing or crashing.
