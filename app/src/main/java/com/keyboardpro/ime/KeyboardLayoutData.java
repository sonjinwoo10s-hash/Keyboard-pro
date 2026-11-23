package com.keyboardpro.ime;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines complete multi-language keyboard layouts:
 * - Arabic (ar: default unified, iphone, swiftkey, pc, arabic2)
 * - English (en: qwerty, azerty, qwertz, colemak)
 * - French (fr: AZERTY + French diacritics)
 * - Spanish (es: QWERTY with ñ + Spanish diacritics)
 * - German (de: QWERTZ with ü/ö/ä + ß)
 * - Russian (ru: ЙЦУКЕН Cyrillic)
 * Plus Numbers/Symbols, optional Number Row / Emoji Row, and long-press popups.
 */
public final class KeyboardLayoutData {

    public static final int CODE_SHIFT = -1;
    public static final int CODE_BACKSPACE = -2;
    public static final int CODE_MODE_SYMBOLS = -3;
    public static final int CODE_MODE_ALPHA = -4;
    public static final int CODE_SPACE = -5;
    public static final int CODE_ENTER = -6;
    public static final int CODE_LANG_SWITCH = -7;
    public static final int CODE_EMOJI = -8;
    public static final int CODE_SYMBOLS_PAGE2 = -9;

    public static class KeyItem {
        public final String label;
        public final String output;
        public final String secondaryLabel;
        public final int code;
        public final float weight;
        public final boolean isSpecial;
        public final boolean isAccent;
        public final String[] popupChars;

        public KeyItem(String label, String output, String secondaryLabel, int code,
                       float weight, boolean isSpecial, boolean isAccent, String[] popupChars) {
            this.label = label;
            this.output = output;
            this.secondaryLabel = secondaryLabel;
            this.code = code;
            this.weight = weight;
            this.isSpecial = isSpecial;
            this.isAccent = isAccent;
            this.popupChars = popupChars;
        }

        public static KeyItem ch(String c, float weight, String sec, String[] popups) {
            return new KeyItem(c, c, sec, 0, weight, false, false, popups);
        }

        public static KeyItem special(String label, int code, float weight, boolean accent) {
            return new KeyItem(label, "", "", code, weight, true, accent, null);
        }
    }

    private static final String[] NUMBER_ROW = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    private static final String[] QUICK_EMOJI_ROW = {"😂", "❤️", "👍", "🔥", "😍", "🙏", "😊", "✨", "🎉", "👌"};

    private static final Map<String, String[]> POPUP_MAP = new HashMap<>();

    static {
        // Arabic diacritics & letter variants + Date/Time special long-press tokens
        POPUP_MAP.put("ا", new String[]{"أ", "إ", "آ", "ٱ", "ء", "اً"});
        POPUP_MAP.put("ت", new String[]{"التاريخ", "ة", "ث", "تَ", "تُ", "تِ", "تْ"});
        POPUP_MAP.put("ه", new String[]{"الهجري", "ة", "ھ", "هَ", "هُ", "هِ"});
        POPUP_MAP.put("و", new String[]{"الوقت", "ؤ", "وَ", "وُ", "وِ", "وْ"});
        POPUP_MAP.put("ي", new String[]{"ى", "ئ", "يَ", "يُ", "يِ", "يْ"});
        POPUP_MAP.put("ك", new String[]{"گ", "ڪ", "كَ", "كُ", "كِ"});
        POPUP_MAP.put("ة", new String[]{"ه", "ت", "ةً", "ةٌ", "ةٍ"});
        POPUP_MAP.put("ى", new String[]{"ي", "ئ", "أ"});
        POPUP_MAP.put("ر", new String[]{"رَ", "رُ", "رِ", "رْ", "رّ"});
        POPUP_MAP.put("ل", new String[]{"لا", "لأ", "لإ", "لآ", "لَ", "لُ", "لِ"});
        POPUP_MAP.put("د", new String[]{"ذ", "دَ", "دُ", "دِ", "دْ"});
        POPUP_MAP.put("ب", new String[]{"پ", "بَ", "بُ", "بِ", "بْ"});
        POPUP_MAP.put("ف", new String[]{"ڤ", "فَ", "فُ", "فِ"});
        POPUP_MAP.put("ج", new String[]{"چ", "جَ", "جُ", "جِ"});
        POPUP_MAP.put("ض", new String[]{"ضَ", "ضُ", "ضِ", "ضْ", "َ", "ُ", "ِ", "ْ", "ّ", "ً", "ٌ", "ٍ"});
        POPUP_MAP.put("س", new String[]{"سَ", "سُ", "سِ", "سْ"});
        POPUP_MAP.put("ش", new String[]{"شَ", "شُ", "شِ", "شْ"});
        POPUP_MAP.put("م", new String[]{"مَ", "مُ", "مِ", "مْ"});
        POPUP_MAP.put("ن", new String[]{"نَ", "نُ", "نِ", "نْ"});
        POPUP_MAP.put("ء", new String[]{"ئ", "ؤ", "أ", "إ", "آ", "َ", "ً", "ُ", "ٌ", "ِ", "ٍ", "ْ", "ّ"});
        POPUP_MAP.put(".", new String[]{"؟", "!", "،", "؛", ":", "...", "-", "(", ")", "«", "»"});
        POPUP_MAP.put("،", new String[]{"؟", "؛", "!", "ـ", "َ", "ُ", "ِ", "ّ", "ْ"});
        POPUP_MAP.put(",", new String[]{"!", "?", ";", ":", "-", "'"});

        // Latin accents (English / French / Spanish / German)
        POPUP_MAP.put("a", new String[]{"à", "á", "â", "ä", "æ", "å", "@"});
        POPUP_MAP.put("e", new String[]{"é", "è", "ê", "ë", "€"});
        POPUP_MAP.put("i", new String[]{"í", "ì", "î", "ï"});
        POPUP_MAP.put("o", new String[]{"ó", "ò", "ô", "ö", "œ", "ø"});
        POPUP_MAP.put("u", new String[]{"ú", "ù", "û", "ü"});
        POPUP_MAP.put("s", new String[]{"ß", "š", "$"});
        POPUP_MAP.put("c", new String[]{"ç", "ć", "č", "©"});
        POPUP_MAP.put("n", new String[]{"ñ", "ń"});
        POPUP_MAP.put("y", new String[]{"ÿ", "ý"});
        // Cyrillic popups
        POPUP_MAP.put("е", new String[]{"ё"});
        POPUP_MAP.put("ь", new String[]{"ъ"});
    }

    private KeyboardLayoutData() {
    }

    public static String getLanguageDisplayName(String langCode) {
        if (langCode == null) return "العربية";
        switch (langCode) {
            case "ar": return "العربية";
            case "en": return "English";
            case "fr": return "Français";
            case "es": return "Español";
            case "de": return "Deutsch";
            case "ru": return "Русский";
            default: return "English";
        }
    }

    public static String getLanguageSpaceLabel(String langCode) {
        return "‹ " + getLanguageDisplayName(langCode) + " ›";
    }

    public static String getLanguageSettingsLabel(String langCode) {
        switch (langCode) {
            case "ar": return "العربية (Arabic)";
            case "en": return "English (الإنجليزية)";
            case "fr": return "Français (الفرنسية — AZERTY)";
            case "es": return "Español (الإسبانية — QWERTY Ñ)";
            case "de": return "Deutsch (الألمانية — QWERTZ)";
            case "ru": return "Русский (الروسية — ЙЦУКЕН)";
            default: return langCode;
        }
    }

    public static List<List<KeyItem>> buildRowsForLanguage(String langCode,
                                                           boolean showNumbers,
                                                           boolean showEmojiRow,
                                                           boolean shifted,
                                                           String arabicLayoutType,
                                                           String englishLayoutType,
                                                           String customArabicJson) {
        String lang = langCode == null ? "ar" : langCode;
        switch (lang) {
            case "ar":
                return buildArabicRows(showNumbers, showEmojiRow, arabicLayoutType, customArabicJson);
            case "fr":
                return buildLatinOrCyrillicRows(showNumbers, showEmojiRow, shifted,
                        new String[]{"a", "z", "e", "r", "t", "y", "u", "i", "o", "p"},
                        new String[]{"q", "s", "d", "f", "g", "h", "j", "k", "l", "m"},
                        new String[]{"w", "x", "c", "v", "b", "n", "'"},
                        getLanguageSpaceLabel("fr"), false);
            case "es":
                return buildLatinOrCyrillicRows(showNumbers, showEmojiRow, shifted,
                        new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
                        new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"},
                        new String[]{"z", "x", "c", "v", "b", "n", "m"},
                        getLanguageSpaceLabel("es"), false);
            case "de":
                return buildLatinOrCyrillicRows(showNumbers, showEmojiRow, shifted,
                        new String[]{"q", "w", "e", "r", "t", "z", "u", "i", "o", "p", "ü"},
                        new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l", "ö", "ä"},
                        new String[]{"y", "x", "c", "v", "b", "n", "m", "ß"},
                        getLanguageSpaceLabel("de"), false);
            case "ru":
                return buildLatinOrCyrillicRows(showNumbers, showEmojiRow, shifted,
                        new String[]{"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"},
                        new String[]{"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"},
                        new String[]{"я", "ч", "с", "м", "и", "т", "ь", "б", "ю"},
                        getLanguageSpaceLabel("ru"), false);
            case "en":
            default:
                return buildEnglishRows(showNumbers, showEmojiRow, shifted, englishLayoutType);
        }
    }

    public static List<List<KeyItem>> buildArabicRows(boolean showNumbers, boolean showEmojiRow,
                                                      String arabicLayoutType, String customLayoutJson) {
        List<List<KeyItem>> rows = new ArrayList<>();
        if (showEmojiRow) {
            rows.add(buildSimpleRow(QUICK_EMOJI_ROW, null));
        } else if (showNumbers) {
            rows.add(buildSimpleRow(NUMBER_ROW, null));
        }

        if (customLayoutJson != null && !customLayoutJson.trim().isEmpty()) {
            List<List<KeyItem>> parsed = tryParseCustomArabic(customLayoutJson);
            if (!parsed.isEmpty()) {
                rows.addAll(parsed);
                rows.add(buildBottomRow(getLanguageSpaceLabel("ar"), true));
                return rows;
            }
        }

        String type = arabicLayoutType == null ? "default" : arabicLayoutType;
        if ("iphone".equals(type)) {
            String[] r1 = {"ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"};
            String[] r2 = {"ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"};
            String[] r3 = {"ذ", "ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ"};
            rows.add(buildSimpleRow(r1, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ""}));
            rows.add(buildSimpleRow(r2, null));
            List<KeyItem> row3 = new ArrayList<>();
            row3.add(KeyItem.ch("د", 1.0f, null, POPUP_MAP.get("د")));
            for (String s : r3) row3.add(KeyItem.ch(s, 1.0f, hintFor(s), POPUP_MAP.get(s)));
            row3.add(KeyItem.special("⌫", CODE_BACKSPACE, 1.35f, false));
            rows.add(row3);
            rows.add(buildBottomRow(getLanguageSpaceLabel("ar"), true));
            return rows;
        }

        if ("swiftkey".equals(type)) {
            String[] r1 = {"ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د"};
            String[] r2 = {"ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"};
            String[] r3 = {"ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ"};
            rows.add(buildSimpleRow(r1, null));
            rows.add(buildSimpleRow(r2, null));
            List<KeyItem> row3 = new ArrayList<>();
            row3.add(KeyItem.ch("ذ", 1.0f, null, POPUP_MAP.get("د")));
            for (String s : r3) row3.add(KeyItem.ch(s, 1.0f, hintFor(s), POPUP_MAP.get(s)));
            row3.add(KeyItem.special("⌫", CODE_BACKSPACE, 1.4f, false));
            rows.add(row3);
            rows.add(buildBottomRow(getLanguageSpaceLabel("ar"), true));
            return rows;
        }

        if ("pc".equals(type)) {
            String[] r1 = {"ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د"};
            String[] r2 = {"ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط", "ذ"};
            String[] r3 = {"ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ"};
            rows.add(buildSimpleRow(r1, null));
            rows.add(buildSimpleRow(r2, null));
            List<KeyItem> row3 = new ArrayList<>();
            for (String s : r3) row3.add(KeyItem.ch(s, 1.0f, hintFor(s), POPUP_MAP.get(s)));
            row3.add(KeyItem.special("⌫", CODE_BACKSPACE, 1.5f, false));
            rows.add(row3);
            rows.add(buildBottomRow(getLanguageSpaceLabel("ar"), true));
            return rows;
        }

        if ("arabic2".equals(type)) {
            String[] r1 = {"ج", "ح", "خ", "ه", "ع", "غ", "ف", "ق", "ث", "ص", "ض"};
            String[] r2 = {"ط", "ك", "م", "ن", "ت", "ا", "ل", "ب", "ي", "س", "ش"};
            String[] r3 = {"ظ", "ز", "و", "ة", "ى", "لا", "ر", "ؤ", "ء", "ئ"};
            rows.add(buildSimpleRow(r1, null));
            rows.add(buildSimpleRow(r2, null));
            List<KeyItem> row3 = new ArrayList<>();
            row3.add(KeyItem.ch("د", 1.0f, null, POPUP_MAP.get("د")));
            for (String s : r3) row3.add(KeyItem.ch(s, 1.0f, hintFor(s), POPUP_MAP.get(s)));
            row3.add(KeyItem.special("⌫", CODE_BACKSPACE, 1.4f, false));
            rows.add(row3);
            rows.add(buildBottomRow(getLanguageSpaceLabel("ar"), true));
            return rows;
        }

        // DEFAULT UNIFIED ARABIC LAYOUT
        String[] r1 = {"ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"};
        String[] r2 = {"ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"};
        String[] r3 = {"ذ", "ء", "ؤ", "ر", "ى", "ة", "و", "ز", "ظ", "د"};

        rows.add(buildSimpleRow(r1, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ""}));
        rows.add(buildSimpleRow(r2, null));

        List<KeyItem> row3 = new ArrayList<>();
        for (String s : r3) {
            row3.add(KeyItem.ch(s, 1.0f, hintFor(s), POPUP_MAP.get(s)));
        }
        row3.add(KeyItem.special("⌫", CODE_BACKSPACE, 1.45f, false));
        rows.add(row3);

        rows.add(buildBottomRow(getLanguageSpaceLabel("ar"), true));
        return rows;
    }

    public static List<List<KeyItem>> buildEnglishRows(boolean showNumbers, boolean showEmojiRow,
                                                       boolean shifted, String englishLayoutType) {
        String type = englishLayoutType == null ? "qwerty" : englishLayoutType;
        String[] r1, r2, r3;
        switch (type) {
            case "azerty":
                r1 = new String[]{"a", "z", "e", "r", "t", "y", "u", "i", "o", "p"};
                r2 = new String[]{"q", "s", "d", "f", "g", "h", "j", "k", "l", "m"};
                r3 = new String[]{"w", "x", "c", "v", "b", "n"};
                break;
            case "qwertz":
                r1 = new String[]{"q", "w", "e", "r", "t", "z", "u", "i", "o", "p"};
                r2 = new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"};
                r3 = new String[]{"y", "x", "c", "v", "b", "n", "m"};
                break;
            case "colemak":
                r1 = new String[]{"q", "w", "f", "p", "g", "j", "l", "u", "y", ";"};
                r2 = new String[]{"a", "r", "s", "t", "d", "h", "n", "e", "i", "o"};
                r3 = new String[]{"z", "x", "c", "v", "b", "k", "m"};
                break;
            default:
                r1 = new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
                r2 = new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"};
                r3 = new String[]{"z", "x", "c", "v", "b", "n", "m"};
                break;
        }
        return buildLatinOrCyrillicRows(showNumbers, showEmojiRow, shifted, r1, r2, r3,
                getLanguageSpaceLabel("en"), false);
    }

    private static List<List<KeyItem>> buildLatinOrCyrillicRows(boolean showNumbers, boolean showEmojiRow,
                                                                boolean shifted, String[] r1, String[] r2,
                                                                String[] r3, String spaceLabel, boolean isArabic) {
        List<List<KeyItem>> rows = new ArrayList<>();
        if (showEmojiRow) {
            rows.add(buildSimpleRow(QUICK_EMOJI_ROW, null));
        } else if (showNumbers) {
            rows.add(buildSimpleRow(NUMBER_ROW, null));
        }

        String[] numHints = r1.length == 10 ? NUMBER_ROW : null;
        rows.add(buildCaseRow(r1, shifted, numHints));
        rows.add(buildCaseRow(r2, shifted, null));

        List<KeyItem> row3 = new ArrayList<>();
        row3.add(KeyItem.special(shifted ? "⬆" : "⇧", CODE_SHIFT, 1.45f, shifted));
        for (String s : r3) {
            String out = shifted ? upperFor(s) : s;
            row3.add(KeyItem.ch(out, 1.0f, null, POPUP_MAP.get(s)));
        }
        row3.add(KeyItem.special("⌫", CODE_BACKSPACE, 1.45f, false));
        rows.add(row3);

        rows.add(buildBottomRow(spaceLabel, isArabic));
        return rows;
    }

    public static List<List<KeyItem>> buildSymbolRows(boolean page2, String activeLangCode) {
        boolean isArabic = "ar".equals(activeLangCode);
        List<List<KeyItem>> rows = new ArrayList<>();
        String[] r1 = page2
                ? new String[]{"~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"}
                : new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        String[] r2 = page2
                ? new String[]{"£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"}
                : new String[]{"@", "#", "$", "_", "&", "-", "+", "(", ")", "/"};
        String[] r3 = page2
                ? new String[]{"%", "©", "®", "™", "✓", "[", "]", "<", ">"}
                : new String[]{"*", "\"", "'", ":", ";", "!", "؟", "«", "»"};

        rows.add(buildSimpleRow(r1, null));
        rows.add(buildSimpleRow(r2, null));

        List<KeyItem> row3 = new ArrayList<>();
        row3.add(KeyItem.special(page2 ? "1/2" : "=\\<", CODE_SYMBOLS_PAGE2, 1.4f, false));
        for (String s : r3) row3.add(KeyItem.ch(s, 1.0f, null, null));
        row3.add(KeyItem.special("⌫", CODE_BACKSPACE, 1.4f, false));
        rows.add(row3);

        List<KeyItem> bottom = new ArrayList<>();
        bottom.add(KeyItem.special(isArabic ? "أ ب" : ("ru".equals(activeLangCode) ? "АБВ" : "ABC"),
                CODE_MODE_ALPHA, 1.35f, false));
        bottom.add(KeyItem.ch(isArabic ? "،" : ",", 0.9f, null, POPUP_MAP.get(isArabic ? "،" : ",")));
        bottom.add(KeyItem.special("😊", CODE_EMOJI, 0.95f, false));
        bottom.add(new KeyItem(getLanguageSpaceLabel(activeLangCode), " ", "", CODE_SPACE, 3.8f, false, false, null));
        bottom.add(KeyItem.ch(".", 0.9f, null, POPUP_MAP.get(".")));
        bottom.add(KeyItem.special("↵", CODE_ENTER, 1.55f, true));
        rows.add(bottom);
        return rows;
    }

    private static List<KeyItem> buildBottomRow(String spaceLabel, boolean isArabic) {
        List<KeyItem> bottom = new ArrayList<>();
        bottom.add(KeyItem.special("?123", CODE_MODE_SYMBOLS, 1.35f, false));
        bottom.add(KeyItem.ch(isArabic ? "،" : ",", 0.9f, isArabic ? "؟" : "!", POPUP_MAP.get(isArabic ? "،" : ",")));
        bottom.add(KeyItem.special("😊", CODE_EMOJI, 0.95f, false));
        bottom.add(new KeyItem(spaceLabel, " ", "", CODE_SPACE, 3.85f, false, false, null));
        bottom.add(KeyItem.ch(".", 0.9f, isArabic ? "؟" : "!", POPUP_MAP.get(".")));
        bottom.add(KeyItem.special("↵", CODE_ENTER, 1.55f, true));
        return bottom;
    }

    private static String upperFor(String s) {
        if ("ß".equals(s)) return "ẞ";
        return s.toUpperCase();
    }

    private static String hintFor(String key) {
        if ("ت".equals(key)) return "تاريخ";
        if ("ه".equals(key)) return "هجري";
        if ("و".equals(key)) return "وقت";
        return null;
    }

    private static List<KeyItem> buildSimpleRow(String[] labels, String[] hints) {
        List<KeyItem> row = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            String s = labels[i];
            String h = (hints != null && i < hints.length && !hints[i].isEmpty()) ? hints[i] : hintFor(s);
            row.add(KeyItem.ch(s, 1.0f, h, POPUP_MAP.get(s)));
        }
        return row;
    }

    private static List<KeyItem> buildCaseRow(String[] labels, boolean shifted, String[] hints) {
        List<KeyItem> row = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            String raw = labels[i];
            String out = shifted ? upperFor(raw) : raw;
            String h = (hints != null && i < hints.length) ? hints[i] : null;
            row.add(KeyItem.ch(out, 1.0f, h, POPUP_MAP.get(raw)));
        }
        return row;
    }

    private static List<List<KeyItem>> tryParseCustomArabic(String json) {
        List<List<KeyItem>> rows = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int r = 0; r < arr.length() && r < 3; r++) {
                String line = arr.getString(r).trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                List<KeyItem> row = new ArrayList<>();
                for (String p : parts) row.add(KeyItem.ch(p, 1.0f, hintFor(p), POPUP_MAP.get(p)));
                if (r == 2) row.add(KeyItem.special("⌫", CODE_BACKSPACE, 1.45f, false));
                rows.add(row);
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
        return rows;
    }
}
