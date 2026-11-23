package com.keyboardpro.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages user-created stickers (rendered from styled text or picked photos)
 * stored locally in app internal storage and shareable inside the keyboard panel.
 */
public class StickerRepository {

    public static class StickerItem {
        public final String id;
        public final String label;
        public final String filePath;
        public final boolean isExclusive;

        public StickerItem(String id, String label, String filePath, boolean isExclusive) {
            this.id = id;
            this.label = label;
            this.filePath = filePath;
            this.isExclusive = isExclusive;
        }
    }

    private static final String PREFS_STICKERS = "keyboard_pro_stickers";
    private static final String KEY_LIST = "stickers_json";

    private final Context context;

    public StickerRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<StickerItem> getStickers() {
        List<StickerItem> list = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS_STICKERS, Context.MODE_PRIVATE)
                .getString(KEY_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new StickerItem(
                        o.optString("id"),
                        o.optString("label"),
                        o.optString("path"),
                        o.optBoolean("exclusive", false)
                ));
            }
        } catch (Exception ignored) {
        }
        if (list.isEmpty()) {
            // Seed 4 crisp exclusive text-badge stickers on first open
            createTextSticker("أهلاً وسهلاً ✨", Color.parseColor("#00D4FF"), Color.parseColor("#6366F1"), true);
            createTextSticker("ممتاز 👑", Color.parseColor("#F5C542"), Color.parseColor("#B45309"), true);
            createTextSticker("شكراً جزيلاً 💎", Color.parseColor("#10B981"), Color.parseColor("#047857"), true);
            createTextSticker("مبروك 🎉", Color.parseColor("#EC4899"), Color.parseColor("#8B5CF6"), true);
            return getStickersInternal();
        }
        return list;
    }

    private List<StickerItem> getStickersInternal() {
        List<StickerItem> list = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS_STICKERS, Context.MODE_PRIVATE)
                .getString(KEY_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new StickerItem(
                        o.optString("id"),
                        o.optString("label"),
                        o.optString("path"),
                        o.optBoolean("exclusive", false)
                ));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public StickerItem createTextSticker(String text, int startColor, int endColor, boolean exclusive) {
        int size = 384;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, size, size, startColor, endColor, Shader.TileMode.CLAMP));
        RectF rect = new RectF(16, 48, size - 16, size - 48);
        canvas.drawRoundRect(rect, 56, 56, bgPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setColor(Color.argb(140, 255, 255, 255));
        canvas.drawRoundRect(rect, 56, 56, borderPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(46f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(8f, 0f, 4f, Color.argb(120, 0, 0, 0));

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float y = (size / 2f) - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, size / 2f, y, textPaint);

        return saveStickerBitmap(text, bmp, exclusive);
    }

    public StickerItem saveStickerBitmap(String label, Bitmap bitmap, boolean exclusive) {
        String id = UUID.randomUUID().toString();
        File dir = new File(context.getFilesDir(), "stickers");
        if (!dir.exists()) dir.mkdirs();
        File outFile = new File(dir, "stk_" + id + ".png");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 384, 384, true);
            scaled.compress(Bitmap.CompressFormat.PNG, 95, fos);
        } catch (Exception ignored) {
        }
        List<StickerItem> list = getStickersInternal();
        StickerItem item = new StickerItem(id, label, outFile.getAbsolutePath(), exclusive);
        list.add(0, item);
        saveList(list);
        return item;
    }

    public void deleteSticker(String id) {
        List<StickerItem> list = getStickersInternal();
        for (StickerItem s : list) {
            if (s.id.equals(id) && s.filePath != null) {
                new File(s.filePath).delete();
            }
        }
        list.removeIf(s -> s.id.equals(id));
        saveList(list);
    }

    private void saveList(List<StickerItem> list) {
        JSONArray arr = new JSONArray();
        try {
            for (StickerItem s : list) {
                JSONObject o = new JSONObject();
                o.put("id", s.id);
                o.put("label", s.label);
                o.put("path", s.filePath);
                o.put("exclusive", s.isExclusive);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        context.getSharedPreferences(PREFS_STICKERS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LIST, arr.toString()).apply();
    }
}
