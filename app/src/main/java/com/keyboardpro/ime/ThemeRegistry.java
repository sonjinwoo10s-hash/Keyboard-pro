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
 * Registry of 42 built-in unlocked themes (Crystal Glass series, Material You,
 * iOS / iOS 27 / Google, Mechanical, Neon/RGB, Terminal, Pencil Sketch, Chessboard,
 * Gradients, Special AMOLED/Coffee/Aura) + User Theme Editor custom themes.
 */
public final class ThemeRegistry {

    private ThemeRegistry() {
    }

    private static int c(String hex) {
        return Color.parseColor(hex);
    }

    private static KeyboardTheme crystal(String id, String ar, String en, String tags,
                                         String bg1, String bg2, String accent, boolean light) {
        int keyBg = light ? c("#55FFFFFF") : c("#36263A55");
        int keySpec = Color.argb(90, Color.red(c(accent)), Color.green(c(accent)), Color.blue(c(accent)));
        int txt = light ? c("#0F172A") : c("#F8FAFC");
        return new KeyboardTheme(
                id, ar, en, tags + " زجاج كريستال glass crystal",
                KeyboardTheme.FILTER_GLASS, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                c(bg1), c(bg2), keyBg, keySpec, c(accent), txt, c(accent),
                Color.argb(175, Color.red(c(accent)), Color.green(c(accent)), Color.blue(c(accent))),
                c(accent), c(bg1), ""
        );
    }

    public static List<KeyboardTheme> getBuiltInThemes() {
        List<KeyboardTheme> list = new ArrayList<>(44);

        // --- 1. CRYSTAL GLASS SERIES (9 themes) ---
        list.add(crystal("crystal_blue", "كريستال أزرق", "Crystal Blue", "أزرق blue indigo", "#081228", "#112554", "#4E6BFF", false));
        list.add(crystal("crystal_purple", "كريستال بنفسجي", "Crystal Purple", "بنفسجي purple violet", "#150929", "#2B1154", "#A855F7", false));
        list.add(crystal("crystal_pink", "كريستال وردي", "Crystal Pink", "وردي pink rose", "#240A1E", "#4A123E", "#EC4899", false));
        list.add(crystal("crystal_green", "كريستال أخضر", "Crystal Green", "أخضر green emerald", "#061F17", "#0D3D2E", "#10B981", false));
        list.add(crystal("crystal_turquoise", "كريستال فيروزي", "Crystal Turquoise", "فيروزي سماوي cyan teal", "#061C24", "#0C3847", "#06B6D4", false));
        list.add(crystal("crystal_gold", "كريستال ذهبي", "Crystal Gold", "ذهبي gold amber", "#1C1406", "#38280B", "#F5C542", false));
        list.add(crystal("crystal_red", "كريستال أحمر", "Crystal Red", "أحمر red crimson", "#24080C", "#4A1118", "#EF4444", false));
        list.add(crystal("crystal_black", "كريستال أسود", "Crystal Black", "أسود داكن black obsidian", "#050507", "#141418", "#94A3B8", false));
        list.add(crystal("crystal_light", "كريستال فاتح", "Crystal Light", "أبيض فاتح light frosted", "#DCE6F7", "#EEF4FF", "#4E6BFF", true));

        // --- 2. MATERIAL YOU & APP-COLOR GLASS (6 themes) ---
        list.add(new KeyboardTheme("mat_you_auto", "ماتيريال يو تلقائي", "Material You Auto", "ماتيريال تلقائي material auto",
                KeyboardTheme.FILTER_MATERIAL, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.AUTO_SPLIT, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                c("#141720"), c("#E6ECFA"), c("#23293A"), c("#4E6BFF"), c("#6B83FF"),
                c("#F8FAFC"), c("#FFFFFF"), c("#334E6BFF"), c("#4E6BFF"), c("#10131C"), ""));
        list.add(new KeyboardTheme("mat_you_dark", "ماتيريال يو داكن", "Material You Dark", "ماتيريال داكن أزرق material dark",
                KeyboardTheme.FILTER_MATERIAL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                c("#11131A"), c("#11131A"), c("#1E2230"), c("#303854"), c("#4E6BFF"),
                c("#E6ECFF"), c("#B8C7FF"), c("#24FFFFFF"), c("#4E6BFF"), c("#0D0F15"), ""));
        list.add(new KeyboardTheme("mat_you_light", "ماتيريال يو فاتح", "Material You Light", "ماتيريال فاتح أبيض material light",
                KeyboardTheme.FILTER_MATERIAL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                c("#E7ECF7"), c("#E7ECF7"), c("#FFFFFF"), c("#D0DBF5"), c("#4E6BFF"),
                c("#141926"), c("#25376B"), c("#20000000"), c("#4E6BFF"), c("#EEF2FA"), ""));
        list.add(new KeyboardTheme("app_color_glass_auto", "زجاج لون التطبيق تلقائي", "App-Color Glass Auto", "زجاج ماتيريال تلقائي blur",
                KeyboardTheme.FILTER_GLASS, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.TRANSPARENT_BLUR, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                c("#CC101424"), c("#CC1C2645"), c("#40FFFFFF"), c("#554E6BFF"), c("#884E6BFF"),
                c("#FFFFFF"), c("#C7D4FF"), c("#88A5B4FC"), c("#4E6BFF"), c("#CC0D1120"), ""));
        list.add(new KeyboardTheme("app_color_glass_dark", "زجاج لون التطبيق داكن", "App-Color Glass Dark", "زجاج داكن ضبابي blur dark",
                KeyboardTheme.FILTER_GLASS, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.TRANSPARENT_BLUR, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                c("#D40B0E17"), c("#D4141B2D"), c("#33FFFFFF"), c("#4D4E6BFF"), c("#884E6BFF"),
                c("#F8FAFC"), c("#93C5FD"), c("#6660A5FA"), c("#4E6BFF"), c("#D4090B12"), ""));
        list.add(new KeyboardTheme("app_color_glass_light", "زجاج لون التطبيق فاتح", "App-Color Glass Light", "زجاج فاتح ضبابي blur light",
                KeyboardTheme.FILTER_GLASS, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.TRANSPARENT_BLUR, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                c("#CCF0F4FF"), c("#CCDDE6FF"), c("#88FFFFFF"), c("#554E6BFF"), c("#884E6BFF"),
                c("#0F172A"), c("#1E3A8A"), c("#884E6BFF"), c("#4E6BFF"), c("#CCE5ECFF"), ""));

        // --- 3. iOS, iOS 27 & GOOGLE (6 themes) ---
        list.add(new KeyboardTheme("ios_dark", "آيفون داكن", "iOS Dark", "آيفون داكن ios dark",
                KeyboardTheme.FILTER_DARK, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                c("#1C1C1E"), c("#1C1C1E"), c("#3A3A3C"), c("#2C2C2E"), c("#636366"),
                c("#FFFFFF"), c("#E5E5EA"), c("#18FFFFFF"), c("#0A84FF"), c("#161618"), ""));
        list.add(new KeyboardTheme("ios_light", "آيفون فاتح", "iOS Light", "آيفون أبيض فاتح ios light",
                KeyboardTheme.FILTER_LIGHT, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                c("#D1D5DB"), c("#D1D5DB"), c("#FFFFFF"), c("#AEB5C0"), c("#CBD5E1"),
                c("#0F172A"), c("#1E293B"), c("#22000000"), c("#007AFF"), c("#E2E6EC"), ""));
        list.add(new KeyboardTheme("ios_27_glass", "آيفون 27 الزجاجي", "iOS 27 Liquid Glass", "آيفون زجاج ios 27 liquid glass",
                KeyboardTheme.FILTER_GLASS, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.TRANSPARENT_BLUR, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                c("#CC12141C"), c("#CC212738"), c("#42FFFFFF"), c("#550A84FF"), c("#880A84FF"),
                c("#FFFFFF"), c("#93C5FD"), c("#AAFFFFFF"), c("#0A84FF"), c("#CC101218"), ""));
        list.add(new KeyboardTheme("google_dark", "جوجل داكن", "Google Dark", "جوجل داكن google gboard dark",
                KeyboardTheme.FILTER_MATERIAL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#1B1B1F"), c("#1B1B1F"), c("#2D2E33"), c("#24262B"), c("#4E6BFF"),
                c("#F1F3F4"), c("#BDC1C6"), c("#15FFFFFF"), c("#8AB4F8"), c("#16161A"), ""));
        list.add(new KeyboardTheme("google_light", "جوجل فاتح", "Google Light", "جوجل فاتح google gboard light",
                KeyboardTheme.FILTER_LIGHT, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#ECEFF4"), c("#ECEFF4"), c("#FFFFFF"), c("#DCE2EC"), c("#C2D4F8"),
                c("#202124"), c("#3C4043"), c("#15000000"), c("#1A73E8"), c("#F5F7FA"), ""));
        list.add(new KeyboardTheme("google_auto", "جوجل تلقائي", "Google Auto", "جوجل تلقائي google auto",
                KeyboardTheme.FILTER_MATERIAL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.AUTO_SPLIT, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#1B1B1F"), c("#ECEFF4"), c("#2D2E33"), c("#1A73E8"), c("#8AB4F8"),
                c("#F1F3F4"), c("#FFFFFF"), c("#22FFFFFF"), c("#1A73E8"), c("#18191D"), ""));

        // --- 4. MECHANICAL SERIES (4 themes) ---
        list.add(new KeyboardTheme("mech_night", "ميكانيكي ليلي", "Mechanical Night", "ميكانيكي ليلي mechanical dark",
                KeyboardTheme.FILTER_MECHANICAL, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.MECHANICAL_3D,
                c("#0E0F12"), c("#0E0F12"), c("#22252C"), c("#181B22"), c("#4E6BFF"),
                c("#F1F5F9"), c("#60A5FA"), c("#444B5A"), c("#4E6BFF"), c("#0A0B0E"), ""));
        list.add(new KeyboardTheme("mech_day", "ميكانيكي نهاري", "Mechanical Day", "ميكانيكي نهاري mechanical retro light",
                KeyboardTheme.FILTER_MECHANICAL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.MECHANICAL_3D,
                c("#D4D1C7"), c("#D4D1C7"), c("#F5F3EC"), c("#C4BFB2"), c("#F97316"),
                c("#1E1E1E"), c("#D9480F"), c("#9E9889"), c("#EA580C"), c("#CBC7BC"), ""));
        list.add(new KeyboardTheme("mech_rgb", "ميكانيكي RGB للألعاب", "Mechanical RGB", "ميكانيكي ألعاب نيون rgb gaming",
                KeyboardTheme.FILTER_MECHANICAL, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.MECHANICAL_3D,
                c("#08080D"), c("#140B24"), c("#171722"), c("#221538"), c("#00F5D4"),
                c("#00F5D4"), c("#FF2A85"), c("#FF2A85"), c("#00F5D4"), c("#06060A"), ""));
        list.add(new KeyboardTheme("mech_auto", "ميكانيكي تلقائي", "Mechanical Auto", "ميكانيكي تلقائي mechanical auto",
                KeyboardTheme.FILTER_MECHANICAL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.AUTO_SPLIT, KeyboardTheme.KeyStyleType.MECHANICAL_3D,
                c("#111318"), c("#DCD8CE"), c("#262A34"), c("#4E6BFF"), c("#6B83FF"),
                c("#F8FAFC"), c("#FFFFFF"), c("#44526B"), c("#4E6BFF"), c("#0E1015"), ""));

        // --- 5. NEON, TERMINAL, SKETCH, CHESSBOARD (6 themes) ---
        list.add(new KeyboardTheme("neon_cyber", "نيون سايبر", "Neon Cyber", "نيون أزرق وردي neon cyber",
                KeyboardTheme.FILTER_COLORFUL, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.NEON_OUTLINE,
                c("#060514"), c("#180B33"), c("#140E2B"), c("#2B1354"), c("#FF2E93"),
                c("#00F0FF"), c("#FF2E93"), c("#CC00F0FF"), c("#00F0FF"), c("#09061A"), ""));
        list.add(new KeyboardTheme("terminal_hacker", "ترمينال أخضر", "Terminal Matrix", "أخضر برمجة terminal hacker",
                KeyboardTheme.FILTER_DARK, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.NEON_OUTLINE,
                c("#020A04"), c("#020A04"), c("#071A0C"), c("#0D2E16"), c("#22C55E"),
                c("#4ADE80"), c("#86EFAC"), c("#8822C55E"), c("#22C55E"), c("#030D05"), ""));
        list.add(new KeyboardTheme("pencil_paper", "رسم بقلم رصاص ورقي", "Pencil Sketch Paper", "ورق رسم رصاص فاتح sketch paper",
                KeyboardTheme.FILTER_LIGHT, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.SKETCH_PENCIL,
                c("#FAF6EC"), c("#FAF6EC"), c("#FFFDF7"), c("#ECE5D5"), c("#D6CCB8"),
                c("#2B2A27"), c("#1F1E1C"), c("#7A756A"), c("#3B3A36"), c("#F2ECDF"), ""));
        list.add(new KeyboardTheme("pencil_night", "رسم طباشير ليلي", "Pencil Sketch Night", "رسم طباشير داكن sketch night",
                KeyboardTheme.FILTER_DARK, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.SKETCH_PENCIL,
                c("#181A1E"), c("#181A1E"), c("#22252B"), c("#2C3038"), c("#4E6BFF"),
                c("#E5E7EB"), c("#CBD5E1"), c("#9CA3AF"), c("#93C5FD"), c("#141619"), ""));
        list.add(new KeyboardTheme("pencil_auto", "رسم تلقائي", "Pencil Sketch Auto", "رسم تلقائي sketch auto",
                KeyboardTheme.FILTER_MATERIAL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.AUTO_SPLIT, KeyboardTheme.KeyStyleType.SKETCH_PENCIL,
                c("#181A1E"), c("#FAF6EC"), c("#22252B"), c("#4E6BFF"), c("#6B83FF"),
                c("#F3F4F6"), c("#FFFFFF"), c("#9CA3AF"), c("#4E6BFF"), c("#16181C"), ""));
        list.add(new KeyboardTheme("chessboard", "رقعة الشطرنج", "Chessboard", "شطرنج مربعات أسود أبيض chess",
                KeyboardTheme.FILTER_DARK, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.CHECKERED, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#101014"), c("#1E1E26"), c("#262630"), c("#4E6BFF"), c("#6B83FF"),
                c("#F8FAFC"), c("#FFFFFF"), c("#33FFFFFF"), c("#4E6BFF"), c("#0E0E12"), ""));

        // --- 6. GRADIENTS SERIES (7 themes) ---
        list.add(new KeyboardTheme("grad_forest", "غابة الزمرد", "Forest Gradient", "أخضر غابة تدرج forest green",
                KeyboardTheme.FILTER_COLORFUL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#062118"), c("#104A35"), c("#14523C"), c("#10B981"), c("#34D399"),
                c("#ECFDF5"), c("#052E23"), c("#4434D399"), c("#10B981"), c("#051A13"), ""));
        list.add(new KeyboardTheme("grad_ocean", "محيط أزرق", "Ocean Gradient", "أزرق بحر تدرج ocean blue",
                KeyboardTheme.FILTER_COLORFUL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#061E38"), c("#0E4A7B"), c("#125182"), c("#0EA5E9"), c("#38BDF8"),
                c("#F0F9FF"), c("#FFFFFF"), c("#4438BDF8"), c("#38BDF8"), c("#05182E"), ""));
        list.add(new KeyboardTheme("grad_sunset", "شفق الغروب", "Sunset Gradient", "برتقالي وردي غروب sunset orange",
                KeyboardTheme.FILTER_COLORFUL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#2D1022"), c("#6B1E3D"), c("#4A1931"), c("#F97316"), c("#FB923C"),
                c("#FFF7ED"), c("#FFFFFF"), c("#44FB923C"), c("#F97316"), c("#240C1B"), ""));
        list.add(new KeyboardTheme("grad_rainbow", "قوس قزح", "Rainbow Glow", "ملون قوس قزح rainbow colorful",
                KeyboardTheme.FILTER_COLORFUL, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                c("#2E1065"), c("#BE185D"), c("#38FFFFFF"), c("#66F59E0B"), c("#8838BDF8"),
                c("#FFFFFF"), c("#FEF08A"), c("#88FFFFFF"), c("#F59E0B"), c("#230C4F"), ""));
        list.add(new KeyboardTheme("grad_pastel", "باستيل ناعم", "Pastel Dream", "باستيل وردي سماوي فاتح pastel light",
                KeyboardTheme.FILTER_LIGHT, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                c("#FCE7F3"), c("#E0E7FF"), c("#FFFFFF"), c("#C7D2FE"), c("#A5B4FC"),
                c("#312E81"), c("#4338CA"), c("#33818CF8"), c("#6366F1"), c("#F5E1EE"), ""));
        list.add(new KeyboardTheme("grad_matcha", "ماتشا أخضر", "Matcha Latte", "أخضر ماتشا هادئ matcha green",
                KeyboardTheme.FILTER_LIGHT, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                c("#DCEAD3"), c("#C3DBB4"), c("#F7FBF4"), c("#9DC384"), c("#78A55A"),
                c("#1D3316"), c("#FFFFFF"), c("#334B7B35"), c("#5E8C42"), c("#CFE0C4"), ""));
        list.add(new KeyboardTheme("grad_marshmallow", "مارشميلو", "Marshmallow", "وردي أبيض ناعم marshmallow pink",
                KeyboardTheme.FILTER_COLORFUL, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.IPHONE_ROUNDED,
                c("#FFE4EC"), c("#FFF1F2"), c("#FFFFFF"), c("#FBCFE8"), c("#F472B6"),
                c("#831843"), c("#9D174D"), c("#33F472B6"), c("#EC4899"), c("#FCD9E4"), ""));

        // --- 7. SPECIAL AMOLED, CARBON, COFFEE, AURA (4 themes) ---
        list.add(new KeyboardTheme("pure_amoled", "أموليد أسود صافي", "Pure AMOLED", "أسود أموليد داكن amoled black",
                KeyboardTheme.FILTER_DARK, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.SOLID, KeyboardTheme.KeyStyleType.FLAT,
                c("#000000"), c("#000000"), c("#141416"), c("#1E1E24"), c("#4E6BFF"),
                c("#F8FAFC"), c("#93A5FF"), c("#26262A"), c("#4E6BFF"), c("#000000"), ""));
        list.add(new KeyboardTheme("carbon_fiber", "ألياف الكربون", "Carbon Pro", "كربون رمادي داكن carbon dark",
                KeyboardTheme.FILTER_DARK, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.CHECKERED, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#0E0E10"), c("#17171B"), c("#202026"), c("#2B2B33"), c("#4E6BFF"),
                c("#F3F4F6"), c("#9CA3AF"), c("#33FFFFFF"), c("#4E6BFF"), c("#0B0B0D"), ""));
        list.add(new KeyboardTheme("espresso_coffee", "قهوة إسبريسو", "Coffee Espresso", "بني قهوة دافئ coffee brown",
                KeyboardTheme.FILTER_DARK, KeyboardTheme.Category.BASIC, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.ELEVATED,
                c("#1D140E"), c("#332218"), c("#3E2B1F"), c("#B45309"), c("#D97706"),
                c("#FEF3C7"), c("#FFFFFF"), c("#44F59E0B"), c("#D97706"), c("#17100B"), ""));
        list.add(new KeyboardTheme("aura_indigo", "أورا إنديجو", "Aura Indigo", "أزرق بنفسجي أورا aura indigo",
                KeyboardTheme.FILTER_COLORFUL, KeyboardTheme.Category.NEW, false,
                KeyboardTheme.BackgroundType.GRADIENT, KeyboardTheme.KeyStyleType.GLASS_CRYSTAL,
                c("#0B0D1F"), c("#1E1B4B"), c("#38312E81"), c("#554E6BFF"), c("#884E6BFF"),
                c("#EEF2FF"), c("#A5B4FC"), c("#994E6BFF"), c("#4E6BFF"), c("#090B1A"), ""));

        return list;
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
                int bg1 = o.optInt("bg1", c("#141416"));
                int bg2 = o.optInt("bg2", c("#1E1E24"));
                int keyBg = o.optInt("keyBg", c("#26262C"));
                int accent = o.optInt("accent", c("#4E6BFF"));
                boolean glass = o.optBoolean("glass", false);
                boolean blur = o.optBoolean("blur", false);
                String photo = o.optString("photo", "");
                KeyboardTheme.BackgroundType bgType = !photo.isEmpty()
                        ? KeyboardTheme.BackgroundType.PHOTO
                        : (blur ? KeyboardTheme.BackgroundType.TRANSPARENT_BLUR
                        : (bg1 == bg2 ? KeyboardTheme.BackgroundType.SOLID : KeyboardTheme.BackgroundType.GRADIENT));
                list.add(new KeyboardTheme(
                        o.optString("id"),
                        o.optString("name", "ثيمي المخصص"),
                        o.optString("name", "Custom Theme"),
                        "ثيمي مخصص custom " + o.optString("name", ""),
                        glass ? KeyboardTheme.FILTER_GLASS : KeyboardTheme.FILTER_DARK,
                        KeyboardTheme.Category.MY_THEMES,
                        false,
                        bgType,
                        glass ? KeyboardTheme.KeyStyleType.GLASS_CRYSTAL : KeyboardTheme.KeyStyleType.ELEVATED,
                        bg1, bg2, keyBg, accent, accent,
                        Color.WHITE, Color.WHITE,
                        glass ? accent : Color.argb(65, 255, 255, 255),
                        accent, bg1, photo
                ));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static KeyboardTheme saveCustomTheme(Context context, String name, int bg1, int bg2,
                                                int keyBg, int accent, boolean glass, boolean blur,
                                                String photoPath) {
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
            o.put("photo", photoPath == null ? "" : photoPath);
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
