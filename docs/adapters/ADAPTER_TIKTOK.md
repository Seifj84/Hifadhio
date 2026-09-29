# TikTok Platform Adapter Specification (`ADAPTER_TIKTOK.md`)

## 1. Official Documentation Links
- **TikTok oEmbed Specification**: https://developers.tiktok.com/doc/embed-videos
- **Endpoint URL**: `https://www.tiktok.com/oembed?url={URL}`
- **TikTok Display API Documentation**: https://developers.tiktok.com/doc/display-api-get-started

## 2. Date Verified
- **Verification Date**: 2026-09-29
- **Verified by**: Antigravity Agent (Phase 06)

## 3. Authentication & Scopes
- **Authentication**: None required for the public oEmbed endpoint.
- **API Key**: Not required for basic oEmbed extraction.
- **Display API Scopes (Optional Future Phase)**: `user.info.basic`, `video.list` (requires OAuth app).

## 4. Exact Fields Used
| Field Name | Source | Hifadhio Target | Description |
|---|---|---|---|
| `title` | oEmbed JSON | `title`, `original_title`, `caption` | Creator's caption or video title |
| `author_name` | oEmbed JSON | `creator_name` | Creator display name |
| `author_unique_id` | oEmbed JSON | `creator_handle` | TikTok username handle (e.g. `@username`) |
| `thumbnail_url` | oEmbed JSON | `thumbnail_url` | Video cover image URL |
| `type` | oEmbed JSON | `content_type` | Content type ("video") |

## 5. Quotas & Rate Limits
- TikTok oEmbed rate limit: Typically ~10 requests per second per IP before soft 429 throttling.
- Standard client-side HTTP timeouts: 8000ms connect, 10000ms read.

## 6. Storage & Retention Rules
- Extracted captions, creator handle, and thumbnail URL stored in SQLite `content_items` table.
- Raw JSON response stored in `raw_json`.
- Video media stream downloading is deferred to Phase 07.

## 7. Test Fixture URLs
- Direct Video URL: `https://www.tiktok.com/@tiktok/video/7106594312292453678`
- Short Link (vt): `https://vt.tiktok.com/ZS234567/`
- Short Link (vm): `https://vm.tiktok.com/ZM876543/`
- URL with Tracking Query: `https://www.tiktok.com/@user/video/7106594312292453678?is_from_webapp=1&sender_device=pc`

## 8. Known Failure Modes
1. **Network Offline**: Device offline during capture.
2. **Video Made Private or Removed**: Returns HTTP 404 or empty response.
3. **Short Link Redirection**: Requires following HTTP 301/302 redirects.

## 9. Fallback Behavior
- If network request fails or returns non-200:
  - Canonical URL is maintained (`https://www.tiktok.com/@user/video/{id}`).
  - Creator handle is extracted from URL path (`@user`).
  - Title defaults to `"TikTok Video by @user"` or `"TikTok Video"`.
  - Capture completes cleanly with `READY` state.
