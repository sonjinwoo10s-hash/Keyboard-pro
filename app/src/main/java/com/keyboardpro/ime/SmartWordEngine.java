package com.keyboardpro.ime;

import android.content.Context;

import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.data.KeyboardSqliteHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * On-demand SQLite-backed Smart Word Engine.
 * Queries prefix suggestions and swipe candidates using indexed SQLite LIMIT queries
 * (never loads the entire dictionary into memory).
 */
public class SmartWordEngine {

    private static final Map<String, String> NEXT_WORD_AR = new HashMap<>();
    private static final Map<String, String> OFFLINE_AR_TO_EN = new HashMap<>();
    private static final Map<String, String> OFFLINE_EN_TO_AR = new HashMap<>();

    static {
        NEXT_WORD_AR.put("السلام", "عليكم");
        NEXT_WORD_AR.put("عليكم", "ورحمة");
        NEXT_WORD_AR.put("صباح", "الخير");
        NEXT_WORD_AR.put("مساء", "الخير");
        NEXT_WORD_AR.put("شكرا", "جزيلا");
        NEXT_WORD_AR.put("جزاك", "الله");
        NEXT_WORD_AR.put("الحمد", "لله");
        NEXT_WORD_AR.put("كيف", "حالك");
        NEXT_WORD_AR.put("كيبورد", "برو");

        OFFLINE_AR_TO_EN.put("مرحبا", "Hello");
        OFFLINE_AR_TO_EN.put("السلام عليكم", "Peace be upon you");
        OFFLINE_AR_TO_EN.put("شكرا", "Thank you");
        OFFLINE_AR_TO_EN.put("شكرا جزيلا", "Thank you very much");
        OFFLINE_AR_TO_EN.put("صباح الخير", "Good morning");
        OFFLINE_AR_TO_EN.put("مساء الخير", "Good evening");
        OFFLINE_AR_TO_EN.put("كيف حالك", "How are you?");
        OFFLINE_AR_TO_EN.put("أهلا وسهلا", "Welcome");
        OFFLINE_AR_TO_EN.put("ممتاز", "Excellent");
        OFFLINE_AR_TO_EN.put("بالتوفيق", "Good luck");

        for (Map.Entry<String, String> e : OFFLINE_AR_TO_EN.entrySet()) {
            OFFLINE_EN_TO_AR.put(e.getValue().toLowerCase(), e.getKey());
        }
    }

    private final KeyboardPrefs prefs;
    private final KeyboardSqliteHelper dbHelper;

    public SmartWordEngine(Context context) {
        this.prefs = new KeyboardPrefs(context);
        this.dbHelper = KeyboardSqliteHelper.getInstance(context);
    }

    public List<String> getSuggestions(String currentWord, String previousWord, boolean isArabic) {
        Set<String> results = new LinkedHashSet<>();
        if (!prefs.isSuggestionsEnabled()) {
            return new ArrayList<>();
        }

        String lang = isArabic ? "ar" : "en";

        if (currentWord != null && !currentWord.trim().isEmpty()) {
            Map<String, String> shortcuts = prefs.getTextShortcuts();
            if (shortcuts.containsKey(currentWord)) {
                results.add(shortcuts.get(currentWord));
            }
            for (Map.Entry<String, String> e : shortcuts.entrySet()) {
                if (e.getKey().startsWith(currentWord) && results.size() < 3) {
                    results.add(e.getValue());
                }
            }

            // Indexed SQLite prefix LIMIT query (zero full-dictionary in-memory load)
            List<String> dbMatches = dbHelper.queryPrefixSuggestions(currentWord, lang, 5);
            results.addAll(dbMatches);
        } else if (prefs.isNextWordPredictionEnabled() && previousWord != null) {
            String next = NEXT_WORD_AR.get(previousWord.trim());
            if (next != null) results.add(next);
            results.addAll(isArabic
                    ? Arrays.asList("السلام عليكم", "شكراً", "ممتاز")
                    : Arrays.asList("Hello", "Thanks", "Awesome"));
        }

        List<String> finalList = new ArrayList<>(results);
        return finalList.size() > 5 ? finalList.subList(0, 5) : finalList;
    }

    public String expandShortcutExact(String word) {
        if (word == null || word.isEmpty()) return null;
        return prefs.getTextShortcuts().get(word.trim());
    }

    /**
     * Resolves a swiped letter sequence using an indexed SQLite LIMIT query
     * filtered by the initial character.
     */
    public String matchSwipeSequence(String rawPathChars, boolean isArabic) {
        if (rawPathChars == null || rawPathChars.length() < 2) return null;
        String clean = rawPathChars.toLowerCase();
        char first = clean.charAt(0);
        char last = clean.charAt(clean.length() - 1);

        List<String> candidates = dbHelper.querySwipeCandidates(first, isArabic ? "ar" : "en", 25);

        String bestWord = null;
        int bestScore = -1;

        for (String candidate : candidates) {
            String cLower = candidate.toLowerCase();
            if (cLower.length() < 2) continue;
            int score = 10;
            if (cLower.charAt(cLower.length() - 1) == last) score += 10;
            int matched = 0;
            int idx = 0;
            for (int i = 0; i < cLower.length() && idx < clean.length(); i++) {
                int found = clean.indexOf(cLower.charAt(i), idx);
                if (found >= 0) {
                    matched++;
                    idx = found + 1;
                }
            }
            score += matched * 4 - Math.abs(cLower.length() - matched) * 2;
            if (score > bestScore && matched >= Math.max(2, cLower.length() - 1)) {
                bestScore = score;
                bestWord = candidate;
            }
        }
        return bestWord;
    }

    public String translateOffline(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String key = text.trim();
        if (OFFLINE_AR_TO_EN.containsKey(key)) return OFFLINE_AR_TO_EN.get(key);
        if (OFFLINE_EN_TO_AR.containsKey(key.toLowerCase())) return OFFLINE_EN_TO_AR.get(key.toLowerCase());
        return null;
    }
}
