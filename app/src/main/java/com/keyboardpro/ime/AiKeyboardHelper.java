package com.keyboardpro.ime;

import android.os.Handler;
import android.os.Looper;

import com.keyboardpro.data.KeyboardPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BYOK (Bring Your Own Key) Google Gemini AI Keyboard helper using lightweight
 * HttpURLConnection (zero heavy SDK overhead), supporting 15 customizable prompt actions,
 * user-defined custom AI tools, "About Me" persona injection, and local usage counters.
 */
public final class AiKeyboardHelper {

    public interface AiCallback {
        void onResult(String resultText);
        void onError(String errorMessage);
    }

    public static class AiAction {
        public final String id;
        public final String label;
        public final String prompt;

        public AiAction(String id, String label, String prompt) {
            this.id = id;
            this.label = label;
            this.prompt = prompt;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final List<AiAction> BUILT_IN_ACTIONS = new ArrayList<>();

    static {
        BUILT_IN_ACTIONS.add(new AiAction("fix", "✨ تصحيح إملائي", "صحح الأخطاء الإملائية والنحوية فقط وأرجع النص المصحح دون شرح:"));
        BUILT_IN_ACTIONS.add(new AiAction("rephrase", "🔄 إعادة صياغة", "أعد صياغة النص التالي بأسلوب سلس واحترافي دون تغيير المعنى، وأرجع النص فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("formal", "👔 أسلوب رسمي", "حول النص التالي إلى أسلوب رسمي ومهني راقٍ وأرجع النص فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("friendly", "😊 أسلوب ودي", "حول النص التالي إلى أسلوب ودي ولطيف ودافئ وأرجع النص فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("shorten", "✂️ اختصار", "اختصر النص التالي في جملة أو جملتين واضحتين دون فقدان المعنى:"));
        BUILT_IN_ACTIONS.add(new AiAction("expand", "📝 توسيع", "وسّع النص التالي وأضف تفاصيل مفيدة بأسلوب جميل وأرجع النص فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("translate_en", "🌐 ترجمة للإنجليزية", "Translate the following text into natural fluent English. Return only the translation:"));
        BUILT_IN_ACTIONS.add(new AiAction("translate_ar", "🌐 ترجمة للعربية", "ترجم النص التالي إلى عربية فصحى طبيعية وسلسة، وأرجع الترجمة فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("diacritize", "✍️ تشكيل كامل", "أضف التشكيل العربي الصحيح والكامل للنص التالي وأرجع النص المشكّل فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("fusha", "📖 تحويل للفصحى", "حول النص التالي إلى لغة عربية فصحى سليمة وأرجع النص فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("dialect", "💬 لهجة عامية", "حول النص التالي إلى لهجة عربية بيضاء طبيعية ومحببة وأرجع النص فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("emoji", "😍 إضافة إيموجي", "أضف رموزاً تعبيرية (إيموجي) مناسبة بذكاء داخل النص التالي وأرجعه فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("caption", "📸 كابشن سوشيال", "اكتب كابشن قصير وجذاب للنشر على السوشيال ميديا بناءً على النص التالي:"));
        BUILT_IN_ACTIONS.add(new AiAction("reply", "⚡ رد ذكي", "اكتب رداً ذكياً ومهذباً ومناسباً على الرسالة التالية وأرجع الرد فقط:"));
        BUILT_IN_ACTIONS.add(new AiAction("summarize", "📋 تلخيص", "لخص النص التالي في نقاط قصيرة جداً وواضحة:"));
    }

    private AiKeyboardHelper() {
    }

    public static List<AiAction> getAllActions(KeyboardPrefs prefs) {
        List<AiAction> all = new ArrayList<>(BUILT_IN_ACTIONS);
        try {
            JSONArray arr = new JSONArray(prefs.getCustomAiToolsJson());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                all.add(new AiAction(
                        o.optString("id", "custom_" + i),
                        o.optString("label", "أداة مخصصة"),
                        o.optString("prompt", "")
                ));
            }
        } catch (Exception ignored) {
        }
        return all;
    }

    public static void addCustomAction(KeyboardPrefs prefs, String label, String prompt) {
        try {
            JSONArray arr = new JSONArray(prefs.getCustomAiToolsJson());
            JSONObject o = new JSONObject();
            o.put("id", "custom_" + System.currentTimeMillis());
            o.put("label", label.trim());
            o.put("prompt", prompt.trim());
            arr.put(o);
            prefs.setCustomAiToolsJson(arr.toString());
        } catch (Exception ignored) {
        }
    }

    public static void runAction(KeyboardPrefs prefs, AiAction action, String inputText, AiCallback callback) {
        String apiKey = prefs.getAiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            MAIN.post(() -> callback.onError("يرجى إدخال مفتاح Gemini API من الإعدادات ← كيبورد الذكاء الاصطناعي"));
            return;
        }
        if (inputText == null || inputText.trim().isEmpty()) {
            MAIN.post(() -> callback.onError("اكتب نصاً أولاً لاستخدام أدوات الذكاء الاصطناعي"));
            return;
        }

        final String model = prefs.getAiModel().isEmpty() ? "gemini-flash-latest" : prefs.getAiModel();
        final String persona = prefs.getAiAboutMePersona();

        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            try {
                StringBuilder promptBuilder = new StringBuilder();
                if (persona != null && !persona.isEmpty()) {
                    promptBuilder.append("معلومات عن الكاتب (للسياق فقط): ").append(persona).append("\n\n");
                }
                promptBuilder.append(action.prompt).append("\n\n").append(inputText.trim());

                String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + model + ":generateContent?key=" + apiKey.trim();

                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                JSONObject part = new JSONObject().put("text", promptBuilder.toString());
                JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
                JSONObject payload = new JSONObject().put("contents", new JSONArray().put(content));

                byte[] bodyBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder sb = new StringBuilder();
                if (is != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                }

                if (code >= 200 && code < 300) {
                    JSONObject resp = new JSONObject(sb.toString());
                    String text = resp.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text").trim();

                    int approxTokens = (promptBuilder.length() + text.length()) / 3;
                    prefs.recordAiUsage(approxTokens);
                    MAIN.post(() -> callback.onResult(text));
                } else {
                    MAIN.post(() -> callback.onError("خطأ من الخادم (" + code + "): تأكد من صحة مفتاح API"));
                }
            } catch (Exception e) {
                MAIN.post(() -> callback.onError("تعذر الاتصال: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}
