# Next Action

Phase: Transitioning to Phase 07 — Object Storage and Controlled Media Pipeline

1. User testing and device verification of `Hifadhio-v0.6.0-phase6-debug.apk`:
   - Test saving YouTube videos (watch URLs, shorts, and youtu.be shortlinks). Verify official title, channel name, and thumbnail extraction.
   - Test saving TikTok links. Verify title/caption and creator handle extraction.
   - Test saving Instagram Reels and posts. Verify clean canonicalization and structured fallback.
   - Test saving Reddit discussion links and X/Twitter posts.
2. Prepare Phase 07 per Master Spec Section 1155-1170:
   - Private object storage design for media caching.
   - Media retention lifecycle and temporary working files.
   - Item media-access policy and cleanup worker.
