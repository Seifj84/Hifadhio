# Next Action

1. Build native Android codebase under `android/`:
   - Data models: `ContentItem`, `ContentDb`, `UrlNormalizer`, `PlatformDetector`.
   - UI components: `MainActivity`, `HomeFragment`, `InboxFragment`, `LibraryFragment`, `AskFragment`, `ProfileFragment`, `ContentAdapter`, `SaveLinkBottomSheet`, `ItemDetailBottomSheet`.
   - Layouts, vector drawables, Material 3 theme and color resources matching Hifadhio brand tokens.
2. Generate launcher mipmap icons from `docs/brand_assets/hifadhio_app_icon_1024.png`.
3. Set up Gradle build and GitHub Actions CI workflow to compile the APK.
4. Verify tests, commit, push to `origin main`, trigger build, and collect the compiled APK.
