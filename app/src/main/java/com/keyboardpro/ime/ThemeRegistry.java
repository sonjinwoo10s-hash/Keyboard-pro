package com.keyboardpro.ime;

import android.content.Context;
import android.graphics.Color;

import com.keyboardpro.data.KeyboardPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registry of built-in flagship, Pro crystal, community, and user-created custom themes.
 */
public final class ThemeRegistry {

    private ThemeRegistry() {
    }

    public static List<KeyboardTheme> getBuiltInThemes() {
        List<KeyboardTheme> themes = new ArrayList<>();

        // --- NEW ("جديدة") & PRO ---
        themes.add(new KeyboardTheme(
                "obsidian_crystal", "كريستال أوبسيديان", "Obsidian Crystal",
                "أسود أزرق سماوي زجاج كريستال black cyan glass",
                KeyboardTheme.Category.NEW, true,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                Color.parseColor("#0B111E"), Color.parseColor("#16233B"),
                Color.parseColor("#38253E5C"), Color.parseColor("#4D00D4FF"),
                Color.parseColor("#6600D4FF"), Color.parseColor("#F4FAFF"),
                Color.parseColor("#00E5FF"), Color.parseColor("#AA00D4FF"),
                Color.parseColor("#00D4FF"), Color.parseColor("#0E1525")
        ));

        themes.add(new KeyboardTheme(
                "royal_gold_pro", "الذهبي الملكي برو", "Royal Gold Pro",
                "ذهبي أسود فاخر gold black luxury crown",
                KeyboardTheme.Category.NEW, true,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                Color.parseColor("#120F0A"), Color.parseColor("#241C0E"),
                Color.parseColor("#44362910"), Color.parseColor("#55F5C542"),
                Color.parseColor("#88F5C542"), Color.parseColor("#FFF8E1"),
                Color.parseColor("#F5C542"), Color.parseColor("#BBF5C542"),
                Color.parseColor("#F5C542"), Color.parseColor("#16120B")
        ));

        themes.add(new KeyboardTheme(
                "sapphire_blur", "ضباب الياقوت الشفاف", "Sapphire Blur",
                "شفاف ضبابي أزرق blur transparent blue",
                KeyboardTheme.Category.NEW, true,
                KeyboardTheme.BackgroundType.TRANSPARENT_BLUR, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                Color.parseColor("#CC0D1B2A"), Color.parseColor("#CC1B263B"),
                Color.parseColor("#40FFFFFF"), Color.parseColor("#5538BDF8"),
                Color.parseColor("#8038BDF8"), Color.parseColor("#FFFFFF"),
                Color.parseColor("#7DD3FC"), Color.parseColor("#88E0F2FE"),
                Color.parseColor("#38BDF8"), Color.parseColor("#CC0A1522")
        ));

        themes.add(new KeyboardTheme(
                "auto_day_night", "تلقائي ليلي ونهاري", "Auto Light/Dark",
                "تلقائي أبيض أسود auto split white dark",
                KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.AUTO_SPLIT, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                Color.parseColor("#141923"), Color.parseColor("#E7EDF5"),
                Color.parseColor("#252F42"), Color.parseColor("#00B4D8"),
                Color.parseColor("#38BDF8"), Color.parseColor("#F8FAFC"),
                Color.parseColor("#FFFFFF"), Color.parseColor("#4464748B"),
                Color.parseColor("#00B4D8"), Color.parseColor("#182030")
        ));

        themes.add(new KeyboardTheme(
                "rose_quartz_crystal", "كريستال الكوارتز الوردي", "Rose Crystal",
                "وردي بنفسجي زجاج كريستال pink rose purple glass",
                KeyboardTheme.Category.NEW, true,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                Color.parseColor("#1F0E1F"), Color.parseColor("#3A1538"),
                Color.parseColor("#38F472B6"), Color.parseColor("#55EC4899"),
                Color.parseColor("#88F472B6"), Color.parseColor("#FFF1F7"),
                Color.parseColor("#FBCFE8"), Color.parseColor("#AAF472B6"),
                Color.parseColor("#F472B6"), Color.parseColor("#231024")
        ));

        themes.add(new KeyboardTheme(
                "midnight_checker", "شطرنج منتصف الليل", "Midnight Checker",
                "شطرنج مربعات أسود رمادي checker grid dark",
                KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.CHECKERED, KeyboardTheme.KeyStyleType.ELEVATED,
                Color.parseColor("#111622"), Color.parseColor("#1C2436"),
                Color.parseColor("#232C40"), Color.parseColor("#6366F1"),
                Color.parseColor("#818CF8"), Color.parseColor("#F1F5F9"),
                Color.parseColor("#FFFFFF"), Color.parseColor("#3394A3B8"),
                Color.parseColor("#6366F1"), Color.parseColor("#101520")
        ));

        // --- BASIC ("الأساسية") ---
        themes.add(new KeyboardTheme(
                "carbon_dark", "كربون داكن كلاسيكي", "Carbon Dark",
                "أسود داكن رمادي كلاسيكي dark black grey",
                KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.ELEVATED,
                Color.parseColor("#12151C"), Color.parseColor("#12151C"),
                Color.parseColor("#232936"), Color.parseColor("#1B212C"),
                Color.parseColor("#334155"), Color.parseColor("#F8FAFC"),
                Color.parseColor("#94A3B8"), Color.parseColor("#22FFFFFF"),
                Color.parseColor("#00D4FF"), Color.parseColor("#0D1016")
        ));

        themes.add(new KeyboardTheme(
                "ios_midnight", "آيفون داكن ناعم", "iPhone Dark",
                "آيفون أسود رمادي ios iphone dark",
                KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                Color.parseColor("#1C1C1E"), Color.parseColor("#1C1C1E"),
                Color.parseColor("#3A3A3C"), Color.parseColor("#2C2C2E"),
                Color.parseColor("#636366"), Color.parseColor("#FFFFFF"),
                Color.parseColor("#E5E5EA"), Color.parseColor("#18FFFFFF"),
                Color.parseColor("#0A84FF"), Color.parseColor("#161618")
        ));

        themes.add(new KeyboardTheme(
                "ios_snow", "آيفون أبيض ثلجي", "iPhone Light",
                "أبيض نهاري فاتح آيفون white light snow",
                KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                Color.parseColor("#D5DAE2"), Color.parseColor("#D5DAE2"),
                Color.parseColor("#FFFFFF"), Color.parseColor("#B6BFC9"),
                Color.parseColor("#CBD5E1"), Color.parseColor("#0F172A"),
                Color.parseColor("#334155"), Color.parseColor("#3394A3B8"),
                Color.parseColor("#007AFF"), Color.parseColor("#E2E7EF")
        ));

        themes.add(new KeyboardTheme(
                "emerald_aurora", "شفق الزمرد", "Emerald Aurora",
                "أخضر زمردي تدرج green emerald gradient",
                KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.ELEVATED,
                Color.parseColor("#062019"), Color.parseColor("#0E3B2E"),
                Color.parseColor("#144D3D"), Color.parseColor("#10B981"),
                Color.parseColor("#34D399"), Color.parseColor("#ECFDF5"),
                Color.parseColor("#042F2E"), Color.parseColor("#4434D399"),
                Color.parseColor("#10B981"), Color.parseColor("#051A14")
        ));

        themes.add(new KeyboardTheme(
                "sunset_velvet", "مخمل الغروب", "Sunset Velvet",
                "برتقالي أحمر بنفسجي تدرج orange red sunset",
                KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.FLAT,
                Color.parseColor("#2B111C"), Color.parseColor("#4A192C"),
                Color.parseColor("#3E1D2F"), Color.parseColor("#F97316"),
                Color.parseColor("#FB923C"), Color.parseColor("#FFF7ED"),
                Color.parseColor("#FFFFFF"), Color.parseColor("#44FB923C"),
                Color.parseColor("#F97316"), Color.parseColor("#240E17")
        ));

        themes.add(new KeyboardTheme(
                "pure_amoled", "أموليد أسود صافي", "Pure AMOLED",
                "أسود أموليد موفر طاقة amoled black pure",
                KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.NEON_OUTLINE,
                Color.parseColor("#000000"), Color.parseColor("#000000"),
                Color.parseColor("#0C0F14"), Color.parseColor("#121722"),
                Color.parseColor("#1E293B"), Color.parseColor("#FFFFFF"),
                Color.parseColor("#00D4FF"), Color.parseColor("#5500D4FF"),
                Color.parseColor("#00D4FF"), Color.parseColor("#050507")
        ));

        // --- USERS ("المستخدمين") ---
        themes.add(new KeyboardTheme(
                "cyber_neon_user", "سايبر نيون — أحمد", "Cyber Neon",
                "نيون أزرق بنفسجي مستخدمين neon cyber purple",
                KeyboardTheme.Category.USERS, true,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.NEON_OUTLINE,
                Color.parseColor("#0A061C"), Color.parseColor("#1A0B3D"),
                Color.parseColor("#26124F"), Color.parseColor("#7C3AED"),
                Color.parseColor("#A78BFA"), Color.parseColor("#E0E7FF"),
                Color.parseColor("#38BDF8"), Color.parseColor("#CC00F0FF"),
                Color.parseColor("#00F0FF"), Color.parseColor("#0E0824")
        ));

        themes.add(new KeyboardTheme(
                "matrix_green_user", "ماتريكس أخضر — سارة", "Matrix Code",
                "أخضر أسود هاكر برمجة green matrix hacker",
                KeyboardTheme.Category.USERS, false,
                KeyboardTheme.BackgroundType.CHECKERED, KeyboardTheme.KeyStyleType.NEON_OUTLINE,
                Color.parseColor("#030F07"), Color.parseColor("#081C0E"),
                Color.parseColor("#0A2212"), Color.parseColor("#14532D"),
                Color.parseColor("#22C55E"), Color.parseColor("#4ADE80"),
                Color.parseColor("#BBF7D0"), Color.parseColor("#8822C55E"),
                Color.parseColor("#22C55E"), Color.parseColor("#041208")
        ));

        themes.add(new KeyboardTheme(
                "desert_gold_user", "رمال الصحراء — خالد", "Desert Gold",
                "بني ذهبي رملي صحراء sand gold brown",
                KeyboardTheme.Category.USERS, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.ELEVATED,
                Color.parseColor("#211811"), Color.parseColor("#3D2B1F"),
                Color.parseColor("#4A3525"), Color.parseColor("#D97706"),
                Color.parseColor("#F59E0B"), Color.parseColor("#FEF3C7"),
                Color.parseColor("#FFFFFF"), Color.parseColor("#55F59E0B"),
                Color.parseColor("#F59E0B"), Color.parseColor("#1B140E")
        ));

        themes.add(new KeyboardTheme(
                "ocean_glass_user", "أمواج المحيط الكريستالية — ريم", "Ocean Glass",
                "أزرق بحر زجاج كريستال ocean blue glass",
                KeyboardTheme.Category.USERS, true,
                KeyboardTheme.BackgroundType.TRANSPARENT_BLUR, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                Color.parseColor("#CC082F49"), Color.parseColor("#CC0C4A6E"),
                Color.parseColor("#3838BDF8"), Color.parseColor("#550EA5E9"),
                Color.parseColor("#887DD3FC"), Color.parseColor("#F0F9FF"),
                Color.parseColor("#BAE6FD"), Color.parseColor("#AA38BDF8"),
                Color.parseColor("#38BDF8"), Color.parseColor("#CC072438")
        ));

        return themes;
    }

    public static List<KeyboardTheme> getAllThemes(Context context) {
        List<KeyboardTheme> all = new ArrayList<>(getBuiltInThemes());
        all.addAll(getCustomThemes(context));
        return all;
    }

    public static List<KeyboardTheme> getCustomThemes(Context context) {
        List<KeyboardTheme> list = new ArrayList<>();
        KeyboardPrefs prefs = new KeyboardPrefs(context);
        try {
            JSONArray arr = new JSONArray(prefs.getCustomThemesJson());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int bg1 = o.optInt("bg1", Color.parseColor("#111827"));
                int bg2 = o.optInt("bg2", Color.parseColor("#1E293B"));
                int keyBg = o.optInt("keyBg", Color.parseColor("#273449"));
                int accent = o.optInt("accent", Color.parseColor("#00D4FF"));
                boolean glass = o.optBoolean("glass", false);
                boolean blur = o.optBoolean("blur", false);
                list.add(new KeyboardTheme(
                        o.optString("id"),
                        o.optString("name", "ثيمي المخصص"),
                        o.optString("name", "Custom Theme"),
                        "ثيمي مخصص custom " + o.optString("name", ""),
                        KeyboardTheme.Category.MY_THEMES,
                        false,
                        blur ? KeyboardTheme.BackgroundType.TRANSPARENT_BLUR
                                : (bg1 == bg2 ? KeyboardTheme.BackgroundType.SOLID : KeyboardTheme.BackgroundType.GRADIENT),
                        glass ? KeyboardTheme.KeyStyleType.GLASS_CRYSTAL : KeyboardTheme.KeyStyleType.ELEVATED,
                        bg1, bg2, keyBg, accent, accent,
                        Color.WHITE, Color.WHITE,
                        glass ? accent : Color.argb(60, 255, 255, 255),
                        accent, bg1
                ));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static KeyboardTheme saveCustomTheme(Context context, String name, int bg1, int bg2,
                                                int keyBg, int accent, boolean glass, boolean blur) {
        KeyboardPrefs prefs = new KeyboardPrefs(context);
        String id = "custom_" + UUID.randomUUID().toString().substring(0, 8);
        try {
            JSONArray arr = new JSONArray(prefs.getCustomThemesJson());
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", name.trim().isEmpty() ? "ثيمي المخصص" : name.trim());
            o.put("bg1", bg1);
            o.put("bg2", bg2);
            o.put("keyBg", keyBg);
            o.put("accent", accent);
            o.put("glass", glass);
            o.put("blur", blur);
            arr.put(o);
            prefs.setCustomThemesJson(arr.toString());
        } catch (Exception ignored) {
        }
        return getThemeById(context, id);
    }

    public static KeyboardTheme getThemeById(Context context, String id) {
        for (KeyboardTheme t : getAllThemes(context)) {
            if (t.id.equals(id)) return t;
        }
        return getBuiltInThemes().get(0);
    }
}
