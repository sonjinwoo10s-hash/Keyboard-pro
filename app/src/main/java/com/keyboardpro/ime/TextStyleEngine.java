package com.keyboardpro.ime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Purely algorithmic/lookup-map Decorative Text Engine (<1MB RAM).
 * Provides 26 Arabic decorative styles and 30 English Unicode styles, with automatic
 * password/email/URL field protection handled by KeyboardProService.
 */
public final class TextStyleEngine {

    public static final String STYLE_NORMAL = "normal";

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

    private static final Set<Character> ARABIC_CONNECTING = new HashSet<>(Arrays.asList(
            'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'س', 'ش', 'ص', 'ض',
            'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن',
            'ه', 'ي', 'ئ', 'پ', 'چ', 'ڤ', 'گ'
    ));

    private static final String[] TASHKEEL_SOFT = {"\u064E", "\u0650", "\u064F", "\u0652"};
    private static final String[] TASHKEEL_RICH = {"\u064E", "\u0651\u064E", "\u0650", "\u064F", "\u064B", "\u0652"};
    private static final String[] ZALGO_MARKS = {"\u030D", "\u030E", "\u0304", "\u0305", "\u033F", "\u0311"};

    private static final Map<Character, String> SMALL_CAPS = new HashMap<>();
    private static final Map<Character, Character> UPSIDE_DOWN = new HashMap<>();
    private static final Map<Character, Integer> SCRIPT_EX = new HashMap<>();
    private static final Map<Character, Integer> FRAKTUR_EX = new HashMap<>();
    private static final Map<Character, Integer> DOUBLE_EX = new HashMap<>();

    // 26 Arabic styles metadata: {id, displayName, shortLabel, prefix, suffix, mode}
    private static final String[][] AR_DEFS = {
            {"ar_01", "تطويل ناعم (كشيدة)", "تـطـويـل", "", "", "kashida1"},
            {"ar_02", "تطويل مضاعف", "تــطــويــل", "", "", "kashida2"},
            {"ar_03", "تشكيل خفيف", "تَشْكِيل", "", "", "tashkeel_soft"},
            {"ar_04", "تشكيل ملكي غني", "تَشّكِيلٌ", "", "", "tashkeel_rich"},
            {"ar_05", "تشكيل مع تطويل", "تَـشـكِـيـل", "", "", "kashida_tashkeel"},
            {"ar_06", "إطار ملكي ꧁ ꧂", "꧁ملكي꧂", "꧁ ", " ꧂", "kashida1"},
            {"ar_07", "إطار ماسي ❖ ❖", "❖ماسي❖", "❖ ", " ❖", "plain"},
            {"ar_08", "نجوم لامعة ✦ ✦", "✦لامع✦", "✦ ", " ✦", "kashida1"},
            {"ar_09", "قلوب ناعمة ♡ ♡", "♡قلوب♡", "♡ ", " ♡", "plain"},
            {"ar_10", "قلوب ممتلئة ♥ ♥", "♥قلب♥", "♥ ", " ♥", "kashida1"},
            {"ar_11", "أجنحة فخمة ༺ ༻", "༺أجنحة༻", "༺ ", " ༻", "kashida1"},
            {"ar_12", "أقواس مزخرفة 『 』", "『أقواس』", "『 ", " 』", "plain"},
            {"ar_13", "تيجان ذهبية ♛ ♛", "♛تاج♛", "♛ ", " ♛", "kashida1"},
            {"ar_14", "ورود ✿ ✿", "✿ورود✿", "✿ ", " ✿", "plain"},
            {"ar_15", "أزهار ناعمة ❀ ❀", "❀زهرة❀", "❀ ", " ❀", "kashida1"},
            {"ar_16", "يتوسطه خط (مشطوب)", "م̶ش̶ط̶و̶ب", "", "", "strike"},
            {"ar_17", "تحته خط (تسطير)", "ت̲س̲ط̲ي̲ر", "", "", "underline"},
            {"ar_18", "خط مزدوج سفلي", "م̳ز̳د̳و̳ج", "", "", "double_under"},
            {"ar_19", "عربي فارسي مزخرف", "ڪـيـبـورد", "", "", "ornate"},
            {"ar_20", "شرارات ✨ ✨", "✨شرارة✨", "✨ ", " ✨", "plain"},
            {"ar_21", "فواصل نجوم ★ ★", "★نجم★", "★ ", " ★", "kashida1"},
            {"ar_22", "أقواس عربية ﴿ ﴾", "﴿قوس﴾", "﴿ ", " ﴾", "plain"},
            {"ar_23", "أسهم فخمة ◄ ►", "◄سهم►", "◄ ", " ►", "plain"},
            {"ar_24", "ثلج وكرستال ❄ ❄", "❄كريستال❄", "❄ ", " ❄", "kashida1"},
            {"ar_25", "فخامة مزدوجة ꧁༺ ༻꧂", "꧁༺فخم༻꧂", "꧁༺ ", " ༻꧂", "kashida1"},
            {"ar_26", "فواصل نقطية • •", "•نقطة•", "• ", " •", "tashkeel_soft"}
    };

    // 30 English Unicode styles metadata: {id, displayName, shortLabel}
    private static final String[][] EN_DEFS = {
            {"en_01", "Bold", "𝐁𝐨𝐥𝐝"},
            {"en_02", "Italic", "𝐼𝑡𝑎𝑙𝑖𝑐"},
            {"en_03", "Bold Italic", "𝑩𝒐𝒍𝒅 𝑰𝒕𝒂𝒍"},
            {"en_04", "Script / Cursive", "𝒮𝒸𝓇𝒾𝓅𝓉"},
            {"en_05", "Bold Script", "𝓑𝓸𝓵𝓭 𝓢𝓬𝓻"},
            {"en_06", "Fraktur / Gothic", "𝔊𝔬𝔱𝔥𝔦𝔠"},
            {"en_07", "Bold Fraktur", "𝕲𝖔𝖙𝖍𝖎𝖈"},
            {"en_08", "Double-Struck", "𝔻𝕠𝕦𝕓𝕝𝕖"},
            {"en_09", "Sans-Serif", "𝖲𝖺𝗇𝗌"},
            {"en_10", "Sans Bold", "𝗦𝗮𝗻𝘀 𝗕"},
            {"en_11", "Sans Italic", "𝘚𝘢𝘯𝘴 𝘐"},
            {"en_12", "Sans Bold Italic", "𝙎𝙖𝙣𝙨 𝘽𝙄"},
            {"en_13", "Monospace", "𝙼𝚘𝚗𝚘"},
            {"en_14", "Circled", "Ⓒⓘⓡⓒⓛⓔ"},
            {"en_15", "Dark Bubble", "🅑🅤🅑🅑🅛🅔"},
            {"en_16", "Squared", "🅂🅀🅄🄰🅁🄴"},
            {"en_17", "Dark Squared", "🆂🆀🆄🅰🆁🅴"},
            {"en_18", "Small Caps", "Sᴍᴀʟʟ Cᴀᴘs"},
            {"en_19", "Fullwidth", "Ｆｕｌｌ"},
            {"en_20", "Parenthesized", "⒫⒜⒭⒠⒩"},
            {"en_21", "Upside-Down", "uʍop"},
            {"en_22", "Strikethrough", "S̶t̶r̶i̶k̶e"},
            {"en_23", "Double Strike", "D̸o̸u̸b̸l̸e"},
            {"en_24", "Underline", "U̲n̲d̲e̲r"},
            {"en_25", "Double Underline", "D̳o̳u̳b̳l̳e"},
            {"en_26", "Zalgo Glitch", "Z̍a̎l̄g̅o"},
            {"en_27", "Hearts ♡", "♡ Hᴇᴀʀᴛ ♡"},
            {"en_28", "Stars ★", "★ Sᴛᴀʀ ★"},
            {"en_29", "Sparkles ✦", "✦ Sᴘᴀʀᴋ ✦"},
            {"en_30", "Royal Wings ꧁ ꧂", "꧁ 𝑹𝒐𝒚𝒂𝒍 ꧂"}
    };

    private static final List<StyleOption> CACHED_OPTIONS = new ArrayList<>(60);

    static {
        String[] sc = {"ᴀ","ʙ","ᴄ","ᴅ","ᴇ","ꜰ","ɢ","ʜ","ɪ","ᴊ","ᴋ","ʟ","ᴍ","ɴ","ᴏ","ᴘ","ǫ","ʀ","ꜱ","ᴛ","ᴜ","ᴠ","ᴡ","x","ʏ","ᴢ"};
        for (int i = 0; i < 26; i++) SMALL_CAPS.put((char) ('a' + i), sc[i]);

        String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String flip   = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀qƆpƎℲפHIſʞ˥WNOԀQɹS┴∩ΛMX⅄Z";
        for (int i = 0; i < normal.length() && i < flip.length(); i++) {
            UPSIDE_DOWN.put(normal.charAt(i), flip.charAt(i));
        }

        SCRIPT_EX.put('B', 0x212C); SCRIPT_EX.put('E', 0x2130); SCRIPT_EX.put('F', 0x2131);
        SCRIPT_EX.put('H', 0x210B); SCRIPT_EX.put('I', 0x2110); SCRIPT_EX.put('L', 0x2112);
        SCRIPT_EX.put('M', 0x2133); SCRIPT_EX.put('R', 0x211B);
        SCRIPT_EX.put('e', 0x212F); SCRIPT_EX.put('g', 0x210A); SCRIPT_EX.put('o', 0x2134);

        FRAKTUR_EX.put('C', 0x212D); FRAKTUR_EX.put('H', 0x210C); FRAKTUR_EX.put('I', 0x2111);
        FRAKTUR_EX.put('R', 0x211C); FRAKTUR_EX.put('Z', 0x2128);

        DOUBLE_EX.put('C', 0x2102); DOUBLE_EX.put('H', 0x210D); DOUBLE_EX.put('N', 0x2115);
        DOUBLE_EX.put('P', 0x2119); DOUBLE_EX.put('Q', 0x211A); DOUBLE_EX.put('R', 0x211D);
        DOUBLE_EX.put('Z', 0x2124);

        TextStyleEngine tmp = new TextStyleEngine();
        CACHED_OPTIONS.add(new StyleOption(STYLE_NORMAL, "عادي (بدون زخرفة)", "عادي", "كيبورد برو / Normal", true, false));
        for (String[] def : AR_DEFS) {
            boolean framed = !def[3].isEmpty();
            CACHED_OPTIONS.add(new StyleOption(def[0], def[1], def[2], tmp.transformText("كيبورد برو", def[0]), true, framed));
        }
        for (String[] def : EN_DEFS) {
            boolean framed = def[0].equals("en_27") || def[0].equals("en_28") || def[0].equals("en_29") || def[0].equals("en_30");
            CACHED_OPTIONS.add(new StyleOption(def[0], def[1], def[2], tmp.transformText("Keyboard Pro", def[0]), false, framed));
        }
    }

    private int counter = 0;

    public static List<StyleOption> getAllStyles() {
        return CACHED_OPTIONS;
    }

    public static StyleOption findStyle(String styleId) {
        if (styleId != null) {
            for (StyleOption opt : CACHED_OPTIONS) {
                if (opt.id.equals(styleId)) return opt;
            }
        }
        return CACHED_OPTIONS.get(0);
    }

    public static boolean isFramedStyle(String styleId) {
        return findStyle(styleId).isFramed;
    }

    public String transformTypedChar(String rawChar, String styleId, boolean enabled) {
        if (!enabled || rawChar == null || rawChar.isEmpty() || STYLE_NORMAL.equals(styleId)) {
            return rawChar;
        }
        if (rawChar.length() > 1) return transformText(rawChar, styleId);
        char c = rawChar.charAt(0);
        if (Character.isWhitespace(c)) return rawChar;

        if (styleId.startsWith("ar_")) {
            String mode = getArabicMode(styleId);
            return transformArabicChar(c, mode, counter++);
        }
        return transformEnglishChar(c, styleId, counter++);
    }

    public String wrapFramedWord(String word, String styleId) {
        if (word == null || word.trim().isEmpty()) return word;
        String clean = word.trim();
        for (String[] def : AR_DEFS) {
            if (def[0].equals(styleId) && !def[3].isEmpty()) {
                return def[3] + clean + def[4];
            }
        }
        if ("en_27".equals(styleId)) return "♡ " + clean + " ♡";
        if ("en_28".equals(styleId)) return "★ " + clean + " ★";
        if ("en_29".equals(styleId)) return "✦ " + clean + " ✦";
        if ("en_30".equals(styleId)) return "꧁ " + clean + " ꧂";
        return word;
    }

    public String transformText(String input, String styleId) {
        if (input == null || input.isEmpty() || styleId == null || STYLE_NORMAL.equals(styleId)) {
            return input == null ? "" : input;
        }
        StringBuilder sb = new StringBuilder(input.length() * 2);
        String arMode = styleId.startsWith("ar_") ? getArabicMode(styleId) : "";

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                sb.append(c);
                continue;
            }
            if (styleId.startsWith("ar_")) {
                boolean nextIsAr = i + 1 < input.length() && isArabicLetter(input.charAt(i + 1));
                String piece = transformArabicChar(c, arMode, i);
                if (!nextIsAr && (arMode.startsWith("kashida") || "ornate".equals(arMode))) {
                    piece = piece.replace("\u0640", "");
                }
                sb.append(piece);
            } else {
                sb.append(transformEnglishChar(c, styleId, i));
            }
        }

        String out = "en_21".equals(styleId) ? sb.reverse().toString() : sb.toString();
        if (isFramedStyle(styleId)) {
            return wrapFramedWord(out, styleId);
        }
        return out;
    }

    private String getArabicMode(String id) {
        for (String[] def : AR_DEFS) {
            if (def[0].equals(id)) return def[5];
        }
        return "plain";
    }

    private String transformArabicChar(char c, String mode, int idx) {
        switch (mode) {
            case "kashida1":
                return ARABIC_CONNECTING.contains(c) ? (c + "\u0640") : String.valueOf(c);
            case "kashida2":
                return ARABIC_CONNECTING.contains(c) ? (c + "\u0640\u0640") : String.valueOf(c);
            case "tashkeel_soft":
                return isArabicLetter(c) ? (c + TASHKEEL_SOFT[idx % TASHKEEL_SOFT.length]) : String.valueOf(c);
            case "tashkeel_rich":
                return isArabicLetter(c) ? (c + TASHKEEL_RICH[idx % TASHKEEL_RICH.length]) : String.valueOf(c);
            case "kashida_tashkeel": {
                String base = isArabicLetter(c) ? (c + TASHKEEL_SOFT[idx % TASHKEEL_SOFT.length]) : String.valueOf(c);
                return ARABIC_CONNECTING.contains(c) ? (base + "\u0640") : base;
            }
            case "strike":
                return c + "\u0336";
            case "underline":
                return c + "\u0332";
            case "double_under":
                return c + "\u0333";
            case "ornate":
                if (c == 'ك') return "ڪـ";
                if (c == 'ه') return "ھـ";
                if (c == 'ة') return "ۃ";
                if (c == 'ي') return "يـ";
                if (c == 'ا') return "آ";
                return ARABIC_CONNECTING.contains(c) ? (c + "\u0640") : String.valueOf(c);
            default:
                return String.valueOf(c);
        }
    }

    private String transformEnglishChar(char c, String id, int idx) {
        switch (id) {
            case "en_01": return mathRange(c, 0x1D400, 0x1D41A, 0x1D7CE);
            case "en_02": return c == 'h' ? "\u210E" : mathRange(c, 0x1D434, 0x1D44E, 0);
            case "en_03":
            case "en_30": return mathRange(c, 0x1D468, 0x1D482, 0);
            case "en_04": return SCRIPT_EX.containsKey(c) ? cp(SCRIPT_EX.get(c)) : mathRange(c, 0x1D49C, 0x1D4B6, 0);
            case "en_05": return mathRange(c, 0x1D4D0, 0x1D4EA, 0);
            case "en_06": return FRAKTUR_EX.containsKey(c) ? cp(FRAKTUR_EX.get(c)) : mathRange(c, 0x1D504, 0x1D51E, 0);
            case "en_07": return mathRange(c, 0x1D56C, 0x1D586, 0);
            case "en_08": return DOUBLE_EX.containsKey(c) ? cp(DOUBLE_EX.get(c)) : mathRange(c, 0x1D538, 0x1D552, 0x1D7D8);
            case "en_09": return mathRange(c, 0x1D5A0, 0x1D5BA, 0x1D7E2);
            case "en_10": return mathRange(c, 0x1D5D4, 0x1D5EE, 0x1D7EC);
            case "en_11": return mathRange(c, 0x1D608, 0x1D622, 0);
            case "en_12": return mathRange(c, 0x1D63C, 0x1D656, 0);
            case "en_13": return mathRange(c, 0x1D670, 0x1D68A, 0x1D7F6);
            case "en_14":
                if (c >= 'A' && c <= 'Z') return cp(0x24B6 + (c - 'A'));
                if (c >= 'a' && c <= 'z') return cp(0x24D0 + (c - 'a'));
                if (c >= '1' && c <= '9') return cp(0x2460 + (c - '1'));
                return c == '0' ? "\u24EA" : String.valueOf(c);
            case "en_15": {
                char u = Character.toUpperCase(c);
                return (u >= 'A' && u <= 'Z') ? cp(0x1F150 + (u - 'A')) : String.valueOf(c);
            }
            case "en_16": {
                char u = Character.toUpperCase(c);
                return (u >= 'A' && u <= 'Z') ? cp(0x1F130 + (u - 'A')) : String.valueOf(c);
            }
            case "en_17": {
                char u = Character.toUpperCase(c);
                return (u >= 'A' && u <= 'Z') ? cp(0x1F170 + (u - 'A')) : String.valueOf(c);
            }
            case "en_18":
            case "en_27":
            case "en_28":
            case "en_29":
                return SMALL_CAPS.containsKey(c) ? SMALL_CAPS.get(c) : String.valueOf(c);
            case "en_19":
                return (c >= 0x21 && c <= 0x7E) ? cp(0xFF01 + (c - 0x21)) : String.valueOf(c);
            case "en_20": {
                char l = Character.toLowerCase(c);
                return (l >= 'a' && l <= 'z') ? cp(0x249C + (l - 'a')) : String.valueOf(c);
            }
            case "en_21":
                return UPSIDE_DOWN.containsKey(c) ? String.valueOf(UPSIDE_DOWN.get(c)) : String.valueOf(c);
            case "en_22": return c + "\u0336";
            case "en_23": return c + "\u0338";
            case "en_24": return c + "\u0332";
            case "en_25": return c + "\u0333";
            case "en_26": return c + ZALGO_MARKS[idx % ZALGO_MARKS.length] + "\u0316";
            default: return String.valueOf(c);
        }
    }

    private static String mathRange(char c, int upperStart, int lowerStart, int digitStart) {
        if (c >= 'A' && c <= 'Z') return cp(upperStart + (c - 'A'));
        if (c >= 'a' && c <= 'z') return cp(lowerStart + (c - 'a'));
        if (digitStart != 0 && c >= '0' && c <= '9') return cp(digitStart + (c - '0'));
        return String.valueOf(c);
    }

    private static boolean isArabicLetter(char c) {
        return (c >= '\u0621' && c <= '\u064A') || c == '\u0671' || c == '\u067E' || c == '\u0686';
    }

    private static String cp(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}
