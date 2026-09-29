# Next Action

Phase: Transitioning to Phase 06 - Official Platform Metadata Integrations

1. User testing and device verification of `Hifadhio-v0.5.0-phase5-debug.apk`:
   - Test saving web articles and blogs.
   - Verify extracted titles, OpenGraph descriptions, and thumbnail URLs.
   - Verify Content Detail bottom sheet displaying extracted captions and titles.
2. Prepare Phase 06 per Master Spec Section 1125-1150:
   - Order of implementation:
     1. YouTube (oEmbed + Data API / no-key fallbacks)
     2. TikTok (oEmbed / Display API)
     3. Instagram (oEmbed / metadata parser)
   - Create `ADAPTER_<PLATFORM>.md` for each platform adapter.
   - Register platform adapters into `ContentAdapterRegistry` with higher priority than `GenericWebAdapter`.
