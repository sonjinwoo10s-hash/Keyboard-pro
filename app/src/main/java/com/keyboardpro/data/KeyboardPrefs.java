package com.keyboardpro.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central SharedPreferences store for Keyboard Pro — all Pro features unlocked,
 * 37 preloaded Arabic/English shortcuts, Key FX, Sound/Vibration matrix,
 * Smart Arabic intelligence toggles, Layout modes, Toolbar pinning, and BYOK Gemini AI.
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

    public static final String[] MATRIX_ACTIONS = {
            "letters", "function", "toolbar", "space", "enter",
            "delete", "delete_repeat", "swipe_delete", "autocorrect", "long_press"
    };

    private final SharedPreferences prefs;

    public KeyboardPrefs(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ---- Theme, Key FX & Visuals ----
    public String getActiveThemeId() { return prefs.getString("active_theme_id", "crystal_blue"); }
    public void setActiveThemeId(String id) { prefs.edit().putString("active_theme_id", id).apply(); }

    public String getAppThemeMode() { return prefs.getString("app_theme_mode", APP_THEME_DARK); }
    public void setAppThemeMode(String mode) { prefs.edit().putString("app_theme_mode", mode).apply(); }

    public String getAppLanguage() { return prefs.getString("app_language", "ar"); }
    public void setAppLanguage(String lang) { prefs.edit().putString("app_language", lang).apply(); }

    public int getPortraitHeightDp() { return prefs.getInt("portrait_height_dp", 268); }
    public void setPortraitHeightDp(int dp) { prefs.edit().putInt("portrait_height_dp", Math.max(190, Math.min(380, dp))).apply(); }

    public int getLandscapeHeightDp() { return prefs.getInt("landscape_height_dp", 184); }
    public void setLandscapeHeightDp(int dp) { prefs.edit().putInt("landscape_height_dp", Math.max(130, Math.min(260, dp))).apply(); }

    public int getPortraitKeyTextSp() { return prefs.getInt("portrait_key_text_sp", 19); }
    public void setPortraitKeyTextSp(int sp) { prefs.edit().putInt("portrait_key_text_sp", Math.max(13, Math.min(26, sp))).apply(); }

    public int getLandscapeKeyTextSp() { return prefs.getInt("landscape_key_text_sp", 16); }
    public void setLandscapeKeyTextSp(int sp) { prefs.edit().putInt("landscape_key_text_sp", Math.max(12, Math.min(22, sp))).apply(); }

    public int getKeyCornerRadiusDp() { return prefs.getInt("key_corner_radius_dp", 10); }
    public void setKeyCornerRadiusDp(int dp) { prefs.edit().putInt("key_corner_radius_dp", Math.max(0, Math.min(22, dp))).apply(); }

    public int getKeyOpacityPercent() { return prefs.getInt("key_opacity_pct", 92); }
    public void setKeyOpacityPercent(int pct) { prefs.edit().putInt("key_opacity_pct", Math.max(20, Math.min(100, pct))).apply(); }

    public boolean isSquircleCorners() { return prefs.getBoolean("squircle_corners", true); }
    public void setSquircleCorners(boolean v) { prefs.edit().putBoolean("squircle_corners", v).apply(); }

    public boolean isKey3dDepth() { return prefs.getBoolean("key_3d_depth", true); }
    public void setKey3dDepth(boolean v) { prefs.edit().putBoolean("key_3d_depth", v).apply(); }

    public String getKeyAnimation() { return prefs.getString("key_animation", "press"); }
    public void setKeyAnimation(String anim) { prefs.edit().putString("key_animation", anim).apply(); }

    public String getKeyLightingFx() { return prefs.getString("key_lighting_fx", "glow"); }
    public void setKeyLightingFx(String fx) { prefs.edit().putString("key_lighting_fx", fx).apply(); }

    public boolean isModifierPermanentGlow() { return prefs.getBoolean("mod_perm_glow", true); }
    public void setModifierPermanentGlow(boolean v) { prefs.edit().putBoolean("mod_perm_glow", v).apply(); }

    public int getBlurStrength() { return prefs.getInt("blur_strength", 40); }
    public void setBlurStrength(int s) { prefs.edit().putInt("blur_strength", Math.max(0, Math.min(100, s))).apply(); }

    public boolean isTransparentBlurEnabled() { return prefs.getBoolean("transparent_blur_enabled", false); }
    public void setTransparentBlurEnabled(boolean v) { prefs.edit().putBoolean("transparent_blur_enabled", v).apply(); }

    public boolean isGlassKeysForced() { return prefs.getBoolean("glass_keys_forced", false); }
    public void setGlassKeysForced(boolean v) { prefs.edit().putBoolean("glass_keys_forced", v).apply(); }

    public boolean isShowToolbar() { return prefs.getBoolean("show_toolbar", true); }
    public void setShowToolbar(boolean v) { prefs.edit().putBoolean("show_toolbar", v).apply(); }

    public boolean isIphoneBottomBar() { return prefs.getBoolean("iphone_bottom_bar", false); }
    public void setIphoneBottomBar(boolean v) { prefs.edit().putBoolean("iphone_bottom_bar", v).apply(); }

    public int getBottomMarginDp() { return prefs.getInt("bottom_margin_dp", 0); }
    public void setBottomMarginDp(int dp) { prefs.edit().putInt("bottom_margin_dp", Math.max(0, Math.min(48, dp))).apply(); }

    // ---- Layouts & Tablet Modes (Strictly Unified Non-Split by Default!) ----
    public String getKeyboardLanguage() { return prefs.getString("keyboard_language", "ar"); }
    public void setKeyboardLanguage(String lang) { prefs.edit().putString("keyboard_language", lang).apply(); }

    public String getArabicLayoutType() { return prefs.getString("arabic_layout_type", "default"); }
    public void setArabicLayoutType(String type) { prefs.edit().putString("arabic_layout_type", type).apply(); }

    public String getEnglishLayoutType() { return prefs.getString("english_layout_type", "qwerty"); }
    public void setEnglishLayoutType(String type) { prefs.edit().putString("english_layout_type", type).apply(); }

    public boolean isNumbersRowEnabled() { return prefs.getBoolean("numbers_row_enabled", true); }
    public void setNumbersRowEnabled(boolean v) { prefs.edit().putBoolean("numbers_row_enabled", v).apply(); }

    public boolean isEmojiRowEnabled() { return prefs.getBoolean("emoji_row_enabled", false); }
    public void setEmojiRowEnabled(boolean v) { prefs.edit().putBoolean("emoji_row_enabled", v).apply(); }

    public boolean isIphoneLayoutEnabled() { return "iphone".equals(getArabicLayoutType()); }
    public void setIphoneLayoutEnabled(boolean v) { setArabicLayoutType(v ? "iphone" : "default"); }

    public boolean isSplitModeEnabled() { return prefs.getBoolean("split_mode_enabled", false); }
    public void setSplitModeEnabled(boolean v) { prefs.edit().putBoolean("split_mode_enabled", v).apply(); }

    public boolean isFloatingModeEnabled() { return prefs.getBoolean("floating_mode_enabled", false); }
    public void setFloatingModeEnabled(boolean v) { prefs.edit().putBoolean("floating_mode_enabled", v).apply(); }

    public boolean isOneHandedModeEnabled() { return prefs.getBoolean("one_handed_mode", false); }
    public void setOneHandedModeEnabled(boolean v) { prefs.edit().putBoolean("one_handed_mode", v).apply(); }

    public boolean isLandscapeNumpadEnabled() { return prefs.getBoolean("landscape_numpad", false); }
    public void setLandscapeNumpadEnabled(boolean v) { prefs.edit().putBoolean("landscape_numpad", v).apply(); }

    public boolean isQuickActionsRowEnabled() { return prefs.getBoolean("quick_actions_row", false); }
    public void setQuickActionsRowEnabled(boolean v) { prefs.edit().putBoolean("quick_actions_row", v).apply(); }

    public boolean isIncognitoMode() { return prefs.getBoolean("incognito_mode", false); }
    public void setIncognitoMode(boolean v) { prefs.edit().putBoolean("incognito_mode", v).apply(); }

    // ---- Smart Typing & Arabic Intelligence ----
    public boolean isSuggestionsEnabled() { return prefs.getBoolean("suggestions_enabled", true); }
    public void setSuggestionsEnabled(boolean v) { prefs.edit().putBoolean("suggestions_enabled", v).apply(); }

    public String getSuggestionBarMode() { return prefs.getString("suggestion_bar_mode", "scroll"); }
    public void setSuggestionBarMode(String mode) { prefs.edit().putString("suggestion_bar_mode", mode).apply(); }

    public boolean isAutoCorrectEnabled() { return prefs.getBoolean("auto_correct_enabled", true); }
    public void setAutoCorrectEnabled(boolean v) { prefs.edit().putBoolean("auto_correct_enabled", v).apply(); }

    public boolean isNextWordPredictionEnabled() { return prefs.getBoolean("next_word_prediction", true); }
    public void setNextWordPredictionEnabled(boolean v) { prefs.edit().putBoolean("next_word_prediction", v).apply(); }

    public boolean isHamzaTaFixerEnabled() { return prefs.getBoolean("hamza_ta_fixer", true); }
    public void setHamzaTaFixerEnabled(boolean v) { prefs.edit().putBoolean("hamza_ta_fixer", v).apply(); }

    public boolean isFrancoArabicEnabled() { return prefs.getBoolean("franco_arabic", true); }
    public void setFrancoArabicEnabled(boolean v) { prefs.edit().putBoolean("franco_arabic", v).apply(); }

    public boolean isInlineMathEnabled() { return prefs.getBoolean("inline_math", true); }
    public void setInlineMathEnabled(boolean v) { prefs.edit().putBoolean("inline_math", v).apply(); }

    public boolean isAzkarSuggestionsEnabled() { return prefs.getBoolean("azkar_suggestions", true); }
    public void setAzkarSuggestionsEnabled(boolean v) { prefs.edit().putBoolean("azkar_suggestions", v).apply(); }

    public boolean isEnglishContractionsEnabled() { return prefs.getBoolean("en_contractions", true); }
    public void setEnglishContractionsEnabled(boolean v) { prefs.edit().putBoolean("en_contractions", v).apply(); }

    public boolean isSmartPunctuationEnabled() { return prefs.getBoolean("smart_punctuation", true); }
    public void setSmartPunctuationEnabled(boolean v) { prefs.edit().putBoolean("smart_punctuation", v).apply(); }

    public String getBankExclusionApps() {
        return prefs.getString("bank_exclusion_apps", "com.alrajhibank,com.snb.alahli,com.stc.stcpay");
    }
    public void setBankExclusionApps(String csv) { prefs.edit().putString("bank_exclusion_apps", csv).apply(); }

    // ---- Live Text Decoration & Fonts ----
    public boolean isDecorationEnabled() { return prefs.getBoolean("decoration_enabled", false); }
    public void setDecorationEnabled(boolean v) { prefs.edit().putBoolean("decoration_enabled", v).apply(); }

    public String getActiveStyleId() { return prefs.getString("active_style_id", "normal"); }
    public void setActiveStyleId(String id) { prefs.edit().putString("active_style_id", id).apply(); }

    public String getCustomUiFontPath() { return prefs.getString("custom_ui_font_path", ""); }
    public void setCustomUiFontPath(String path) { prefs.edit().putString("custom_ui_font_path", path == null ? "" : path).apply(); }

    public String getCustomSymbols() {
        return prefs.getString("custom_symbols", "꧁ ꧂ ❖ ✦ ✧ ♛ ♚ ✿ ❀ ✪ ★ ☆ ♥ ♡ ༺ ༻ 『 』 ﴿ ﴾ ✨");
    }
    public void setCustomSymbols(String s) { prefs.edit().putString("custom_symbols", s).apply(); }

    // ---- Emoji & Clipboard Options ----
    public String getEmojiStyle() { return prefs.getString("emoji_style", EMOJI_STYLE_FLUENT); }
    public void setEmojiStyle(String s) { prefs.edit().putString("emoji_style", s).apply(); }

    public String getCustomEmojiFontPath() { return prefs.getString("custom_emoji_font_path", ""); }
    public void setCustomEmojiFontPath(String p) { prefs.edit().putString("custom_emoji_font_path", p == null ? "" : p).apply(); }

    public boolean isEmojiRepeatSuggestions() { return prefs.getBoolean("emoji_repeat_suggestions", true); }
    public void setEmojiRepeatSuggestions(boolean v) { prefs.edit().putBoolean("emoji_repeat_suggestions", v).apply(); }

    public boolean isEnterLongPressEmoji() { return prefs.getBoolean("enter_long_press_emoji", true); }
    public void setEnterLongPressEmoji(boolean v) { prefs.edit().putBoolean("enter_long_press_emoji", v).apply(); }

    public boolean isClipboardAutoDelete3Days() { return prefs.getBoolean("clip_auto_delete_3d", false); }
    public void setClipboardAutoDelete3Days(boolean v) { prefs.edit().putBoolean("clip_auto_delete_3d", v).apply(); }

    public boolean isClipboardSortNewest() { return prefs.getBoolean("clip_sort_newest", true); }
    public void setClipboardSortNewest(boolean v) { prefs.edit().putBoolean("clip_sort_newest", v).apply(); }

    public boolean isClipboardQuickPaste() { return prefs.getBoolean("clip_quick_paste", true); }
    public void setClipboardQuickPaste(boolean v) { prefs.edit().putBoolean("clip_quick_paste", v).apply(); }

    public boolean isClipboardConfirmClear() { return prefs.getBoolean("clip_confirm_clear", true); }
    public void setClipboardConfirmClear(boolean v) { prefs.edit().putBoolean("clip_confirm_clear", v).apply(); }

    // ---- Gestures, 2-Finger Swipes & Sound/Vibration Matrix ----
    public boolean isSwipeTypingEnabled() { return prefs.getBoolean("swipe_typing_enabled", true); }
    public void setSwipeTypingEnabled(boolean v) { prefs.edit().putBoolean("swipe_typing_enabled", v).apply(); }

    public String getSpacebarSwipeMode() { return prefs.getString("spacebar_swipe_mode", "cursor"); }
    public void setSpacebarSwipeMode(String m) { prefs.edit().putString("spacebar_swipe_mode", m).apply(); }

    public boolean isCursorControlEnabled() { return "cursor".equals(getSpacebarSwipeMode()); }
    public void setCursorControlEnabled(boolean v) { setSpacebarSwipeMode(v ? "cursor" : "off"); }

    public boolean isSwipeToDeleteEnabled() { return prefs.getBoolean("swipe_to_delete_enabled", true); }
    public void setSwipeToDeleteEnabled(boolean v) { prefs.edit().putBoolean("swipe_to_delete_enabled", v).apply(); }

    public int getLongPressDelayMs() { return prefs.getInt("long_press_delay_ms", 320); }
    public void setLongPressDelayMs(int ms) { prefs.edit().putInt("long_press_delay_ms", Math.max(150, Math.min(700, ms))).apply(); }

    public boolean isLetterFirstLongPress() { return prefs.getBoolean("letter_first_long_press", false); }
    public void setLetterFirstLongPress(boolean v) { prefs.edit().putBoolean("letter_first_long_press", v).apply(); }

    public String getTwoFingerSwipeAction(String dir) {
        switch (dir) {
            case "up": return prefs.getString("2f_up", "select_all");
            case "down": return prefs.getString("2f_down", "hide");
            case "left": return prefs.getString("2f_left", "copy");
            case "right": return prefs.getString("2f_right", "paste");
            default: return "none";
        }
    }
    public void setTwoFingerSwipeAction(String dir, String action) {
        prefs.edit().putString("2f_" + dir, action).apply();
    }

    public boolean isVibrationEnabled() { return prefs.getBoolean("vibration_enabled", true); }
    public void setVibrationEnabled(boolean v) { prefs.edit().putBoolean("vibration_enabled", v).apply(); }

    public int getVibrationStrengthMs() { return prefs.getInt("vibration_strength_ms", 16); }
    public void setVibrationStrengthMs(int ms) { prefs.edit().putInt("vibration_strength_ms", Math.max(5, Math.min(60, ms))).apply(); }

    public boolean isKeySoundEnabled() { return prefs.getBoolean("key_sound_enabled", false); }
    public void setKeySoundEnabled(boolean v) { prefs.edit().putBoolean("key_sound_enabled", v).apply(); }

    public int getSoundVolumePct() { return prefs.getInt("sound_volume_pct", 60); }
    public void setSoundVolumePct(int pct) { prefs.edit().putInt("sound_volume_pct", Math.max(0, Math.min(100, pct))).apply(); }

    public String getSoundPack() { return prefs.getString("sound_pack", SOUND_PACK_CRYSTAL); }
    public void setSoundPack(String p) { prefs.edit().putString("sound_pack", p).apply(); }

    public boolean isActionSoundEnabled(String action) { return prefs.getBoolean("snd_" + action, true); }
    public void setActionSoundEnabled(String action, boolean v) { prefs.edit().putBoolean("snd_" + action, v).apply(); }

    public boolean isActionVibeEnabled(String action) { return prefs.getBoolean("vib_" + action, true); }
    public void setActionVibeEnabled(String action, boolean v) { prefs.edit().putBoolean("vib_" + action, v).apply(); }

    // ---- Pinned Toolbar Tools ----
    public List<String> getPinnedToolbarTools() {
        String raw = prefs.getString("pinned_toolbar_tools", "ai,clipboard,emoji,translate,resize,settings,decorate");
        List<String> list = new ArrayList<>();
        for (String s : raw.split(",")) {
            if (!s.trim().isEmpty()) list.add(s.trim());
        }
        return list;
    }

    public void togglePinnedToolbarTool(String toolId) {
        List<String> list = getPinnedToolbarTools();
        if (list.contains(toolId)) {
            list.remove(toolId);
        } else {
            list.add(toolId);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        prefs.edit().putString("pinned_toolbar_tools", sb.toString()).apply();
    }

    // ---- AI Keyboard (BYOK Google Gemini) ----
    public String getAiApiKey() { return CryptoVault.decrypt(prefs.getString("ai_api_key_enc", "")); }
    public void setAiApiKey(String key) { prefs.edit().putString("ai_api_key_enc", CryptoVault.encrypt(key == null ? "" : key.trim())).apply(); }

    public String getAiProvider() { return prefs.getString("ai_provider", "gemini"); }
    public void setAiProvider(String p) { prefs.edit().putString("ai_provider", p).apply(); }

    public String getAiModel() { return prefs.getString("ai_model", "gemini-flash-latest"); }
    public void setAiModel(String m) { prefs.edit().putString("ai_model", m).apply(); }

    public String getAiAboutMePersona() { return prefs.getString("ai_about_me", ""); }
    public void setAiAboutMePersona(String s) { prefs.edit().putString("ai_about_me", s == null ? "" : s.trim()).apply(); }

    public int getAiRequestCount() { return prefs.getInt("ai_req_count", 0); }
    public int getAiTokenCount() { return prefs.getInt("ai_tok_count", 0); }
    public void recordAiUsage(int approxTokens) {
        prefs.edit()
                .putInt("ai_req_count", getAiRequestCount() + 1)
                .putInt("ai_tok_count", getAiTokenCount() + Math.max(1, approxTokens))
                .apply();
    }

    public String getCustomAiToolsJson() { return prefs.getString("custom_ai_tools_json", "[]"); }
    public void setCustomAiToolsJson(String json) { prefs.edit().putString("custom_ai_tools_json", json == null ? "[]" : json).apply(); }

    public String getTranslationTargetLang() { return prefs.getString("translation_target_lang", "English"); }
    public void setTranslationTargetLang(String l) { prefs.edit().putString("translation_target_lang", l).apply(); }

    public boolean isOfflineTranslationEnabled() { return prefs.getBoolean("offline_translation_enabled", true); }
    public void setOfflineTranslationEnabled(boolean v) { prefs.edit().putBoolean("offline_translation_enabled", v).apply(); }

    // ---- Cloud Sync & General ----
    public boolean isDailyCloudBackupEnabled() { return prefs.getBoolean("daily_cloud_backup", false); }
    public void setDailyCloudBackupEnabled(boolean v) { prefs.edit().putBoolean("daily_cloud_backup", v).apply(); }

    public String getDriveAccessToken() { return CryptoVault.decrypt(prefs.getString("drive_token_enc", "")); }
    public void setDriveAccessToken(String t) { prefs.edit().putString("drive_token_enc", CryptoVault.encrypt(t == null ? "" : t.trim())).apply(); }

    public long getLastCloudSyncTime() { return prefs.getLong("last_cloud_sync_time", 0L); }
    public void setLastCloudSyncTime(long t) { prefs.edit().putLong("last_cloud_sync_time", t).apply(); }

    public boolean isSplashScreenEnabled() { return prefs.getBoolean("splash_screen_enabled", false); }
    public void setSplashScreenEnabled(boolean v) { prefs.edit().putBoolean("splash_screen_enabled", v).apply(); }

    public boolean isProUnlocked() { return true; } // 100% Unlocked, zero paywalls!
    public void setProUnlocked(boolean unlocked) { prefs.edit().putBoolean("pro_unlocked", true).apply(); }

    // ---- Personal Dictionary ----
    public List<String> getPersonalDictionary() {
        List<String> list = new ArrayList<>();
        String raw = prefs.getString("personal_dictionary_json", "");
        if (raw.isEmpty()) {
            return new ArrayList<>(Arrays.asList("كيبورد برو", "السلام عليكم", "جزاك الله خيراً"));
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        } catch (Exception ignored) {
        }
        return list;
    }

    public void savePersonalDictionary(List<String> words) {
        JSONArray arr = new JSONArray();
        for (String w : words) {
            if (w != null && !w.trim().isEmpty()) arr.put(w.trim());
        }
        prefs.edit().putString("personal_dictionary_json", arr.toString()).apply();
    }

    // ---- 37 Preloaded Arabic & English Shortcuts ----
    public Map<String, String> getTextShortcuts() {
        Map<String, String> map = new LinkedHashMap<>();
        String raw = prefs.getString("text_shortcuts_json", "");
        if (raw.isEmpty()) {
            map.put("سع", "السلام عليكم ورحمة الله وبركاته");
            map.put("وع", "وعليكم السلام ورحمة الله وبركاته");
            map.put("جخ", "جزاك الله خيراً وبارك فيك");
            map.put("انش", "إن شاء الله");
            map.put("ماش", "ما شاء الله تبارك الله");
            map.put("صبخ", "صباح الخير والسعادة");
            map.put("مسخ", "مساء الخير والنور");
            map.put("شكر", "شكراً جزيلاً لك");
            map.put("عفو", "عفواً، على الرحب والسعة");
            map.put("تمن", "أتمنى لك يوماً سعيداً");
            map.put("حمد", "الحمد لله رب العالمين");
            map.put("استغ", "أستغفر الله العظيم وأتوب إليه");
            map.put("سبح", "سبحان الله وبحمده سبحان الله العظيم");
            map.put("صلو", "اللهم صلِّ وسلم على نبينا محمد ﷺ");
            map.put("بارك", "بارك الله فيك وأسعدك");
            map.put("عظم", "عظّم الله أجركم وأحسن عزاءكم");
            map.put("مبرو", "ألف مبروك، بالتوفيق الدائم!");
            map.put("كلع", "كل عام وأنتم بخير");
            map.put("تقب", "تقبل الله منا ومنكم صالح الأعمال");
            map.put("فيام", "في أمان الله وحفظه");
            map.put("معس", "مع السلامة، نراك قريباً");
            map.put("اسف", "أعتذر منك بشدة");
            map.put("لحظ", "لحظة واحدة من فضلك");
            map.put("كيف", "كيف حالك؟ أتمنى أن تكون بخير");
            map.put("اهلا", "أهلاً وسهلاً بك");
            map.put("brb", "Be right back!");
            map.put("idk", "I don't know");
            map.put("omw", "On my way!");
            map.put("hbd", "Happy Birthday! 🎉");
            map.put("ty", "Thank you so much!");
            map.put("np", "No problem at all!");
            map.put("gm", "Good morning!");
            map.put("gn", "Good night!");
            map.put("asap", "As soon as possible");
            map.put("fyi", "For your information");
            map.put("imo", "In my opinion");
            map.put("ttyl", "Talk to you later!");
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

    // ---- Custom Layout, Themes & Requests JSON ----
    public String getCustomArabicLayoutJson() { return prefs.getString("custom_ar_layout_json", ""); }
    public void setCustomArabicLayoutJson(String json) { prefs.edit().putString("custom_ar_layout_json", json == null ? "" : json).apply(); }

    public String getCustomThemesJson() { return prefs.getString("custom_themes_json", "[]"); }
    public void setCustomThemesJson(String json) { prefs.edit().putString("custom_themes_json", json == null ? "[]" : json).apply(); }

    public String getUserRequestsJson() { return prefs.getString("user_requests_json", "[]"); }
    public void setUserRequestsJson(String json) { prefs.edit().putString("user_requests_json", json == null ? "[]" : json).apply(); }

    public void resetToDefaults() {
        prefs.edit().clear().apply();
    }

    public JSONObject exportToJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("active_theme_id", getActiveThemeId());
            root.put("app_theme_mode", getAppThemeMode());
            root.put("portrait_height_dp", getPortraitHeightDp());
            root.put("landscape_height_dp", getLandscapeHeightDp());
            root.put("key_corner_radius_dp", getKeyCornerRadiusDp());
            root.put("key_animation", getKeyAnimation());
            root.put("key_lighting_fx", getKeyLightingFx());
            root.put("arabic_layout_type", getArabicLayoutType());
            root.put("english_layout_type", getEnglishLayoutType());
            root.put("numbers_row_enabled", isNumbersRowEnabled());
            root.put("suggestions_enabled", isSuggestionsEnabled());
            root.put("hamza_ta_fixer", isHamzaTaFixerEnabled());
            root.put("franco_arabic", isFrancoArabicEnabled());
            root.put("inline_math", isInlineMathEnabled());
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
            if (root.has("key_animation")) setKeyAnimation(root.getString("key_animation"));
            if (root.has("key_lighting_fx")) setKeyLightingFx(root.getString("key_lighting_fx"));
            if (root.has("arabic_layout_type")) setArabicLayoutType(root.getString("arabic_layout_type"));
            if (root.has("english_layout_type")) setEnglishLayoutType(root.getString("english_layout_type"));
            if (root.has("numbers_row_enabled")) setNumbersRowEnabled(root.getBoolean("numbers_row_enabled"));
            if (root.has("suggestions_enabled")) setSuggestionsEnabled(root.getBoolean("suggestions_enabled"));
            if (root.has("hamza_ta_fixer")) setHamzaTaFixerEnabled(root.getBoolean("hamza_ta_fixer"));
            if (root.has("franco_arabic")) setFrancoArabicEnabled(root.getBoolean("franco_arabic"));
            if (root.has("inline_math")) setInlineMathEnabled(root.getBoolean("inline_math"));
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
