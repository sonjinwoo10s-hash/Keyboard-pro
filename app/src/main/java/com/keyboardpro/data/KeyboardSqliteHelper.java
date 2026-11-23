package com.keyboardpro.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight on-demand SQLite store for Dictionary suggestions and Clipboard history.
 * Uses indexed PREFIX and ORDER BY LIMIT queries so neither the full dictionary
 * nor full clipboard history is ever loaded into RAM.
 */
public class KeyboardSqliteHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "keyboard_pro.db";
    private static final int DB_VERSION = 1;

    private static volatile KeyboardSqliteHelper instance;

    public static KeyboardSqliteHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (KeyboardSqliteHelper.class) {
                if (instance == null) {
                    instance = new KeyboardSqliteHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private KeyboardSqliteHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE dictionary ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "word TEXT NOT NULL UNIQUE,"
                + "word_lower TEXT NOT NULL,"
                + "lang TEXT NOT NULL,"
                + "freq INTEGER NOT NULL DEFAULT 10,"
                + "is_personal INTEGER NOT NULL DEFAULT 0"
                + ")");
        db.execSQL("CREATE INDEX idx_dict_lang_prefix ON dictionary(lang, word_lower, is_personal DESC, freq DESC)");

        db.execSQL("CREATE TABLE clipboard ("
                + "id TEXT PRIMARY KEY,"
                + "text TEXT NOT NULL UNIQUE,"
                + "pinned INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL"
                + ")");
        db.execSQL("CREATE INDEX idx_clip_order ON clipboard(pinned DESC, created_at DESC)");

        seedInitialWords(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Preserves existing schema cleanly across upgrades
    }

    private void seedInitialWords(SQLiteDatabase db) {
        String[] arWords = {
                "السلام", "عليكم", "ورحمة", "الله", "وبركاته", "مرحبا", "أهلا", "وسهلا",
                "شكرا", "جزيلا", "جزاك", "خيرا", "صباح", "الخير", "مساء", "النور",
                "كيف", "حالك", "الحمد", "لله", "تمام", "إن", "شاء", "ممتاز", "رائع",
                "كيبورد", "برو", "التطبيق", "التحديث", "الرسالة", "اليوم", "غدا", "الآن",
                "ممكن", "تفضل", "معذرة", "عفوا", "مبروك", "بالتوفيق", "حسنا", "جميل"
        };
        String[] enWords = {
                "hello", "thanks", "thank", "please", "keyboard", "pro", "android",
                "great", "awesome", "welcome", "morning", "evening", "today", "tomorrow",
                "message", "perfect", "custom", "theme", "crystal", "glass", "style",
                "friend", "update", "working", "meeting", "happy", "love"
        };

        db.beginTransaction();
        try {
            int freq = 100;
            for (String w : arWords) {
                insertWordInternal(db, w, "ar", freq--, 0);
            }
            freq = 100;
            for (String w : enWords) {
                insertWordInternal(db, w, "en", freq--, 0);
            }
            insertWordInternal(db, "كيبورد برو", "ar", 200, 1);
            insertWordInternal(db, "السلام عليكم", "ar", 190, 1);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertWordInternal(SQLiteDatabase db, String word, String lang, int freq, int isPersonal) {
        ContentValues cv = new ContentValues();
        cv.put("word", word);
        cv.put("word_lower", word.toLowerCase(Locale.ROOT));
        cv.put("lang", lang);
        cv.put("freq", freq);
        cv.put("is_personal", isPersonal);
        db.insertWithOnConflict("dictionary", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ---- On-Demand Indexed Dictionary Queries ----

    public List<String> queryPrefixSuggestions(String prefix, String lang, int limit) {
        List<String> results = new ArrayList<>(limit);
        if (prefix == null || prefix.trim().isEmpty()) return results;

        String clean = prefix.trim().toLowerCase(Locale.ROOT);
        String sql = "SELECT word FROM dictionary WHERE lang = ? AND word_lower LIKE ? "
                + "ORDER BY is_personal DESC, freq DESC LIMIT ?";
        try (Cursor c = getReadableDatabase().rawQuery(
                sql, new String[]{lang, clean + "%", String.valueOf(limit)})) {
            while (c.moveToNext()) {
                results.add(c.getString(0));
            }
        }
        return results;
    }

    public List<String> querySwipeCandidates(char firstChar, String lang, int limit) {
        List<String> results = new ArrayList<>(limit);
        String prefix = String.valueOf(Character.toLowerCase(firstChar)) + "%";
        String sql = "SELECT word FROM dictionary WHERE lang = ? AND word_lower LIKE ? "
                + "ORDER BY is_personal DESC, freq DESC LIMIT ?";
        try (Cursor c = getReadableDatabase().rawQuery(
                sql, new String[]{lang, prefix, String.valueOf(limit)})) {
            while (c.moveToNext()) {
                results.add(c.getString(0));
            }
        }
        return results;
    }

    public void addPersonalWord(String word) {
        if (word == null || word.trim().isEmpty()) return;
        String trimmed = word.trim();
        String lang = isArabicWord(trimmed) ? "ar" : "en";
        insertWordInternal(getWritableDatabase(), trimmed, lang, 250, 1);
    }

    public void deletePersonalWord(String word) {
        if (word == null) return;
        getWritableDatabase().delete("dictionary", "word = ? AND is_personal = 1", new String[]{word});
    }

    public void bulkAddPersonalWords(List<String> words) {
        if (words == null || words.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (String w : words) {
                if (w == null || w.trim().isEmpty()) continue;
                String trimmed = w.trim();
                String lang = isArabicWord(trimmed) ? "ar" : "en";
                insertWordInternal(db, trimmed, lang, 250, 1);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void learnTypedWord(String word) {
        if (word == null) return;
        String trimmed = word.trim();
        if (trimmed.length() < 2 || trimmed.length() > 36) return;
        String lang = isArabicWord(trimmed) ? "ar" : "en";
        SQLiteDatabase db = getWritableDatabase();
        int currentFreq = 120;
        try (Cursor c = db.rawQuery("SELECT freq FROM dictionary WHERE word = ? LIMIT 1", new String[]{trimmed})) {
            if (c.moveToFirst()) currentFreq = Math.min(999, c.getInt(0) + 5);
        }
        insertWordInternal(db, trimmed, lang, currentFreq, 1);
    }

    public void clearPersonalWords() {
        getWritableDatabase().delete("dictionary", "is_personal = 1", null);
    }

    public List<String> queryPersonalWords(int limit) {
        List<String> results = new ArrayList<>();
        String sql = "SELECT word FROM dictionary WHERE is_personal = 1 ORDER BY id DESC LIMIT ?";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(limit)})) {
            while (c.moveToNext()) {
                results.add(c.getString(0));
            }
        }
        return results;
    }

    // ---- On-Demand Indexed Clipboard Queries ----

    public List<ClipboardRepository.ClipItem> queryClipboard(int limit) {
        return queryClipboard(limit, true);
    }

    public List<ClipboardRepository.ClipItem> queryClipboard(int limit, boolean newestFirst) {
        List<ClipboardRepository.ClipItem> results = new ArrayList<>();
        String dir = newestFirst ? "DESC" : "ASC";
        String sql = "SELECT id, text, created_at, pinned FROM clipboard "
                + "ORDER BY pinned DESC, created_at " + dir + " LIMIT ?";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(limit)})) {
            while (c.moveToNext()) {
                results.add(new ClipboardRepository.ClipItem(
                        c.getString(0),
                        c.getString(1),
                        c.getLong(2),
                        c.getInt(3) == 1
                ));
            }
        }
        return results;
    }

    public void deleteOldUnpinnedClips(long cutoffTimestampMs) {
        getWritableDatabase().delete("clipboard", "pinned = 0 AND created_at < ?",
                new String[]{String.valueOf(cutoffTimestampMs)});
    }

    public void upsertClip(String id, String text, long timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        int wasPinned = 0;
        try (Cursor c = db.rawQuery("SELECT pinned FROM clipboard WHERE text = ? LIMIT 1", new String[]{text})) {
            if (c.moveToFirst()) {
                wasPinned = c.getInt(0);
            }
        }
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("text", text);
        cv.put("pinned", wasPinned);
        cv.put("created_at", timestamp);
        db.insertWithOnConflict("clipboard", null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        // Keep DB compact: prune oldest unpinned rows beyond 40
        db.execSQL("DELETE FROM clipboard WHERE pinned = 0 AND id NOT IN ("
                + "SELECT id FROM clipboard WHERE pinned = 0 ORDER BY created_at DESC LIMIT 40)");
    }

    public void toggleClipPin(String id) {
        getWritableDatabase().execSQL(
                "UPDATE clipboard SET pinned = CASE WHEN pinned = 1 THEN 0 ELSE 1 END WHERE id = ?",
                new Object[]{id});
    }

    public void deleteClip(String id) {
        getWritableDatabase().delete("clipboard", "id = ?", new String[]{id});
    }

    public void clearUnpinnedClips() {
        getWritableDatabase().delete("clipboard", "pinned = 0", null);
    }

    private static boolean isArabicWord(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\u0600' && c <= '\u06FF') return true;
        }
        return false;
    }
}
