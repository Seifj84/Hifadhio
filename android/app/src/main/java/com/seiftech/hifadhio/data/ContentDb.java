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
    public static final int DB_VERSION = 2;
    public static final String TABLE_ITEMS = "items";
    public static final String TABLE_COLLECTIONS = "collections";

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

    public int moveToCollection(long id, String newCollection) {
        ContentValues cv = new ContentValues();
        cv.put("collection_name", newCollection != null && !newCollection.trim().isEmpty() ? newCollection.trim() : "Inbox");
        cv.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().update(TABLE_ITEMS, cv, "id=?", new String[]{String.valueOf(id)});
    }

    public int getInboxCount() {
        return getCountByCollection("Inbox");
    }

    public List<ContentItem> getAll() {
        return search("", "All", "All");
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

    public List<ContentItem> search(String query, String platformFilter, String collectionFilter) {
        List<ContentItem> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_ITEMS + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim() + "%";
            sql.append(" AND (title LIKE ? OR url LIKE ? OR caption LIKE ? OR notes LIKE ? OR tags LIKE ? OR collection_name LIKE ?)");
            for (int i = 0; i < 6; i++) {
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

    public boolean createCollection(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("created_at", System.currentTimeMillis());
        long res = getWritableDatabase().insertWithOnConflict(TABLE_COLLECTIONS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        return res != -1;
    }

    public boolean renameCollection(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) return false;
        String trimmedOld = oldName.trim();
        String trimmedNew = newName.trim();
        if (trimmedOld.equalsIgnoreCase("Inbox")) return false;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cvCol = new ContentValues();
            cvCol.put("name", trimmedNew);
            db.update(TABLE_COLLECTIONS, cvCol, "name=?", new String[]{trimmedOld});

            ContentValues cvItem = new ContentValues();
            cvItem.put("collection_name", trimmedNew);
            cvItem.put("updated_at", System.currentTimeMillis());
            db.update(TABLE_ITEMS, cvItem, "collection_name=?", new String[]{trimmedOld});

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
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

    public int getFavoritesCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_ITEMS + " WHERE is_favorite=1", null);
        try {
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            c.close();
        }
        return 0;
    }

    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT tags FROM " + TABLE_ITEMS + " WHERE tags IS NOT NULL AND tags != ''", null);
        try {
            while (c.moveToNext()) {
                String raw = c.getString(0);
                if (raw != null) {
                    String[] split = raw.split("[,\\s]+");
                    for (String s : split) {
                        String clean = s.trim();
                        if (!clean.isEmpty()) {
                            if (!clean.startsWith("#")) clean = "#" + clean;
                            if (!tags.contains(clean)) {
                                tags.add(clean);
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

    public int getTotalCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_ITEMS, null);
        try {
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            c.close();
        }
        return 0;
    }

    public int getCountByCollection(String collection) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_ITEMS + " WHERE collection_name = ?", new String[]{collection});
        try {
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            c.close();
        }
        return 0;
    }

    public String exportToJson() {
        try {
            List<ContentItem> items = getAll();
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
            root.put("version", "0.3.0-phase3");
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
