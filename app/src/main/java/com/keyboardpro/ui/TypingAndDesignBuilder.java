package com.keyboardpro.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.R;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.ime.AiKeyboardHelper;
import com.keyboardpro.ime.KeyboardLayoutData;
import com.keyboardpro.ime.MiniKeyboardPreviewView;
import com.keyboardpro.ime.TextStyleEngine;
import com.keyboardpro.ime.ThemeRegistry;

import org.json.JSONArray;

import java.util.List;
import java.util.Map;

/**
 * Builds Settings sub-screens for Languages, Smart Typing, Translation, Dictionary,
 * Text Shortcuts, Appearance/Blur/Glass/Dimensions, Fonts, Key Layout Editor, and Live Decoration.
 */
public final class TypingAndDesignBuilder {

    public interface HostActions {
        void pickFontFile();
        void refreshScreen();
    }

    private TypingAndDesignBuilder() {
    }

    public static void buildLanguages(Context ctx, LinearLayout root, KeyboardPrefs prefs) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "تخطيط اللغات والمفاتيح"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "صف الأرقام الدائم",
                "إظهار صف الأرقام (١-٠ / 1-0) أعلى الأحرف دائماً",
                prefs.isNumbersRowEnabled(), prefs::setNumbersRowEnabled));
        card.addView(DetailUiKit.createSwitchRow(ctx, "تخطيط آيفون (iPhone Layout)",
                "ترتيب أحرف عربية شبيه بلوحة مفاتيح iOS",
                prefs.isIphoneLayoutEnabled(), prefs::setIphoneLayoutEnabled));
        card.addView(DetailUiKit.createBodyText(ctx,
                "ملاحظة: الوضع الأفقي (Landscape) يعمل دائماً بملء العرض الكامل (No-Split) بدون أي فراغ وسطي مع تعطيل وضع الشاشة الكاملة."));
        root.addView(card);

        LinearLayout langCard = DetailUiKit.createCard(ctx);
        langCard.addView(DetailUiKit.createSectionTitle(ctx, "اللغة الافتراضية عند فتح الكيبورد"));
        String cur = prefs.getKeyboardLanguage();
        langCard.addView(DetailUiKit.createChipOption(ctx, "العربية (Arabic)", "ar".equals(cur), v -> {
            prefs.setKeyboardLanguage("ar");
            Toast.makeText(ctx, "تم اختيار العربية كلغة افتراضية", Toast.LENGTH_SHORT).show();
        }));
        langCard.addView(DetailUiKit.createChipOption(ctx, "الإنجليزية (English QWERTY)", "en".equals(cur), v -> {
            prefs.setKeyboardLanguage("en");
            Toast.makeText(ctx, "English selected as default", Toast.LENGTH_SHORT).show();
        }));
        root.addView(langCard);
    }

    public static void buildSmartTyping(Context ctx, LinearLayout root, KeyboardPrefs prefs) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "الذكاء اللغوي والاقتراحات"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "شريط الاقتراحات",
                "عرض الكلمات المقترحة والاختصارات أعلى لوحة المفاتيح",
                prefs.isSuggestionsEnabled(), prefs::setSuggestionsEnabled));
        card.addView(DetailUiKit.createSwitchRow(ctx, "التصحيح التلقائي",
                "تصحيح الأخطاء الإملائية الشائعة أثناء الكتابة السريعة",
                prefs.isAutoCorrectEnabled(), prefs::setAutoCorrectEnabled));
        card.addView(DetailUiKit.createSwitchRow(ctx, "التنبؤ بالكلمة التالية",
                "توقع الكلمة القادمة تلقائياً بناءً على السياق",
                prefs.isNextWordPredictionEnabled(), prefs::setNextWordPredictionEnabled));
        root.addView(card);
    }

    public static void buildTranslation(Context ctx, LinearLayout root, KeyboardPrefs prefs) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "إعدادات الترجمة الفورية"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "الترجمة بدون إنترنت (Offline)",
                "ترجمة العبارات العربية والإنجليزية الشائعة فورياً بدون اتصال",
                prefs.isOfflineTranslationEnabled(), prefs::setOfflineTranslationEnabled));

        String[] langs = {"English", "Arabic", "French", "Spanish", "Turkish", "German"};
        String current = prefs.getTranslationTargetLang();
        card.addView(DetailUiKit.createBodyText(ctx, "لغة الترجمة المستهدفة: " + current));
        for (String lang : langs) {
            card.addView(DetailUiKit.createChipOption(ctx, lang, lang.equals(current), v -> {
                prefs.setTranslationTargetLang(lang);
                Toast.makeText(ctx, "لغة الترجمة: " + lang, Toast.LENGTH_SHORT).show();
            }));
        }
        root.addView(card);

        LinearLayout testCard = DetailUiKit.createCard(ctx);
        testCard.addView(DetailUiKit.createSectionTitle(ctx, "مترجم فوري سريع"));
        EditText etInput = DetailUiKit.createInput(ctx, "اكتب عبارة للترجمة (مثلاً: صباح الخير)...", false);
        TextView tvResult = DetailUiKit.createBodyText(ctx, "النتيجة ستظهر هنا...");
        testCard.addView(etInput);
        testCard.addView(DetailUiKit.createPrimaryButton(ctx, "ترجمة الآن", v -> {
            tvResult.setText("جاري الترجمة...");
            new AiKeyboardHelper(ctx).processText(etInput.getText().toString(),
                    AiKeyboardHelper.Action.TRANSLATE,
                    (ok, out) -> tvResult.setText(out));
        }));
        testCard.addView(tvResult);
        root.addView(testCard);
    }

    public static void buildDictionary(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions actions) {
        com.keyboardpro.data.KeyboardSqliteHelper db = com.keyboardpro.data.KeyboardSqliteHelper.getInstance(ctx);
        LinearLayout addCard = DetailUiKit.createCard(ctx);
        addCard.addView(DetailUiKit.createSectionTitle(ctx, "إضافة كلمة إلى القاموس الشخصي (SQLite مفهرس)"));
        EditText etWord = DetailUiKit.createInput(ctx, "اكتب الكلمة الجديدة...", false);
        addCard.addView(etWord);
        addCard.addView(DetailUiKit.createPrimaryButton(ctx, "إضافة للقاموس", v -> {
            String w = etWord.getText().toString().trim();
            if (w.isEmpty()) return;
            db.addPersonalWord(w);
            List<String> list = db.queryPersonalWords(200);
            prefs.savePersonalDictionary(list);
            actions.refreshScreen();
        }));
        root.addView(addCard);

        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "الكلمات المحفوظة"));
        List<String> words = db.queryPersonalWords(50);
        for (String w : words) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, DetailUiKit.dp(ctx, 6), 0, DetailUiKit.dp(ctx, 6));

            TextView tv = new TextView(ctx);
            tv.setText(w);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
            tv.setTextSize(14);
            row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = new TextView(ctx);
            del.setText("حذف");
            del.setTextColor(ContextCompat.getColor(ctx, R.color.kp_danger));
            del.setPadding(DetailUiKit.dp(ctx, 10), DetailUiKit.dp(ctx, 4), DetailUiKit.dp(ctx, 10), DetailUiKit.dp(ctx, 4));
            del.setOnClickListener(v -> {
                db.deletePersonalWord(w);
                prefs.savePersonalDictionary(db.queryPersonalWords(200));
                actions.refreshScreen();
            });
            row.addView(del);
            listCard.addView(row);
        }
        root.addView(listCard);
    }

    public static void buildShortcuts(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions actions) {
        LinearLayout addCard = DetailUiKit.createCard(ctx);
        addCard.addView(DetailUiKit.createSectionTitle(ctx, "إضافة اختصار نصي جديد"));
        EditText etKey = DetailUiKit.createInput(ctx, "الاختصار (مثلاً: سع)", false);
        EditText etVal = DetailUiKit.createInput(ctx, "النص الكامل (مثلاً: السلام عليكم ورحمة الله)", false);
        addCard.addView(etKey);
        addCard.addView(etVal);
        addCard.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ الاختصار", v -> {
            String k = etKey.getText().toString().trim();
            String val = etVal.getText().toString().trim();
            if (k.isEmpty() || val.isEmpty()) return;
            Map<String, String> map = prefs.getTextShortcuts();
            map.put(k, val);
            prefs.saveTextShortcuts(map);
            actions.refreshScreen();
        }));
        root.addView(addCard);

        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "اختصاراتي النشطة"));
        for (Map.Entry<String, String> e : prefs.getTextShortcuts().entrySet()) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, DetailUiKit.dp(ctx, 6), 0, DetailUiKit.dp(ctx, 6));

            TextView tv = new TextView(ctx);
            tv.setText(e.getKey() + "  ←  " + e.getValue());
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
            tv.setTextSize(13.5f);
            row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = new TextView(ctx);
            del.setText("حذف");
            del.setTextColor(ContextCompat.getColor(ctx, R.color.kp_danger));
            del.setOnClickListener(v -> {
                Map<String, String> map = prefs.getTextShortcuts();
                map.remove(e.getKey());
                prefs.saveTextShortcuts(map);
                actions.refreshScreen();
            });
            row.addView(del);
            listCard.addView(row);
        }
        root.addView(listCard);
    }

    public static void buildAppearance(Context ctx, LinearLayout root, KeyboardPrefs prefs) {
        LinearLayout previewCard = DetailUiKit.createCard(ctx);
        previewCard.addView(DetailUiKit.createSectionTitle(ctx, "معاينة الثيم الحالي"));
        MiniKeyboardPreviewView preview = new MiniKeyboardPreviewView(ctx);
        preview.setTheme(ThemeRegistry.getThemeById(ctx, prefs.getActiveThemeId()));
        previewCard.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, DetailUiKit.dp(ctx, 130)));
        root.addView(previewCard);

        LinearLayout dimCard = DetailUiKit.createCard(ctx);
        dimCard.addView(DetailUiKit.createSectionTitle(ctx, "الأبعاد المستقلة (عمودي / أفقي)"));
        dimCard.addView(DetailUiKit.createSliderRow(ctx, "ارتفاع الكيبورد (عمودي Portrait)", 200, 360,
                prefs.getPortraitHeightDp(), "dp", prefs::setPortraitHeightDp));
        dimCard.addView(DetailUiKit.createSliderRow(ctx, "ارتفاع الكيبورد (أفقي Landscape)", 140, 250,
                prefs.getLandscapeHeightDp(), "dp", prefs::setLandscapeHeightDp));
        dimCard.addView(DetailUiKit.createSliderRow(ctx, "حجم أحرف المفاتيح (عمودي)", 14, 25,
                prefs.getPortraitKeyTextSp(), "sp", prefs::setPortraitKeyTextSp));
        dimCard.addView(DetailUiKit.createSliderRow(ctx, "حجم أحرف المفاتيح (أفقي)", 12, 21,
                prefs.getLandscapeKeyTextSp(), "sp", prefs::setLandscapeKeyTextSp));
        dimCard.addView(DetailUiKit.createSliderRow(ctx, "استدارة زوايا المفاتيح", 0, 20,
                prefs.getKeyCornerRadiusDp(), "dp", prefs::setKeyCornerRadiusDp));
        root.addView(dimCard);

        LinearLayout fxCard = DetailUiKit.createCard(ctx);
        fxCard.addView(DetailUiKit.createSectionTitle(ctx, "الزجاج الكريستالي والتمويه الشفاف (Pro)"));
        fxCard.addView(DetailUiKit.createSwitchRow(ctx, "خلفية شفافة مموهة (Blur)",
                "إظهار التطبيق خلف الكيبورد مع تأثير ضبابي زجاجي فوري",
                prefs.isTransparentBlurEnabled(), prefs::setTransparentBlurEnabled));
        fxCard.addView(DetailUiKit.createSliderRow(ctx, "قوة التمويه والشفافية", 10, 95,
                prefs.getBlurStrength(), "%", prefs::setBlurStrength));
        fxCard.addView(DetailUiKit.createSwitchRow(ctx, "مفاتيح كريستال وزجاج مضيء",
                "تفعيل الحدود المتوهجة واللمعان الزجاجي على جميع الثيمات",
                prefs.isGlassKeysForced(), prefs::setGlassKeysForced));
        fxCard.addView(DetailUiKit.createSwitchRow(ctx, "إظهار شريط الأدوات العلوي",
                "شريط الزخرفة السريعة، الذكاء الاصطناعي، الحافظة، والخزنة",
                prefs.isShowToolbar(), prefs::setShowToolbar));
        root.addView(fxCard);
    }

    public static void buildFonts(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions actions) {
        LinearLayout uiFontCard = DetailUiKit.createCard(ctx);
        uiFontCard.addView(DetailUiKit.createSectionTitle(ctx, "خط واجهة الكيبورد (استيراد من ملف)"));
        String currentPath = prefs.getCustomUiFontPath();
        uiFontCard.addView(DetailUiKit.createBodyText(ctx,
                currentPath.isEmpty() ? "الخط الحالي: خط النظام الافتراضي العريض"
                        : "ملف الخط المخصص: " + currentPath));
        uiFontCard.addView(DetailUiKit.createPrimaryButton(ctx, "استيراد ملف خط (.ttf / .otf)", v -> actions.pickFontFile()));
        if (!currentPath.isEmpty()) {
            uiFontCard.addView(DetailUiKit.createChipOption(ctx, "إعادة الخط الافتراضي", false, v -> {
                prefs.setCustomUiFontPath("");
                actions.refreshScreen();
            }));
        }
        root.addView(uiFontCard);

        LinearLayout unicodeCard = DetailUiKit.createCard(ctx);
        unicodeCard.addView(DetailUiKit.createSectionTitle(ctx, "خطوط الكتابة الإنجليزية الفورية (12 خط يونيكود)"));
        String activeId = prefs.getActiveStyleId();
        for (TextStyleEngine.StyleOption opt : TextStyleEngine.getAllStyles()) {
            if (opt.isArabic && !TextStyleEngine.STYLE_NORMAL.equals(opt.id)) continue;
            boolean sel = opt.id.equals(activeId) && prefs.isDecorationEnabled();
            unicodeCard.addView(DetailUiKit.createChipOption(ctx,
                    opt.displayName + "  —  " + opt.samplePreview, sel, v -> {
                        if (TextStyleEngine.STYLE_NORMAL.equals(opt.id)) {
                            prefs.setDecorationEnabled(false);
                            prefs.setActiveStyleId(TextStyleEngine.STYLE_NORMAL);
                        } else {
                            prefs.setDecorationEnabled(true);
                            prefs.setActiveStyleId(opt.id);
                        }
                        Toast.makeText(ctx, "تم تفعيل الخط: " + opt.shortLabel, Toast.LENGTH_SHORT).show();
                        actions.refreshScreen();
                    }));
        }
        root.addView(unicodeCard);
    }

    public static void buildDecorationManager(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions actions) {
        TextStyleEngine engine = new TextStyleEngine();

        LinearLayout toggleCard = DetailUiKit.createCard(ctx);
        toggleCard.addView(DetailUiKit.createSectionTitle(ctx, "محرك الزخرفة والخطوط الفوري"));
        toggleCard.addView(DetailUiKit.createSwitchRow(ctx, "تفعيل الزخرفة التلقائية أثناء الكتابة",
                "تحويل الأحرف العربية والإنجليزية مباشرة قبل الإدخال في أي تطبيق",
                prefs.isDecorationEnabled(), prefs::setDecorationEnabled));

        EditText etPreview = DetailUiKit.createInput(ctx, "اكتب نصاً عربياً أو إنجليزياً للمعاينة الفورية...", false);
        etPreview.setText("كيبورد برو Keyboard Pro");
        toggleCard.addView(etPreview);
        root.addView(toggleCard);

        LinearLayout stylesList = DetailUiKit.createCard(ctx);
        stylesList.addView(DetailUiKit.createSectionTitle(ctx, "معاينة وتفعيل أنماط الزخرفة (اضغط للتفعيل / مطوّل للنسخ)"));
        LinearLayout dynamicRows = new LinearLayout(ctx);
        dynamicRows.setOrientation(LinearLayout.VERTICAL);
        stylesList.addView(dynamicRows);

        Runnable renderSamples = () -> {
            dynamicRows.removeAllViews();
            String input = etPreview.getText().toString();
            if (input.isEmpty()) input = "كيبورد برو Keyboard Pro";
            String active = prefs.getActiveStyleId();

            for (TextStyleEngine.StyleOption opt : TextStyleEngine.getAllStyles()) {
                String rendered = engine.transformText(input, opt.id);
                boolean selected = opt.id.equals(active) && prefs.isDecorationEnabled();
                TextView row = DetailUiKit.createChipOption(ctx,
                        opt.displayName + "\n" + rendered, selected, v -> {
                            boolean enable = !TextStyleEngine.STYLE_NORMAL.equals(opt.id);
                            prefs.setDecorationEnabled(enable);
                            prefs.setActiveStyleId(opt.id);
                            Toast.makeText(ctx, "تم تفعيل: " + opt.displayName, Toast.LENGTH_SHORT).show();
                            actions.refreshScreen();
                        });
                row.setOnLongClickListener(v -> {
                    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("decorated", rendered));
                        Toast.makeText(ctx, "تم نسخ النص المزخرف!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
                dynamicRows.addView(row);
            }
        };
        renderSamples.run();

        etPreview.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderSamples.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        root.addView(stylesList);

        LinearLayout symCard = DetailUiKit.createCard(ctx);
        symCard.addView(DetailUiKit.createSectionTitle(ctx, "الرموز الزخرفية المخصصة"));
        EditText etSyms = DetailUiKit.createInput(ctx, "أضف رموزك الزخرفية المفضلة...", false);
        etSyms.setText(prefs.getCustomSymbols());
        symCard.addView(etSyms);
        symCard.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ الرموز المخصصة", v -> {
            prefs.setCustomSymbols(etSyms.getText().toString());
            Toast.makeText(ctx, "تم حفظ الرموز المخصصة", Toast.LENGTH_SHORT).show();
        }));
        root.addView(symCard);
    }

    public static void buildLayoutEditor(Context ctx, LinearLayout root, KeyboardPrefs prefs, HostActions actions) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "محرر تخطيط المفاتيح التفاعلي"));
        card.addView(DetailUiKit.createBodyText(ctx,
                "اضغط على أي مفتاحين لتبديل موقعيهما فوراً، ثم اضغط «حفظ التخطيط» لتطبيقه على لوحة المفاتيح."));

        String[][] grid = KeyboardLayoutData.parseCustomArabicOrDefault(
                prefs.getCustomArabicLayoutJson(), prefs.isIphoneLayoutEnabled());
        final int[] firstRow = {-1};
        final int[] firstCol = {-1};

        LinearLayout board = new LinearLayout(ctx);
        board.setOrientation(LinearLayout.VERTICAL);

        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            board.removeAllViews();
            for (int r = 0; r < grid.length; r++) {
                LinearLayout rowView = new LinearLayout(ctx);
                rowView.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, DetailUiKit.dp(ctx, 44));
                rlp.bottomMargin = DetailUiKit.dp(ctx, 6);
                rowView.setLayoutParams(rlp);

                for (int c = 0; c < grid[r].length; c++) {
                    final int fr = r;
                    final int fc = c;
                    TextView key = new TextView(ctx);
                    key.setText(grid[r][c]);
                    key.setGravity(Gravity.CENTER);
                    key.setTypeface(Typeface.DEFAULT_BOLD);
                    key.setTextSize(15);
                    boolean isSel = (r == firstRow[0] && c == firstCol[0]);
                    key.setBackgroundResource(isSel ? R.drawable.bg_chip_selected : R.drawable.bg_search_bar);
                    key.setTextColor(ContextCompat.getColor(ctx,
                            isSel ? R.color.kp_primary : R.color.kp_text_primary));
                    LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                    klp.setMargins(DetailUiKit.dp(ctx, 2), 0, DetailUiKit.dp(ctx, 2), 0);
                    key.setOnClickListener(v -> {
                        if (firstRow[0] == -1) {
                            firstRow[0] = fr;
                            firstCol[0] = fc;
                        } else {
                            String tmp = grid[firstRow[0]][firstCol[0]];
                            grid[firstRow[0]][firstCol[0]] = grid[fr][fc];
                            grid[fr][fc] = tmp;
                            firstRow[0] = -1;
                            firstCol[0] = -1;
                        }
                        rebuild[0].run();
                    });
                    rowView.addView(key, klp);
                }
                board.addView(rowView);
            }
        };
        rebuild[0].run();
        card.addView(board);

        card.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ التخطيط المخصص", v -> {
            JSONArray outer = new JSONArray();
            for (String[] row : grid) {
                JSONArray inner = new JSONArray();
                for (String k : row) inner.put(k);
                outer.put(inner);
            }
            prefs.setCustomArabicLayoutJson(outer.toString());
            Toast.makeText(ctx, "تم حفظ تخطيط المفاتيح المخصص ✅", Toast.LENGTH_SHORT).show();
        }));

        card.addView(DetailUiKit.createChipOption(ctx, "استعادة التخطيط الافتراضي", false, v -> {
            prefs.setCustomArabicLayoutJson("");
            Toast.makeText(ctx, "تمت استعادة التخطيط الافتراضي", Toast.LENGTH_SHORT).show();
            actions.refreshScreen();
        }));

        root.addView(card);
    }
}
