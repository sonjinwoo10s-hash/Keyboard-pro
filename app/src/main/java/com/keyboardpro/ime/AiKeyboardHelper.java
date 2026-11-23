package com.keyboardpro.ime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.keyboardpro.data.KeyboardPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI Keyboard Engine supporting user-provided free API keys (Gemini API or OpenAI-compatible)
 * for instant proofreading, rewriting, and translation directly from the keyboard or AI bench.
 */
public class AiKeyboardHelper {

    public enum Action {
        PROOFREAD,
        REWRITE,
        TRANSLATE
    }

    public interface AiCallback {
        void onResult(boolean success, String output);
    }

    private final KeyboardPrefs prefs;
    private final SmartWordEngine offlineEngine;
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AiKeyboardHelper(Context context) {
        this.prefs = new KeyboardPrefs(context);
        this.offlineEngine = new SmartWordEngine(context);
    }

    public void processText(String inputText, Action action, AiCallback callback) {
        if (inputText == null || inputText.trim().isEmpty()) {
            mainHandler.post(() -> callback.onResult(false, "اكتب نصاً أولاً لاستخدام الذكاء الاصطناعي."));
            return;
        }

        // Offline translation fast-path when offline dictionary has exact phrase and no API key
        String apiKey = prefs.getAiApiKey();
        if (action == Action.TRANSLATE && prefs.isOfflineTranslationEnabled() && apiKey.isEmpty()) {
            String offline = offlineEngine.translateOffline(inputText);
            if (offline != null) {
                mainHandler.post(() -> callback.onResult(true, offline));
                return;
            }
        }

        if (apiKey.isEmpty()) {
            mainHandler.post(() -> callback.onResult(false,
                    "الرجاء إضافة مفتاح API مجاني في الإعدادات ← كيبورد الذكاء الاصطناعي."));
            return;
        }

        String prompt = buildPrompt(inputText.trim(), action);
        String provider = prefs.getAiProvider();
        String model = prefs.getAiModel();

        executor.execute(() -> {
            try {
                if ("openai".equals(provider)) {
                    callOpenAiCompatible(apiKey, model, prompt, callback);
                } else {
                    callGemini(apiKey, model, prompt, callback);
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onResult(false, "تعذر الاتصال بخدمة الذكاء الاصطناعي: " + e.getMessage()));
            }
        });
    }

    private String buildPrompt(String text, Action action) {
        switch (action) {
            case PROOFREAD:
                return "Correct spelling and grammar mistakes in the following text. Return ONLY the corrected text in the same language without explanations:\n" + text;
            case REWRITE:
                return "Rewrite the following text to sound clearer, more polished, and natural. Return ONLY the rewritten text in the same language:\n" + text;
            case TRANSLATE:
            default:
                String target = prefs.getTranslationTargetLang();
                return "Translate the following text accurately into " + target + ". Return ONLY the translation:\n" + text;
        }
    }

    private void callGemini(String apiKey, String model, String prompt, AiCallback callback) throws Exception {
        String safeModel = (model == null || model.trim().isEmpty()) ? "gemini-2.0-flash" : model.trim();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + safeModel + ":generateContent?key=" + apiKey;

        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject bodyJson = new JSONObject().put("contents", new JSONArray().put(content));

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyJson.toString().getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                int code = resp.code();
                mainHandler.post(() -> callback.onResult(false, "فشل طلب Gemini API (رمز " + code + "). تحقق من المفتاح."));
                return;
            }
            JSONObject root = new JSONObject(resp.body().string());
            String text = root.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();
            mainHandler.post(() -> callback.onResult(true, text));
        }
    }

    private void callOpenAiCompatible(String apiKey, String model, String prompt, AiCallback callback) throws Exception {
        String safeModel = (model == null || model.trim().isEmpty()) ? "gpt-4o-mini" : model.trim();
        JSONObject msg = new JSONObject().put("role", "user").put("content", prompt);
        JSONObject bodyJson = new JSONObject()
                .put("model", safeModel)
                .put("messages", new JSONArray().put(msg));

        Request req = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(bodyJson.toString().getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                int code = resp.code();
                mainHandler.post(() -> callback.onResult(false, "فشل طلب API (رمز " + code + "). تحقق من المفتاح."));
                return;
            }
            JSONObject root = new JSONObject(resp.body().string());
            String text = root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
            mainHandler.post(() -> callback.onResult(true, text));
        }
    }
}
