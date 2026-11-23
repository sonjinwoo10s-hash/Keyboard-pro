package com.keyboardpro.ime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Real-time Text Decoration & Multi-Font Unicode Typing Engine.
 * Transforms Arabic and English characters/words before calling InputConnection.commitText(...),
 * enabling live decorated typing and 12+ Unicode font styles in ANY Android app.
 */
public final class TextStyleEngine {

    public static final String STYLE_NORMAL = "normal";

    // Arabic Live Decoration Styles
    public static final String STYLE_AR_KASHIDA = "ar_kashida";
    public static final String STYLE_AR_TASHKEEL = "ar_tashkeel";
    public static final String STYLE_AR_ROYAL_FRAME = "ar_royal_frame";
    public static final String STYLE_AR_DIAMOND_FRAME = "ar_diamond_frame";
    public static final String STYLE_AR_SPARKLE = "ar_sparkle";
    public static final String STYLE_AR_ORNATE = "ar_ornate";

    // English Live Unicode Font Styles (12 styles)
    public static final String STYLE_EN_BOLD = "en_bold";
    public static final String STYLE_EN_ITALIC = "en_italic";
    public static final String STYLE_EN_BOLD_ITALIC = "en_bold_italic";
    public static final String STYLE_EN_SCRIPT = "en_script";
    public static final String STYLE_EN_FRAKTUR = "en_fraktur";
    public static final String STYLE_EN_DOUBLE = "en_double";
    public static final String STYLE_EN_MONO = "en_mono";
    public static final String STYLE_EN_CIRCLED = "en_circled";
    public static final String STYLE_EN_SQUARED = "en_squared";
    public static final String STYLE_EN_SMALL_CAPS = "en_small_caps";
    public static final String STYLE_EN_FULLWIDTH = "en_fullwidth";
    public static final String STYLE_EN_BUBBLE_DARK = "en_bubble_dark";

    public static class StyleOption {
        public final String id;
        public final String displayName;
        public final String shortLabel;
        public final String samplePreview;
        public final boolean isArabic;
        public final boolean isFramed;

        public StyleOption(String id, String displayName, String shortLabel,
                           String samplePreview, boolean isArabic, boolean isFramed) {
            this.id = id;
            this.displayName = displayName;
            this.shortLabel = shortLabel;
            this.samplePreview = samplePreview;
            this.isArabic = isArabic;
            this.isFramed = isFramed;
        }
    }

    // Arabic letters that connect to the following letter (safe for Tatweel/Kashida insertion)
    private static final Set<Character> ARABIC_CONNECTING = new HashSet<>(Arrays.asList(
            'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'س', 'ش', 'ص', 'ض',
            'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن',
            'ه', 'ي', 'ئ', 'پ', 'چ', 'ڤ', 'گ'
    ));

    private static final String[] TASHKEEL_CYCLE = new String[]{
            "\u064E", // Fatha
            "\u0650", // Kasra
            "\u064F", // Damma
            "\u0652", // Sukun
            "\u0651\u064E", // Shadda + Fatha
            "\u064B"  // Fathatan
    };

    private static final Map<Character, String> ORNATE_ARABIC_MAP = new HashMap<>();
    private static final Map<Character, String> SMALL_CAPS_MAP = new HashMap<>();
    private static final Map<Character, Integer> SCRIPT_UPPER_EXCEPTIONS = new HashMap<>();
    private static final Map<Character, Integer> SCRIPT_LOWER_EXCEPTIONS = new HashMap<>();
    private static final Map<Character, Integer> FRAKTUR_UPPER_EXCEPTIONS = new HashMap<>();
    private static final Map<Character, Integer> DOUBLE_UPPER_EXCEPTIONS = new HashMap<>();

    static {
        // Stylized Arabic Unicode presentation/variant glyphs
        ORNATE_ARABIC_MAP.put('ا', "آ");
        ORNATE_ARABIC_MAP.put('ب', "بـ");
        ORNATE_ARABIC_MAP.put('ت', "تـ");
        ORNATE_ARABIC_MAP.put('ث', "ثـ");
        ORNATE_ARABIC_MAP.put('ج', "جـ");
        ORNATE_ARABIC_MAP.put('ح', "حـ");
        ORNATE_ARABIC_MAP.put('خ', "خـ");
        ORNATE_ARABIC_MAP.put('س', "سـ");
        ORNATE_ARABIC_MAP.put('ش', "شـ");
        ORNATE_ARABIC_MAP.put('ص', "صـ");
        ORNATE_ARABIC_MAP.put('ض', "ضـ");
        ORNATE_ARABIC_MAP.put('ع', "عـ");
        ORNATE_ARABIC_MAP.put('غ', "غـ");
        ORNATE_ARABIC_MAP.put('ف', "فـ");
        ORNATE_ARABIC_MAP.put('ق', "قـ");
        ORNATE_ARABIC_MAP.put('ك', "ڪ");
        ORNATE_ARABIC_MAP.put('ل', "لـ");
        ORNATE_ARABIC_MAP.put('م', "مـ");
        ORNATE_ARABIC_MAP.put('ن', "نـ");
        ORNATE_ARABIC_MAP.put('ه', "هـ");
        ORNATE_ARABIC_MAP.put('ي', "يـ");
        ORNATE_ARABIC_MAP.put('ى', "ۍ");
        ORNATE_ARABIC_MAP.put('ة', "ۃ");

        // Small Caps Phonetic Unicode mapping
        String[] smallCaps = {
                "ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ꜰ", "ɢ", "ʜ", "ɪ", "ᴊ",
                "ᴋ", "ʟ", "ᴍ", "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "ꜱ", "ᴛ",
                "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ"
        };
        for (int i = 0; i < 26; i++) {
            SMALL_CAPS_MAP.put((char) ('a' + i), smallCaps[i]);
        }

        // Mathematical Script exceptions
        SCRIPT_UPPER_EXCEPTIONS.put('B', 0x212C);
        SCRIPT_UPPER_EXCEPTIONS.put('E', 0x2130);
        SCRIPT_UPPER_EXCEPTIONS.put('F', 0x2131);
        SCRIPT_UPPER_EXCEPTIONS.put('H', 0x210B);
        SCRIPT_UPPER_EXCEPTIONS.put('I', 0x2110);
        SCRIPT_UPPER_EXCEPTIONS.put('L', 0x2112);
        SCRIPT_UPPER_EXCEPTIONS.put('M', 0x2133);
        SCRIPT_UPPER_EXCEPTIONS.put('R', 0x211B);
        SCRIPT_LOWER_EXCEPTIONS.put('e', 0x212F);
        SCRIPT_LOWER_EXCEPTIONS.put('g', 0x210A);
        SCRIPT_LOWER_EXCEPTIONS.put('o', 0x2134);

        // Mathematical Fraktur exceptions
        FRAKTUR_UPPER_EXCEPTIONS.put('C', 0x212D);
        FRAKTUR_UPPER_EXCEPTIONS.put('H', 0x210C);
        FRAKTUR_UPPER_EXCEPTIONS.put('I', 0x2111);
        FRAKTUR_UPPER_EXCEPTIONS.put('R', 0x211C);
        FRAKTUR_UPPER_EXCEPTIONS.put('Z', 0x2128);

        // Double-Struck exceptions
        DOUBLE_UPPER_EXCEPTIONS.put('C', 0x2102);
        DOUBLE_UPPER_EXCEPTIONS.put('H', 0x210D);
        DOUBLE_UPPER_EXCEPTIONS.put('N', 0x2115);
        DOUBLE_UPPER_EXCEPTIONS.put('P', 0x2119);
        DOUBLE_UPPER_EXCEPTIONS.put('Q', 0x211A);
        DOUBLE_UPPER_EXCEPTIONS.put('R', 0x211D);
        DOUBLE_UPPER_EXCEPTIONS.put('Z', 0x2124);
    }

    private int tashkeelCounter = 0;

    public static List<StyleOption> getAllStyles() {
        List<StyleOption> list = new ArrayList<>();
        list.add(new StyleOption(STYLE_NORMAL, "عادي (بدون زخرفة)", "عادي", "كيبورد برو / Normal", true, false));

        // Arabic Decorations
        list.add(new StyleOption(STYLE_AR_KASHIDA, "تطويل عربي (كشيدة)", "تـطـويـل", "كـيـبـورد بـرو", true, false));
        list.add(new StyleOption(STYLE_AR_TASHKEEL, "تشكيل زخرفي عربي", "تَشْكِيل", "كَيِبوُردْ بَرو", true, false));
        list.add(new StyleOption(STYLE_AR_ROYAL_FRAME, "إطار ملكي ꧁ ... ꧂", "꧁ملكي꧂", "꧁ كـيـبـورد بـرو ꧂", true, true));
        list.add(new StyleOption(STYLE_AR_DIAMOND_FRAME, "إطار ماسي ❖ ... ❖", "❖ماسي❖", "❖ كيبورد برو ❖", true, true));
        list.add(new StyleOption(STYLE_AR_SPARKLE, "نجوم لامعة ✦ ... ✦", "✦لامع✦", "✦ كـيـبـورد بـرو ✦", true, true));
        list.add(new StyleOption(STYLE_AR_ORNATE, "خط عربي مزخرف", "مـزخـرف", "ڪـيـبـورد بـرو", true, false));

        // English Multi-Font Unicode Typing (12 styles)
        list.add(new StyleOption(STYLE_EN_BOLD, "Bold عريض", "𝐁𝐨𝐥𝐝", "𝐁𝐨𝐥𝐝 𝐊𝐞𝐲𝐛𝐨𝐚𝐫𝐝", false, false));
        list.add(new StyleOption(STYLE_EN_ITALIC, "Italic مائل", "𝐼𝑡𝑎𝑙𝑖𝑐", "𝐼𝑡𝑎𝑙𝑖𝑐 𝐾𝑒𝑦𝑏𝑜𝑎𝑟𝑑", false, false));
        list.add(new StyleOption(STYLE_EN_BOLD_ITALIC, "Bold Italic عريض مائل", "𝑩𝒐𝒍𝒅 𝑰𝒕𝒂𝒍𝒊𝒄", "𝑩𝒐𝒍𝒅 𝑰𝒕𝒂𝒍𝒊𝒄", false, false));
        list.add(new StyleOption(STYLE_EN_SCRIPT, "Script / Cursive متصل", "𝒮𝒸𝓇𝒾𝓅𝓉", "𝒮𝒸𝓇𝒾𝓅𝓉 𝒫𝓇𝑜", false, false));
        list.add(new StyleOption(STYLE_EN_FRAKTUR, "Fraktur / Gothic قوطي", "𝔊𝔬𝔱𝔥𝔦𝔠", "𝔊𝔬𝔱𝔥𝔦𝔠 𝔓𝔯𝔬", false, false));
        list.add(new StyleOption(STYLE_EN_DOUBLE, "Double-Struck مزدوج", "𝔻𝕠𝕦𝕓𝕝𝕖", "𝔻𝕠𝕦𝕓𝕝𝕖 ℙ𝕣𝕠", false, false));
        list.add(new StyleOption(STYLE_EN_MONO, "Monospace أحادي", "𝙼𝚘𝚗𝚘", "𝙼𝚘𝚗𝚘𝚜𝚙𝚊𝚌𝚎", false, false));
        list.add(new StyleOption(STYLE_EN_CIRCLED, "Circled دوائر", "Ⓒⓘⓡⓒⓛⓔⓓ", "Ⓒⓘⓡⓒⓛⓔⓓ", false, false));
        list.add(new StyleOption(STYLE_EN_SQUARED, "Squared مربعات", "🅂🅀🅄🄰🅁🄴🄳", "🅂🅀🅄🄰🅁🄴🄳", false, false));
        list.add(new StyleOption(STYLE_EN_SMALL_CAPS, "Small Caps أحرف صغيرة", "Sᴍᴀʟʟ Cᴀᴘs", "Sᴍᴀʟʟ Cᴀᴘs Pʀᴏ", false, false));
        list.add(new StyleOption(STYLE_EN_FULLWIDTH, "Fullwidth عريض ممتد", "Ｆｕｌｌ", "Ｋｅｙｂｏａｒｄ Ｐｒｏ", false, false));
        list.add(new StyleOption(STYLE_EN_BUBBLE_DARK, "Dark Bubble فقاعات", "🅑🅤🅑🅑🅛🅔", "🅚🅔🅨🅑🅞🅐🅡🅓", false, false));

        return list;
    }

    public static StyleOption findStyle(String styleId) {
        for (StyleOption opt : getAllStyles()) {
            if (opt.id.equals(styleId)) return opt;
        }
        return getAllStyles().get(0);
    }

    public static boolean isFramedStyle(String styleId) {
        return STYLE_AR_ROYAL_FRAME.equals(styleId)
                || STYLE_AR_DIAMOND_FRAME.equals(styleId)
                || STYLE_AR_SPARKLE.equals(styleId);
    }

    /**
     * Real-time transformation of a single typed character before InputConnection.commitText().
     */
    public String transformTypedChar(String rawChar, String styleId, boolean enabled) {
        if (!enabled || rawChar == null || rawChar.isEmpty() || STYLE_NORMAL.equals(styleId)) {
            return rawChar;
        }
        if (rawChar.length() > 1) {
            return transformText(rawChar, styleId);
        }
        char c = rawChar.charAt(0);
        if (Character.isWhitespace(c)) {
            return rawChar;
        }

        switch (styleId) {
            case STYLE_AR_KASHIDA:
                return ARABIC_CONNECTING.contains(c) ? (c + "\u0640") : rawChar;

            case STYLE_AR_TASHKEEL:
                if (isArabicLetter(c)) {
                    String mark = TASHKEEL_CYCLE[tashkeelCounter++ % TASHKEEL_CYCLE.length];
                    return c + mark;
                }
                return rawChar;

            case STYLE_AR_ORNATE:
                String ornate = ORNATE_ARABIC_MAP.get(c);
                return ornate != null ? ornate : rawChar;

            case STYLE_AR_ROYAL_FRAME:
            case STYLE_AR_SPARKLE:
                // Apply subtle Kashida while typing; full frame wraps on word commit or transformText
                return ARABIC_CONNECTING.contains(c) ? (c + "\u0640") : rawChar;

            case STYLE_AR_DIAMOND_FRAME:
                return rawChar;

            default:
                return transformEnglishChar(c, styleId);
        }
    }

    /**
     * Wraps a completed word/phrase for framed styles (e.g. ꧁ ... ꧂, ❖ ... ❖, ✦ ... ✦).
     */
    public String wrapFramedWord(String word, String styleId) {
        if (word == null || word.trim().isEmpty()) return word;
        String clean = word.trim();
        switch (styleId) {
            case STYLE_AR_ROYAL_FRAME:
                return "꧁ " + clean + " ꧂";
            case STYLE_AR_DIAMOND_FRAME:
                return "❖ " + clean + " ❖";
            case STYLE_AR_SPARKLE:
                return "✦ " + clean + " ✦";
            default:
                return word;
        }
    }

    /**
     * Transforms an entire string/sentence in one pass (used for previews,
     * suggestion commits, clipboard decoration, and decoration playground).
     */
    public String transformText(String input, String styleId) {
        if (input == null || input.isEmpty() || styleId == null || STYLE_NORMAL.equals(styleId)) {
            return input == null ? "" : input;
        }
        StringBuilder sb = new StringBuilder();
        int localTashkeel = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                sb.append(c);
                continue;
            }
            switch (styleId) {
                case STYLE_AR_KASHIDA:
                    sb.append(c);
                    if (ARABIC_CONNECTING.contains(c)
                            && i + 1 < input.length()
                            && isArabicLetter(input.charAt(i + 1))) {
                        sb.append('\u0640');
                    }
                    break;
                case STYLE_AR_TASHKEEL:
                    sb.append(c);
                    if (isArabicLetter(c)) {
                        sb.append(TASHKEEL_CYCLE[localTashkeel++ % TASHKEEL_CYCLE.length]);
                    }
                    break;
                case STYLE_AR_ORNATE:
                    String rep = ORNATE_ARABIC_MAP.get(c);
                    if (rep != null && (i + 1 >= input.length() || !isArabicLetter(input.charAt(i + 1)))) {
                        sb.append(rep.replace("\u0640", ""));
                    } else {
                        sb.append(rep != null ? rep : c);
                    }
                    break;
                case STYLE_AR_ROYAL_FRAME:
                case STYLE_AR_SPARKLE:
                    sb.append(c);
                    if (ARABIC_CONNECTING.contains(c)
                            && i + 1 < input.length()
                            && isArabicLetter(input.charAt(i + 1))) {
                        sb.append('\u0640');
                    }
                    break;
                case STYLE_AR_DIAMOND_FRAME:
                    sb.append(c);
                    break;
                default:
                    sb.append(transformEnglishChar(c, styleId));
                    break;
            }
        }

        String transformed = sb.toString();
        if (isFramedStyle(styleId)) {
            return wrapFramedWord(transformed, styleId);
        }
        return transformed;
    }

    private String transformEnglishChar(char c, String styleId) {
        switch (styleId) {
            case STYLE_EN_BOLD:
                if (c >= 'A' && c <= 'Z') return cp(0x1D400 + (c - 'A'));
                if (c >= 'a' && c <= 'z') return cp(0x1D41A + (c - 'a'));
                if (c >= '0' && c <= '9') return cp(0x1D7CE + (c - '0'));
                return String.valueOf(c);

            case STYLE_EN_ITALIC:
                if (c >= 'A' && c <= 'Z') return cp(0x1D434 + (c - 'A'));
                if (c == 'h') return "\u210E";
                if (c >= 'a' && c <= 'z') return cp(0x1D44E + (c - 'a'));
                return String.valueOf(c);

            case STYLE_EN_BOLD_ITALIC:
                if (c >= 'A' && c <= 'Z') return cp(0x1D468 + (c - 'A'));
                if (c >= 'a' && c <= 'z') return cp(0x1D482 + (c - 'a'));
                return String.valueOf(c);

            case STYLE_EN_SCRIPT:
                if (SCRIPT_UPPER_EXCEPTIONS.containsKey(c)) return cp(SCRIPT_UPPER_EXCEPTIONS.get(c));
                if (SCRIPT_LOWER_EXCEPTIONS.containsKey(c)) return cp(SCRIPT_LOWER_EXCEPTIONS.get(c));
                if (c >= 'A' && c <= 'Z') return cp(0x1D49C + (c - 'A'));
                if (c >= 'a' && c <= 'z') return cp(0x1D4B6 + (c - 'a'));
                return String.valueOf(c);

            case STYLE_EN_FRAKTUR:
                if (FRAKTUR_UPPER_EXCEPTIONS.containsKey(c)) return cp(FRAKTUR_UPPER_EXCEPTIONS.get(c));
                if (c >= 'A' && c <= 'Z') return cp(0x1D504 + (c - 'A'));
                if (c >= 'a' && c <= 'z') return cp(0x1D51E + (c - 'a'));
                return String.valueOf(c);

            case STYLE_EN_DOUBLE:
                if (DOUBLE_UPPER_EXCEPTIONS.containsKey(c)) return cp(DOUBLE_UPPER_EXCEPTIONS.get(c));
                if (c >= 'A' && c <= 'Z') return cp(0x1D538 + (c - 'A'));
                if (c >= 'a' && c <= 'z') return cp(0x1D552 + (c - 'a'));
                if (c >= '0' && c <= '9') return cp(0x1D7D8 + (c - '0'));
                return String.valueOf(c);

            case STYLE_EN_MONO:
                if (c >= 'A' && c <= 'Z') return cp(0x1D670 + (c - 'A'));
                if (c >= 'a' && c <= 'z') return cp(0x1D68A + (c - 'a'));
                if (c >= '0' && c <= '9') return cp(0x1D7F6 + (c - '0'));
                return String.valueOf(c);

            case STYLE_EN_CIRCLED:
                if (c >= 'A' && c <= 'Z') return cp(0x24B6 + (c - 'A'));
                if (c >= 'a' && c <= 'z') return cp(0x24D0 + (c - 'a'));
                if (c >= '1' && c <= '9') return cp(0x2460 + (c - '1'));
                if (c == '0') return "\u24EA";
                return String.valueOf(c);

            case STYLE_EN_SQUARED: {
                char upper = Character.toUpperCase(c);
                if (upper >= 'A' && upper <= 'Z') return cp(0x1F130 + (upper - 'A'));
                return String.valueOf(c);
            }

            case STYLE_EN_SMALL_CAPS:
                if (SMALL_CAPS_MAP.containsKey(c)) return SMALL_CAPS_MAP.get(c);
                return String.valueOf(c);

            case STYLE_EN_FULLWIDTH:
                if (c >= 0x21 && c <= 0x7E) return cp(0xFF01 + (c - 0x21));
                return String.valueOf(c);

            case STYLE_EN_BUBBLE_DARK: {
                char upper = Character.toUpperCase(c);
                if (upper >= 'A' && upper <= 'Z') return cp(0x1F150 + (upper - 'A'));
                return String.valueOf(c);
            }

            default:
                return String.valueOf(c);
        }
    }

    private static boolean isArabicLetter(char c) {
        return (c >= '\u0621' && c <= '\u064A') || c == '\u0671' || c == '\u067E' || c == '\u0686';
    }

    private static String cp(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}
