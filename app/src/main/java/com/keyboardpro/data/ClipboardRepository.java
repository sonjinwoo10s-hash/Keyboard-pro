package com.keyboardpro.data;

import android.content.Context;

import java.util.List;
import java.util.UUID;

/**
 * On-demand SQLite-backed clipboard history repository using indexed LIMIT queries,
 * optional 3-day auto-expiry for unpinned items, and configurable newest/oldest sort.
 */
public class ClipboardRepository {

    private static final long THREE_DAYS_MS = 3L * 24L * 60L * 60L * 1000L;

    public static class ClipItem {
        public final String id;
        public final String text;
        public final long timestamp;
        public boolean pinned;

        public ClipItem(String id, String text, long timestamp, boolean pinned) {
            this.id = id;
            this.text = text;
            this.timestamp = timestamp;
            this.pinned = pinned;
        }
    }

    private final KeyboardSqliteHelper dbHelper;

    public ClipboardRepository(Context context) {
        this.dbHelper = KeyboardSqliteHelper.getInstance(context);
    }

    public List<ClipItem> getClips() {
        return getClips(24, true, false);
    }

    public List<ClipItem> getClips(int limit) {
        return getClips(limit, true, false);
    }

    public List<ClipItem> getClips(int limit, boolean newestFirst, boolean autoDelete3Days) {
        if (autoDelete3Days) {
            dbHelper.deleteOldUnpinnedClips(System.currentTimeMillis() - THREE_DAYS_MS);
        }
        return dbHelper.queryClipboard(limit, newestFirst);
    }

    public void addClip(String text) {
        if (text == null || text.trim().isEmpty()) return;
        dbHelper.upsertClip(UUID.randomUUID().toString(), text.trim(), System.currentTimeMillis());
    }

    public void togglePin(String id) {
        dbHelper.toggleClipPin(id);
    }

    public void deleteClip(String id) {
        dbHelper.deleteClip(id);
    }

    public void clearUnpinned() {
        dbHelper.clearUnpinnedClips();
    }
}
