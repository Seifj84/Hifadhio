# Instagram Platform Adapter Specification (`ADAPTER_INSTAGRAM.md`)

## 1. Official Documentation Links
- **Instagram Basic Display API (Deprecated)**: https://developers.facebook.com/docs/instagram-basic-display-api
- **Instagram Graph API Reference**: https://developers.facebook.com/docs/instagram-api
- **Meta oEmbed Endpoint (Requires App Review & Access Token)**: `https://graph.facebook.com/v20.0/instagram_oembed`

## 2. Date Verified
- **Verification Date**: 2026-09-29
- **Verified by**: Antigravity Agent (Phase 06)

## 3. Authentication & Scopes
- **Public Scrapes**: No authentication supported; Meta blocks automated unauthenticated bot traffic with HTTP 400/403 or redirects to `instagram.com/accounts/login/`.
- **Graph API oEmbed (Future)**: Requires Meta App Review, Business Verification, and `oEmbed Read` permission.
- **Current Adapter Policy**: Safe local canonicalization and structured fallback metadata without external bot-scraping risks.

## 4. Exact Fields Used
| Field Name | Source | Hifadhio Target | Description |
|---|---|---|---|
| `shortcode` | URL Parser | `platform_item_id` | Alphanumeric shortcode of the post/reel |
| `type` | URL Parser | `content_type` | `video` for reels/tv, `image` for posts |
| `title` | Fallback / HTML | `title`, `original_title` | Descriptive title (`Instagram Reel`, `Instagram Post`) |
| `creator_name` | Fallback | `creator_name` | `"Instagram"` |

## 5. Quotas & Rate Limits
- Unauthenticated requests from cloud IPs are rejected immediately.
- Mobile client timeouts: 8000ms connect, 10000ms read.

## 6. Storage & Retention Rules
- Canonicalized URLs (`https://www.instagram.com/reel/{shortcode}/`) and metadata stored in SQLite `content_items` table.
- Media binaries are not downloaded in Phase 06.

## 7. Test Fixture URLs
- Instagram Reel: `https://www.instagram.com/reel/C3zY123abcD/`
- Instagram Share Reel: `https://www.instagram.com/share/reel/C3zY123abcD/?igshid=tracking123`
- Instagram Post: `https://www.instagram.com/p/C9x8765uvwX/`
- Instagram TV: `https://www.instagram.com/tv/B5mno987xyz/`

## 8. Known Failure Modes
1. **Login Wall / Bot Blocking**: Meta edge servers actively detect non-browser clients and demand authentication.
2. **Private Accounts**: Content from private accounts cannot be retrieved without user credentials.

## 9. Fallback Behavior
- The adapter intercepts Instagram links before generic scraping.
- Immediately generates clean canonical URLs (`https://www.instagram.com/reel/{id}/` or `https://www.instagram.com/p/{id}/`).
- Saves structured fallback metadata immediately (`Instagram Reel`, `Instagram Post`).
- Captures content with `READY` state without throwing failed exceptions.
