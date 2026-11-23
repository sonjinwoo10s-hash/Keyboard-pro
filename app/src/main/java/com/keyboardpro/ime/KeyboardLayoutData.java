package com.keyboardpro.ime;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines Arabic (Standard & iPhone layouts), English QWERTY, Numbers/Symbols,
 * customizable Arabic key overrides, and long-press alternate popups.
 */
public final class KeyboardLayoutData {

    public static final int CODE_SHIFT = -1;
    public static final int CODE_DELETE = -5;
    public static final int CODE_ENTER = -4;
    public static final int CODE_SPACE = 32;
    public static final int CODE_MODE_SYMBOLS = -2;
    public static final int CODE_MODE_SYMBOLS_MORE = -7;
    public static final int CODE_SWITCH_LANG = -101;
    public static final int CODE_EMOJI = -102;

    public enum KeyboardMode {
        ARABIC,
        ENGLISH,
        SYMBOLS_1,
        SYMBOLS_2
    }

    public static class KeySpec {
        public final String label;
        public final String output;
        public final int code;
        public final float weight;
        public final boolean isSpecial;
        public final String[] longPressAlts;

        public KeySpec(String label, String output, int code, float weight, boolean isSpecial, String[] longPressAlts) {
            this.label = label;
            this.output = output;
            this.code = code;
            this.weight = weight;
            this.isSpecial = isSpecial;
            this.longPressAlts = longPressAlts;
        }

        public static KeySpec letter(String ch) {
            return new KeySpec(ch, ch, ch.codePointAt(0), 1f, false, LONG_PRESS_MAP.get(ch));
        }

        public static KeySpec special(String label, int code, float weight) {
            return new KeySpec(label, "", code, weight, true, null);
        }
    }

    private static final Map<String, String[]> LONG_PRESS_MAP = new HashMap<>();

    static {
        LONG_PRESS_MAP.put("ا", new String[]{"أ", "إ", "آ", "ى", "ٱ"});
        LONG_PRESS_MAP.put("ه", new String[]{"ة", "ـه"});
        LONG_PRESS_MAP.put("ي", new String[]{"ى", "ئ", "يـ"});
        LONG_PRESS_MAP.put("و", new String[]{"ؤ"});
        LONG_PRESS_MAP.put("ت", new String[]{"ة"});
        LONG_PRESS_MAP.put("ل", new String[]{"لا", "لأ", "لإ", "لآ"});
        LONG_PRESS_MAP.put("ك", new String[]{"گ", "ڪ"});
        LONG_PRESS_MAP.put("ب", new String[]{"پ"});
        LONG_PRESS_MAP.put("ج", new String[]{"چ"});
        LONG_PRESS_MAP.put("ف", new String[]{"ڤ"});
        LONG_PRESS_MAP.put("a", new String[]{"@", "á", "à", "â", "ä"});
        LONG_PRESS_MAP.put("e", new String[]{"3", "é", "è", "ê"});
        LONG_PRESS_MAP.put("i", new String[]{"8", "í", "î"});
        LONG_PRESS_MAP.put("o", new String[]{"9", "ó", "ô", "ö"});
        LONG_PRESS_MAP.put("u", new String[]{"7", "ú", "ü"});
        LONG_PRESS_MAP.put(".", new String[]{"،", "؟", "!", ":", ";", "…"});
    }

    public static final String[][] DEFAULT_ARABIC_ROWS = {
            {"ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"},
            {"ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"},
            {"ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ"}
    };

    public static final String[][] IPHONE_ARABIC_ROWS = {
            {"ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"},
            {"ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "د"},
            {"ذ", "ط", "ز", "و", "ة", "ى", "ر", "ؤ", "ء", "ئ"}
    };

    public static List<List<KeySpec>> buildRows(
            KeyboardMode mode,
            boolean shifted,
            boolean showNumbersRow,
            boolean iphoneLayout,
            String customArabicJson
    ) {
        List<List<KeySpec>> rows = new ArrayList<>();

        if (showNumbersRow && (mode == KeyboardMode.ARABIC || mode == KeyboardMode.ENGLISH)) {
            List<KeySpec> numRow = new ArrayList<>();
            String[] nums = mode == KeyboardMode.ARABIC
                    ? new String[]{"١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "٠"}
                    : new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
            for (String n : nums) {
                numRow.add(new KeySpec(n, n, n.codePointAt(0), 1f, false, null));
            }
            rows.add(numRow);
        }

        if (mode == KeyboardMode.ARABIC) {
            String[][] base = parseCustomArabicOrDefault(customArabicJson, iphoneLayout);
            rows.add(toKeyRow(base[0]));
            rows.add(toKeyRow(base[1]));

            List<KeySpec> row3 = new ArrayList<>();
            row3.add(KeySpec.special(shifted ? "َُِ" : "د", shifted ? CODE_SHIFT : 'د', 1.2f));
            for (String s : base[2]) {
                String ch = shifted ? getShiftedArabic(s) : s;
                row3.add(KeySpec.letter(ch));
            }
            row3.add(KeySpec.special("⌫", CODE_DELETE, 1.45f));
            rows.add(row3);

            rows.add(buildBottomRow("١٢٣", "العربية — كيبورد برو", "،", "."));
        } else if (mode == KeyboardMode.ENGLISH) {
            String[] r1 = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
            String[] r2 = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
            String[] r3 = {"z", "x", "c", "v", "b", "n", "m"};

            rows.add(toEnglishRow(r1, shifted));
            rows.add(toEnglishRow(r2, shifted));

            List<KeySpec> row3 = new ArrayList<>();
            row3.add(KeySpec.special(shifted ? "⬆" : "⇧", CODE_SHIFT, 1.45f));
            for (String s : r3) {
                String ch = shifted ? s.toUpperCase() : s;
                row3.add(KeySpec.letter(ch));
            }
            row3.add(KeySpec.special("⌫", CODE_DELETE, 1.45f));
            rows.add(row3);

            rows.add(buildBottomRow("123", "English — Keyboard Pro", ",", "."));
        } else if (mode == KeyboardMode.SYMBOLS_1) {
            rows.add(toKeyRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}));
            rows.add(toKeyRow(new String[]{"@", "#", "$", "_", "&", "-", "+", "(", ")", "/"}));
            List<KeySpec> r3 = new ArrayList<>();
            r3.add(KeySpec.special("=\\<", CODE_MODE_SYMBOLS_MORE, 1.45f));
            for (String s : new String[]{"*", "\"", "'", ":", ";", "!", "؟"}) {
                r3.add(KeySpec.letter(s));
            }
            r3.add(KeySpec.special("⌫", CODE_DELETE, 1.45f));
            rows.add(r3);
            rows.add(buildBottomRow("أبج", "مسافة", "،", "."));
        } else {
            rows.add(toKeyRow(new String[]{"~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"}));
            rows.add(toKeyRow(new String[]{"£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"}));
            List<KeySpec> r3 = new ArrayList<>();
            r3.add(KeySpec.special("123", CODE_MODE_SYMBOLS, 1.45f));
            for (String s : new String[]{"%", "©", "®", "™", "✓", "[", "]"}) {
                r3.add(KeySpec.letter(s));
            }
            r3.add(KeySpec.special("⌫", CODE_DELETE, 1.45f));
            rows.add(r3);
            rows.add(buildBottomRow("أبج", "مسافة", "꧁", "꧂"));
        }

        return rows;
    }

    private static List<KeySpec> buildBottomRow(String modeLabel, String spaceLabel, String leftPunct, String rightPunct) {
        List<KeySpec> bottom = new ArrayList<>();
        bottom.add(KeySpec.special(modeLabel, CODE_MODE_SYMBOLS, 1.35f));
        bottom.add(KeySpec.special("🌐", CODE_SWITCH_LANG, 1.05f));
        bottom.add(KeySpec.special("☺", CODE_EMOJI, 1.05f));
        bottom.add(KeySpec.letter(leftPunct));
        bottom.add(new KeySpec(spaceLabel, " ", CODE_SPACE, 4.1f, false, null));
        bottom.add(KeySpec.letter(rightPunct));
        bottom.add(KeySpec.special("↵", CODE_ENTER, 1.55f));
        return bottom;
    }

    private static List<KeySpec> toKeyRow(String[] chars) {
        List<KeySpec> row = new ArrayList<>();
        for (String c : chars) row.add(KeySpec.letter(c));
        return row;
    }

    private static List<KeySpec> toEnglishRow(String[] chars, boolean shifted) {
        List<KeySpec> row = new ArrayList<>();
        for (String c : chars) {
            row.add(KeySpec.letter(shifted ? c.toUpperCase() : c));
        }
        return row;
    }

    private static String getShiftedArabic(String s) {
        switch (s) {
            case "ا": return "أ";
            case "و": return "ؤ";
            case "ي": return "ئ";
            case "ه": return "ة";
            case "لا": return "لأ";
            default: return s;
        }
    }

    public static String[][] parseCustomArabicOrDefault(String json, boolean iphoneLayout) {
        String[][] fallback = iphoneLayout ? IPHONE_ARABIC_ROWS : DEFAULT_ARABIC_ROWS;
        if (json == null || json.trim().isEmpty()) return fallback;
        try {
            JSONArray outer = new JSONArray(json);
            if (outer.length() != 3) return fallback;
            String[][] custom = new String[3][];
            for (int r = 0; r < 3; r++) {
                JSONArray inner = outer.getJSONArray(r);
                custom[r] = new String[inner.length()];
                for (int c = 0; c < inner.length(); c++) {
                    custom[r][c] = inner.getString(c);
                }
            }
            return custom;
        } catch (Exception e) {
            return fallback;
        }
    }
}
