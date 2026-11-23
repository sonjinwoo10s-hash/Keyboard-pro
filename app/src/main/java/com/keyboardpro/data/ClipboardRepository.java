package com.keyboardpro.data;

import android.content.Context;

import java.util.List;
import java.util.UUID;

/**
 * On-demand SQLite-backed clipboard history repository using indexed LIMIT queries
 * to keep memory footprint minimal inside the InputMethodService process.
 */
public class ClipboardRepository {

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
        return getClips(20);
    }

    public List<ClipItem> getClips(int limit) {
        return dbHelper.queryClipboard(limit);
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
