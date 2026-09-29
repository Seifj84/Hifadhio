package com.seiftech.hifadhio.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ContentDb extends SQLiteOpenHelper {
    public static final String DB_NAME = "hifadhio.db";
    public static final int DB_VERSION = 3;
    public static final String TABLE_ITEMS = "items";
    public static final String TABLE_COLLECTIONS = "collections";
    public static final String TABLE_JOBS = "processing_jobs";
    public static final String TABLE_EVENTS = "processing_events";

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

    public List<ContentItem> getAllItems() {
        List<ContentItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, null, null, null, null, "saved_at DESC");
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
        List<ContentItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, "collection_name=?", new String[]{"Inbox"}, null, null, "saved_at DESC");
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public int getInboxCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_ITEMS + " WHERE collection_name='Inbox'", null);
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
        List<ContentItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, "collection_name=?", new String[]{collection}, null, null, "saved_at DESC");
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

    public int getFavoriteCount() {
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

    public List<ContentItem> getItemsByPlatform(String platform) {
        List<ContentItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, "platform=?", new String[]{platform}, null, null, "saved_at DESC");
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<ContentItem> search(String query) {
        List<ContentItem> list = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return getAllItems();
        }
        String pattern = "%" + query.trim() + "%";
        String selection = "title LIKE ? OR original_title LIKE ? OR caption LIKE ? OR notes LIKE ? OR tags LIKE ? OR url LIKE ? OR platform LIKE ? OR collection_name LIKE ?";
        String[] args = new String[]{pattern, pattern, pattern, pattern, pattern, pattern, pattern, pattern};
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, selection, args, null, null, "saved_at DESC");
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
        Cursor c = getReadableDatabase().query(TABLE_COLLECTIONS, new String[]{"name"}, null, null, null, null, "name ASC");
        try {
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<CollectionStat> getCollectionStats() {
        List<CollectionStat> stats = new ArrayList<>();
        List<String> collections = getCollections();
        for (String cName : collections) {
            Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_ITEMS + " WHERE collection_name=?", new String[]{cName});
            int count = 0;
            try {
                if (c.moveToFirst()) {
                    count = c.getInt(0);
                }
            } finally {
                c.close();
            }
            stats.add(new CollectionStat(cName, count));
        }
        return stats;
    }

    public boolean addCollection(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("created_at", System.currentTimeMillis());
        long res = getWritableDatabase().insertWithOnConflict(TABLE_COLLECTIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        return res != -1;
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

    public boolean deleteCollection(String name) {
        if (name == null || "Inbox".equalsIgnoreCase(name)) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int deleted = db.delete(TABLE_COLLECTIONS, "name=?", new String[]{name});
            if (deleted > 0) {
                ContentValues cv = new ContentValues();
                cv.put("collection_name", "Inbox");
                db.update(TABLE_ITEMS, cv, "collection_name=?", new String[]{name});
                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, new String[]{"tags"}, "tags IS NOT NULL AND tags != ''", null, null, null, null);
        try {
            while (c.moveToNext()) {
                String raw = c.getString(0);
                if (raw != null) {
                    String[] parts = raw.split("[,\\s]+");
                    for (String p : parts) {
                        String clean = p.trim().replaceAll("^#+", "");
                        if (!clean.isEmpty() && !tags.contains(clean)) {
                            tags.add(clean);
                        }
                    }
                }
            }
        } finally {
            c.close();
        }
        return tags;
    }

    public List<ContentItem> getItemsByTag(String tag) {
        List<ContentItem> list = new ArrayList<>();
        if (tag == null || tag.trim().isEmpty()) return list;
        String cleanTag = tag.trim().replaceAll("^#+", "");
        String pattern = "%" + cleanTag + "%";
        Cursor c = getReadableDatabase().query(TABLE_ITEMS, null, "tags LIKE ?", new String[]{pattern}, null, null, "saved_at DESC");
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
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
}
