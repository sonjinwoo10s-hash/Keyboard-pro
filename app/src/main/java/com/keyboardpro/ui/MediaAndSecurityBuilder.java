package com.keyboardpro.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.R;
import com.keyboardpro.data.BackupManager;
import com.keyboardpro.data.ClipboardRepository;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.data.StickerRepository;
import com.keyboardpro.data.VaultRepository;
import com.keyboardpro.ime.AiKeyboardHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Builds Settings sub-screens for Emoji Styles, My Stickers, Gestures, Vibration & Sound Packs,
 * Clipboard History, Keyboard Pass (BiometricPrompt Vault), AI Keyboard Config,
 * Local Backup, Google Drive Cloud Sync, and Pro Crown Showcase.
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

    private MediaAndSecurityBuilder() {
    }

    public static void buildEmojiStyles(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "أنماط الإيموجي (Emoji Styles)"));
        card.addView(DetailUiKit.createBodyText(ctx, "معاينة: 😀 🤩 👑 💎 🔥 ✨ ❤️ 🚀 🌙 🎉"));

        String cur = prefs.getEmojiStyle();
        String[][] styles = {
                {KeyboardPrefs.EMOJI_STYLE_FLUENT, "Fluent 3D (إيموجي ثلاثي الأبعاد — برو)"},
                {KeyboardPrefs.EMOJI_STYLE_TWITTER, "Twitter / Twemoji (إيموجي تويتر المسطح)"},
                {KeyboardPrefs.EMOJI_STYLE_GOOGLE, "Google Noto (إيموجي جوجل الأصلي)"},
                {KeyboardPrefs.EMOJI_STYLE_CUSTOM, "ملف إيموجي مخصص (TTF)"}
        };

        for (String[] s : styles) {
            card.addView(DetailUiKit.createChipOption(ctx, s[1], s[0].equals(cur), v -> {
                prefs.setEmojiStyle(s[0]);
                Toast.makeText(ctx, "تم اختيار نمط: " + s[1], Toast.LENGTH_SHORT).show();
                host.refreshScreen();
            }));
        }

        card.addView(DetailUiKit.createPrimaryButton(ctx, "استيراد ملف إيموجي مخصص (.ttf)", v -> host.pickEmojiFontFile()));
        if (!prefs.getCustomEmojiFontPath().isEmpty()) {
            card.addView(DetailUiKit.createBodyText(ctx, "الملف المستورد: " + prefs.getCustomEmojiFontPath()));
        }
        root.addView(card);
    }

    public static void buildStickers(Context ctx, LinearLayout root, SecurityHostActions host) {
        StickerRepository repo = new StickerRepository(ctx);

        LinearLayout createCard = DetailUiKit.createCard(ctx);
        createCard.addView(DetailUiKit.createSectionTitle(ctx, "صانع ملصقاتي السريع (من نص أو صورة)"));
        EditText etText = DetailUiKit.createInput(ctx, "اكتب عبارة لإنشاء ملصق فوري (مثلاً: يا هلا ✨)...", false);
        createCard.addView(etText);
        createCard.addView(DetailUiKit.createPrimaryButton(ctx, "إنشاء ملصق من النص", v -> {
            String txt = etText.getText().toString().trim();
            if (txt.isEmpty()) return;
            repo.createTextSticker(txt, Color.parseColor("#00D4FF"), Color.parseColor("#6366F1"), false);
            Toast.makeText(ctx, "تم إنشاء الملصق وإضافته للوحة المفاتيح!", Toast.LENGTH_SHORT).show();
            host.refreshScreen();
        }));
        createCard.addView(DetailUiKit.createChipOption(ctx, "📷 إنشاء ملصق من صورة (بدون صلاحيات تخزين)", false,
                v -> host.pickStickerPhoto()));
        root.addView(createCard);

        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "ملصقاتي والحزم الحصرية"));
        for (StickerRepository.StickerItem s : repo.getStickers()) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, DetailUiKit.dp(ctx, 6), 0, DetailUiKit.dp(ctx, 6));

            ImageView iv = new ImageView(ctx);
            int thumbSize = DetailUiKit.dp(ctx, 52);
            if (s.filePath != null && new File(s.filePath).exists()) {
                iv.setImageBitmap(com.keyboardpro.data.BitmapUtils.decodeSampledFromFile(s.filePath, thumbSize, thumbSize));
            }
            row.addView(iv, new LinearLayout.LayoutParams(thumbSize, thumbSize));

            TextView tv = new TextView(ctx);
            tv.setText(s.label + (s.isExclusive ? "  👑 حصري" : ""));
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
            tv.setPadding(DetailUiKit.dp(ctx, 12), 0, DetailUiKit.dp(ctx, 8), 0);
            row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = new TextView(ctx);
            del.setText("حذف");
            del.setTextColor(ContextCompat.getColor(ctx, R.color.kp_danger));
            del.setOnClickListener(v -> {
                repo.deleteSticker(s.id);
                host.refreshScreen();
            });
            row.addView(del);
            listCard.addView(row);
        }
        root.addView(listCard);
    }

    public static void buildGestures(Context ctx, LinearLayout root, KeyboardPrefs prefs) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "إيماءات السحب الذكية"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "الكتابة بالسحب (Swipe Typing)",
                "مرر إصبعك بين الأحرف مع أثر نيون مضيء لكتابة الكلمات بسرعة",
                prefs.isSwipeTypingEnabled(), prefs::setSwipeTypingEnabled));
        card.addView(DetailUiKit.createSwitchRow(ctx, "تحريك المؤشر من المسافة",
                "اسحب يميناً أو يساراً على زر المسافة لتحريك مؤشر الكتابة بدقة",
                prefs.isCursorControlEnabled(), prefs::setCursorControlEnabled));
        card.addView(DetailUiKit.createSwitchRow(ctx, "السحب للحذف السريع",
                "اسحب من زر الحذف ⌫ لحذف كلمة كاملة دفعة واحدة",
                prefs.isSwipeToDeleteEnabled(), prefs::setSwipeToDeleteEnabled));
        root.addView(card);
    }

    public static void buildHapticsAndSound(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "الاهتزاز والاستجابة اللمسية"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "اهتزاز عند الضغط",
                "تفعيل الاهتزاز الخفيف عند لمس المفاتيح",
                prefs.isVibrationEnabled(), prefs::setVibrationEnabled));
        card.addView(DetailUiKit.createSliderRow(ctx, "قوة الاهتزاز", 5, 55,
                prefs.getVibrationStrengthMs(), "ms", prefs::setVibrationStrengthMs));
        root.addView(card);

        LinearLayout soundCard = DetailUiKit.createCard(ctx);
        soundCard.addView(DetailUiKit.createSectionTitle(ctx, "حزم أصوات المفاتيح الحصرية"));
        soundCard.addView(DetailUiKit.createSwitchRow(ctx, "صوت المفاتيح",
                "تشغيل المؤثرات الصوتية عند الكتابة",
                prefs.isKeySoundEnabled(), prefs::setKeySoundEnabled));

        String cur = prefs.getSoundPack();
        String[][] packs = {
                {KeyboardPrefs.SOUND_PACK_CRYSTAL, "💎 كريستال زجاجي (حصري برو)"},
                {KeyboardPrefs.SOUND_PACK_MECHANICAL, "⌨️ ميكانيكي أزرق (Mechanical)"},
                {KeyboardPrefs.SOUND_PACK_IPHONE, "📱 نقرة آيفون الناعمة"},
                {KeyboardPrefs.SOUND_PACK_SILENT, "🔇 صامت"}
        };
        for (String[] p : packs) {
            soundCard.addView(DetailUiKit.createChipOption(ctx, p[1], p[0].equals(cur), v -> {
                prefs.setSoundPack(p[0]);
                host.refreshScreen();
            }));
        }
        root.addView(soundCard);
    }

    public static void buildClipboard(Context ctx, LinearLayout root, SecurityHostActions host) {
        ClipboardRepository repo = new ClipboardRepository(ctx);

        LinearLayout addCard = DetailUiKit.createCard(ctx);
        addCard.addView(DetailUiKit.createSectionTitle(ctx, "إضافة نص للحافظة"));
        EditText etClip = DetailUiKit.createInput(ctx, "أضف نصاً متكرراً لتثبيته في الحافظة...", false);
        addCard.addView(etClip);
        addCard.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ في الحافظة", v -> {
            if (!etClip.getText().toString().trim().isEmpty()) {
                repo.addClip(etClip.getText().toString().trim());
                host.refreshScreen();
            }
        }));
        addCard.addView(DetailUiKit.createChipOption(ctx, "مسح العناصر غير المثبتة", false, v -> {
            repo.clearUnpinned();
            host.refreshScreen();
        }));
        root.addView(addCard);

        LinearLayout listCard = DetailUiKit.createCard(ctx);
        listCard.addView(DetailUiKit.createSectionTitle(ctx, "سجل الحافظة المحفوظ"));
        List<ClipboardRepository.ClipItem> clips = repo.getClips();
        if (clips.isEmpty()) {
            listCard.addView(DetailUiKit.createBodyText(ctx, "الحافظة فارغة حالياً. انسخ أي نص أو أضف عنصراً أعلاه."));
        }
        for (ClipboardRepository.ClipItem clip : clips) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, DetailUiKit.dp(ctx, 6), 0, DetailUiKit.dp(ctx, 6));

            TextView tv = new TextView(ctx);
            tv.setText((clip.pinned ? "📌 " : "") + clip.text);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
            tv.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("clip", clip.text));
                    Toast.makeText(ctx, "تم النسخ!", Toast.LENGTH_SHORT).show();
                }
            });
            row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView pin = new TextView(ctx);
            pin.setText(clip.pinned ? "إلغاء تثبيت" : "تثبيت");
            pin.setTextColor(ContextCompat.getColor(ctx, R.color.kp_primary));
            pin.setPadding(DetailUiKit.dp(ctx, 8), 0, DetailUiKit.dp(ctx, 8), 0);
            pin.setOnClickListener(v -> {
                repo.togglePin(clip.id);
                host.refreshScreen();
            });
            row.addView(pin);

            TextView del = new TextView(ctx);
            del.setText("حذف");
            del.setTextColor(ContextCompat.getColor(ctx, R.color.kp_danger));
            del.setOnClickListener(v -> {
                repo.deleteClip(clip.id);
                host.refreshScreen();
            });
            row.addView(del);
            listCard.addView(row);
        }
        root.addView(listCard);
    }

    public static void buildKeyboardPass(Context ctx, LinearLayout root, SecurityHostActions host) {
        VaultRepository vault = new VaultRepository(ctx);

        if (!vault.isSessionUnlocked()) {
            LinearLayout lockCard = DetailUiKit.createCard(ctx);
            lockCard.addView(DetailUiKit.createSectionTitle(ctx, "🔐 Keyboard Pass — خزنة البصمة المشفرة"));
            lockCard.addView(DetailUiKit.createBodyText(ctx,
                    "احفظ حساباتك، أرقام البطاقات، ورقم الآيبان بتشفير AES-256-GCM داخل AndroidKeyStore.\nبعد فتح الخزنة ببصمة الإصبع، يمكنك الكتابة التلقائية مباشرة من شريط الكيبورد لمدة دقيقتين."));
            lockCard.addView(DetailUiKit.createPrimaryButton(ctx, "المصادقة ببصمة الإصبع 🔓", v ->
                    host.authenticateVaultBiometric(() -> {
                        vault.markSessionUnlocked(120_000L);
                        Toast.makeText(ctx, "تم فتح Keyboard Pass بنجاح!", Toast.LENGTH_SHORT).show();
                        host.refreshScreen();
                    })));
            root.addView(lockCard);
            return;
        }

        LinearLayout addCard = DetailUiKit.createCard(ctx);
        addCard.addView(DetailUiKit.createSectionTitle(ctx, "إضافة سجل مشفر جديد (مفتوح بالبصمة ✓)"));
        EditText etLabel = DetailUiKit.createInput(ctx, "العنوان (مثلاً: آيبان البنك الأهلي / حساب جوجل)", false);
        EditText etMeta = DetailUiKit.createInput(ctx, "النوع أو اسم المستخدم (IBAN / Email / Card)", false);
        EditText etSecret = DetailUiKit.createInput(ctx, "القيمة السرية للكتابة التلقائية", true);
        addCard.addView(etLabel);
        addCard.addView(etMeta);
        addCard.addView(etSecret);
        addCard.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ بتشفير AES-256", v -> {
            String l = etLabel.getText().toString().trim();
            String m = etMeta.getText().toString().trim();
            String s = etSecret.getText().toString().trim();
            if (l.isEmpty() || s.isEmpty()) {
                Toast.makeText(ctx, "أدخل العنوان والقيمة السرية", Toast.LENGTH_SHORT).show();
                return;
            }
            vault.addItem("secret", l, m, s);
            host.refreshScreen();
        }));
        addCard.addView(DetailUiKit.createChipOption(ctx, "قفل الخزنة الآن 🔒", false, v -> {
            vault.lockSession();
            host.refreshScreen();
        }));
        root.addView(addCard);

        LinearLayout itemsCard = DetailUiKit.createCard(ctx);
        itemsCard.addView(DetailUiKit.createSectionTitle(ctx, "السجلات المشفرة المحفوظة"));
        List<VaultRepository.VaultItem> items = vault.getItems();
        if (items.isEmpty()) {
            itemsCard.addView(DetailUiKit.createBodyText(ctx, "لا توجد سجلات محفوظة بعد."));
        }
        for (VaultRepository.VaultItem item : items) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, DetailUiKit.dp(ctx, 6), 0, DetailUiKit.dp(ctx, 6));

            TextView tv = new TextView(ctx);
            tv.setText("🔑 " + item.label + "\n" + item.usernameOrMeta + "  •  ••••••••");
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
            row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView copy = new TextView(ctx);
            copy.setText("نسخ");
            copy.setTextColor(ContextCompat.getColor(ctx, R.color.kp_primary));
            copy.setPadding(DetailUiKit.dp(ctx, 8), 0, DetailUiKit.dp(ctx, 8), 0);
            copy.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText(item.label, item.secretValue));
                    Toast.makeText(ctx, "تم نسخ القيمة الآمنة", Toast.LENGTH_SHORT).show();
                }
            });
            row.addView(copy);

            TextView del = new TextView(ctx);
            del.setText("حذف");
            del.setTextColor(ContextCompat.getColor(ctx, R.color.kp_danger));
            del.setOnClickListener(v -> {
                vault.deleteItem(item.id);
                host.refreshScreen();
            });
            row.addView(del);
            itemsCard.addView(row);
        }
        root.addView(itemsCard);
    }

    public static void buildAiKeyboard(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout cfgCard = DetailUiKit.createCard(ctx);
        cfgCard.addView(DetailUiKit.createSectionTitle(ctx, "إعداد مفتاح الذكاء الاصطناعي المجاني"));
        cfgCard.addView(DetailUiKit.createBodyText(ctx,
                "أدخل مفتاح Gemini API المجاني (من Google AI Studio) أو مفتاح OpenAI الخاص بك. يُحفظ المفتاح مشفراً محلياً على جهازك فقط."));

        String provider = prefs.getAiProvider();
        cfgCard.addView(DetailUiKit.createChipOption(ctx, "Google Gemini API (مجاني وموصى به)",
                "gemini".equals(provider), v -> {
                    prefs.setAiProvider("gemini");
                    prefs.setAiModel("gemini-2.0-flash");
                    host.refreshScreen();
                }));
        cfgCard.addView(DetailUiKit.createChipOption(ctx, "OpenAI-Compatible API",
                "openai".equals(provider), v -> {
                    prefs.setAiProvider("openai");
                    prefs.setAiModel("gpt-4o-mini");
                    host.refreshScreen();
                }));

        EditText etKey = DetailUiKit.createInput(ctx, "الصق مفتاح API الخاص بك هنا...", true);
        etKey.setText(prefs.getAiApiKey());
        EditText etModel = DetailUiKit.createInput(ctx, "اسم النموذج (مثلاً: gemini-2.0-flash)", false);
        etModel.setText(prefs.getAiModel());
        cfgCard.addView(etKey);
        cfgCard.addView(etModel);
        cfgCard.addView(DetailUiKit.createPrimaryButton(ctx, "حفظ الإعدادات المشفرة", v -> {
            prefs.setAiApiKey(etKey.getText().toString().trim());
            prefs.setAiModel(etModel.getText().toString().trim());
            Toast.makeText(ctx, "تم حفظ مفتاح الذكاء الاصطناعي بأمان ✅", Toast.LENGTH_SHORT).show();
        }));
        root.addView(cfgCard);

        LinearLayout testCard = DetailUiKit.createCard(ctx);
        testCard.addView(DetailUiKit.createSectionTitle(ctx, "تجربة أدوات الذكاء الاصطناعي"));
        EditText etSample = DetailUiKit.createInput(ctx, "اكتب نصاً لتجربة التدقيق أو إعادة الصياغة أو الترجمة...", false);
        TextView tvOut = DetailUiKit.createBodyText(ctx, "النتيجة ستظهر هنا...");
        testCard.addView(etSample);

        AiKeyboardHelper helper = new AiKeyboardHelper(ctx);
        testCard.addView(DetailUiKit.createChipOption(ctx, "✓ تدقيق إملائي ونحوي", true, v -> {
            tvOut.setText("جاري التدقيق...");
            helper.processText(etSample.getText().toString(), AiKeyboardHelper.Action.PROOFREAD, (ok, res) -> tvOut.setText(res));
        }));
        testCard.addView(DetailUiKit.createChipOption(ctx, "✨ إعادة صياغة احترافية", true, v -> {
            tvOut.setText("جاري إعادة الصياغة...");
            helper.processText(etSample.getText().toString(), AiKeyboardHelper.Action.REWRITE, (ok, res) -> tvOut.setText(res));
        }));
        testCard.addView(DetailUiKit.createChipOption(ctx, "🌍 ترجمة فورية", true, v -> {
            tvOut.setText("جاري الترجمة...");
            helper.processText(etSample.getText().toString(), AiKeyboardHelper.Action.TRANSLATE, (ok, res) -> tvOut.setText(res));
        }));
        testCard.addView(tvOut);
        root.addView(testCard);
    }

    public static void buildLocalBackup(Context ctx, LinearLayout root, SecurityHostActions host) {
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "النسخ الاحتياطي المحلي (JSON)"));
        card.addView(DetailUiKit.createBodyText(ctx,
                "يشمل النسخ الاحتياطي المحلي: جميع الإعدادات، التخطيطات المخصصة، القاموس الشخصي، الاختصارات النصية، والثيمات المخصصة."));
        card.addView(DetailUiKit.createPrimaryButton(ctx, "تصدير نسخة احتياطية إلى الجهاز", v -> host.exportLocalBackup()));
        card.addView(DetailUiKit.createChipOption(ctx, "استيراد نسخة احتياطية من ملف JSON", true, v -> host.importLocalBackup()));
        root.addView(card);
    }

    public static void buildCloudSync(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        BackupManager backupManager = new BackupManager(ctx);
        LinearLayout card = DetailUiKit.createCard(ctx);
        card.addView(DetailUiKit.createSectionTitle(ctx, "المزامنة السحابية عبر Google Drive"));
        card.addView(DetailUiKit.createSwitchRow(ctx, "نسخ احتياطي يومي تلقائي",
                "مزامنة التخطيطات والقاموس والإعدادات تلقائياً كل 24 ساعة",
                prefs.isDailyCloudBackupEnabled(), prefs::setDailyCloudBackupEnabled));

        long lastSync = prefs.getLastCloudSyncTime();
        String syncLabel = lastSync == 0L ? "لم تتم المزامنة بعد"
                : "آخر مزامنة: " + new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date(lastSync));
        TextView tvSyncStatus = DetailUiKit.createBodyText(ctx, syncLabel);
        card.addView(tvSyncStatus);

        EditText etToken = DetailUiKit.createInput(ctx, "رمز وصول Google Drive OAuth Token", true);
        etToken.setText(prefs.getDriveAccessToken());
        card.addView(etToken);

        card.addView(DetailUiKit.createPrimaryButton(ctx, "رفع النسخة إلى Google Drive الآن ☁️", v -> {
            String token = etToken.getText().toString().trim();
            prefs.setDriveAccessToken(token);
            tvSyncStatus.setText("جاري الرفع إلى Google Drive...");
            backupManager.uploadToGoogleDrive(token, (ok, msg) -> {
                tvSyncStatus.setText(msg);
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
            });
        }));

        card.addView(DetailUiKit.createChipOption(ctx, "استعادة النسخة من Google Drive", true, v -> {
            String token = etToken.getText().toString().trim();
            prefs.setDriveAccessToken(token);
            tvSyncStatus.setText("جاري الاستعادة من Google Drive...");
            backupManager.restoreFromGoogleDrive(token, (ok, msg) -> {
                tvSyncStatus.setText(msg);
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
            });
        }));
        root.addView(card);
    }

    public static void buildProShowcase(Context ctx, LinearLayout root, KeyboardPrefs prefs, SecurityHostActions host) {
        LinearLayout hero = DetailUiKit.createCard(ctx);
        hero.setBackgroundResource(R.drawable.bg_card_hero);
        hero.addView(DetailUiKit.createSectionTitle(ctx, "👑 كيبورد برو — الميزات الذهبية الحصرية"));
        hero.addView(DetailUiKit.createBodyText(ctx,
                "• ثيمات الكريستال والزجاج المضيء بحدود متوهجة ولمعان فوري.\n"
                        + "• خلفية شفافة مموهة (Transparent Blur) تعرض التطبيق خلف الكيبورد.\n"
                        + "• أنماط إيموجي Fluent 3D و Twitter و Google + استيراد خطوط إيموجي.\n"
                        + "• حزم ملصقات وأصوات مفاتيح حصرية (كريستال / ميكانيكي).\n"
                        + "• نسخ احتياطي سحابي يومي عبر Google Drive ومزامنة الأجهزة."));
        hero.addView(DetailUiKit.createSwitchRow(ctx, "وضع كيبورد برو الذهبي مفعّل",
                "افتح جميع الثيمات الكريستالية والمحتوى الحصري بالكامل",
                prefs.isProUnlocked(), checked -> {
                    prefs.setProUnlocked(checked);
                    Toast.makeText(ctx, checked ? "تم تفعيل جميع ميزات برو 👑" : "تم تعطيل وضع برو", Toast.LENGTH_SHORT).show();
                    host.refreshScreen();
                }));
        root.addView(hero);
    }
}
