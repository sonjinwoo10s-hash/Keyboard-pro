package com.keyboardpro.ime;

import android.content.Context;

import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.data.KeyboardSqliteHelper;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Smart Arabic & English Typing Intelligence Engine (zero full-dictionary RAM bloat).
 * Combines:
 * - On-demand SQLite prefix & swipe queries
 * - Hamza & Ta Marbuta auto-fixer
 * - Offline Franco-Arabic (Arabizi) converter
 * - Inline Math evaluator (e.g., 15*3+2= -> 47)
 * - Hijri & Gregorian Date/Time formatter
 * - Azkar & Quranic Verse quick suggestions/search
 * - English contractions fixer & smart punctuation
 */
public class SmartWordEngine {

    private final KeyboardSqliteHelper dbHelper;

    private static final Map<String, String[]> NEXT_WORD_MAP = new HashMap<>();
    private static final Map<String, String> HAMZA_TA_MAP = new HashMap<>();
    private static final Map<String, String> FRANCO_MAP = new HashMap<>();
    private static final Map<String, String> EN_CONTRACTIONS = new HashMap<>();
    private static final Map<String, String> AZKAR_MAP = new HashMap<>();
    private static final Map<String, String> QURAN_VERSES = new HashMap<>();

    static {
        // Next-word predictions
        NEXT_WORD_MAP.put("السلام", new String[]{"عليكم", "ورحمة", "يا"});
        NEXT_WORD_MAP.put("عليكم", new String[]{"ورحمة", "السلام", "جميعاً"});
        NEXT_WORD_MAP.put("صباح", new String[]{"الخير", "النور", "الورد"});
        NEXT_WORD_MAP.put("مساء", new String[]{"الخير", "النور", "الفل"});
        NEXT_WORD_MAP.put("إن", new String[]{"شاء", "الله", "كنت"});
        NEXT_WORD_MAP.put("شاء", new String[]{"الله", "ربنا"});
        NEXT_WORD_MAP.put("كيف", new String[]{"حالك", "الأمور", "يمكن"});
        NEXT_WORD_MAP.put("شكراً", new String[]{"جزيلاً", "لك", "كثيراً"});
        NEXT_WORD_MAP.put("الحمد", new String[]{"لله", "رب", "دائماً"});
        NEXT_WORD_MAP.put("جزاك", new String[]{"الله", "خيراً"});
        NEXT_WORD_MAP.put("hello", new String[]{"there", "friend", "everyone"});
        NEXT_WORD_MAP.put("thank", new String[]{"you", "you so much"});
        NEXT_WORD_MAP.put("good", new String[]{"morning", "evening", "night"});

        // Hamza & Ta Marbuta auto-fixer
        HAMZA_TA_MAP.put("انا", "أنا");
        HAMZA_TA_MAP.put("انت", "أنت");
        HAMZA_TA_MAP.put("انتم", "أنتم");
        HAMZA_TA_MAP.put("الى", "إلى");
        HAMZA_TA_MAP.put("اذا", "إذا");
        HAMZA_TA_MAP.put("ان", "إن");
        HAMZA_TA_MAP.put("او", "أو");
        HAMZA_TA_MAP.put("اي", "أي");
        HAMZA_TA_MAP.put("احمد", "أحمد");
        HAMZA_TA_MAP.put("اكيد", "أكيد");
        HAMZA_TA_MAP.put("اهلا", "أهلاً");
        HAMZA_TA_MAP.put("شكرا", "شكراً");
        HAMZA_TA_MAP.put("عفوا", "عفواً");
        HAMZA_TA_MAP.put("حسنا", "حسناً");
        HAMZA_TA_MAP.put("ايضا", "أيضاً");
        HAMZA_TA_MAP.put("جدا", "جداً");
        HAMZA_TA_MAP.put("ابدا", "أبداً");
        HAMZA_TA_MAP.put("انشاء", "إن شاء");
        HAMZA_TA_MAP.put("مدرسه", "مدرسة");
        HAMZA_TA_MAP.put("جامعه", "جامعة");
        HAMZA_TA_MAP.put("شركه", "شركة");
        HAMZA_TA_MAP.put("مكتبه", "مكتبة");
        HAMZA_TA_MAP.put("سياره", "سيارة");
        HAMZA_TA_MAP.put("رساله", "رسالة");
        HAMZA_TA_MAP.put("صوره", "صورة");
        HAMZA_TA_MAP.put("فكره", "فكرة");
        HAMZA_TA_MAP.put("مشكله", "مشكلة");
        HAMZA_TA_MAP.put("لحظه", "لحظة");
        HAMZA_TA_MAP.put("دقيقه", "دقيقة");
        HAMZA_TA_MAP.put("ساعه", "ساعة");
        HAMZA_TA_MAP.put("حاله", "حالة");
        HAMZA_TA_MAP.put("قائمه", "قائمة");
        HAMZA_TA_MAP.put("بركه", "بركة");
        HAMZA_TA_MAP.put("رحمه", "رحمة");

        // Offline Franco-Arabic (Arabizi) dictionary
        FRANCO_MAP.put("salam", "سلام");
        FRANCO_MAP.put("alsalam", "السلام");
        FRANCO_MAP.put("marhaba", "مرحبا");
        FRANCO_MAP.put("ahlan", "أهلاً");
        FRANCO_MAP.put("7abibi", "حبيبي");
        FRANCO_MAP.put("habibi", "حبيبي");
        FRANCO_MAP.put("7abibti", "حبيبتي");
        FRANCO_MAP.put("shokran", "شكراً");
        FRANCO_MAP.put("shukran", "شكراً");
        FRANCO_MAP.put("3afwan", "عفواً");
        FRANCO_MAP.put("kifak", "كيفك");
        FRANCO_MAP.put("keefak", "كيفك");
        FRANCO_MAP.put("tamam", "تمام");
        FRANCO_MAP.put("al7amdulillah", "الحمد لله");
        FRANCO_MAP.put("inshallah", "إن شاء الله");
        FRANCO_MAP.put("mashallah", "ما شاء الله");
        FRANCO_MAP.put("sabah", "صباح");
        FRANCO_MAP.put("masa2", "مساء");
        FRANCO_MAP.put("yalla", "يلا");
        FRANCO_MAP.put("wallah", "والله");
        FRANCO_MAP.put("mabrook", "مبروك");
        FRANCO_MAP.put("mabrouk", "مبروك");
        FRANCO_MAP.put("3eed", "عيد");
        FRANCO_MAP.put("ramadan", "رمضان");
        FRANCO_MAP.put("kolo", "كله");
        FRANCO_MAP.put("shlonak", "شلونك");
        FRANCO_MAP.put("ezzayak", "إزيك");
        FRANCO_MAP.put("3amel", "عامل");
        FRANCO_MAP.put("5alas", "خلاص");
        FRANCO_MAP.put("khalas", "خلاص");
        FRANCO_MAP.put("mumtaz", "ممتاز");
        FRANCO_MAP.put("tayeb", "طيب");
        FRANCO_MAP.put("7ader", "حاضر");

        // English contractions
        EN_CONTRACTIONS.put("dont", "don't");
        EN_CONTRACTIONS.put("cant", "can't");
        EN_CONTRACTIONS.put("wont", "won't");
        EN_CONTRACTIONS.put("didnt", "didn't");
        EN_CONTRACTIONS.put("doesnt", "doesn't");
        EN_CONTRACTIONS.put("isnt", "isn't");
        EN_CONTRACTIONS.put("arent", "aren't");
        EN_CONTRACTIONS.put("wasnt", "wasn't");
        EN_CONTRACTIONS.put("werent", "weren't");
        EN_CONTRACTIONS.put("havent", "haven't");
        EN_CONTRACTIONS.put("hasnt", "hasn't");
        EN_CONTRACTIONS.put("wouldnt", "wouldn't");
        EN_CONTRACTIONS.put("couldnt", "couldn't");
        EN_CONTRACTIONS.put("shouldnt", "shouldn't");
        EN_CONTRACTIONS.put("im", "I'm");
        EN_CONTRACTIONS.put("ive", "I've");
        EN_CONTRACTIONS.put("ill", "I'll");
        EN_CONTRACTIONS.put("id", "I'd");
        EN_CONTRACTIONS.put("youre", "you're");
        EN_CONTRACTIONS.put("theyre", "they're");
        EN_CONTRACTIONS.put("thats", "that's");
        EN_CONTRACTIONS.put("whats", "what's");
        EN_CONTRACTIONS.put("its", "it's");

        // Azkar quick completions
        AZKAR_MAP.put("سبحان", "سبحان الله وبحمده سبحان الله العظيم");
        AZKAR_MAP.put("استغفر", "أستغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه");
        AZKAR_MAP.put("الحمد", "الحمد لله رب العالمين حمداً كثيراً طيباً مباركاً فيه");
        AZKAR_MAP.put("اللهم", "اللهم صلِّ وسلم وبارك على نبينا محمد ﷺ");
        AZKAR_MAP.put("لا حول", "لا حول ولا قوة إلا بالله العلي العظيم");
        AZKAR_MAP.put("حسبي", "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم");
        AZKAR_MAP.put("بسم", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم");

        // Built-in Quranic Verse quick search library (offline)
        QURAN_VERSES.put("الكرسي", "﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ﴾ [البقرة: 255]");
        QURAN_VERSES.put("الإخلاص", "﴿قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ﴾ [الإخلاص]");
        QURAN_VERSES.put("الفاتحة", "﴿بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۝ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ﴾ [الفاتحة]");
        QURAN_VERSES.put("الشرح", "﴿فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا﴾ [الشرح: 5-6]");
        QURAN_VERSES.put("الدعاء", "﴿رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ﴾ [البقرة: 201]");
        QURAN_VERSES.put("الرزق", "﴿وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا ۝ وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ﴾ [الطلاق: 2-3]");
        QURAN_VERSES.put("الصبر", "﴿يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ۚ إِنَّ اللَّهَ مَعَ الصَّابِرِينَ﴾ [البقرة: 153]");
        QURAN_VERSES.put("الذكر", "﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾ [الرعد: 28]");
        QURAN_VERSES.put("التوكل", "﴿وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ﴾ [الطلاق: 3]");
        QURAN_VERSES.put("الرحمة", "﴿قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ﴾ [الزمر: 53]");
    }

    public SmartWordEngine(Context context) {
        this.dbHelper = KeyboardSqliteHelper.getInstance(context);
    }

    public void learnWord(String word, boolean incognito) {
        if (incognito || word == null) return;
        dbHelper.learnTypedWord(word);
    }

    public List<String> getSuggestions(String currentWord, String previousWord, boolean isArabic,
                                       KeyboardPrefs prefs) {
        Set<String> out = new LinkedHashSet<>();
        int maxCount = "3".equals(prefs.getSuggestionBarMode()) ? 3
                : ("5".equals(prefs.getSuggestionBarMode()) ? 5 : 8);

        if (currentWord == null || currentWord.trim().isEmpty()) {
            if (prefs.isNextWordPredictionEnabled() && previousWord != null && !previousWord.isEmpty()) {
                String[] nexts = NEXT_WORD_MAP.get(previousWord.trim().toLowerCase(Locale.ROOT));
                if (nexts == null) nexts = NEXT_WORD_MAP.get(previousWord.trim());
                if (nexts != null) {
                    for (String n : nexts) out.add(n);
                }
            }
            if (out.isEmpty()) {
                if (isArabic) {
                    out.add("السلام"); out.add("شكراً"); out.add("إن شاء الله"); out.add("مرحباً");
                } else {
                    out.add("Hello"); out.add("Thanks"); out.add("Okay"); out.add("Great");
                }
            }
            return trimList(out, maxCount);
        }

        String raw = currentWord.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        // 1. Inline Math evaluator (e.g. 15*3+2 or 15*3+2=)
        if (prefs.isInlineMathEnabled()) {
            String mathResult = tryEvaluateMath(raw);
            if (mathResult != null) {
                out.add(mathResult);
            }
        }

        // 2. Text Shortcuts expansion
        Map<String, String> shortcuts = prefs.getTextShortcuts();
        if (shortcuts.containsKey(raw)) {
            out.add(shortcuts.get(raw));
        } else if (shortcuts.containsKey(lower)) {
            out.add(shortcuts.get(lower));
        }

        // 3. Hamza & Ta Marbuta auto-fixer
        if (prefs.isHamzaTaFixerEnabled() && HAMZA_TA_MAP.containsKey(raw)) {
            out.add(HAMZA_TA_MAP.get(raw));
        }

        // 4. Offline Franco-Arabic converter
        if (prefs.isFrancoArabicEnabled() && FRANCO_MAP.containsKey(lower)) {
            out.add(FRANCO_MAP.get(lower));
        }

        // 5. English contractions fixer
        if (prefs.isEnglishContractionsEnabled() && EN_CONTRACTIONS.containsKey(lower)) {
            out.add(EN_CONTRACTIONS.get(lower));
        }

        // 6. Azkar suggestions
        if (prefs.isAzkarSuggestionsEnabled() && AZKAR_MAP.containsKey(raw)) {
            out.add(AZKAR_MAP.get(raw));
        }

        // 7. On-demand SQLite prefix query (LIMIT)
        String lang = isArabic ? "ar" : "en";
        List<String> dbMatches = dbHelper.queryPrefixSuggestions(raw, lang, maxCount);
        out.addAll(dbMatches);

        // 8. Auto-correct edit-distance fallback on Arabic Hamza/Alef
        if (out.isEmpty() && prefs.isAutoCorrectEnabled() && raw.length() >= 3 && isArabic) {
            String alt = raw.startsWith("ا") ? ("أ" + raw.substring(1)) : raw;
            if (!alt.equals(raw)) {
                out.addAll(dbHelper.queryPrefixSuggestions(alt, "ar", 3));
            }
        }

        return trimList(out, maxCount);
    }

    public String getAutoCommitCorrection(String rawWord, KeyboardPrefs prefs) {
        if (rawWord == null || rawWord.isEmpty()) return null;
        String clean = rawWord.trim();
        String lower = clean.toLowerCase(Locale.ROOT);

        Map<String, String> shortcuts = prefs.getTextShortcuts();
        if (shortcuts.containsKey(clean)) return shortcuts.get(clean);
        if (shortcuts.containsKey(lower)) return shortcuts.get(lower);

        if (prefs.isAutoCorrectEnabled()) {
            if (prefs.isHamzaTaFixerEnabled() && HAMZA_TA_MAP.containsKey(clean)) {
                return HAMZA_TA_MAP.get(clean);
            }
            if (prefs.isEnglishContractionsEnabled() && EN_CONTRACTIONS.containsKey(lower)) {
                return EN_CONTRACTIONS.get(lower);
            }
            if (prefs.isFrancoArabicEnabled() && FRANCO_MAP.containsKey(lower)) {
                return FRANCO_MAP.get(lower);
            }
        }
        return null;
    }

    public String resolveSwipePath(List<String> touchedKeys, boolean isArabic) {
        if (touchedKeys == null || touchedKeys.size() < 2) return null;
        String first = touchedKeys.get(0).toLowerCase(Locale.ROOT);
        String last = touchedKeys.get(touchedKeys.size() - 1).toLowerCase(Locale.ROOT);
        if (first.isEmpty() || last.isEmpty()) return null;

        String lang = isArabic ? "ar" : "en";
        List<String> candidates = dbHelper.querySwipeCandidates(first.charAt(0), lang, 30);

        String bestWord = null;
        int bestScore = -1;
        for (String w : candidates) {
            String lw = w.toLowerCase(Locale.ROOT);
            if (lw.length() < 2) continue;
            if (!lw.endsWith(last)) continue;
            int score = 0;
            for (String k : touchedKeys) {
                if (lw.contains(k.toLowerCase(Locale.ROOT))) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                bestWord = w;
            }
        }
        return bestWord;
    }

    // ---- Inline Math Evaluator (Recursive Descent: +, -, *, /, %, parentheses) ----
    public static String tryEvaluateMath(String raw) {
        if (raw == null) return null;
        String expr = raw.trim().replace('×', '*').replace('÷', '/');
        boolean endsWithEq = expr.endsWith("=");
        if (endsWithEq) expr = expr.substring(0, expr.length() - 1).trim();
        if (expr.length() < 3) return null;

        boolean hasOp = false;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%') {
                if (i > 0) hasOp = true;
            } else if (!Character.isDigit(c) && c != '.' && c != '(' && c != ')') {
                return null;
            }
        }
        if (!hasOp) return null;

        try {
            double val = new MathParser(expr).parse();
            if (Double.isNaN(val) || Double.isInfinite(val)) return null;
            DecimalFormat df = new DecimalFormat("0.######");
            return df.format(val);
        } catch (Exception e) {
            return null;
        }
    }

    private static final class MathParser {
        private final String s;
        private int pos = -1, ch;

        MathParser(String s) { this.s = s; }

        void nextChar() { ch = (++pos < s.length()) ? s.charAt(pos) : -1; }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) { nextChar(); return true; }
            return false;
        }

        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < s.length()) throw new RuntimeException("Unexpected");
            return x;
        }

        double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else if (eat('%')) x %= parseFactor();
                else return x;
            }
        }

        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();
            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) throw new RuntimeException("Missing )");
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(s.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Bad factor");
            }
            return x;
        }
    }

    // ---- Hijri & Gregorian Date/Time Helpers (API 26+) ----
    public static String getGregorianDate() {
        return new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(new Date());
    }

    public static String getCurrentTime() {
        return new SimpleDateFormat("hh:mm a", new Locale("ar")).format(new Date());
    }

    public static String getHijriDate() {
        try {
            HijrahDate h = HijrahDate.now();
            return DateTimeFormatter.ofPattern("yyyy/MM/dd هـ", Locale.ENGLISH).format(h);
        } catch (Exception e) {
            return getGregorianDate();
        }
    }

    public static List<String> getAllAzkar() {
        return new ArrayList<>(AZKAR_MAP.values());
    }

    public static List<String> searchQuranVerses(String query) {
        List<String> out = new ArrayList<>();
        String q = query == null ? "" : query.trim();
        for (Map.Entry<String, String> e : QURAN_VERSES.entrySet()) {
            if (q.isEmpty() || e.getKey().contains(q) || e.getValue().contains(q)) {
                out.add(e.getValue());
            }
        }
        return out;
    }

    public static String toggleSecretCipher(String text) {
        if (text == null || text.isEmpty()) return "";
        // Reversible readable cipher for "Secret Message" tool
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u0621' && c <= '\u064A') {
                sb.append((char) ('\u0621' + ('\u064A' - c)));
            } else if (c >= 'a' && c <= 'z') {
                sb.append((char) ('a' + ('z' - c)));
            } else if (c >= 'A' && c <= 'Z') {
                sb.append((char) ('A' + ('Z' - c)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<String> trimList(Set<String> set, int max) {
        List<String> list = new ArrayList<>(Math.min(set.size(), max));
        for (String s : set) {
            if (list.size() >= max) break;
            list.add(s);
        }
        return list;
    }
}
