package com.keyboardpro.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.R;
import com.keyboardpro.data.BackupManager;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.data.StickerRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Host activity for all Settings sub-screens, Pro Crown Showcase, BiometricPrompt Keyboard Pass,
 * zero-permission Photo Picker sticker creation, and SAF font/backup import/export.
 */
public class SettingsDetailActivity extends AppCompatActivity
        implements TypingAndDesignBuilder.HostActions, MediaAndSecurityBuilder.SecurityHostActions {

    public static final String EXTRA_SECTION = "extra_section";

    public static final String SEC_PRO = "pro";
    public static final String SEC_AI = "ai";
    public static final String SEC_LANGUAGES = "languages";
    public static final String SEC_SMART_TYPING = "smart_typing";
    public static final String SEC_TRANSLATION = "translation";
    public static final String SEC_DICTIONARY = "dictionary";
    public static final String SEC_SHORTCUTS = "shortcuts";
    public static final String SEC_APPEARANCE = "appearance";
    public static final String SEC_FONTS = "fonts";
    public static final String SEC_LAYOUT_EDITOR = "layout_editor";
    public static final String SEC_EMOJI = "emoji";
    public static final String SEC_STICKERS = "stickers";
    public static final String SEC_DECORATION = "decoration";
    public static final String SEC_GESTURES = "gestures";
    public static final String SEC_HAPTICS = "haptics";
    public static final String SEC_CLIPBOARD = "clipboard";
    public static final String SEC_KEYBOARD_PASS = "keyboard_pass";
    public static final String SEC_BACKUP = "backup";
    public static final String SEC_CLOUD_SYNC = "cloud_sync";

    private KeyboardPrefs prefs;
    private BackupManager backupManager;
    private String section;
    private LinearLayout contentContainer;
    private boolean pickingEmojiFont = false;

    private final ActivityResultLauncher<String[]> fontPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) copyImportedFont(uri, pickingEmojiFont);
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> stickerPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) createStickerFromPhoto(uri);
            });

    private final ActivityResultLauncher<String> exportBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) {
                    boolean ok = backupManager.exportLocalBackup(uri);
                    Toast.makeText(this, ok ? "تم تصدير النسخة الاحتياطية بنجاح ✅" : "فشل التصدير", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> importBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    boolean ok = backupManager.importLocalBackup(uri);
                    Toast.makeText(this, ok ? "تم استيراد النسخة الاحتياطية بنجاح ✅" : "ملف غير صالح", Toast.LENGTH_LONG).show();
                    refreshScreen();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = new KeyboardPrefs(this);
        MainActivity.applySavedNightMode(prefs.getAppThemeMode());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_detail);

        backupManager = new BackupManager(this);
        section = getIntent().getStringExtra(EXTRA_SECTION);
        if (section == null) section = SEC_APPEARANCE;

        View toolbar = findViewById(R.id.detail_toolbar);
        int baseTop = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(toolbar.getPaddingLeft(), baseTop + bars.top,
                    toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            v.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });

        findViewById(R.id.btn_detail_back).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_detail_title);
        tvTitle.setText(resolveTitle(section));

        contentContainer = findViewById(R.id.detail_content_container);
        renderSection();
    }

    private void renderSection() {
        contentContainer.removeAllViews();
        switch (section) {
            case SEC_LANGUAGES:
                TypingAndDesignBuilder.buildLanguages(this, contentContainer, prefs);
                break;
            case SEC_SMART_TYPING:
                TypingAndDesignBuilder.buildSmartTyping(this, contentContainer, prefs);
                break;
            case SEC_TRANSLATION:
                TypingAndDesignBuilder.buildTranslation(this, contentContainer, prefs);
                break;
            case SEC_DICTIONARY:
                TypingAndDesignBuilder.buildDictionary(this, contentContainer, prefs, this);
                break;
            case SEC_SHORTCUTS:
                TypingAndDesignBuilder.buildShortcuts(this, contentContainer, prefs, this);
                break;
            case SEC_APPEARANCE:
                TypingAndDesignBuilder.buildAppearance(this, contentContainer, prefs);
                break;
            case SEC_FONTS:
                TypingAndDesignBuilder.buildFonts(this, contentContainer, prefs, this);
                break;
            case SEC_LAYOUT_EDITOR:
                TypingAndDesignBuilder.buildLayoutEditor(this, contentContainer, prefs, this);
                break;
            case SEC_DECORATION:
                TypingAndDesignBuilder.buildDecorationManager(this, contentContainer, prefs, this);
                break;
            case SEC_EMOJI:
                MediaAndSecurityBuilder.buildEmojiStyles(this, contentContainer, prefs, this);
                break;
            case SEC_STICKERS:
                MediaAndSecurityBuilder.buildStickers(this, contentContainer, this);
                break;
            case SEC_GESTURES:
                MediaAndSecurityBuilder.buildGestures(this, contentContainer, prefs);
                break;
            case SEC_HAPTICS:
                MediaAndSecurityBuilder.buildHapticsAndSound(this, contentContainer, prefs, this);
                break;
            case SEC_CLIPBOARD:
                MediaAndSecurityBuilder.buildClipboard(this, contentContainer, this);
                break;
            case SEC_KEYBOARD_PASS:
                MediaAndSecurityBuilder.buildKeyboardPass(this, contentContainer, this);
                break;
            case SEC_AI:
                MediaAndSecurityBuilder.buildAiKeyboard(this, contentContainer, prefs, this);
                break;
            case SEC_BACKUP:
                MediaAndSecurityBuilder.buildLocalBackup(this, contentContainer, this);
                break;
            case SEC_CLOUD_SYNC:
                MediaAndSecurityBuilder.buildCloudSync(this, contentContainer, prefs, this);
                break;
            case SEC_PRO:
            default:
                MediaAndSecurityBuilder.buildProShowcase(this, contentContainer, prefs, this);
                break;
        }
    }

    private String resolveTitle(String sec) {
        switch (sec) {
            case SEC_LANGUAGES: return getString(R.string.set_languages_title);
            case SEC_SMART_TYPING: return getString(R.string.set_smart_typing_title);
            case SEC_TRANSLATION: return getString(R.string.set_translation_title);
            case SEC_DICTIONARY: return getString(R.string.set_dictionary_title);
            case SEC_SHORTCUTS: return getString(R.string.set_shortcuts_title);
            case SEC_APPEARANCE: return getString(R.string.set_appearance_title);
            case SEC_FONTS: return getString(R.string.set_fonts_title);
            case SEC_LAYOUT_EDITOR: return getString(R.string.set_layout_editor_title);
            case SEC_EMOJI: return getString(R.string.set_emoji_title);
            case SEC_STICKERS: return getString(R.string.set_stickers_title);
            case SEC_DECORATION: return getString(R.string.set_decoration_title);
            case SEC_GESTURES: return getString(R.string.set_gestures_title);
            case SEC_HAPTICS: return getString(R.string.set_vibration_sound_title);
            case SEC_CLIPBOARD: return getString(R.string.set_clipboard_title);
            case SEC_KEYBOARD_PASS: return getString(R.string.set_keyboard_pass_title);
            case SEC_AI: return getString(R.string.ai_keyboard_title);
            case SEC_BACKUP: return getString(R.string.set_local_backup_title);
            case SEC_CLOUD_SYNC: return getString(R.string.set_cloud_sync_title);
            case SEC_PRO:
            default: return getString(R.string.pro_title);
        }
    }

    @Override
    public void pickFontFile() {
        pickingEmojiFont = false;
        fontPickerLauncher.launch(new String[]{"font/ttf", "font/otf", "application/octet-stream", "*/*"});
    }

    @Override
    public void pickEmojiFontFile() {
        pickingEmojiFont = true;
        fontPickerLauncher.launch(new String[]{"font/ttf", "font/otf", "application/octet-stream", "*/*"});
    }

    @Override
    public void pickStickerPhoto() {
        stickerPhotoLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    @Override
    public void exportLocalBackup() {
        exportBackupLauncher.launch("keyboard_pro_backup.json");
    }

    @Override
    public void importLocalBackup() {
        importBackupLauncher.launch(new String[]{"application/json", "*/*"});
    }

    @Override
    public void authenticateVaultBiometric(Runnable onSuccess) {
        BiometricManager bm = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.BIOMETRIC_WEAK
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        if (bm.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // Device/emulator has no enrolled fingerprint/PIN — allow fallback with notice
            Toast.makeText(this, "لم يتم إعداد قفل بصمة على الجهاز — تم فتح الخزنة مؤقتاً", Toast.LENGTH_SHORT).show();
            onSuccess.run();
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        onSuccess.run();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(SettingsDetailActivity.this, errString, Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("فتح Keyboard Pass")
                .setSubtitle("استخدم بصمة الإصبع أو قفل الشاشة للوصول إلى حساباتك المشفرة")
                .setAllowedAuthenticators(authenticators)
                .build();
        prompt.authenticate(info);
    }

    @Override
    public void refreshScreen() {
        renderSection();
    }

    private void copyImportedFont(Uri uri, boolean isEmoji) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return;
            File dir = new File(getFilesDir(), "fonts");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, isEmoji ? "custom_emoji.ttf" : "custom_ui_font.ttf");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
            }
            if (isEmoji) {
                prefs.setCustomEmojiFontPath(out.getAbsolutePath());
                prefs.setEmojiStyle(KeyboardPrefs.EMOJI_STYLE_CUSTOM);
            } else {
                prefs.setCustomUiFontPath(out.getAbsolutePath());
            }
            Toast.makeText(this, "تم استيراد ملف الخط بنجاح ✅", Toast.LENGTH_SHORT).show();
            refreshScreen();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر استيراد ملف الخط", Toast.LENGTH_SHORT).show();
        }
    }

    private void createStickerFromPhoto(Uri uri) {
        try {
            Bitmap bmp = com.keyboardpro.data.BitmapUtils.decodeSampledFromUri(this, uri, 384, 384);
            if (bmp == null) return;
            new StickerRepository(this).saveStickerBitmap("ملصق صورة 📷", bmp, false);
            bmp.recycle();
            Toast.makeText(this, "تم حفظ الملصق الجديد في ملصقاتي ✅", Toast.LENGTH_SHORT).show();
            refreshScreen();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر إنشاء الملصق من الصورة", Toast.LENGTH_SHORT).show();
        }
    }
}
