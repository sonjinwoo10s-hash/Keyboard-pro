package com.keyboardpro.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.data.KeyboardSqliteHelper;
import com.keyboardpro.ime.TextStyleEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modular sub-screen builders for Languages & Multi-Layouts, Smart Arabic Intelligence,
 * Translation, Bulk Dictionary (.txt import), 37+ Searchable Shortcuts, Appearance/Dimensions,
 * Fonts, Layout Maker, and 56-Style Live Decoration Manager.
 */
public final class TypingAndDesignBuilder {

    public interface HostActions {
        void pickFontFile();
        void pickDictionaryTxtFile();
        void refreshScreen();
    }

    private TypingAndDesignBuilder() {
    }

    public static void buildLanguages(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions host) {
        root.addView(DetailUiKit.createSectionTitle(ctx, "تخطيط لوحة المفاتيح العربية (5 تخطيطات)"));
        LinearLayout arCard = DetailUiKit.createCard(ctx);
        String curAr = prefs.getArabicLayoutType();
        String[][] arLayouts = {
                {"default", "العربية الموحدة الافتراضية (ج ح خ... ض + ⌫ يمين)"},
                {"iphone", "تخطيط آيفون العربي (iOS)"},
                {"swiftkey", "تخطيط SwiftKey العربي"},
                {"pc", "تخطيط الكمبيوتر العربي (PC 12 مفتاح)"},
                {"arabic2", "عربية 2 (توزيع بديل)"}
        };
        for (String[] l : arLayouts) {
            arCard.addView(DetailUiKit.createChipOption(ctx, l[1], l[0].equals(curAr), v -> {
                prefs.setArabicLayoutType(l[0]);
                host.refreshScreen();
            }));
        }
        root.addView(arCard);

        root.addView(DetailUiKit.createSectionTitle(ctx, "تخطيط اللغة الإنجليزية (4 تخطيطات)"));
        LinearLayout enCard = DetailUiKit.createCard(ctx);
        String curEn = prefs.getEnglishLayoutType();
        String[][] enLayouts = {
                {"qwerty", "QWERTY القياسي"},
                {"azerty", "AZERTY الفرنسي"},
                {"qwertz", "QWERTZ الأوروبي"},
                {"colemak", "Colemak السريع"}
        };
        for (String[] l : enLayouts) {
            enCard.addView(DetailUiKit.createChipOption(ctx, l[1], l[0].equals(curEn), v -> {
                prefs.setEnglishLayoutType(l[0]);
                host.refreshScreen();
            }));
        }
        root.addView(enCard);

        root.addView(DetailUiKit.createSectionTitle(ctx, "الصفوف الإضافية وأوضاع الشاشة"));
        LinearLayout modesCard = DetailUiKit.createCard(ctx);
        modesCard.addView(DetailUiKit.createBodyText(ctx,
                "افتراضياً: لوحة المفاتيح موحّدة بعرض الشاشة الكامل بدون أي تقسيم في الوضع الأفقي والعمودي."));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "صف الأرقام الدائم (1-0)",
                "إظهار صف أرقام مستقل أعلى الحروف", prefs.isNumbersRowEnabled(), prefs::setNumbersRowEnabled));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "صف الإيموجي السريع بدلاً من الأرقام",
                "صف إيموجي فوري أعلى الحروف", prefs.isEmojiRowEnabled(), prefs::setEmojiRowEnabled));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "شريط الأدوات العلوي",
                "الأدوات المثبتة وزر المزيد (⋯)", prefs.isShowToolbar(), prefs::setShowToolbar));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "صف الإجراءات السريعة",
                "تحديد الكل، نسخ، قص، ولصق أعلى المفاتيح", prefs.isQuickActionsRowEnabled(), prefs::setQuickActionsRowEnabled));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "شريط آيفون السفلي",
                "أيقونة الكرة الأرضية والإخفاء أسفل اللوحة", prefs.isIphoneBottomBar(), prefs::setIphoneBottomBar));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "وضع اليد الواحدة (اختياري)",
                "تقليص عرض الكيبورد للكتابة بيد واحدة", prefs.isOneHandedModeEnabled(), prefs::setOneHandedModeEnabled));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "الوضع المنقسم للشاشات العريضة (اختياري)",
                "معطّل افتراضياً — فعّله فقط إذا رغبت في تقسيم اللوحة", prefs.isSplitModeEnabled(), prefs::setSplitModeEnabled));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "الوضع العائم (اختياري)",
                "لوحة مفاتيح عائمة بحواف مستديرة", prefs.isFloatingModeEnabled(), prefs::setFloatingModeEnabled));
        modesCard.addView(DetailUiKit.createSwitchRow(ctx, "لوحة أرقام يمينية في الوضع الأفقي (اختياري)",
                "إضافة Numpad على اليمين عند تدوير الشاشة", prefs.isLandscapeNumpadEnabled(), prefs::setLandscapeNumpadEnabled));
        root.addView(modesCard);
    }

    public static void buildSmartTyping(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions host) {
        root.addView(DetailUiKit.createSectionTitle(ctx, "شريط الاقتراحات والتصحيح"));
        LinearLayout barCard = DetailUiKit.createCard(ctx);
        barCard.addView(DetailUiKit.createSwitchRow(ctx, "إظهار شريط الاقتراحات",
                "اقتراحات فورية من قاعدة بيانات SQLite خفيفة", prefs.isSuggestionsEnabled(), prefs::setSuggestionsEnabled));
        String curBar = prefs.getSuggestionBarMode();
        barCard.addView(DetailUiKit.createChipOption(ctx, "شريط قابل للتمرير (حتى 8 اقتراحات)", "scroll".equals(curBar), v -> {
            prefs.setSuggestionBarMode("scroll"); host.refreshScreen();
        }));
        barCard.addView(DetailUiKit.createChipOption(ctx, "3 اقتراحات ثابتة", "3".equals(curBar), v -> {
            prefs.setSuggestionBarMode("3"); host.refreshScreen();
        }));
        barCard.addView(DetailUiKit.createChipOption(ctx, "5 اقتراحات ثابتة", "5".equals(curBar), v -> {
            prefs.setSuggestionBarMode("5"); host.refreshScreen();
        }));
        root.addView(barCard);

        root.addView(DetailUiKit.createSectionTitle(ctx, "محرك الذكاء العربي الفوري (بدون إنترنت)"));
        LinearLayout smartCard = DetailUiKit.createCard(ctx);
        smartCard.addView(DetailUiKit.createSwitchRow(ctx, "التصحيح التلقائي عند المسافة",
                "تصحيح الكلمات الشائعة وتطبيق الاختصارات تلقائياً", prefs.isAutoCorrectEnabled(), prefs::setAutoCorrectEnabled));
        smartCard.addView(DetailUiKit.createSwitchRow(ctx, "توقع الكلمة التالية",
                "مثال: السلام ← عليكم ← ورحمة", prefs.isNextWordPredictionEnabled(), prefs::setNextWordPredictionEnabled));
        smartCard.addView(DetailUiKit.createSwitchRow(ctx, "مصحّح الهمزات والتاء المربوطة",
                "تحويل (انا ← أنا، الى ← إلى، مدرسه ← مدرسة)", prefs.isHamzaTaFixerEnabled(), prefs::setHamzaTaFixerEnabled));
        smartCard.addView(DetailUiKit.createSwitchRow(ctx, "محوّل الفرانكو إلى عربي (Arabizi)",
                "تحويل (7abibi ← حبيبي، salam ← سلام، shokran ← شكراً)", prefs.isFrancoArabicEnabled(), prefs::setFrancoArabicEnabled));
        smartCard.addView(DetailUiKit.createSwitchRow(ctx, "الحاسبة الفورية داخل الكتابة",
                "اكتب (15*3+2=) ليظهر الناتج 47 فوراً", prefs.isInlineMathEnabled(), prefs::setInlineMathEnabled));
        smartCard.addView(DetailUiKit.createSwitchRow(ctx, "اقتراحات الأذكار السريعة",
                "إكمال تلقائي للأذكار عند كتابة سبحان / استغفر / اللهم", prefs.isAzkarSuggestionsEnabled(), prefs::setAzkarSuggestionsEnabled));
        smartCard.addView(DetailUiKit.createSwitchRow(ctx, "تصحيح الاختصارات الإنجليزية",
                "تحويل (dont ← don't، im ← I'm، cant ← can't)", prefs.isEnglishContractionsEnabled(), prefs::setEnglishContractionsEnabled));
        smartCard.addView(DetailUiKit.createSwitchRow(ctx, "المسافة الذكية بعد علامات الترقيم",
                "إضافة مسافة تلقائية بعد النقطة والفاصلة وعلامة الاستفهام", prefs.isSmartPunctuationEnabled(), prefs::setSmartPunctuationEnabled));
        root.addView(smartCard);

        root.addView(DetailUiKit.createSectionTitle(ctx, "قائمة استثناء تطبيقات البنوك (خصوصية قصوى)"));
        LinearLayout bankCard = DetailUiKit.createCard(ctx);
        bankCard.addView(DetailUiKit.createBodyText(ctx,
                "في التطبيقات المدرجة أدناه يتم تعطيل الحافظة والزخرفة والتعلم تلقائياً (افصل بين أسماء الحزم بفاصلة):"));
        EditText etBanks = DetailUiKit.createInput(ctx, "com.alrajhibank,com.snb.alahli", false);
        etBanks.setText(prefs.getBankExclusionApps());
        bankCard.addView(etBanks);
        bankCard.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ قائمة استثناء البنوك", v -> {
            prefs.setBankExclusionApps(etBanks.getText().toString().trim());
            Toast.makeText(ctx, "تم تحديث قائمة الاستثناء بنجاح ✅", Toast.LENGTH_SHORT).show();
        }));
        root.addView(bankCard);
    }

    public static void buildTranslation(Context ctx, LinearLayout root, KeyboardPrefs prefs) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "إعدادات الترجمة الفورية"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "تفعيل الترجمة السريعة في شريط الأدوات",
                "استخدم أيقونة 🌐 داخل الكيبورد لترجمة نصك فوراً",
                prefs.isOfflineTranslationEnabled(), prefs::setOfflineTranslationEnabled));
        EditText etLang = DetailUiKit.createInput(ctx, "اللغة المستهدفة (مثال: English أو Arabic أو French)", false);
        etLang.setText(prefs.getTranslationTargetLang());
        card.addView(etLang);
        card.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ اللغة المستهدفة", v -> {
            prefs.setTranslationTargetLang(etLang.getText().toString().trim());
            Toast.makeText(ctx, "تم حفظ لغة الترجمة ✅", Toast.LENGTH_SHORT).show();
        }));
        root.addView(card);
    }

    public static void buildDictionary(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions host) {
        KeyboardSqliteHelper db = KeyboardSqliteHelper.getInstance(ctx);

        LinearLayout addCard = DetailUiKit.createCard(ctx);
        addCard.addView(DetailUiKit.createSectionTitle(ctx, "إضافة كلمات أو استيراد جماعي (.txt)"));
        EditText etWord = DetailUiKit.createInput(ctx, "أضف كلمة أو الصق عدة كلمات (سطر لكل كلمة)...", false);
        etWord.setMinLines(2);
        addCard.addView(etWord);

        addCard.addView(DetailUiKit.createPrimaryButton(ctx, "+ إضافة للقاموس الشخصي", v -> {
            String raw = etWord.getText().toString().trim();
            if (raw.isEmpty()) return;
            List<String> lines = new ArrayList<>();
            for (String s : raw.split("[\\r\\n,]+")) {
                if (!s.trim().isEmpty()) lines.add(s.trim());
            }
            db.bulkAddPersonalWords(lines);
            Toast.makeText(ctx, "تمت إضافة " + lines.size() + " كلمة إلى القاموس ✅", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));

        addCard.addView(DetailUiKit.createChipOption(ctx, "📄 استيراد قاموس من ملف نصي (.txt)", true, v ->
                host.pickDictionaryTxtFile()));

        addCard.addView(DetailUiKit.createChipOption(ctx, "🗑 مسح جميع الكلمات المتعلّمة والشخصية", false, v -> {
            db.clearPersonalWords();
            Toast.makeText(ctx, "تم مسح الكلمات المتعلّمة", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));
        root.addView(addCard);

        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "كلماتك المحفوظة في SQLite"));
        List<String> words = db.queryPersonalWords(60);
        if (words.isEmpty()) {
            listCard.addView(DetailUiKit.createBodyText(ctx, "لا توجد كلمات شخصية بعد — اكتب أو أضف كلماتك أعلاه."));
        } else {
            for (String w : words) {
                listCard.addView(DetailUiKit.createChipOption(ctx, w + "   ✕", false, v -> {
                    db.deletePersonalWord(w);
                    host.refreshScreen();
                }));
            }
        }
        root.addView(listCard);
    }

    public static void buildShortcuts(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions host) {
        LinearLayout addCard = DetailUiKit.createCard(ctx);
        addCard.addView(DetailUiKit.createSectionTitle(ctx, "إضافة اختصار نصي جديد"));
        EditText etKey = DetailUiKit.createInput(ctx, "الاختصار (مثال: سع أو brb)", false);
        EditText etVal = DetailUiKit.createInput(ctx, "النص الكامل (مثال: السلام عليكم ورحمة الله وبركاته)", false);
        addCard.addView(etKey);
        addCard.addView(etVal);
        addCard.addView(DetailUiKit.createPrimaryButton(ctx, "+ حفظ الاختصار", v -> {
            String k = etKey.getText().toString().trim();
            String val = etVal.getText().toString().trim();
            if (k.isEmpty() || val.isEmpty()) return;
            Map<String, String> map = prefs.getTextShortcuts();
            map.put(k, val);
            prefs.saveTextShortcuts(map);
            Toast.makeText(ctx, "تم حفظ الاختصار ✅", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));
        root.addView(addCard);

        Map<String, String> shortcuts = prefs.getTextShortcuts();
        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "الاختصارات الجاهزة (" + shortcuts.size() + " اختصار)"));

        EditText etSearch = DetailUiKit.createInput(ctx, "ابحث داخل الاختصارات...", false);
        listCard.addView(etSearch);

        LinearLayout itemsBox = new LinearLayout(ctx);
        itemsBox.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(itemsBox);

        Runnable render = () -> {
            itemsBox.removeAllViews();
            String q = etSearch.getText().toString().trim().toLowerCase();
            for (Map.Entry<String, String> e : shortcuts.entrySet()) {
                if (!q.isEmpty() && !e.getKey().toLowerCase().contains(q) && !e.getValue().toLowerCase().contains(q)) {
                    continue;
                }
                itemsBox.addView(DetailUiKit.createChipOption(ctx,
                        e.getKey() + "  ←  " + e.getValue() + "   ✕", false, v -> {
                            Map<String, String> m = prefs.getTextShortcuts();
                            m.remove(e.getKey());
                            prefs.saveTextShortcuts(m);
                            host.refreshScreen();
                        }));
            }
        };
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        render.run();
        root.addView(listCard);
    }

    public static void buildAppearance(Context ctx, LinearLayout root, KeyboardPrefs prefs) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "أبعاد مستقلة للوضع الطولي والأفقي"));
        card.addView(DetailUiKit.createSliderRow(ctx, "ارتفاع الكيبورد (طولي Portrait)", 190, 380,
                prefs.getPortraitHeightDp(), "dp", prefs::setPortraitHeightDp));
        card.addView(DetailUiKit.createSliderRow(ctx, "ارتفاع الكيبورد (أفقي Landscape)", 130, 260,
                prefs.getLandscapeHeightDp(), "dp", prefs::setLandscapeHeightDp));
        card.addView(DetailUiKit.createSliderRow(ctx, "حجم خط المفاتيح (طولي)", 13, 26,
                prefs.getPortraitKeyTextSp(), "sp", prefs::setPortraitKeyTextSp));
        card.addView(DetailUiKit.createSliderRow(ctx, "حجم خط المفاتيح (أفقي)", 12, 22,
                prefs.getLandscapeKeyTextSp(), "sp", prefs::setLandscapeKeyTextSp));
        card.addView(DetailUiKit.createSliderRow(ctx, "الهامش السفلي للوحة", 0, 48,
                prefs.getBottomMarginDp(), "dp", prefs::setBottomMarginDp));
        root.addView(card);

        LinearLayout blurCard = DetailUiKit.createCard(ctx);
        blurCard.addView(DetailUiKit.createSectionTitle(ctx, "تأثيرات الزجاج الكريستالي والتمويه"));
        blurCard.addView(DetailUiKit.createSwitchRow(ctx, "خلفية شفافة مموهة (Hardware Blur)",
                "تمويه حقيقي على Android 12+ وشفافية خفيفة على أندرويد 8-11",
                prefs.isTransparentBlurEnabled(), prefs::setTransparentBlurEnabled));
        blurCard.addView(DetailUiKit.createSwitchRow(ctx, "فرض أزرار زجاج كريستال على كل الثيمات",
                "تطبيق تأثير الزجاج اللامع فوق الثيم الحالي",
                prefs.isGlassKeysForced(), prefs::setGlassKeysForced));
        blurCard.addView(DetailUiKit.createSliderRow(ctx, "شدة التمويه / الشفافية", 0, 100,
                prefs.getBlurStrength(), "%", prefs::setBlurStrength));
        root.addView(blurCard);
    }

    public static void buildFonts(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "خط واجهة الكيبورد (TTF / OTF)"));
        String cur = prefs.getCustomUiFontPath();
        card.addView(DetailUiKit.createBodyText(ctx,
                cur.isEmpty() ? "الخط الحالي: خط النظام الافتراضي العريض" : "خط مخصص: " + cur));
        card.addView(DetailUiKit.createPrimaryButton(ctx, "📂 استيراد ملف خط (.ttf / .otf)", v -> host.pickFontFile()));
        if (!cur.isEmpty()) {
            card.addView(DetailUiKit.createChipOption(ctx, "استعادة خط النظام الافتراضي", false, v -> {
                prefs.setCustomUiFontPath("");
                host.refreshScreen();
            }));
        }
        root.addView(card);
    }

    public static void buildLayoutEditor(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "صانع وتعديل تخطيط الحروف العربية"));
        card.addView(DetailUiKit.createBodyText(ctx,
                "اكتب 3 أسطر مفصولة بمسافات بين كل مفتاح. اترك الحقل فارغاً لاستخدام التخطيط الموحد الافتراضي:"));
        EditText etRows = DetailUiKit.createInput(ctx,
                "ض ص ث ق ف غ ع ه خ ح ج\nش س ي ب ل ا ت ن م ك ط\nذ ء ؤ ر ى ة و ز ظ د", false);
        etRows.setMinLines(4);
        String saved = prefs.getCustomArabicLayoutJson();
        if (!saved.isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(saved);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length(); i++) {
                    if (i > 0) sb.append('\n');
                    sb.append(arr.getString(i));
                }
                etRows.setText(sb.toString());
            } catch (Exception ignored) {
            }
        }
        card.addView(etRows);
        card.addView(DetailUiKit.createPrimaryButton(ctx, "✅ حفظ التخطيط المخصص", v -> {
            String raw = etRows.getText().toString().trim();
            if (raw.isEmpty()) {
                prefs.setCustomArabicLayoutJson("");
            } else {
                org.json.JSONArray arr = new org.json.JSONArray();
                for (String line : raw.split("\\r?\\n")) {
                    if (!line.trim().isEmpty()) arr.put(line.trim());
                }
                prefs.setCustomArabicLayoutJson(arr.toString());
            }
            Toast.makeText(ctx, "تم حفظ التخطيط بنجاح ✅", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));
        card.addView(DetailUiKit.createChipOption(ctx, "↺ استعادة التخطيط العربي الموحد الافتراضي", false, v -> {
            prefs.setCustomArabicLayoutJson("");
            Toast.makeText(ctx, "تمت استعادة التخطيط الافتراضي", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));
        root.addView(card);
    }

    public static void buildDecorationManager(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions host) {
        LinearLayout topCard = DetailUiKit.createCard(ctx);
        topCard.addView(DetailUiKit.createSectionTitle(ctx, "الزخرفة الفورية (26 عربي + 30 إنجليزي)"));
        topCard.addView(DetailUiKit.createBodyText(ctx,
                "تتوقف الزخرفة تلقائياً في حقول كلمات المرور والبريد الإلكتروني والروابط وتطبيقات البنوك لحمايتك."));
        topCard.addView(DetailUiKit.createSwitchRow(ctx, "تفعيل الزخرفة أثناء الكتابة",
                "تطبيق النمط المختار مباشرة في أي تطبيق", prefs.isDecorationEnabled(), prefs::setDecorationEnabled));

        EditText etTest = DetailUiKit.createInput(ctx, "اكتب نصاً للمعاينة الحية لكل الأنماط...", false);
        etTest.setText("كيبورد برو Keyboard Pro");
        topCard.addView(etTest);

        TextStyleEngine engine = new TextStyleEngine();
        TextView tvLiveOut = DetailUiKit.createBodyText(ctx, engine.transformText(etTest.getText().toString(), prefs.getActiveStyleId()));
        tvLiveOut.setTextSize(16);
        topCard.addView(tvLiveOut);

        etTest.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvLiveOut.setText(engine.transformText(s == null ? "" : s.toString(), prefs.getActiveStyleId()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        root.addView(topCard);

        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "اختر نمط الزخرفة (56 نمطاً جاهزاً)"));
        String curId = prefs.getActiveStyleId();
        for (TextStyleEngine.StyleOption opt : TextStyleEngine.getAllStyles()) {
            boolean sel = opt.id.equals(curId);
            listCard.addView(DetailUiKit.createChipOption(ctx,
                    opt.displayName + "  —  " + opt.samplePreview, sel, v -> {
                        prefs.setActiveStyleId(opt.id);
                        prefs.setDecorationEnabled(!TextStyleEngine.STYLE_NORMAL.equals(opt.id));
                        host.refreshScreen();
                    }));
        }
        root.addView(listCard);
    }
}
