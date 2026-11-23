package com.keyboardpro.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central SharedPreferences store for all Keyboard Pro settings, independent
 * Portrait/Landscape dimensions, themes, live text decoration styles,
 * dictionary, shortcuts, custom key layout overrides, AI config, and requests.
 */
public class KeyboardPrefs {

    public static final String PREFS_NAME = "keyboard_pro_prefs";

    public static final String APP_THEME_DARK = "dark";
    public static final String APP_THEME_LIGHT = "light";
    public static final String APP_THEME_AUTO = "auto";

    public static final String EMOJI_STYLE_FLUENT = "fluent_3d";
    public static final String EMOJI_STYLE_TWITTER = "twitter";
    public static final String EMOJI_STYLE_GOOGLE = "google";
    public static final String EMOJI_STYLE_CUSTOM = "custom";

    public static final String SOUND_PACK_CRYSTAL = "crystal";
    public static final String SOUND_PACK_MECHANICAL = "mechanical";
    public static final String SOUND_PACK_IPHONE = "iphone";
    public static final String SOUND_PACK_SILENT = "silent";

    private final SharedPreferences prefs;

    public KeyboardPrefs(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ---- Theme & Visuals ----
    public String getActiveThemeId() {
        return prefs.getString("active_theme_id", "obsidian_crystal");
    }

    public void setActiveThemeId(String themeId) {
        prefs.edit().putString("active_theme_id", themeId).apply();
    }

    public String getAppThemeMode() {
        return prefs.getString("app_theme_mode", APP_THEME_DARK);
    }

    public void setAppThemeMode(String mode) {
        prefs.edit().putString("app_theme_mode", mode).apply();
    }

    public String getAppLanguage() {
        return prefs.getString("app_language", "ar");
    }

    public void setAppLanguage(String lang) {
        prefs.edit().putString("app_language", lang).apply();
    }

    public int getPortraitHeightDp() {
        return prefs.getInt("portrait_height_dp", 268);
    }

    public void setPortraitHeightDp(int dp) {
        prefs.edit().putInt("portrait_height_dp", Math.max(190, Math.min(380, dp))).apply();
    }

    public int getLandscapeHeightDp() {
        return prefs.getInt("landscape_height_dp", 184);
    }

    public void setLandscapeHeightDp(int dp) {
        prefs.edit().putInt("landscape_height_dp", Math.max(130, Math.min(260, dp))).apply();
    }

    public int getPortraitKeyTextSp() {
        return prefs.getInt("portrait_key_text_sp", 19);
    }

    public void setPortraitKeyTextSp(int sp) {
        prefs.edit().putInt("portrait_key_text_sp", Math.max(13, Math.min(26, sp))).apply();
    }

    public int getLandscapeKeyTextSp() {
        return prefs.getInt("landscape_key_text_sp", 16);
    }

    public void setLandscapeKeyTextSp(int sp) {
        prefs.edit().putInt("landscape_key_text_sp", Math.max(12, Math.min(22, sp))).apply();
    }

    public int getKeyCornerRadiusDp() {
        return prefs.getInt("key_corner_radius_dp", 9);
    }

    public void setKeyCornerRadiusDp(int dp) {
        prefs.edit().putInt("key_corner_radius_dp", Math.max(0, Math.min(22, dp))).apply();
    }

    public int getBlurStrength() {
        return prefs.getInt("blur_strength", 45);
    }

    public void setBlurStrength(int strength) {
        prefs.edit().putInt("blur_strength", Math.max(0, Math.min(100, strength))).apply();
    }

    public boolean isTransparentBlurEnabled() {
        return prefs.getBoolean("transparent_blur_enabled", false);
    }

    public void setTransparentBlurEnabled(boolean enabled) {
        prefs.edit().putBoolean("transparent_blur_enabled", enabled).apply();
    }

    public boolean isGlassKeysForced() {
        return prefs.getBoolean("glass_keys_forced", false);
    }

    public void setGlassKeysForced(boolean enabled) {
        prefs.edit().putBoolean("glass_keys_forced", enabled).apply();
    }

    public boolean isShowToolbar() {
        return prefs.getBoolean("show_toolbar", true);
    }

    public void setShowToolbar(boolean show) {
        prefs.edit().putBoolean("show_toolbar", show).apply();
    }

    // ---- Languages & Layout ----
    public String getKeyboardLanguage() {
        return prefs.getString("keyboard_language", "ar");
    }

    public void setKeyboardLanguage(String lang) {
        prefs.edit().putString("keyboard_language", lang).apply();
    }

    public boolean isNumbersRowEnabled() {
        return prefs.getBoolean("numbers_row_enabled", true);
    }

    public void setNumbersRowEnabled(boolean enabled) {
        prefs.edit().putBoolean("numbers_row_enabled", enabled).apply();
    }

    public boolean isIphoneLayoutEnabled() {
        return prefs.getBoolean("iphone_layout_enabled", false);
    }

    public void setIphoneLayoutEnabled(boolean enabled) {
        prefs.edit().putBoolean("iphone_layout_enabled", enabled).apply();
    }

    // ---- Smart Typing ----
    public boolean isSuggestionsEnabled() {
        return prefs.getBoolean("suggestions_enabled", true);
    }

    public void setSuggestionsEnabled(boolean enabled) {
        prefs.edit().putBoolean("suggestions_enabled", enabled).apply();
    }

    public boolean isAutoCorrectEnabled() {
        return prefs.getBoolean("auto_correct_enabled", true);
    }

    public void setAutoCorrectEnabled(boolean enabled) {
        prefs.edit().putBoolean("auto_correct_enabled", enabled).apply();
    }

    public boolean isNextWordPredictionEnabled() {
        return prefs.getBoolean("next_word_prediction", true);
    }

    public void setNextWordPredictionEnabled(boolean enabled) {
        prefs.edit().putBoolean("next_word_prediction", enabled).apply();
    }

    // ---- Live Text Decoration & Fonts ----
    public boolean isDecorationEnabled() {
        return prefs.getBoolean("decoration_enabled", false);
    }

    public void setDecorationEnabled(boolean enabled) {
        prefs.edit().putBoolean("decoration_enabled", enabled).apply();
    }

    public String getActiveStyleId() {
        return prefs.getString("active_style_id", "normal");
    }

    public void setActiveStyleId(String styleId) {
        prefs.edit().putString("active_style_id", styleId).apply();
    }

    public String getCustomUiFontPath() {
        return prefs.getString("custom_ui_font_path", "");
    }

    public void setCustomUiFontPath(String path) {
        prefs.edit().putString("custom_ui_font_path", path == null ? "" : path).apply();
    }

    public String getCustomSymbols() {
        return prefs.getString("custom_symbols", "꧁ ꧂ ❖ ✦ ✧ ♛ ♚ ✿ ❀ ✪ ❖ ★ ☆ ♥ ♦ ♣ ♠ ⚡ ❄ 🔥 ✨");
    }

    public void setCustomSymbols(String symbols) {
        prefs.edit().putString("custom_symbols", symbols).apply();
    }

    // ---- Emoji & Stickers ----
    public String getEmojiStyle() {
        return prefs.getString("emoji_style", EMOJI_STYLE_FLUENT);
    }

    public void setEmojiStyle(String style) {
        prefs.edit().putString("emoji_style", style).apply();
    }

    public String getCustomEmojiFontPath() {
        return prefs.getString("custom_emoji_font_path", "");
    }

    public void setCustomEmojiFontPath(String path) {
        prefs.edit().putString("custom_emoji_font_path", path == null ? "" : path).apply();
    }

    // ---- Gestures, Sound & Haptics ----
    public boolean isSwipeTypingEnabled() {
        return prefs.getBoolean("swipe_typing_enabled", true);
    }

    public void setSwipeTypingEnabled(boolean enabled) {
        prefs.edit().putBoolean("swipe_typing_enabled", enabled).apply();
    }

    public boolean isCursorControlEnabled() {
        return prefs.getBoolean("cursor_control_enabled", true);
    }

    public void setCursorControlEnabled(boolean enabled) {
        prefs.edit().putBoolean("cursor_control_enabled", enabled).apply();
    }

    public boolean isSwipeToDeleteEnabled() {
        return prefs.getBoolean("swipe_to_delete_enabled", true);
    }

    public void setSwipeToDeleteEnabled(boolean enabled) {
        prefs.edit().putBoolean("swipe_to_delete_enabled", enabled).apply();
    }

    public boolean isVibrationEnabled() {
        return prefs.getBoolean("vibration_enabled", true);
    }

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply();
    }

    public int getVibrationStrengthMs() {
        return prefs.getInt("vibration_strength_ms", 18);
    }

    public void setVibrationStrengthMs(int ms) {
        prefs.edit().putInt("vibration_strength_ms", Math.max(5, Math.min(60, ms))).apply();
    }

    public boolean isKeySoundEnabled() {
        return prefs.getBoolean("key_sound_enabled", false);
    }

    public void setKeySoundEnabled(boolean enabled) {
        prefs.edit().putBoolean("key_sound_enabled", enabled).apply();
    }

    public String getSoundPack() {
        return prefs.getString("sound_pack", SOUND_PACK_CRYSTAL);
    }

    public void setSoundPack(String pack) {
        prefs.edit().putString("sound_pack", pack).apply();
    }

    // ---- AI Keyboard ----
    public String getAiApiKey() {
        return CryptoVault.decrypt(prefs.getString("ai_api_key_enc", ""));
    }

    public void setAiApiKey(String key) {
        prefs.edit().putString("ai_api_key_enc", CryptoVault.encrypt(key == null ? "" : key.trim())).apply();
    }

    public String getAiProvider() {
        return prefs.getString("ai_provider", "gemini");
    }

    public void setAiProvider(String provider) {
        prefs.edit().putString("ai_provider", provider).apply();
    }

    public String getAiModel() {
        return prefs.getString("ai_model", "gemini-2.0-flash");
    }

    public void setAiModel(String model) {
        prefs.edit().putString("ai_model", model).apply();
    }

    public String getTranslationTargetLang() {
        return prefs.getString("translation_target_lang", "English");
    }

    public void setTranslationTargetLang(String lang) {
        prefs.edit().putString("translation_target_lang", lang).apply();
    }

    public boolean isOfflineTranslationEnabled() {
        return prefs.getBoolean("offline_translation_enabled", true);
    }

    public void setOfflineTranslationEnabled(boolean enabled) {
        prefs.edit().putBoolean("offline_translation_enabled", enabled).apply();
    }

    // ---- Cloud Sync & General ----
    public boolean isDailyCloudBackupEnabled() {
        return prefs.getBoolean("daily_cloud_backup", false);
    }

    public void setDailyCloudBackupEnabled(boolean enabled) {
        prefs.edit().putBoolean("daily_cloud_backup", enabled).apply();
    }

    public String getDriveAccessToken() {
        return CryptoVault.decrypt(prefs.getString("drive_token_enc", ""));
    }

    public void setDriveAccessToken(String token) {
        prefs.edit().putString("drive_token_enc", CryptoVault.encrypt(token == null ? "" : token.trim())).apply();
    }

    public long getLastCloudSyncTime() {
        return prefs.getLong("last_cloud_sync_time", 0L);
    }

    public void setLastCloudSyncTime(long timeMs) {
        prefs.edit().putLong("last_cloud_sync_time", timeMs).apply();
    }

    public boolean isSplashScreenEnabled() {
        return prefs.getBoolean("splash_screen_enabled", true);
    }

    public void setSplashScreenEnabled(boolean enabled) {
        prefs.edit().putBoolean("splash_screen_enabled", enabled).apply();
    }

    public boolean isProUnlocked() {
        return prefs.getBoolean("pro_unlocked", true);
    }

    public void setProUnlocked(boolean unlocked) {
        prefs.edit().putBoolean("pro_unlocked", unlocked).apply();
    }

    // ---- Personal Dictionary ----
    public List<String> getPersonalDictionary() {
        List<String> list = new ArrayList<>();
        String raw = prefs.getString("personal_dictionary_json", "");
        if (raw.isEmpty()) {
            list.add("كيبورد برو");
            list.add("السلام عليكم");
            list.add("جزاك الله خيراً");
            return list;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void savePersonalDictionary(List<String> words) {
        JSONArray arr = new JSONArray();
        for (String w : words) {
            if (w != null && !w.trim().isEmpty()) {
                arr.put(w.trim());
            }
        }
        prefs.edit().putString("personal_dictionary_json", arr.toString()).apply();
    }

    // ---- Text Shortcuts ----
    public Map<String, String> getTextShortcuts() {
        Map<String, String> map = new LinkedHashMap<>();
        String raw = prefs.getString("text_shortcuts_json", "");
        if (raw.isEmpty()) {
            map.put("سع", "السلام عليكم ورحمة الله وبركاته");
            map.put("جز", "جزاك الله خيراً وبارك فيك");
            map.put("شكر", "شكراً جزيلاً لك");
            map.put("omw", "On my way!");
            return map;
        }
        try {
            JSONObject obj = new JSONObject(raw);
            JSONArray keys = obj.names();
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    String k = keys.getString(i);
                    map.put(k, obj.getString(k));
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    public void saveTextShortcuts(Map<String, String> shortcuts) {
        JSONObject obj = new JSONObject();
        try {
            for (Map.Entry<String, String> e : shortcuts.entrySet()) {
                obj.put(e.getKey(), e.getValue());
            }
        } catch (Exception ignored) {
        }
        prefs.edit().putString("text_shortcuts_json", obj.toString()).apply();
    }

    // ---- Custom Key Layout Overrides (Arabic Row 1..3) ----
    public String getCustomArabicLayoutJson() {
        return prefs.getString("custom_ar_layout_json", "");
    }

    public void setCustomArabicLayoutJson(String json) {
        prefs.edit().putString("custom_ar_layout_json", json == null ? "" : json).apply();
    }

    // ---- Custom Themes JSON ----
    public String getCustomThemesJson() {
        return prefs.getString("custom_themes_json", "[]");
    }

    public void setCustomThemesJson(String json) {
        prefs.edit().putString("custom_themes_json", json == null ? "[]" : json).apply();
    }

    // ---- User Requests History JSON ----
    public String getUserRequestsJson() {
        return prefs.getString("user_requests_json", "[]");
    }

    public void setUserRequestsJson(String json) {
        prefs.edit().putString("user_requests_json", json == null ? "[]" : json).apply();
    }

    // ---- Export/Import All Settings JSON ----
    public JSONObject exportToJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("active_theme_id", getActiveThemeId());
            root.put("app_theme_mode", getAppThemeMode());
            root.put("portrait_height_dp", getPortraitHeightDp());
            root.put("landscape_height_dp", getLandscapeHeightDp());
            root.put("key_corner_radius_dp", getKeyCornerRadiusDp());
            root.put("blur_strength", getBlurStrength());
            root.put("transparent_blur_enabled", isTransparentBlurEnabled());
            root.put("glass_keys_forced", isGlassKeysForced());
            root.put("numbers_row_enabled", isNumbersRowEnabled());
            root.put("iphone_layout_enabled", isIphoneLayoutEnabled());
            root.put("suggestions_enabled", isSuggestionsEnabled());
            root.put("auto_correct_enabled", isAutoCorrectEnabled());
            root.put("swipe_typing_enabled", isSwipeTypingEnabled());
            root.put("cursor_control_enabled", isCursorControlEnabled());
            root.put("swipe_to_delete_enabled", isSwipeToDeleteEnabled());
            root.put("vibration_enabled", isVibrationEnabled());
            root.put("vibration_strength_ms", getVibrationStrengthMs());
            root.put("key_sound_enabled", isKeySoundEnabled());
            root.put("sound_pack", getSoundPack());
            root.put("emoji_style", getEmojiStyle());
            root.put("active_style_id", getActiveStyleId());
            root.put("custom_symbols", getCustomSymbols());
            root.put("personal_dictionary", new JSONArray(getPersonalDictionary()));
            root.put("text_shortcuts", new JSONObject(getTextShortcuts()));
            root.put("custom_ar_layout_json", getCustomArabicLayoutJson());
            root.put("custom_themes_json", getCustomThemesJson());
        } catch (Exception ignored) {
        }
        return root;
    }

    public boolean importFromJson(String jsonStr) {
        try {
            JSONObject root = new JSONObject(jsonStr);
            if (root.has("active_theme_id")) setActiveThemeId(root.getString("active_theme_id"));
            if (root.has("app_theme_mode")) setAppThemeMode(root.getString("app_theme_mode"));
            if (root.has("portrait_height_dp")) setPortraitHeightDp(root.getInt("portrait_height_dp"));
            if (root.has("landscape_height_dp")) setLandscapeHeightDp(root.getInt("landscape_height_dp"));
            if (root.has("key_corner_radius_dp")) setKeyCornerRadiusDp(root.getInt("key_corner_radius_dp"));
            if (root.has("blur_strength")) setBlurStrength(root.getInt("blur_strength"));
            if (root.has("transparent_blur_enabled")) setTransparentBlurEnabled(root.getBoolean("transparent_blur_enabled"));
            if (root.has("glass_keys_forced")) setGlassKeysForced(root.getBoolean("glass_keys_forced"));
            if (root.has("numbers_row_enabled")) setNumbersRowEnabled(root.getBoolean("numbers_row_enabled"));
            if (root.has("iphone_layout_enabled")) setIphoneLayoutEnabled(root.getBoolean("iphone_layout_enabled"));
            if (root.has("suggestions_enabled")) setSuggestionsEnabled(root.getBoolean("suggestions_enabled"));
            if (root.has("auto_correct_enabled")) setAutoCorrectEnabled(root.getBoolean("auto_correct_enabled"));
            if (root.has("swipe_typing_enabled")) setSwipeTypingEnabled(root.getBoolean("swipe_typing_enabled"));
            if (root.has("cursor_control_enabled")) setCursorControlEnabled(root.getBoolean("cursor_control_enabled"));
            if (root.has("swipe_to_delete_enabled")) setSwipeToDeleteEnabled(root.getBoolean("swipe_to_delete_enabled"));
            if (root.has("vibration_enabled")) setVibrationEnabled(root.getBoolean("vibration_enabled"));
            if (root.has("vibration_strength_ms")) setVibrationStrengthMs(root.getInt("vibration_strength_ms"));
            if (root.has("key_sound_enabled")) setKeySoundEnabled(root.getBoolean("key_sound_enabled"));
            if (root.has("sound_pack")) setSoundPack(root.getString("sound_pack"));
            if (root.has("emoji_style")) setEmojiStyle(root.getString("emoji_style"));
            if (root.has("active_style_id")) setActiveStyleId(root.getString("active_style_id"));
            if (root.has("custom_symbols")) setCustomSymbols(root.getString("custom_symbols"));
            if (root.has("personal_dictionary")) {
                JSONArray arr = root.getJSONArray("personal_dictionary");
                List<String> words = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) words.add(arr.getString(i));
                savePersonalDictionary(words);
            }
            if (root.has("text_shortcuts")) {
                JSONObject obj = root.getJSONObject("text_shortcuts");
                Map<String, String> map = new LinkedHashMap<>();
                JSONArray keys = obj.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String k = keys.getString(i);
                        map.put(k, obj.getString(k));
                    }
                }
                saveTextShortcuts(map);
            }
            if (root.has("custom_ar_layout_json")) setCustomArabicLayoutJson(root.getString("custom_ar_layout_json"));
            if (root.has("custom_themes_json")) setCustomThemesJson(root.getString("custom_themes_json"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
