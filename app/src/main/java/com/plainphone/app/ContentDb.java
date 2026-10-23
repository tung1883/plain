package com.plainphone.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The content index: one row per indexed file, its passages (raw text for snippets) and a full-text
 * index over the same passages with accents folded. Lives in the app's private storage.
 */
final class ContentDb extends SQLiteOpenHelper {

    private static final String NAME = "content_index.db";
    private static ContentDb instance;
    private final Context context;

    static synchronized ContentDb get(Context context) {
        if (instance == null) instance = new ContentDb(context.getApplicationContext());
        return instance;
    }

    private ContentDb(Context context) {
        super(context, NAME, null, 1);
        this.context = context;
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE docs(id INTEGER PRIMARY KEY, path TEXT UNIQUE NOT NULL, size INTEGER, mtime INTEGER,"
                + " ext TEXT, title TEXT, status TEXT NOT NULL, error TEXT, indexed_at INTEGER)");
        db.execSQL("CREATE TABLE chunk_text(id INTEGER PRIMARY KEY, doc INTEGER NOT NULL, locator TEXT, body TEXT)");
        db.execSQL("CREATE INDEX chunk_text_doc ON chunk_text(doc)");
        db.execSQL("CREATE VIRTUAL TABLE chunks USING fts4(folded, tokenize=unicode61)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS chunks");
        db.execSQL("DROP TABLE IF EXISTS chunk_text");
        db.execSQL("DROP TABLE IF EXISTS docs");
        onCreate(db);
    }

    // ------------------------------------------------------------------- reading

    static final class Doc {
        final long id, size, mtime, indexedAt;
        final String status;

        Doc(long id, long size, long mtime, String status, long indexedAt) {
            this.id = id;
            this.size = size;
            this.mtime = mtime;
            this.status = status;
            this.indexedAt = indexedAt;
        }
    }

    /** Every indexed file by path. */
    Map<String, Doc> allDocs() {
        Map<String, Doc> out = new HashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id, path, size, mtime, status, indexed_at FROM docs", null)) {
            while (c.moveToNext()) out.put(c.getString(1), new Doc(c.getLong(0), c.getLong(2), c.getLong(3), c.getString(4), c.getLong(5)));
        }
        return out;
    }

    /** One file in the index with how it went. */
    static final class Entry {
        final String path, ext, status, error;
        final long size;

        Entry(String path, String ext, String status, String error, long size) {
            this.path = path;
            this.ext = ext;
            this.status = status;
            this.error = error;
            this.size = size;
        }
    }

    /** Every file in the index, the ones that are not read (anything but "ok") first, then by path. */
    List<Entry> listFiles() {
        List<Entry> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT path, ext, status, error, size FROM docs ORDER BY (status = 'ok'), status, path COLLATE NOCASE", null)) {
            while (c.moveToNext()) out.add(new Entry(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4)));
        }
        return out;
    }

    static final class Stats {
        final Map<String, Integer> byStatus = new LinkedHashMap<>();
        int chunks;
        long bytes;

        int files() {
            int n = 0;
            for (int v : byStatus.values()) n += v;
            return n;
        }

        int count(String status) {
            Integer n = byStatus.get(status);
            return n == null ? 0 : n;
        }
    }

    Stats stats() {
        Stats s = new Stats();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT status, COUNT(*) FROM docs GROUP BY status", null)) {
            while (c.moveToNext()) s.byStatus.put(c.getString(0), c.getInt(1));
        }
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM chunk_text", null)) {
            if (c.moveToFirst()) s.chunks = c.getInt(0);
        }
        s.bytes = sizeBytes();
        return s;
    }

    /** Size of the database on disk, including its write-ahead log. */
    long sizeBytes() {
        File db = context.getDatabasePath(NAME);
        long n = db.length();
        n += new File(db.getPath() + "-wal").length();
        return n;
    }

    /** A passage that matched, with its file. */
    static final class Row {
        final long chunkId, docId, mtime;
        final String locator, body, path, title;

        Row(long chunkId, long docId, String locator, String body, String path, String title, long mtime) {
            this.chunkId = chunkId;
            this.docId = docId;
            this.locator = locator == null ? "" : locator;
            this.body = body == null ? "" : body;
            this.path = path;
            this.title = title;
            this.mtime = mtime;
        }
    }

    /** {chunk id, BM25 score} of the best {@code limit} passages for a full-text query (unordered). */
    List<double[]> match(String fts, int limit) {
        List<double[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT docid, matchinfo(chunks, 'pcnalx') FROM chunks WHERE chunks MATCH ? LIMIT ?",
                new String[]{fts, String.valueOf(limit)})) {
            while (c.moveToNext()) {
                out.add(new double[]{c.getLong(0), Bm25.score(Bm25.parse(c.getBlob(1)))});
            }
        } catch (SQLiteException e) {
            android.util.Log.w("PlainContentIndex", "query failed: " + fts, e);
        }
        return out;
    }

    /** The passages with the given ids, with their files. */
    Map<Long, Row> rows(List<Long> chunkIds) {
        Map<Long, Row> out = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        for (int from = 0; from < chunkIds.size(); from += 200) {
            List<Long> part = chunkIds.subList(from, Math.min(chunkIds.size(), from + 200));
            StringBuilder marks = new StringBuilder();
            String[] args = new String[part.size()];
            for (int i = 0; i < part.size(); i++) {
                marks.append(i == 0 ? "?" : ",?");
                args[i] = String.valueOf(part.get(i));
            }
            try (Cursor c = db.rawQuery("SELECT t.id, t.doc, t.locator, t.body, d.path, d.title, d.mtime"
                    + " FROM chunk_text t JOIN docs d ON d.id = t.doc WHERE t.id IN (" + marks + ")", args)) {
                while (c.moveToNext()) {
                    out.put(c.getLong(0), new Row(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3),
                            c.getString(4), c.getString(5), c.getLong(6)));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------- writing

    /** Forget a file and its passages. */
    void deleteDoc(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            deleteChunks(db, id);
            db.delete("docs", "id = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void deleteChunks(SQLiteDatabase db, long docId) {
        String[] arg = {String.valueOf(docId)};
        db.execSQL("DELETE FROM chunks WHERE docid IN (SELECT id FROM chunk_text WHERE doc = ?)", arg);
        db.delete("chunk_text", "doc = ?", arg);
    }

    /** Store the result for a file: its row (with {@code status}) and its passages, replacing earlier ones. */
    void replace(String path, long size, long mtime, String ext, String title, String status, String error,
                 List<ExtractRunner.Passage> passages) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long id = -1;
            try (Cursor c = db.rawQuery("SELECT id FROM docs WHERE path = ?", new String[]{path})) {
                if (c.moveToFirst()) id = c.getLong(0);
            }
            ContentValues v = new ContentValues();
            v.put("path", path);
            v.put("size", size);
            v.put("mtime", mtime);
            v.put("ext", ext);
            v.put("title", title);
            v.put("status", status);
            v.put("error", error);
            v.put("indexed_at", System.currentTimeMillis());
            if (id >= 0) {
                deleteChunks(db, id);
                db.update("docs", v, "id = ?", new String[]{String.valueOf(id)});
            } else {
                id = db.insertOrThrow("docs", null, v);
            }
            for (ExtractRunner.Passage p : passages) {
                ContentValues t = new ContentValues();
                t.put("doc", id);
                t.put("locator", p.locator);
                t.put("body", p.body);
                long chunkId = db.insertOrThrow("chunk_text", null, t);
                db.execSQL("INSERT INTO chunks(docid, folded) VALUES (?, ?)", new Object[]{chunkId, Fold.forIndex(p.body)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Drop everything (the "Clear index" button). */
    void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM chunks");
            db.execSQL("DELETE FROM chunk_text");
            db.execSQL("DELETE FROM docs");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.execSQL("VACUUM");
    }
}
