# Current Phase

Phase: Phase 08
Name: Audio Extraction and Transcription Pipeline
Status: IN_PROGRESS
Started: 2026-09-29T20:53:00+03:00
Completed: 
Target specification section: Sections 15.3, 20.2, 35 (ADR-007) & 1171-1188 of Master Spec

## Objective
Implement an extensible, traceable audio extraction and speech-to-text transcription pipeline for Hifadhio. Provide vendor-neutral provider abstraction (`TranscriptionProvider`), audio validation and duration checks, deterministic local transcript storage backed by SQLite Schema v5 (`transcripts` table), strict adherence to the "never fabricate timestamps" rule, transcript search-on-detail in UI, and automated integration with Hifadhio's asynchronous processing job queue.

## In scope
- [ ] Database Schema v5 migration in `ContentDb.java`: add `transcripts` table (item ID, full text, language, provider, model, duration, segments JSON, confidence, cost, timestamps).
- [ ] Domain models: `Transcript.java`, `TranscriptSegment.java`, `TranscriptionOptions.java`, `TranscriptionResult.java`.
- [ ] Provider architecture: `TranscriptionProvider` interface, `TranscriptionRegistry`, `SubtitlesExtractorProvider` (captions/tracks from YouTube/oEmbed/subtitles), `OfflineSpeechProvider` (local deterministic engine), and `CloudWhisperProvider` (OpenAI/Groq compliant API abstraction).
- [ ] Audio management: `AudioExtractor.java` handling media audio preparation, file size/duration validation, and storage under `media/audio/`.
- [ ] Processing pipeline integration: `ProcessingJobManager.java` with `JOB_TYPE_TRANSCRIBE`, automated transcription scheduling for video/audio items, artifact persistence, and cascade deletion.
- [ ] UI integration: `ContentDetailBottomSheet` transcript tab/card with live search within transcript ("search-on-detail"), timestamp navigation, language/provider metadata badges, and copy action.
- [ ] Unit test suites: `TranscriptionProviderTest.java`, `TranscriptModelTest.java`, and `AudioExtractorTest.java`.
- [ ] Version bump to `0.8.0-phase8` (versionCode 8), automated CI/CD build, and verified APK release.

## Out of scope
- Visual frame OCR (scheduled for Phase 09).
- AI summaries and RAG embeddings (scheduled for Phase 10 & 12).
- Real-time microphone dictation (scheduled for Phase 14 note capture).

## Preconditions
- Phase 07 completed, verified, and published as `v0.7.0-phase7`.
- Clean working directory on `main`.

## Work units
- [ ] WU-01 Database Schema v5 migration (`transcripts` table, foreign keys, cascade deletion, queries)
- [ ] WU-02 Transcription domain models (`Transcript`, `TranscriptSegment`, `TranscriptionOptions`, `TranscriptionResult`)
- [ ] WU-03 `TranscriptionProvider` interface, `TranscriptionRegistry`, and providers
- [ ] WU-04 `AudioExtractor` subsystem (duration/size checks, audio partition management)
- [ ] WU-05 `ProcessingJobManager` transcription pipeline integration & artifact archiving
- [ ] WU-06 UI transcript rendering with search-on-detail & timestamp chips in `ContentDetailBottomSheet`
- [ ] WU-07 Unit test suites (`TranscriptionProviderTest`, `TranscriptModelTest`, `AudioExtractorTest`)
- [ ] WU-08 Version bump to `0.8.0-phase8` (versionCode 8), CI/CD verification, and release packaging

## Phase acceptance criteria
1. Database Schema v5 seamlessly migrates existing v4 databases with clean foreign keys and indexes.
2. Transcription provider abstraction allows runtime provider registration without hardcoding vendors.
3. True timestamps are preserved only when returned by providers; never fabricated.
4. Extracted transcripts are stored in SQLite and registered in object storage as traceable artifacts.
5. Content detail sheet provides seamless reading, searching within transcript text, and copying.
6. All unit tests pass in CI/CD, and an installable debug APK `v0.8.0-phase8` is produced.
