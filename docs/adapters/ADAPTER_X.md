# X / Twitter Platform Adapter Specification (`ADAPTER_X.md`)

## 1. Official Documentation Links
- **Twitter oEmbed / Publish Documentation**: https://developer.twitter.com/en/docs/twitter-for-websites/embedded-tweets/overview
- **Endpoint URL**: `https://publish.twitter.com/oembed?url={URL}`

## 2. Date Verified
- **Verification Date**: 2026-09-29
- **Verified by**: Antigravity Agent (Phase 06)

## 3. Authentication & Scopes
- **Authentication**: None required for the public publish oEmbed endpoint.
- **API Key**: Not required.
- **X API v2 (Optional Future Phase)**: Requires developer portal registration and Bearer Token.

## 4. Exact Fields Used
| Field Name | Source | Hifadhio Target | Description |
|---|---|---|---|
| `author_name` | oEmbed JSON | `creator_name` | Name of the author |
| `username` | URL Parser | `creator_handle` | Account handle (`@username`) |
| `html` | oEmbed JSON | `description`, `title` | Embedded tweet HTML containing text content |
| `type` | URL Parser | `content_type` | Content type identifier ("post") |

## 5. Quotas & Rate Limits
- Public publish oEmbed endpoints are rate-limited per IP.
- Mobile client timeouts: 8000ms connect, 10000ms read.

## 6. Storage & Retention Rules
- Tweet text, author name, handle, and canonical URL stored in SQLite `content_items` table.
- Raw JSON stored in `raw_json`.

## 7. Test Fixture URLs
- Direct Status Link (x.com): `https://x.com/AndroidDev/status/1765432109876543210`
- Direct Status Link (twitter.com): `https://twitter.com/SeifTech/status/1834567890123456789`
- Link with Tracking: `https://x.com/AndroidDev/status/1765432109876543210?s=20&t=abcdef`

## 8. Known Failure Modes
1. **Network Disconnection**: Connectivity drops.
2. **Deleted / Protected Tweets**: oEmbed returns 404 or 403.

## 9. Fallback Behavior
- Canonicalized to `https://x.com/{username}/status/{id}`.
- Author handle extracted from URL (`@{username}`).
- Title defaults to `"Post by @{username} on X"` or `"Post on X"`.
- Capture completes with `READY` status.
