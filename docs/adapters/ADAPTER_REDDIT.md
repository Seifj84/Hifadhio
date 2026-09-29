# Reddit Platform Adapter Specification (`ADAPTER_REDDIT.md`)

## 1. Official Documentation Links
- **Reddit oEmbed Specification**: https://www.reddit.com/dev/api/#GET_oembed
- **Endpoint URL**: `https://www.reddit.com/oembed?url={URL}`

## 2. Date Verified
- **Verification Date**: 2026-09-29
- **Verified by**: Antigravity Agent (Phase 06)

## 3. Authentication & Scopes
- **Authentication**: None required for the public oEmbed endpoint.
- **API Key**: Not required.
- **User Scopes**: Public read access only.

## 4. Exact Fields Used
| Field Name | Source | Hifadhio Target | Description |
|---|---|---|---|
| `title` | oEmbed JSON | `title`, `original_title` | Title of the Reddit post |
| `author_name` | oEmbed JSON | `creator_name` | Author Reddit username (prefixed with `u/`) |
| `subreddit` | URL Parser | `creator_handle` | Subreddit community name (prefixed with `r/`) |
| `type` | oEmbed JSON | `content_type` | Content type identifier ("post") |

## 5. Quotas & Rate Limits
- Public oEmbed endpoints are cached and throttled per IP.
- Standard client-side HTTP timeouts: 8000ms connect, 10000ms read.

## 6. Storage & Retention Rules
- Post metadata and subreddit references stored in SQLite `content_items` table.
- Raw JSON stored in `raw_json`.

## 7. Test Fixture URLs
- Subreddit Comment Thread: `https://www.reddit.com/r/androiddev/comments/17xyz/hifadhio_offline_first_storage/`
- Shortened Reddit Link: `https://redd.it/17xyz`
- Subreddit Page: `https://www.reddit.com/r/technology/`

## 8. Known Failure Modes
1. **Network Disconnection**: Returns connection error.
2. **Subreddit Quarantined / Private**: oEmbed returns 403 / 404.

## 9. Fallback Behavior
- URL is canonicalized to standard thread structure.
- Subreddit name is extracted from URL path (`r/{subreddit}`).
- Title defaults to `"Reddit post in r/{subreddit}"` or `"Reddit Post"`.
- Content capture completes with `READY` status.
