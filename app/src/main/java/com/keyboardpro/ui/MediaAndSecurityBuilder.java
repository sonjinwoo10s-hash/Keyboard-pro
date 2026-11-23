package com.keyboardpro.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.R;
import com.keyboardpro.data.ClipboardRepository;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.data.StickerRepository;
import com.keyboardpro.data.VaultRepository;
import com.keyboardpro.ime.AiKeyboardHelper;

import java.util.List;

/**
 * Modular sub-screen builders for Gestures & 2-Finger Swipes, Per-Action Sound/Vibration Matrix (10 actions),
 * BYOK Gemini AI Keyboard (15 actions + custom tools + persona + usage counter),
 * Clipboard (3-day expiry / sort / pin), Emoji, Stickers, Biometric Keyboard Pass,
 * Local Backup & Reset to Defaults, Cloud Sync, and Unlocked Pro Showcase.
 */
public final class MediaAndSecurityBuilder {

    public interface SecurityHostActions {
        void pickEmojiFontFile();
        void pickStickerPhoto();
        void exportLocalBackup();
        void importLocalBackup();
        void authenticateVaultBiometric(Runnable onSuccess);
        void refreshScreen();
    }

    private static final String[][] MATRIX_LABELS = {
            {"letters", "الحروف والأرقام"},
            {"function", "مفاتيح الوظائف (?123 / Shift)"},
            {"toolbar", "أيقونات الشريط العلوي"},
            {"space", "مفتاح المسافة"},
            {"enter", "مفتاح الإدخال / البحث"},
            {"delete", "مفتاح المسح (ضغطة)"},
            {"delete_repeat", "تكرار المسح المطوّل"},
            {"swipe_delete", "السحب لحذف كلمة"},
            {"autocorrect", "التصحيح التلقائي"},
            {"long_press", "الضغط المطوّل"}
    };

    private MediaAndSecurityBuilder() {
    }

    public static void buildGestures(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "إيماءات الكتابة والمسافة"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "الكتابة بالسحب (Swipe Typing)",
                "اسحب إصبعك بين الحروف لكتابة الكلمات بسرعة", prefs.isSwipeTypingEnabled(), prefs::setSwipeTypingEnabled));
        card.addView(DetailUiKit.createSwitchRow(ctx, "السحب من زر المسح لحذف كلمة",
                "اسحب يساراً من مفتاح ⌫ لحذف الكلمة كاملة", prefs.isSwipeToDeleteEnabled(), prefs::setSwipeToDeleteEnabled));
        card.addView(DetailUiKit.createSwitchRow(ctx, "الحرف الأساسي أولاً عند الضغط المطوّل",
                "إظهار الحرف الأصلي كأول خيار في القائمة المنبثقة", prefs.isLetterFirstLongPress(), prefs::setLetterFirstLongPress));
        card.addView(DetailUiKit.createSliderRow(ctx, "تأخير الضغط المطوّل", 150, 700,
                prefs.getLongPressDelayMs(), "ms", prefs::setLongPressDelayMs));

        card.addView(DetailUiKit.createBodyText(ctx, "سلوك السحب على مفتاح المسافة:"));
        String spMode = prefs.getSpacebarSwipeMode();
        card.addView(DetailUiKit.createChipOption(ctx, "تحريك المؤشر بدقة (Cursor Control)", "cursor".equals(spMode), v -> {
            prefs.setSpacebarSwipeMode("cursor"); host.refreshScreen();
        }));
        card.addView(DetailUiKit.createChipOption(ctx, "تبديل اللغة (عربي ⇄ English)", "lang".equals(spMode), v -> {
            prefs.setSpacebarSwipeMode("lang"); host.refreshScreen();
        }));
        card.addView(DetailUiKit.createChipOption(ctx, "إيقاف سحب المسافة", "off".equals(spMode), v -> {
            prefs.setSpacebarSwipeMode("off"); host.refreshScreen();
        }));
        root.addView(card);

        // 2-Finger Swipes
        LinearLayout twoCard = DetailUiKit.createCard(ctx);
        twoCard.addView(DetailUiKit.createSectionTitle(ctx, "إيماءات السحب بإصبعين (4 اتجاهات)"));
        String[][] dirs = {{"up", "سحب إصبعين للأعلى ↑"}, {"down", "سحب إصبعين للأسفل ↓"},
                {"left", "سحب إصبعين لليسار ←"}, {"right", "سحب إصبعين لليمين →"}};
        String[] actions = {"select_all", "copy", "paste", "cut", "hide", "none"};
        String[] actionNames = {"تحديد الكل", "نسخ", "لصق", "قص", "إخفاء الكيبورد", "بدون إجراء"};

        for (String[] d : dirs) {
            String curAct = prefs.getTwoFingerSwipeAction(d[0]);
            String curName = actionLabel(curAct, actions, actionNames);
            twoCard.addView(DetailUiKit.createChipOption(ctx, d[1] + "  :  " + curName, false, v -> {
                new AlertDialog.Builder(ctx)
                        .setTitle(d[1])
                        .setItems(actionNames, (dlg, idx) -> {
                            prefs.setTwoFingerSwipeAction(d[0], actions[idx]);
                            host.refreshScreen();
                        }).show();
            }));
        }
        root.addView(twoCard);
    }

    public static void buildHapticsAndSound(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout masterCard = DetailUiKit.createCard(ctx);
        masterCard.addView(DetailUiKit.createSectionTitle(ctx, "التحكم العام بالاهتزاز والصوت"));
        masterCard.addView(DetailUiKit.createSwitchRow(ctx, "تفعيل الاهتزاز عند اللمس",
                "استجابة لمسية فورية", prefs.isVibrationEnabled(), prefs::setVibrationEnabled));
        masterCard.addView(DetailUiKit.createSliderRow(ctx, "مدة وقوة الاهتزاز", 5, 60,
                prefs.getVibrationStrengthMs(), "ms", prefs::setVibrationStrengthMs));
        masterCard.addView(DetailUiKit.createSwitchRow(ctx, "تفعيل صوت نقرات المفاتيح",
                "تشغيل صوت خفيف عند الضغط", prefs.isKeySoundEnabled(), prefs::setKeySoundEnabled));
        masterCard.addView(DetailUiKit.createSliderRow(ctx, "مستوى صوت النقرات", 0, 100,
                prefs.getSoundVolumePct(), "%", prefs::setSoundVolumePct));

        String pack = prefs.getSoundPack();
        masterCard.addView(DetailUiKit.createBodyText(ctx, "حزمة الصوت:"));
        masterCard.addView(DetailUiKit.createChipOption(ctx, "💎 صوت كريستال ناعم", KeyboardPrefs.SOUND_PACK_CRYSTAL.equals(pack), v -> {
            prefs.setSoundPack(KeyboardPrefs.SOUND_PACK_CRYSTAL); host.refreshScreen();
        }));
        masterCard.addView(DetailUiKit.createChipOption(ctx, "⌨️ صوت كيبورد ميكانيكي", KeyboardPrefs.SOUND_PACK_MECHANICAL.equals(pack), v -> {
            prefs.setSoundPack(KeyboardPrefs.SOUND_PACK_MECHANICAL); host.refreshScreen();
        }));
        masterCard.addView(DetailUiKit.createChipOption(ctx, "📱 صوت آيفون كلاسيكي", KeyboardPrefs.SOUND_PACK_IPHONE.equals(pack), v -> {
            prefs.setSoundPack(KeyboardPrefs.SOUND_PACK_IPHONE); host.refreshScreen();
        }));
        masterCard.addView(DetailUiKit.createChipOption(ctx, "🔇 صامت", KeyboardPrefs.SOUND_PACK_SILENT.equals(pack), v -> {
            prefs.setSoundPack(KeyboardPrefs.SOUND_PACK_SILENT); host.refreshScreen();
        }));
        root.addView(masterCard);

        // Full 10-Action Per-Action Sound/Vibration Matrix
        LinearLayout matrixCard = DetailUiKit.createCard(ctx);
        matrixCard.addView(DetailUiKit.createSectionTitle(ctx, "مصفوفة الصوت والاهتزاز (10 إجراءات مستقلة)"));
        matrixCard.addView(DetailUiKit.createBodyText(ctx, "تحكم مستقل بتشغيل الصوت 🔊 والاهتزاز 📳 لكل نوع مفتاح:"));

        for (String[] item : MATRIX_LABELS) {
            final String key = item[0];
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, DetailUiKit.dp(ctx, 4), 0, DetailUiKit.dp(ctx, 4));

            TextView lbl = new TextView(ctx);
            lbl.setText(item[1]);
            lbl.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
            lbl.setTextSize(13);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(lbl);

            CheckBox cbSnd = new CheckBox(ctx);
            cbSnd.setText("🔊");
            cbSnd.setChecked(prefs.isActionSoundEnabled(key));
            cbSnd.setOnCheckedChangeListener((b, c) -> prefs.setActionSoundEnabled(key, c));
            row.addView(cbSnd);

            CheckBox cbVib = new CheckBox(ctx);
            cbVib.setText("📳");
            cbVib.setChecked(prefs.isActionVibeEnabled(key));
            cbVib.setOnCheckedChangeListener((b, c) -> prefs.setActionVibeEnabled(key, c));
            row.addView(cbVib);

            matrixCard.addView(row);
        }
        root.addView(matrixCard);
    }

    public static void buildAiKeyboard(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout keyCard = DetailUiKit.createCard(ctx);
        keyCard.addView(DetailUiKit.createSectionTitle(ctx, "إعداد مفتاح Google Gemini API (BYOK مشفّر)"));
        keyCard.addView(DetailUiKit.createBodyText(ctx,
                "مفتاحك يُخزّن محلياً بتشفير AES-256 داخل AndroidKeyStore ولا يغادر جهازك إلا للاتصال المباشر مع خوادم Gemini."));

        EditText etKey = DetailUiKit.createInput(ctx, "أدخل مفتاح Gemini API هنا...", true);
        etKey.setText(prefs.getAiApiKey());
        keyCard.addView(etKey);

        String curModel = prefs.getAiModel();
        keyCard.addView(DetailUiKit.createBodyText(ctx, "النموذج المستخدم:"));
        String[] models = {"gemini-flash-latest", "gemini-2.0-flash", "gemini-1.5-flash"};
        for (String m : models) {
            keyCard.addView(DetailUiKit.createChipOption(ctx, m, m.equals(curModel), v -> {
                prefs.setAiModel(m); host.refreshScreen();
            }));
        }

        EditText etPersona = DetailUiKit.createInput(ctx, "نبذة عني (About Me Persona) — مثال: مهندس برمجيات أكتب بأسلوب مهني مختصر", false);
        etPersona.setText(prefs.getAiAboutMePersona());
        keyCard.addView(etPersona);

        keyCard.addView(DetailUiKit.createPrimaryButton(ctx, "🔒 حفظ المفتاح والشخصية بأمان", v -> {
            prefs.setAiApiKey(etKey.getText().toString().trim());
            prefs.setAiAboutMePersona(etPersona.getText().toString().trim());
            Toast.makeText(ctx, "تم تشفير وحفظ إعدادات Gemini بنجاح ✅", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));

        keyCard.addView(DetailUiKit.createBodyText(ctx,
                "إحصائيات استخدامك المحلي:  " + prefs.getAiRequestCount() + " طلب  •  ~" + prefs.getAiTokenCount() + " توكن"));
        root.addView(keyCard);

        // Add Custom AI Tool
        LinearLayout addToolCard = DetailUiKit.createCard(ctx);
        addToolCard.addView(DetailUiKit.createSectionTitle(ctx, "إضافة أداة ذكاء اصطناعي مخصصة"));
        EditText etToolName = DetailUiKit.createInput(ctx, "اسم الأداة (مثال: ✉️ رد بريد رسمي)", false);
        EditText etToolPrompt = DetailUiKit.createInput(ctx, "التعليمات (Prompt) للذكاء الاصطناعي...", false);
        addToolCard.addView(etToolName);
        addToolCard.addView(etToolPrompt);
        addToolCard.addView(DetailUiKit.createPrimaryButton(ctx, "+ إضافة الأداة لشريط الذكاء", v -> {
            String n = etToolName.getText().toString().trim();
            String p = etToolPrompt.getText().toString().trim();
            if (n.isEmpty() || p.isEmpty()) return;
            AiKeyboardHelper.addCustomAction(prefs, n, p);
            Toast.makeText(ctx, "تمت إضافة الأداة المخصصة ✅", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));
        root.addView(addToolCard);

        // List all 15+ available actions
        LinearLayout actionsCard = DetailUiKit.createCard(ctx);
        List<AiKeyboardHelper.AiAction> actions = AiKeyboardHelper.getAllActions(prefs);
        actionsCard.addView(DetailUiKit.createSectionTitle(ctx, "الأدوات المتاحة داخل الكيبورد (" + actions.size() + " أداة)"));
        for (AiKeyboardHelper.AiAction a : actions) {
            actionsCard.addView(DetailUiKit.createBodyText(ctx, "• " + a.label));
        }
        root.addView(actionsCard);
    }

    public static void buildClipboard(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        ClipboardRepository repo = new ClipboardRepository(ctx);
        LinearLayout optCard = DetailUiKit.createCard(ctx);
        optCard.addView(DetailUiKit.createSectionTitle(ctx, "إعدادات الحافظة الذكية"));
        optCard.addView(DetailUiKit.createSwitchRow(ctx, "حذف تلقائي بعد 3 أيام للعناصر غير المثبتة",
                "يحافظ على خصوصيتك وخفة قاعدة البيانات", prefs.isClipboardAutoDelete3Days(), prefs::setClipboardAutoDelete3Days));
        optCard.addView(DetailUiKit.createSwitchRow(ctx, "ترتيب الأحدث أولاً",
                "إظهار آخر ما تم نسخه في الأعلى", prefs.isClipboardSortNewest(), v -> {
                    prefs.setClipboardSortNewest(v); host.refreshScreen();
                }));
        optCard.addView(DetailUiKit.createSwitchRow(ctx, "اللصق السريع من شريط الأدوات",
                "تفعيل أداة اللصق السريع الفوري", prefs.isClipboardQuickPaste(), prefs::setClipboardQuickPaste));
        optCard.addView(DetailUiKit.createSwitchRow(ctx, "تأكيد قبل مسح الحافظة",
                "إظهار رسالة تأكيد لحماية نصوصك", prefs.isClipboardConfirmClear(), prefs::setClipboardConfirmClear));
        optCard.addView(DetailUiKit.createPrimaryButton(ctx, "🗑 مسح جميع العناصر غير المثبتة", v -> {
            Runnable doClear = () -> {
                repo.clearUnpinned();
                Toast.makeText(ctx, "تم مسح العناصر غير المثبتة", Toast.LENGTH_SHORT).show();
                host.refreshScreen();
            };
            if (prefs.isClipboardConfirmClear()) {
                new AlertDialog.Builder(ctx)
                        .setTitle("تأكيد مسح الحافظة")
                        .setMessage("هل تريد مسح جميع العناصر غير المثبتة؟")
                        .setPositiveButton("مسح", (d, w) -> doClear.run())
                        .setNegativeButton("إلغاء", null).show();
            } else {
                doClear.run();
            }
        }));
        root.addView(optCard);

        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "العناصر المحفوظة"));
        List<ClipboardRepository.ClipItem> clips = repo.getClips(30, prefs.isClipboardSortNewest(), prefs.isClipboardAutoDelete3Days());
        if (clips.isEmpty()) {
            listCard.addView(DetailUiKit.createBodyText(ctx, "لا توجد عناصر محفوظة في الحافظة حالياً."));
        } else {
            for (ClipboardRepository.ClipItem item : clips) {
                listCard.addView(DetailUiKit.createChipOption(ctx,
                        (item.pinned ? "📌 " : "") + item.text, item.pinned, v -> {
                            repo.togglePin(item.id);
                            host.refreshScreen();
                        }));
            }
        }
        root.addView(listCard);
    }

    public static void buildEmojiStyles(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "أنماط وخيارات الإيموجي"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "شريط تكرار الإيموجي السريع (3× / 5× / 7×)",
                "إرسال الإيموجي مكرراً بضغطة واحدة", prefs.isEmojiRepeatSuggestions(), prefs::setEmojiRepeatSuggestions));
        card.addView(DetailUiKit.createSwitchRow(ctx, "الضغط المطوّل على Enter لفتح الإيموجي",
                "وصول فوري للوحة الإيموجي", prefs.isEnterLongPressEmoji(), prefs::setEnterLongPressEmoji));

        String style = prefs.getEmojiStyle();
        card.addView(DetailUiKit.createChipOption(ctx, "Fluent 3D (الافتراضي)", KeyboardPrefs.EMOJI_STYLE_FLUENT.equals(style), v -> {
            prefs.setEmojiStyle(KeyboardPrefs.EMOJI_STYLE_FLUENT); host.refreshScreen();
        }));
        card.addView(DetailUiKit.createChipOption(ctx, "Twitter / Twemoji", KeyboardPrefs.EMOJI_STYLE_TWITTER.equals(style), v -> {
            prefs.setEmojiStyle(KeyboardPrefs.EMOJI_STYLE_TWITTER); host.refreshScreen();
        }));
        card.addView(DetailUiKit.createChipOption(ctx, "Google Noto Emoji", KeyboardPrefs.EMOJI_STYLE_GOOGLE.equals(style), v -> {
            prefs.setEmojiStyle(KeyboardPrefs.EMOJI_STYLE_GOOGLE); host.refreshScreen();
        }));
        card.addView(DetailUiKit.createPrimaryButton(ctx, "📂 استيراد ملف إيموجي مخصص (.ttf)", v -> host.pickEmojiFontFile()));
        root.addView(card);
    }

    public static void buildStickers(Context ctx, LinearLayout root, SecurityHostActions host) {
        StickerRepository repo = new StickerRepository(ctx);
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "صانع الملصقات السريع"));
        EditText etTxt = DetailUiKit.createInput(ctx, "اكتب نص الملصق (مثال: أحسنت 🔥)", false);
        card.addView(etTxt);
        card.addView(DetailUiKit.createPrimaryButton(ctx, "✨ إنشاء ملصق نصي", v -> {
            String t = etTxt.getText().toString().trim();
            if (t.isEmpty()) return;
            repo.createTextSticker(t, 0xFF4E6BFF, 0xFFFFFFFF, false);
            Toast.makeText(ctx, "تم حفظ الملصق ✅", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));
        card.addView(DetailUiKit.createChipOption(ctx, "📷 إنشاء ملصق من صورة (Photo Picker بدون صلاحيات)", true, v ->
                host.pickStickerPhoto()));
        root.addView(card);

        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "ملصقاتي المحفوظة"));
        List<StickerRepository.StickerItem> items = repo.getStickers();
        if (items.isEmpty()) {
            listCard.addView(DetailUiKit.createBodyText(ctx, "لم تقم بإنشاء ملصقات بعد."));
        } else {
            for (StickerRepository.StickerItem s : items) {
                listCard.addView(DetailUiKit.createChipOption(ctx, "🎨 " + s.label + "   ✕", false, v -> {
                    repo.deleteSticker(s.id);
                    host.refreshScreen();
                }));
            }
        }
        root.addView(listCard);
    }

    public static void buildKeyboardPass(Context ctx, LinearLayout root, SecurityHostActions host) {
        VaultRepository vault = new VaultRepository(ctx);
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "Keyboard Pass — خزنة البصمة المشفّرة (AES-256)"));
        card.addView(DetailUiKit.createBodyText(ctx,
                "احفظ أرقام الآيبان والحسابات والعناوين بشكل مشفّر محلياً وافتحها ببصمة الإصبع فقط."));

        LinearLayout unlockedBox = new LinearLayout(ctx);
        unlockedBox.setOrientation(LinearLayout.VERTICAL);

        card.addView(DetailUiKit.createPrimaryButton(ctx, "🔓 فتح الخزنة ببصمة الإصبع", v ->
                host.authenticateVaultBiometric(() -> {
                    vault.markSessionUnlocked(120_000L);
                    unlockedBox.removeAllViews();
                    EditText etLabel = DetailUiKit.createInput(ctx, "العنوان (مثال: آيبان البنك)", false);
                    EditText etSecret = DetailUiKit.createInput(ctx, "البيانات السرية", false);
                    unlockedBox.addView(etLabel);
                    unlockedBox.addView(etSecret);
                    unlockedBox.addView(DetailUiKit.createPrimaryButton(ctx, "+ حفظ في الخزنة المشفرة", v2 -> {
                        String l = etLabel.getText().toString().trim();
                        String s = etSecret.getText().toString().trim();
                        if (l.isEmpty() || s.isEmpty()) return;
                        vault.addItem("account", l, "", s);
                        Toast.makeText(ctx, "تم التشفير والحفظ بأمان 🔒", Toast.LENGTH_SHORT).show();
                        host.refreshScreen();
                    }));
                    for (VaultRepository.VaultItem e : vault.getItems()) {
                        unlockedBox.addView(DetailUiKit.createChipOption(ctx,
                                "🔐 " + e.label + " : " + e.secretValue + "   ✕", false, v3 -> {
                                    vault.deleteItem(e.id);
                                    host.refreshScreen();
                                }));
                    }
                })));
        card.addView(unlockedBox);
        root.addView(card);
    }

    public static void buildLocalBackup(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "النسخ الاحتياطي المحلي (JSON)"));
        card.addView(DetailUiKit.createBodyText(ctx,
                "صدّر جميع إعداداتك، اختصاراتك، ثيماتك المخصصة، وتخطيطك في ملف JSON واحد آمن."));
        card.addView(DetailUiKit.createPrimaryButton(ctx, "⬆️ تصدير نسخة احتياطية (JSON)", v -> host.exportLocalBackup()));
        card.addView(DetailUiKit.createChipOption(ctx, "⬇️ استيراد نسخة احتياطية من جهازك", true, v -> host.importLocalBackup()));
        root.addView(card);

        LinearLayout resetCard = DetailUiKit.createCard(ctx);
        resetCard.addView(DetailUiKit.createSectionTitle(ctx, "إعادة الضبط إلى الوضع الافتراضي"));
        resetCard.addView(DetailUiKit.createBodyText(ctx,
                "يعيد جميع التفضيلات والأبعاد والحركات والثيم إلى إعدادات المصنع النظيفة."));
        resetCard.addView(DetailUiKit.createChipOption(ctx, "↺ إعادة ضبط جميع الإعدادات للافتراضي", false, v ->
                new AlertDialog.Builder(ctx)
                        .setTitle("إعادة الضبط للافتراضي")
                        .setMessage("هل أنت متأكد من إعادة جميع إعدادات الكيبورد للوضع الافتراضي؟")
                        .setPositiveButton("نعم، إعادة ضبط", (d, w) -> {
                            prefs.resetToDefaults();
                            Toast.makeText(ctx, "تمت إعادة جميع الإعدادات للافتراضي ✅", Toast.LENGTH_LONG).show();
                            host.refreshScreen();
                        })
                        .setNegativeButton("إلغاء", null)
                        .show()));
        root.addView(resetCard);
    }

    public static void buildCloudSync(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "مزامنة Google Drive الاختيارية"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "نسخ احتياطي يومي تلقائي",
                "يتطلب رمز وصول Google Drive اختياري", prefs.isDailyCloudBackupEnabled(), prefs::setDailyCloudBackupEnabled));
        EditText etTok = DetailUiKit.createInput(ctx, "Google Drive OAuth Access Token (اختياري)", true);
        etTok.setText(prefs.getDriveAccessToken());
        card.addView(etTok);
        card.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ رمز المزامنة المشفّر", v -> {
            prefs.setDriveAccessToken(etTok.getText().toString().trim());
            Toast.makeText(ctx, "تم حفظ الرمز بتشفير AES-256 ✅", Toast.LENGTH_SHORT).show();
        }));
        root.addView(card);
    }

    public static void buildProShowcase(Context ctx, LinearLayout root) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "كيبورد برو — جميع الميزات مفتوحة 100%"));
        card.addView(DetailUiKit.createBodyText(ctx,
                "هذا التطبيق مفتوح بالكامل ومجاني 100% — بدون اشتراكات، بدون إعلانات، بدون تتبع.\n\n"
                        + "• أكثر من 42 ثيماً احترافياً (كريستال زجاجي، ماتيريال، آيفون 27، ميكانيكي، نيون، رسم).\n"
                        + "• محرر ثيمات كامل مع خلفية صورة واستخراج ألوان وتراجع/إعادة.\n"
                        + "• 9 حركات ضغط مفاتيح و5 تأثيرات إضاءة وزوايا Squircle وعمق 3D.\n"
                        + "• 56 نمط زخرفة فورية (26 عربي + 30 إنجليزي) مع حماية تلقائية لكلمات المرور.\n"
                        + "• شريط أدوات قابل للتخصيص + 26 أداة مدمجة (حاسبة، تراك باد، آيات، أذكار، رسالة سرية).\n"
                        + "• ذكاء عربي فوري (همزات، فرانكو، حاسبة، هجري/ميلادي) + Gemini BYOK بـ 15 أداة."));
        root.addView(card);
    }

    private static String actionLabel(String code, String[] codes, String[] names) {
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(code)) return names[i];
        }
        return "بدون إجراء";
    }
}
