package com.seiftech.hifadhio.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import com.seiftech.hifadhio.media.MediaArtifact;
import com.seiftech.hifadhio.transcription.Transcript;
import com.seiftech.hifadhio.transcription.TranscriptSegment;
import com.seiftech.hifadhio.ocr.OcrFrame;
import com.seiftech.hifadhio.ocr.OcrRecord;
import java.util.ArrayList;
import java.util.List;

public class ContentDb extends SQLiteOpenHelper {
    public static final String DB_NAME = "hifadhio.db";
    public static final int DB_VERSION = 6;
    public static final String TABLE_ITEMS = "items";
    public static final String TABLE_COLLECTIONS = "collections";
    public static final String TABLE_JOBS = "processing_jobs";
    public static final String TABLE_EVENTS = "processing_events";
    public static final String TABLE_ARTIFACTS = "artifacts";
    public static final String TABLE_TRANSCRIPTS = "transcripts";
    public static final String TABLE_OCR_RECORDS = "ocr_records";

    public static class CollectionStat {
        private final String name;
        private final int itemCount;

        public CollectionStat(String name, int itemCount) {
            this.name = name;
            this.itemCount = itemCount;
        }

        public String getName() { return name; }
        public int getItemCount() { return itemCount; }
    }

    public ContentDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ITEMS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "url TEXT NOT NULL, "
                + "canonical_url TEXT NOT NULL, "
                + "platform TEXT, "
                + "title TEXT, "
                + "original_title TEXT, "
                + "caption TEXT, "
                + "thumbnail_url TEXT, "
                + "notes TEXT, "
                + "collection_name TEXT DEFAULT 'Inbox', "
                + "tags TEXT, "
                + "is_favorite INTEGER DEFAULT 0, "
                + "status TEXT DEFAULT 'SAVED', "
                + "saved_at INTEGER NOT NULL, "
                + "updated_at INTEGER NOT NULL"
                + ")");

        db.execSQL("CREATE INDEX idx_canonical ON " + TABLE_ITEMS + " (canonical_url)");
        db.execSQL("CREATE INDEX idx_platform ON " + TABLE_ITEMS + " (platform)");
        db.execSQL("CREATE INDEX idx_collection ON " + TABLE_ITEMS + " (collection_name)");
        db.execSQL("CREATE INDEX idx_saved_at ON " + TABLE_ITEMS + " (saved_at DESC)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_COLLECTIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT UNIQUE NOT NULL, "
                + "created_at INTEGER NOT NULL"
                + ")");
        seedDefaultCollections(db);

        createJobsTables(db);
        createArtifactsTable(db);
        createTranscriptsTable(db);
        createOcrRecordsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ITEMS + " ADD COLUMN original_title TEXT");
            } catch (Exception ignored) {}
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_COLLECTIONS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT UNIQUE NOT NULL, "
                    + "created_at INTEGER NOT NULL"
                    + ")");
            seedDefaultCollections(db);
        }
        if (oldVersion < 3) {
            createJobsTables(db);
        }
        if (oldVersion < 4) {
            createArtifactsTable(db);
        }
        if (oldVersion < 5) {
            createTranscriptsTable(db);
        }
        if (oldVersion < 6) {
            createOcrRecordsTable(db);
        }
    }

    private void createOcrRecordsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_OCR_RECORDS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "content_item_id INTEGER NOT NULL UNIQUE, "
                + "full_text TEXT NOT NULL, "
                + "provider_id TEXT NOT NULL, "
                + "model TEXT, "
                + "frames_count INTEGER DEFAULT 0, "
                + "frames_json TEXT, "
                + "cost_usd REAL DEFAULT 0.0, "
                + "created_at INTEGER NOT NULL, "
                + "updated_at INTEGER NOT NULL"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ocr_item ON " + TABLE_OCR_RECORDS + " (content_item_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ocr_created ON " + TABLE_OCR_RECORDS + " (created_at)");
    }

    private void createTranscriptsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TRANSCRIPTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "content_item_id INTEGER NOT NULL UNIQUE, "
                + "full_text TEXT NOT NULL, "
                + "language TEXT, "
                + "provider_id TEXT NOT NULL, "
                + "model TEXT, "
                + "duration_ms INTEGER DEFAULT 0, "
                + "segments_json TEXT, "
                + "confidence REAL DEFAULT 1.0, "
                + "cost_usd REAL DEFAULT 0.0, "
                + "created_at INTEGER NOT NULL, "
                + "updated_at INTEGER NOT NULL"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transcripts_item ON " + TABLE_TRANSCRIPTS + " (content_item_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transcripts_created ON " + TABLE_TRANSCRIPTS + " (created_at)");
    }

    private void createArtifactsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ARTIFACTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "content_item_id INTEGER NOT NULL, "
                + "artifact_type TEXT NOT NULL, "
                + "storage_path TEXT NOT NULL, "
                + "content_uri TEXT, "
                + "mime_type TEXT, "
                + "file_size_bytes INTEGER DEFAULT 0, "
                + "sha256 TEXT, "
                + "retention_policy TEXT DEFAULT 'CACHE', "
                + "expires_at INTEGER DEFAULT 0, "
                + "created_at INTEGER NOT NULL"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_art_item ON " + TABLE_ARTIFACTS + " (content_item_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_art_type ON " + TABLE_ARTIFACTS + " (artifact_type)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_art_expires ON " + TABLE_ARTIFACTS + " (expires_at)");
    }

    private void createJobsTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_JOBS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "content_item_id INTEGER NOT NULL, "
                + "job_type TEXT NOT NULL, "
                + "state TEXT NOT NULL, "
                + "priority INTEGER DEFAULT 0, "
                + "attempt_count INTEGER DEFAULT 0, "
                + "max_attempts INTEGER DEFAULT 3, "
                + "progress_percent INTEGER DEFAULT 0, "
                + "stage_message TEXT, "
                + "worker_id TEXT, "
                + "lease_expires_at INTEGER DEFAULT 0, "
                + "scheduled_at INTEGER NOT NULL, "
                + "started_at INTEGER DEFAULT 0, "
                + "completed_at INTEGER DEFAULT 0, "
                + "failed_at INTEGER DEFAULT 0, "
                + "error_code TEXT, "
                + "sanitized_error_message TEXT, "
                + "idempotency_key TEXT UNIQUE, "
                + "created_at INTEGER NOT NULL, "
                + "updated_at INTEGER NOT NULL"
                + ")");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_jobs_state_sched ON " + TABLE_JOBS + " (state, scheduled_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_jobs_content_item ON " + TABLE_JOBS + " (content_item_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_jobs_idempotency ON " + TABLE_JOBS + " (idempotency_key)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EVENTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "job_id INTEGER NOT NULL, "
                + "content_item_id INTEGER NOT NULL, "
                + "event_type TEXT NOT NULL, "
                + "stage TEXT, "
                + "status TEXT, "
                + "message TEXT, "
                + "metadata_json TEXT, "
                + "created_at INTEGER NOT NULL"
                + ")");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_job ON " + TABLE_EVENTS + " (job_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_content ON " + TABLE_EVENTS + " (content_item_id)");
    }

    private void seedDefaultCollections(SQLiteDatabase db) {
        String[] defaults = new String[]{"Inbox", "Articles", "Research", "Inspiration", "Work", "Personal"};
        long now = System.currentTimeMillis();
        for (String name : defaults) {
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("created_at", now);
            db.insertWithOnConflict(TABLE_COLLECTIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public long insert(ContentItem item) {
        ContentValues cv = toContentValues(item);
        long id = getWritableDatabase().insert(TABLE_ITEMS, null, cv);
        item.setId(id);
        return id;
    }

    public int update(ContentItem item) {
        item.setUpdatedAt(System.currentTimeMillis());
        ContentValues cv = toContentValues(item);
        return getWritableDatabase().update(TABLE_ITEMS, cv, "id=?", new String[]{String.valueOf(item.getId())});
    }

    public int updateStatus(long itemId, String status) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        cv.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().update(TABLE_ITEMS, cv, "id=?", new String[]{String.valueOf(itemId)});
    }

    public int delete(long id) {
        getWritableDatabase().delete(TABLE_JOBS, "content_item_id=?", new String[]{String.valueOf(id)});
        getWritableDatabase().delete(TABLE_EVENTS, "content_item_id=?", new String[]{String.valueOf(id)});
        deleteArtifactsForItem(id);
        deleteTranscriptForItem(id);
        deleteOcrRecordForItem(id);
        return getWritableDatabase().delete(TABLE_ITEMS, "id=?", new String[]{String.valueOf(id)});
    }

    public ContentItem getById(long id) {
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public ContentItem findByCanonicalUrl(String canonicalUrl) {
        if (canonicalUrl == null || canonicalUrl.trim().isEmpty()) {
            return null;
        }
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, "canonical_url=?", new String[]{canonicalUrl.trim()}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public boolean existsByCanonicalUrl(String canonicalUrl) {
        return findByCanonicalUrl(canonicalUrl) != null;
    }

    public List<ContentItem> getAll() {
        return search("", "All", "All");
    }

    public List<ContentItem> getAllItems() {
        return getAll();
    }

    public List<ContentItem> getRecent(int limit) {
        List<ContentItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, null, null, null, null, "saved_at DESC", String.valueOf(limit));
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<ContentItem> getInboxItems() {
        return search("", "All", "Inbox");
    }

    public int getInboxCount() {
        return getCountByCollection("Inbox");
    }

    public int getCountByCollection(String collection) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_ITEMS + " WHERE collection_name = ?", new String[]{collection});
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public List<ContentItem> getItemsByCollection(String collection) {
        return search("", "All", collection);
    }

    public List<ContentItem> getFavorites() {
        List<ContentItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, "is_favorite=1", null, null, null, "saved_at DESC");
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<ContentItem> getFavoriteItems() {
        return getFavorites();
    }

    public int getFavoritesCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_ITEMS + " WHERE is_favorite=1", null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public int getFavoriteCount() {
        return getFavoritesCount();
    }

    public List<ContentItem> getItemsByPlatform(String platform) {
        return search("", platform, "All");
    }

    public List<ContentItem> search(String query) {
        return search(query, "All", "All");
    }

    public List<ContentItem> search(String query, String platformFilter, String collectionFilter) {
        List<ContentItem> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_ITEMS + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim() + "%";
            sql.append(" AND (title LIKE ? OR original_title LIKE ? OR caption LIKE ? OR notes LIKE ? OR tags LIKE ? OR url LIKE ? OR platform LIKE ? OR collection_name LIKE ? OR id IN (SELECT content_item_id FROM " + TABLE_TRANSCRIPTS + " WHERE full_text LIKE ?) OR id IN (SELECT content_item_id FROM " + TABLE_OCR_RECORDS + " WHERE full_text LIKE ?))");
            for (int i = 0; i < 10; i++) {
                args.add(q);
            }
        }

        if (platformFilter != null && !platformFilter.isEmpty() && !"All".equalsIgnoreCase(platformFilter)) {
            sql.append(" AND platform = ?");
            args.add(platformFilter);
        }

        if (collectionFilter != null && !collectionFilter.isEmpty() && !"All".equalsIgnoreCase(collectionFilter)) {
            sql.append(" AND collection_name = ?");
            args.add(collectionFilter);
        }

        sql.append(" ORDER BY saved_at DESC");

        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public int moveToCollection(long id, String newCollection) {
        ContentValues cv = new ContentValues();
        cv.put("collection_name", (newCollection == null || newCollection.trim().isEmpty()) ? "Inbox" : newCollection.trim());
        cv.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().update(TABLE_ITEMS, cv, "id=?", new String[]{String.valueOf(id)});
    }

    public List<String> getCollections() {
        List<String> list = new ArrayList<>();
        list.add("Inbox");
        String sql = "SELECT name FROM " + TABLE_COLLECTIONS + " " +
                "UNION SELECT DISTINCT collection_name AS name FROM " + TABLE_ITEMS + " WHERE collection_name IS NOT NULL AND collection_name != '' " +
                "ORDER BY name ASC";
        Cursor c = getReadableDatabase().rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                String name = c.getString(0);
                if (name != null && !name.trim().isEmpty() && !list.contains(name.trim())) {
                    list.add(name.trim());
                }
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<CollectionStat> getCollectionsWithCounts() {
        List<CollectionStat> result = new ArrayList<>();
        int inboxCount = getInboxCount();
        result.add(new CollectionStat("Inbox", inboxCount));

        String sql = "SELECT c.name, COUNT(i.id) AS cnt " +
                "FROM (SELECT name FROM " + TABLE_COLLECTIONS + " " +
                "      UNION SELECT DISTINCT collection_name AS name FROM " + TABLE_ITEMS + " WHERE collection_name IS NOT NULL AND collection_name != '') c " +
                "LEFT JOIN " + TABLE_ITEMS + " i ON c.name = i.collection_name " +
                "WHERE c.name != 'Inbox' " +
                "GROUP BY c.name ORDER BY c.name ASC";

        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        try {
            while (cursor.moveToNext()) {
                String colName = cursor.getString(0);
                int count = cursor.getInt(1);
                if (colName != null && !colName.trim().isEmpty() && !colName.equalsIgnoreCase("Inbox")) {
                    result.add(new CollectionStat(colName.trim(), count));
                }
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public List<CollectionStat> getCollectionStats() {
        return getCollectionsWithCounts();
    }

    public boolean createCollection(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("created_at", System.currentTimeMillis());
        long res = getWritableDatabase().insertWithOnConflict(TABLE_COLLECTIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        return res != -1;
    }

    public boolean addCollection(String name) {
        return createCollection(name);
    }

    public boolean renameCollection(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) return false;
        String cleanNew = newName.trim();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("name", cleanNew);
            int updated = db.update(TABLE_COLLECTIONS, cv, "name=?", new String[]{oldName});
            if (updated > 0) {
                ContentValues cvItems = new ContentValues();
                cvItems.put("collection_name", cleanNew);
                db.update(TABLE_ITEMS, cvItems, "collection_name=?", new String[]{oldName});
                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteCollection(String name, boolean moveItemsToInbox) {
        if (name == null || name.trim().equalsIgnoreCase("Inbox")) return false;
        String trimmed = name.trim();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_COLLECTIONS, "name=?", new String[]{trimmed});
            if (moveItemsToInbox) {
                ContentValues cv = new ContentValues();
                cv.put("collection_name", "Inbox");
                cv.put("updated_at", System.currentTimeMillis());
                db.update(TABLE_ITEMS, cv, "collection_name=?", new String[]{trimmed});
            } else {
                db.delete(TABLE_ITEMS, "collection_name=?", new String[]{trimmed});
            }
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteCollection(String name) {
        return deleteCollection(name, true);
    }

    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT tags FROM " + TABLE_ITEMS + " WHERE tags IS NOT NULL AND tags != ''", null);
        try {
            while (c.moveToNext()) {
                String raw = c.getString(0);
                if (raw != null) {
                    String[] parts = raw.split("[,\\s]+");
                    for (String p : parts) {
                        String clean = p.trim().replaceAll("^#+", "");
                        if (!clean.isEmpty()) {
                            String formatted = "#" + clean;
                            if (!tags.contains(formatted)) {
                                tags.add(formatted);
                            }
                        }
                    }
                }
            }
        } finally {
            c.close();
        }
        return tags;
    }

    public List<ContentItem> getByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) return new ArrayList<>();
        String clean = tag.replace("#", "").trim();
        return search(clean, "All", "All");
    }

    public List<ContentItem> getItemsByTag(String tag) {
        return getByTag(tag);
    }

    public int getTotalCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_ITEMS, null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    // ==========================================
    // Phase 04: Processing Jobs & Events API
    // ==========================================

    public long createJob(ProcessingJob job) {
        SQLiteDatabase db = getWritableDatabase();
        if (job.getIdempotencyKey() != null && !job.getIdempotencyKey().trim().isEmpty()) {
            Cursor c = db.query(TABLE_JOBS, null,
                    "idempotency_key=? AND state NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')",
                    new String[]{job.getIdempotencyKey().trim()}, null, null, null);
            try {
                if (c.moveToFirst()) {
                    return c.getLong(c.getColumnIndexOrThrow("id"));
                }
            } finally {
                c.close();
            }
        }

        ContentValues cv = toJobContentValues(job);
        long id = db.insert(TABLE_JOBS, null, cv);
        job.setId(id);

        recordEvent(new ProcessingEvent(id, job.getContentItemId(), ProcessingEvent.EVENT_ENQUEUED,
                "ENQUEUE", ProcessingJob.STATE_QUEUED, "Job enqueued for execution"));

        updateStatus(job.getContentItemId(), ProcessingJob.STATE_QUEUED);
        return id;
    }

    public ProcessingJob claimNextJob(String workerId, long leaseDurationMs) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        String selection = "scheduled_at <= ? AND (state IN ('" + ProcessingJob.STATE_QUEUED + "', '" + ProcessingJob.STATE_RETRYING + "') "
                + "OR (state = '" + ProcessingJob.STATE_CLAIMED + "' AND lease_expires_at < ?))";
        String[] args = new String[]{String.valueOf(now), String.valueOf(now)};

        db.beginTransaction();
        try {
            Cursor c = db.query(TABLE_JOBS, null, selection, args, null, null, "priority DESC, scheduled_at ASC, id ASC", "1");
            ProcessingJob job = null;
            try {
                if (c.moveToFirst()) {
                    job = fromJobCursor(c);
                }
            } finally {
                c.close();
            }

            if (job != null) {
                job.setState(ProcessingJob.STATE_CLAIMED);
                job.setWorkerId(workerId);
                job.setAttemptCount(job.getAttemptCount() + 1);
                job.setLeaseExpiresAt(now + leaseDurationMs);
                if (job.getStartedAt() == 0L) {
                    job.setStartedAt(now);
                }
                job.setUpdatedAt(now);

                ContentValues cv = new ContentValues();
                cv.put("state", job.getState());
                cv.put("worker_id", job.getWorkerId());
                cv.put("attempt_count", job.getAttemptCount());
                cv.put("lease_expires_at", job.getLeaseExpiresAt());
                cv.put("started_at", job.getStartedAt());
                cv.put("updated_at", job.getUpdatedAt());
                db.update(TABLE_JOBS, cv, "id=?", new String[]{String.valueOf(job.getId())});

                recordEvent(new ProcessingEvent(job.getId(), job.getContentItemId(), ProcessingEvent.EVENT_CLAIMED,
                        "LEASE", ProcessingJob.STATE_CLAIMED, "Job claimed by worker: " + workerId));

                updateStatus(job.getContentItemId(), "PROCESSING");
                db.setTransactionSuccessful();
                return job;
            }
            db.setTransactionSuccessful();
            return null;
        } finally {
            db.endTransaction();
        }
    }

    public boolean renewLease(long jobId, String workerId, long extensionMs) {
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put("lease_expires_at", now + extensionMs);
        cv.put("updated_at", now);
        int rows = getWritableDatabase().update(TABLE_JOBS, cv, "id=? AND worker_id=?",
                new String[]{String.valueOf(jobId), workerId});
        return rows > 0;
    }

    public void updateJobProgress(long jobId, int progressPercent, String stageMessage) {
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put("state", ProcessingJob.STATE_RUNNING);
        cv.put("progress_percent", progressPercent);
        cv.put("stage_message", stageMessage);
        cv.put("updated_at", now);
        getWritableDatabase().update(TABLE_JOBS, cv, "id=?", new String[]{String.valueOf(jobId)});

        ProcessingJob job = getJobById(jobId);
        if (job != null) {
            recordEvent(new ProcessingEvent(jobId, job.getContentItemId(), ProcessingEvent.EVENT_PROGRESS,
                    stageMessage, ProcessingJob.STATE_RUNNING, "Progress " + progressPercent + "%: " + stageMessage));
        }
    }

    public void completeJob(long jobId) {
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put("state", ProcessingJob.STATE_COMPLETED);
        cv.put("completed_at", now);
        cv.put("progress_percent", 100);
        cv.put("stage_message", "Completed");
        cv.put("updated_at", now);
        getWritableDatabase().update(TABLE_JOBS, cv, "id=?", new String[]{String.valueOf(jobId)});

        ProcessingJob job = getJobById(jobId);
        if (job != null) {
            recordEvent(new ProcessingEvent(jobId, job.getContentItemId(), ProcessingEvent.EVENT_COMPLETED,
                    "DONE", ProcessingJob.STATE_COMPLETED, "Processing completed successfully"));
            updateStatus(job.getContentItemId(), "READY");
        }
    }

    public void failJob(long jobId, String errorCode, String sanitizedMessage, long retryDelayMs) {
        long now = System.currentTimeMillis();
        ProcessingJob job = getJobById(jobId);
        if (job == null) return;

        ContentValues cv = new ContentValues();
        cv.put("error_code", errorCode);
        cv.put("sanitized_error_message", sanitizedMessage);
        cv.put("updated_at", now);

        if (job.getAttemptCount() < job.getMaxAttempts()) {
            cv.put("state", ProcessingJob.STATE_RETRYING);
            cv.put("scheduled_at", now + retryDelayMs);
            cv.put("stage_message", "Retry scheduled (Attempt " + job.getAttemptCount() + "/" + job.getMaxAttempts() + ")");
            getWritableDatabase().update(TABLE_JOBS, cv, "id=?", new String[]{String.valueOf(jobId)});

            recordEvent(new ProcessingEvent(jobId, job.getContentItemId(), ProcessingEvent.EVENT_RETRY,
                    "RETRY", ProcessingJob.STATE_RETRYING, "Attempt " + job.getAttemptCount() + " failed: " + sanitizedMessage + ". Next retry in " + (retryDelayMs / 1000) + "s"));
            updateStatus(job.getContentItemId(), ProcessingJob.STATE_RETRYING);
        } else {
            cv.put("state", ProcessingJob.STATE_FAILED);
            cv.put("failed_at", now);
            cv.put("stage_message", sanitizedMessage);
            getWritableDatabase().update(TABLE_JOBS, cv, "id=?", new String[]{String.valueOf(jobId)});

            recordEvent(new ProcessingEvent(jobId, job.getContentItemId(), ProcessingEvent.EVENT_FAILED,
                    "FAILED", ProcessingJob.STATE_FAILED, "Job failed after " + job.getAttemptCount() + " attempts: " + sanitizedMessage));
            updateStatus(job.getContentItemId(), ProcessingJob.STATE_FAILED);
        }
    }

    public void cancelJob(long jobId) {
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put("state", ProcessingJob.STATE_CANCELLED);
        cv.put("stage_message", "Cancelled by user");
        cv.put("updated_at", now);
        getWritableDatabase().update(TABLE_JOBS, cv, "id=?", new String[]{String.valueOf(jobId)});

        ProcessingJob job = getJobById(jobId);
        if (job != null) {
            recordEvent(new ProcessingEvent(jobId, job.getContentItemId(), ProcessingEvent.EVENT_CANCELLED,
                    "CANCEL", ProcessingJob.STATE_CANCELLED, "Processing cancelled by user"));
            updateStatus(job.getContentItemId(), ProcessingJob.STATE_CANCELLED);
        }
    }

    public int recoverStaleJobs() {
        long now = System.currentTimeMillis();
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_JOBS, null, "state=? AND lease_expires_at < ?",
                new String[]{ProcessingJob.STATE_CLAIMED, String.valueOf(now)}, null, null, null);
        int recovered = 0;
        try {
            while (c.moveToNext()) {
                long jobId = c.getLong(c.getColumnIndexOrThrow("id"));
                long contentId = c.getLong(c.getColumnIndexOrThrow("content_item_id"));
                ContentValues cv = new ContentValues();
                cv.put("state", ProcessingJob.STATE_QUEUED);
                cv.put("worker_id", "");
                cv.put("updated_at", now);
                db.update(TABLE_JOBS, cv, "id=?", new String[]{String.valueOf(jobId)});

                recordEvent(new ProcessingEvent(jobId, contentId, ProcessingEvent.EVENT_STALE_RECOVERED,
                        "RECOVER", ProcessingJob.STATE_QUEUED, "Stale worker lease expired; reset to QUEUED"));
                recovered++;
            }
        } finally {
            c.close();
        }
        return recovered;
    }

    public ProcessingJob getJobById(long jobId) {
        Cursor c = getReadableDatabase().query(TABLE_JOBS, null, "id=?", new String[]{String.valueOf(jobId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return fromJobCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public ProcessingJob getActiveJobForContentItem(long contentItemId) {
        Cursor c = getReadableDatabase().query(TABLE_JOBS, null,
                "content_item_id=? AND state NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')",
                new String[]{String.valueOf(contentItemId)}, null, null, "id DESC", "1");
        try {
            if (c.moveToFirst()) {
                return fromJobCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public ProcessingJob getLatestJobForContentItem(long contentItemId) {
        Cursor c = getReadableDatabase().query(TABLE_JOBS, null,
                "content_item_id=?", new String[]{String.valueOf(contentItemId)}, null, null, "id DESC", "1");
        try {
            if (c.moveToFirst()) {
                return fromJobCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public long recordEvent(ProcessingEvent event) {
        ContentValues cv = toEventContentValues(event);
        long id = getWritableDatabase().insert(TABLE_EVENTS, null, cv);
        event.setId(id);
        return id;
    }

    public List<ProcessingEvent> getEventsForContentItem(long contentItemId) {
        List<ProcessingEvent> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_EVENTS, null, "content_item_id=?",
                new String[]{String.valueOf(contentItemId)}, null, null, "created_at ASC, id ASC");
        try {
            while (c.moveToNext()) {
                list.add(fromEventCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<ProcessingEvent> getEventsForJob(long jobId) {
        List<ProcessingEvent> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_EVENTS, null, "job_id=?",
                new String[]{String.valueOf(jobId)}, null, null, "created_at ASC, id ASC");
        try {
            while (c.moveToNext()) {
                list.add(fromEventCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    private ContentValues toJobContentValues(ProcessingJob job) {
        ContentValues cv = new ContentValues();
        cv.put("content_item_id", job.getContentItemId());
        cv.put("job_type", job.getJobType());
        cv.put("state", job.getState());
        cv.put("priority", job.getPriority());
        cv.put("attempt_count", job.getAttemptCount());
        cv.put("max_attempts", job.getMaxAttempts());
        cv.put("progress_percent", job.getProgressPercent());
        cv.put("stage_message", job.getStageMessage());
        cv.put("worker_id", job.getWorkerId());
        cv.put("lease_expires_at", job.getLeaseExpiresAt());
        cv.put("scheduled_at", job.getScheduledAt());
        cv.put("started_at", job.getStartedAt());
        cv.put("completed_at", job.getCompletedAt());
        cv.put("failed_at", job.getFailedAt());
        cv.put("error_code", job.getErrorCode());
        cv.put("sanitized_error_message", job.getSanitizedErrorMessage());
        cv.put("idempotency_key", job.getIdempotencyKey());
        cv.put("created_at", job.getCreatedAt());
        cv.put("updated_at", job.getUpdatedAt());
        return cv;
    }

    private ProcessingJob fromJobCursor(Cursor c) {
        ProcessingJob job = new ProcessingJob();
        job.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        job.setContentItemId(c.getLong(c.getColumnIndexOrThrow("content_item_id")));
        job.setJobType(c.getString(c.getColumnIndexOrThrow("job_type")));
        job.setState(c.getString(c.getColumnIndexOrThrow("state")));
        job.setPriority(c.getInt(c.getColumnIndexOrThrow("priority")));
        job.setAttemptCount(c.getInt(c.getColumnIndexOrThrow("attempt_count")));
        job.setMaxAttempts(c.getInt(c.getColumnIndexOrThrow("max_attempts")));
        job.setProgressPercent(c.getInt(c.getColumnIndexOrThrow("progress_percent")));
        job.setStageMessage(c.getString(c.getColumnIndexOrThrow("stage_message")));
        job.setWorkerId(c.getString(c.getColumnIndexOrThrow("worker_id")));
        job.setLeaseExpiresAt(c.getLong(c.getColumnIndexOrThrow("lease_expires_at")));
        job.setScheduledAt(c.getLong(c.getColumnIndexOrThrow("scheduled_at")));
        job.setStartedAt(c.getLong(c.getColumnIndexOrThrow("started_at")));
        job.setCompletedAt(c.getLong(c.getColumnIndexOrThrow("completed_at")));
        job.setFailedAt(c.getLong(c.getColumnIndexOrThrow("failed_at")));
        job.setErrorCode(c.getString(c.getColumnIndexOrThrow("error_code")));
        job.setSanitizedErrorMessage(c.getString(c.getColumnIndexOrThrow("sanitized_error_message")));
        job.setIdempotencyKey(c.getString(c.getColumnIndexOrThrow("idempotency_key")));
        job.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        job.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow("updated_at")));
        return job;
    }

    private ContentValues toEventContentValues(ProcessingEvent event) {
        ContentValues cv = new ContentValues();
        cv.put("job_id", event.getJobId());
        cv.put("content_item_id", event.getContentItemId());
        cv.put("event_type", event.getEventType());
        cv.put("stage", event.getStage());
        cv.put("status", event.getStatus());
        cv.put("message", event.getMessage());
        cv.put("metadata_json", event.getMetadataJson());
        cv.put("created_at", event.getCreatedAt());
        return cv;
    }

    private ProcessingEvent fromEventCursor(Cursor c) {
        ProcessingEvent event = new ProcessingEvent();
        event.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        event.setJobId(c.getLong(c.getColumnIndexOrThrow("job_id")));
        event.setContentItemId(c.getLong(c.getColumnIndexOrThrow("content_item_id")));
        event.setEventType(c.getString(c.getColumnIndexOrThrow("event_type")));
        event.setStage(c.getString(c.getColumnIndexOrThrow("stage")));
        event.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
        event.setMessage(c.getString(c.getColumnIndexOrThrow("message")));
        event.setMetadataJson(c.getString(c.getColumnIndexOrThrow("metadata_json")));
        event.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return event;
    }

    public String exportToJson() {
        return exportJson();
    }

    public String exportJson() {
        try {
            List<ContentItem> items = getAllItems();
            JSONArray arr = new JSONArray();
            for (ContentItem i : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", i.getId());
                obj.put("url", i.getUrl());
                obj.put("canonical_url", i.getCanonicalUrl());
                obj.put("platform", i.getPlatform());
                obj.put("title", i.getTitle());
                obj.put("original_title", i.getOriginalTitle());
                obj.put("caption", i.getCaption());
                obj.put("thumbnail_url", i.getThumbnailUrl());
                obj.put("notes", i.getNotes());
                obj.put("collection_name", i.getCollectionName());
                obj.put("tags", i.getTags());
                obj.put("is_favorite", i.isFavorite());
                obj.put("status", i.getStatus());
                obj.put("saved_at", i.getSavedAt());
                obj.put("updated_at", i.getUpdatedAt());
                arr.put(obj);
            }
            JSONObject root = new JSONObject();
            root.put("app", "Hifadhio");
            root.put("version", "0.4.0-phase4");
            root.put("exported_at", System.currentTimeMillis());
            root.put("total_items", items.size());
            root.put("items", arr);
            return root.toString(2);
        } catch (Exception e) {
            return "{\"error\":\"Export failed: " + e.getMessage() + "\"}";
        }
    }

    private ContentValues toContentValues(ContentItem item) {
        ContentValues cv = new ContentValues();
        cv.put("url", item.getUrl());
        cv.put("canonical_url", item.getCanonicalUrl() != null && !item.getCanonicalUrl().isEmpty() ? item.getCanonicalUrl() : UrlNormalizer.normalize(item.getUrl()));
        cv.put("platform", item.getPlatform());
        cv.put("title", item.getTitle());
        cv.put("original_title", item.getOriginalTitle());
        cv.put("caption", item.getCaption());
        cv.put("thumbnail_url", item.getThumbnailUrl());
        cv.put("notes", item.getNotes());
        cv.put("collection_name", item.getCollectionName());
        cv.put("tags", item.getTags());
        cv.put("is_favorite", item.isFavorite() ? 1 : 0);
        cv.put("status", item.getStatus());
        cv.put("saved_at", item.getSavedAt());
        cv.put("updated_at", item.getUpdatedAt());
        return cv;
    }

    private ContentItem fromCursor(Cursor c) {
        ContentItem item = new ContentItem();
        item.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        item.setUrl(c.getString(c.getColumnIndexOrThrow("url")));
        item.setCanonicalUrl(c.getString(c.getColumnIndexOrThrow("canonical_url")));
        item.setPlatform(c.getString(c.getColumnIndexOrThrow("platform")));
        item.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        int origIdx = c.getColumnIndex("original_title");
        if (origIdx >= 0) {
            item.setOriginalTitle(c.getString(origIdx));
        }
        item.setCaption(c.getString(c.getColumnIndexOrThrow("caption")));
        item.setThumbnailUrl(c.getString(c.getColumnIndexOrThrow("thumbnail_url")));
        item.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
        item.setCollectionName(c.getString(c.getColumnIndexOrThrow("collection_name")));
        item.setTags(c.getString(c.getColumnIndexOrThrow("tags")));
        item.setFavorite(c.getInt(c.getColumnIndexOrThrow("is_favorite")) == 1);
        item.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
        item.setSavedAt(c.getLong(c.getColumnIndexOrThrow("saved_at")));
        item.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow("updated_at")));
        return item;
    }

    // --- Artifacts Table Operations (Master Spec Phase 07 / Section 13.1) ---

    public long insertArtifact(MediaArtifact artifact) {
        if (artifact == null) return -1;
        ContentValues cv = new ContentValues();
        cv.put("content_item_id", artifact.getContentItemId());
        cv.put("artifact_type", artifact.getArtifactType());
        cv.put("storage_path", artifact.getStoragePath());
        cv.put("content_uri", artifact.getContentUri());
        cv.put("mime_type", artifact.getMimeType());
        cv.put("file_size_bytes", artifact.getFileSizeBytes());
        cv.put("sha256", artifact.getSha256());
        cv.put("retention_policy", artifact.getRetentionPolicy());
        cv.put("expires_at", artifact.getExpiresAt());
        cv.put("created_at", artifact.getCreatedAt());
        long id = getWritableDatabase().insert(TABLE_ARTIFACTS, null, cv);
        artifact.setId(id);
        return id;
    }

    public List<MediaArtifact> getArtifactsForItem(long itemId) {
        List<MediaArtifact> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ARTIFACTS, null, "content_item_id=?",
                new String[]{String.valueOf(itemId)}, null, null, "created_at ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(artifactFromCursor(c));
            }
            c.close();
        }
        return list;
    }

    public MediaArtifact getArtifact(long itemId, String type) {
        MediaArtifact artifact = null;
        Cursor c = getReadableDatabase().query(TABLE_ARTIFACTS, null, "content_item_id=? AND artifact_type=?",
                new String[]{String.valueOf(itemId), type}, null, null, "created_at DESC", "1");
        if (c != null) {
            if (c.moveToFirst()) {
                artifact = artifactFromCursor(c);
            }
            c.close();
        }
        return artifact;
    }

    public List<MediaArtifact> getExpiredArtifacts(long now) {
        List<MediaArtifact> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ARTIFACTS, null, "expires_at > 0 AND expires_at <= ?",
                new String[]{String.valueOf(now)}, null, null, "expires_at ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(artifactFromCursor(c));
            }
            c.close();
        }
        return list;
    }

    public int deleteArtifact(long id) {
        return getWritableDatabase().delete(TABLE_ARTIFACTS, "id=?", new String[]{String.valueOf(id)});
    }

    public int deleteArtifactsForItem(long itemId) {
        List<MediaArtifact> list = getArtifactsForItem(itemId);
        for (MediaArtifact art : list) {
            if (art.getStoragePath() != null) {
                try {
                    java.io.File f = new java.io.File(art.getStoragePath());
                    if (f.exists()) {
                        f.delete();
                    }
                } catch (Exception ignored) {}
            }
        }
        return getWritableDatabase().delete(TABLE_ARTIFACTS, "content_item_id=?", new String[]{String.valueOf(itemId)});
    }

    public List<String> getAllArtifactStoragePaths() {
        List<String> paths = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ARTIFACTS, new String[]{"storage_path"}, null, null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                paths.add(c.getString(0));
            }
            c.close();
        }
        return paths;
    }

    public long getTotalArtifactsSize() {
        long total = 0;
        Cursor c = getReadableDatabase().rawQuery("SELECT SUM(file_size_bytes) FROM " + TABLE_ARTIFACTS, null);
        if (c != null) {
            if (c.moveToFirst() && !c.isNull(0)) {
                total = c.getLong(0);
            }
            c.close();
        }
        return total;
    }

    private MediaArtifact artifactFromCursor(Cursor c) {
        MediaArtifact a = new MediaArtifact();
        a.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        a.setContentItemId(c.getLong(c.getColumnIndexOrThrow("content_item_id")));
        a.setArtifactType(c.getString(c.getColumnIndexOrThrow("artifact_type")));
        a.setStoragePath(c.getString(c.getColumnIndexOrThrow("storage_path")));
        int uriIdx = c.getColumnIndex("content_uri");
        if (uriIdx >= 0) {
            a.setContentUri(c.getString(uriIdx));
        }
        int mimeIdx = c.getColumnIndex("mime_type");
        if (mimeIdx >= 0) {
            a.setMimeType(c.getString(mimeIdx));
        }
        a.setFileSizeBytes(c.getLong(c.getColumnIndexOrThrow("file_size_bytes")));
        int shaIdx = c.getColumnIndex("sha256");
        if (shaIdx >= 0) {
            a.setSha256(c.getString(shaIdx));
        }
        a.setRetentionPolicy(c.getString(c.getColumnIndexOrThrow("retention_policy")));
        a.setExpiresAt(c.getLong(c.getColumnIndexOrThrow("expires_at")));
        a.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return a;
    }

    // ==========================================
    // Phase 08: Transcripts Storage API
    // ==========================================

    public long saveTranscript(Transcript transcript) {
        if (transcript == null || transcript.getContentItemId() <= 0) {
            return -1;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("content_item_id", transcript.getContentItemId());
        cv.put("full_text", transcript.getFullText());
        cv.put("language", transcript.getLanguage());
        cv.put("provider_id", transcript.getProviderId());
        cv.put("model", transcript.getModel());
        cv.put("duration_ms", transcript.getDurationMs());
        cv.put("segments_json", transcript.toSegmentsJson());
        cv.put("confidence", transcript.getConfidence());
        cv.put("cost_usd", transcript.getCostUsd());
        long now = System.currentTimeMillis();
        cv.put("created_at", transcript.getCreatedAt() > 0 ? transcript.getCreatedAt() : now);
        cv.put("updated_at", now);

        long id = db.insertWithOnConflict(TABLE_TRANSCRIPTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (id > 0) {
            transcript.setId(id);
        }
        return id;
    }

    public Transcript getTranscriptForItem(long contentItemId) {
        Cursor c = getReadableDatabase().query(TABLE_TRANSCRIPTS, null, "content_item_id=?", new String[]{String.valueOf(contentItemId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return transcriptFromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public boolean hasTranscript(long contentItemId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM " + TABLE_TRANSCRIPTS + " WHERE content_item_id=? LIMIT 1", new String[]{String.valueOf(contentItemId)});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public int deleteTranscriptForItem(long contentItemId) {
        return getWritableDatabase().delete(TABLE_TRANSCRIPTS, "content_item_id=?", new String[]{String.valueOf(contentItemId)});
    }

    private Transcript transcriptFromCursor(Cursor c) {
        Transcript t = new Transcript();
        t.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        t.setContentItemId(c.getLong(c.getColumnIndexOrThrow("content_item_id")));
        t.setFullText(c.getString(c.getColumnIndexOrThrow("full_text")));
        int langIdx = c.getColumnIndex("language");
        if (langIdx >= 0) t.setLanguage(c.getString(langIdx));
        t.setProviderId(c.getString(c.getColumnIndexOrThrow("provider_id")));
        int modelIdx = c.getColumnIndex("model");
        if (modelIdx >= 0) t.setModel(c.getString(modelIdx));
        t.setDurationMs(c.getLong(c.getColumnIndexOrThrow("duration_ms")));
        int segIdx = c.getColumnIndex("segments_json");
        if (segIdx >= 0) {
            t.setSegments(Transcript.parseSegmentsJson(c.getString(segIdx)));
        }
        int confIdx = c.getColumnIndex("confidence");
        if (confIdx >= 0) t.setConfidence(c.getDouble(confIdx));
        int costIdx = c.getColumnIndex("cost_usd");
        if (costIdx >= 0) t.setCostUsd(c.getDouble(costIdx));
        t.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        int updIdx = c.getColumnIndex("updated_at");
        if (updIdx >= 0) t.setUpdatedAt(c.getLong(updIdx));
        return t;
    }

    // ==========================================
    // Phase 09: OCR Records Storage API
    // ==========================================

    public long saveOcrRecord(OcrRecord record) {
        if (record == null || record.getContentItemId() <= 0) {
            return -1;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("content_item_id", record.getContentItemId());
        cv.put("full_text", record.getFullText());
        cv.put("provider_id", record.getProviderId());
        cv.put("model", record.getModel());
        cv.put("frames_count", record.getFramesCount());
        cv.put("frames_json", record.toFramesJson());
        cv.put("cost_usd", record.getCostUsd());
        long now = System.currentTimeMillis();
        cv.put("created_at", record.getCreatedAt() > 0 ? record.getCreatedAt() : now);
        cv.put("updated_at", now);

        long id = db.insertWithOnConflict(TABLE_OCR_RECORDS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (id > 0) {
            record.setId(id);
        }
        return id;
    }

    public OcrRecord getOcrRecordForItem(long contentItemId) {
        Cursor c = getReadableDatabase().query(TABLE_OCR_RECORDS, null, "content_item_id=?", new String[]{String.valueOf(contentItemId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return ocrRecordFromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public boolean hasOcrRecord(long contentItemId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM " + TABLE_OCR_RECORDS + " WHERE content_item_id=? LIMIT 1", new String[]{String.valueOf(contentItemId)});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public int deleteOcrRecordForItem(long contentItemId) {
        return getWritableDatabase().delete(TABLE_OCR_RECORDS, "content_item_id=?", new String[]{String.valueOf(contentItemId)});
    }

    private OcrRecord ocrRecordFromCursor(Cursor c) {
        OcrRecord r = new OcrRecord();
        r.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        r.setContentItemId(c.getLong(c.getColumnIndexOrThrow("content_item_id")));
        r.setFullText(c.getString(c.getColumnIndexOrThrow("full_text")));
        r.setProviderId(c.getString(c.getColumnIndexOrThrow("provider_id")));
        int modelIdx = c.getColumnIndex("model");
        if (modelIdx >= 0) r.setModel(c.getString(modelIdx));
        int countIdx = c.getColumnIndex("frames_count");
        if (countIdx >= 0) r.setFramesCount(c.getInt(countIdx));
        int jsonIdx = c.getColumnIndex("frames_json");
        if (jsonIdx >= 0) {
            r.setFrames(OcrRecord.parseFramesJson(c.getString(jsonIdx)));
        }
        int costIdx = c.getColumnIndex("cost_usd");
        if (costIdx >= 0) r.setCostUsd(c.getDouble(costIdx));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        int updIdx = c.getColumnIndex("updated_at");
        if (updIdx >= 0) r.setUpdatedAt(c.getLong(updIdx));
        return r;
    }
}

