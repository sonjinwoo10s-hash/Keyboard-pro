package com.keyboardpro.ime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.R;
import com.keyboardpro.data.ClipboardRepository;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.data.StickerRepository;
import com.keyboardpro.data.VaultRepository;
import com.keyboardpro.ui.MainActivity;
import com.keyboardpro.ui.SettingsDetailActivity;

import java.io.File;
import java.util.List;

/**
 * Core InputMethodService for Keyboard Pro (كيبورد برو).
 * Engineered for strict low-RAM (~30-60MB) footprint:
 * - Disables landscape fullscreen ExtractView (onEvaluateFullscreenMode() always returns false).
 * - Lazy-loads Emoji, Stickers, Clipboard, Vault, AI helper, and custom Emoji fonts ONLY on demand.
 * - Immediately releases panel views, cached shaders, and custom emoji Typeface inside
 *   onFinishInputView() and onTrimMemory().
 * - Smart Blur: hardware-accelerated Window.setBackgroundBlurRadius() on API 31+,
 *   lightweight translucent alpha overlay on API 26-30 (disabled by default).
 */
public class KeyboardProService extends InputMethodService implements ProKeyboardView.OnKeyboardActionListener {

    private static final String[] EMOJI_LIST = {
            "😀", "😂", "🥰", "😍", "🤩", "😎", "🥳", "🤔", "😴", "😭",
            "🔥", "✨", "💎", "👑", "❤️", "💙", "💚", "💛", "💜", "🤍",
            "👍", "👏", "🙏", "🤝", "💪", "🎉", "🎊", "🌹", "🌙", "⭐",
            "☕", "🚀", "⚡", "🎯", "🏆", "📱", "💻", "🎵", "📸", "✅"
    };

    private KeyboardPrefs prefs;
    private TextStyleEngine styleEngine;
    private SmartWordEngine wordEngine;
    private ClipboardManager systemClipboard;

    // Strictly lazy-loaded helpers (null until explicitly accessed by user)
    private ClipboardRepository clipboardRepo;
    private VaultRepository vaultRepo;
    private StickerRepository stickerRepo;
    private AiKeyboardHelper aiHelper;
    private Typeface lazyCustomEmojiTypeface;

    private View rootView;
    private ProKeyboardView keyboardView;
    private LinearLayout toolbar;
    private TextView btnDecorate;
    private HorizontalScrollView styleStripScroll;
    private LinearLayout styleStripContainer;
    private LinearLayout aiBar;
    private HorizontalScrollView suggestionsScroll;
    private LinearLayout suggestionsContainer;
    private ScrollView inlinePanelScroll;
    private LinearLayout inlinePanelContent;

    private final StringBuilder currentWord = new StringBuilder();
    private String previousWord = "";
    private String activeInlinePanel = "";
    private boolean styleStripVisible = false;

    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = () -> {
        if (systemClipboard == null) return;
        ClipData data = systemClipboard.getPrimaryClip();
        if (data != null && data.getItemCount() > 0) {
            CharSequence text = data.getItemAt(0).coerceToText(this);
            if (text != null && text.length() > 0) {
                getClipboardRepo().addClip(text.toString());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new KeyboardPrefs(this);
        styleEngine = new TextStyleEngine();
        wordEngine = new SmartWordEngine(this);

        systemClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (systemClipboard != null) {
            systemClipboard.addPrimaryClipChangedListener(clipListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (systemClipboard != null) {
            systemClipboard.removePrimaryClipChangedListener(clipListener);
        }
        releaseHeavyResources();
    }

    private ClipboardRepository getClipboardRepo() {
        if (clipboardRepo == null) clipboardRepo = new ClipboardRepository(this);
        return clipboardRepo;
    }

    private VaultRepository getVaultRepo() {
        if (vaultRepo == null) vaultRepo = new VaultRepository(this);
        return vaultRepo;
    }

    private StickerRepository getStickerRepo() {
        if (stickerRepo == null) stickerRepo = new StickerRepository(this);
        return stickerRepo;
    }

    private AiKeyboardHelper getAiHelper() {
        if (aiHelper == null) aiHelper = new AiKeyboardHelper(this);
        return aiHelper;
    }

    /**
     * CRITICAL REQUIREMENT: Always disable landscape fullscreen ExtractView
     * so the underlying app stays visible behind the keyboard in landscape mode.
     */
    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public View onCreateInputView() {
        rootView = getLayoutInflater().inflate(R.layout.ime_keyboard_container, null);
        keyboardView = rootView.findViewById(R.id.ime_keyboard_view);
        toolbar = rootView.findViewById(R.id.ime_toolbar);
        btnDecorate = rootView.findViewById(R.id.btn_ime_decorate);
        styleStripScroll = rootView.findViewById(R.id.ime_style_strip_scroll);
        styleStripContainer = rootView.findViewById(R.id.ime_style_strip_container);
        aiBar = rootView.findViewById(R.id.ime_ai_bar);
        suggestionsScroll = rootView.findViewById(R.id.ime_suggestions_scroll);
        suggestionsContainer = rootView.findViewById(R.id.ime_suggestions_container);
        inlinePanelScroll = rootView.findViewById(R.id.ime_inline_panel_scroll);
        inlinePanelContent = rootView.findViewById(R.id.ime_inline_panel_content);

        keyboardView.setListener(this);
        setupToolbarButtons();
        applyThemeToContainer();
        refreshSuggestions();

        return rootView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (keyboardView != null) {
            String lang = prefs.getKeyboardLanguage();
            keyboardView.setMode("en".equals(lang)
                    ? KeyboardLayoutData.KeyboardMode.ENGLISH
                    : KeyboardLayoutData.KeyboardMode.ARABIC);
            keyboardView.reloadConfig();
            applyThemeToContainer();
            applySmartWindowBlur();
            updateDecorateButtonUi();
            closeInlinePanels();
            currentWord.setLength(0);
            refreshSuggestions();
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        // Immediately release panel views, emoji font typeface, and transient caches when keyboard hides
        releaseHeavyResources();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_UI_HIDDEN || level >= TRIM_MEMORY_BACKGROUND) {
            releaseHeavyResources();
        }
    }

    private void releaseHeavyResources() {
        closeInlinePanels();
        if (styleStripContainer != null) {
            styleStripContainer.removeAllViews();
        }
        if (suggestionsContainer != null) {
            suggestionsContainer.removeAllViews();
        }
        styleStripVisible = false;
        if (styleStripScroll != null) {
            styleStripScroll.setVisibility(View.GONE);
        }
        if (keyboardView != null) {
            keyboardView.releaseCaches();
        }
        lazyCustomEmojiTypeface = null;
        stickerRepo = null;
        aiHelper = null;
        currentWord.setLength(0);
    }

    /**
     * Smart Blur Optimization:
     * - Disabled by default.
     * - Android 12+ (API 31+): hardware-accelerated Window.setBackgroundBlurRadius()
     * - Android 8-11 (API 26-30): lightweight translucent alpha overlay only (zero software blur).
     */
    private void applySmartWindowBlur() {
        if (getWindow() == null || getWindow().getWindow() == null) return;
        Window imeWindow = getWindow().getWindow();
        boolean blurOn = prefs.isTransparentBlurEnabled();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurOn) {
                int radiusPx = Math.max(12, Math.min(80, prefs.getBlurStrength()));
                imeWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                imeWindow.setBackgroundBlurRadius(radiusPx);
            } else {
                imeWindow.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                imeWindow.setBackgroundBlurRadius(0);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (keyboardView != null) {
            keyboardView.reloadConfig();
        }
    }

    private void setupToolbarButtons() {
        btnDecorate.setOnClickListener(v -> {
            if (!styleStripVisible) {
                styleStripVisible = true;
                styleStripScroll.setVisibility(View.VISIBLE);
                if (!prefs.isDecorationEnabled()) {
                    prefs.setDecorationEnabled(true);
                    if (TextStyleEngine.STYLE_NORMAL.equals(prefs.getActiveStyleId())) {
                        prefs.setActiveStyleId(TextStyleEngine.STYLE_AR_KASHIDA);
                    }
                }
                populateStyleStrip();
            } else {
                prefs.setDecorationEnabled(!prefs.isDecorationEnabled());
                styleStripVisible = false;
                styleStripScroll.setVisibility(View.GONE);
                styleStripContainer.removeAllViews();
            }
            updateDecorateButtonUi();
        });

        rootView.findViewById(R.id.btn_ime_ai).setOnClickListener(v -> {
            boolean show = aiBar.getVisibility() != View.VISIBLE;
            aiBar.setVisibility(show ? View.VISIBLE : View.GONE);
        });

        rootView.findViewById(R.id.btn_ai_proofread).setOnClickListener(v -> runAiAction(AiKeyboardHelper.Action.PROOFREAD));
        rootView.findViewById(R.id.btn_ai_rewrite).setOnClickListener(v -> runAiAction(AiKeyboardHelper.Action.REWRITE));
        rootView.findViewById(R.id.btn_ai_translate).setOnClickListener(v -> runAiAction(AiKeyboardHelper.Action.TRANSLATE));

        rootView.findViewById(R.id.btn_ime_clipboard).setOnClickListener(v -> toggleInlinePanel("clipboard"));
        rootView.findViewById(R.id.btn_ime_vault).setOnClickListener(v -> toggleInlinePanel("vault"));
        rootView.findViewById(R.id.btn_ime_emoji).setOnClickListener(v -> toggleInlinePanel("emoji"));

        rootView.findViewById(R.id.btn_ime_settings).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void applyThemeToContainer() {
        if (rootView == null) return;
        KeyboardTheme theme = ThemeRegistry.getThemeById(this, prefs.getActiveThemeId());
        int alpha = prefs.isTransparentBlurEnabled() ? 195 : 255;
        int tbColor = Color.argb(alpha,
                Color.red(theme.toolbarBgColor),
                Color.green(theme.toolbarBgColor),
                Color.blue(theme.toolbarBgColor));
        toolbar.setBackgroundColor(tbColor);
        toolbar.setVisibility(prefs.isShowToolbar() ? View.VISIBLE : View.GONE);
        suggestionsScroll.setVisibility(prefs.isSuggestionsEnabled() ? View.VISIBLE : View.GONE);
        rootView.setBackgroundColor(prefs.isTransparentBlurEnabled() ? Color.TRANSPARENT : theme.bgStartColor);
        updateDecorateButtonUi();
    }

    private void updateDecorateButtonUi() {
        if (btnDecorate == null) return;
        boolean on = prefs.isDecorationEnabled() && !TextStyleEngine.STYLE_NORMAL.equals(prefs.getActiveStyleId());
        if (on) {
            TextStyleEngine.StyleOption opt = TextStyleEngine.findStyle(prefs.getActiveStyleId());
            btnDecorate.setText("✨ " + opt.shortLabel);
            btnDecorate.setBackgroundResource(R.drawable.bg_chip_selected);
            btnDecorate.setTextColor(getColor(R.color.kp_primary));
        } else {
            btnDecorate.setText("✨ زخرفة");
            btnDecorate.setBackgroundResource(R.drawable.bg_chip_unselected);
            btnDecorate.setTextColor(getColor(R.color.kp_text_primary));
        }
    }

    private void populateStyleStrip() {
        styleStripContainer.removeAllViews();
        String activeId = prefs.getActiveStyleId();
        boolean enabled = prefs.isDecorationEnabled();

        for (TextStyleEngine.StyleOption style : TextStyleEngine.getAllStyles()) {
            TextView chip = new TextView(this);
            chip.setText(style.shortLabel);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setGravity(Gravity.CENTER);
            int padH = dp(10);
            chip.setPadding(padH, dp(4), padH, dp(4));

            boolean isSelected = (enabled && style.id.equals(activeId))
                    || (!enabled && TextStyleEngine.STYLE_NORMAL.equals(style.id));

            chip.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(isSelected ? getColor(R.color.kp_primary) : getColor(R.color.kp_text_primary));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(28));
            lp.setMargins(dp(3), 0, dp(3), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                if (TextStyleEngine.STYLE_NORMAL.equals(style.id)) {
                    prefs.setDecorationEnabled(false);
                    prefs.setActiveStyleId(TextStyleEngine.STYLE_NORMAL);
                } else {
                    prefs.setDecorationEnabled(true);
                    prefs.setActiveStyleId(style.id);
                }
                updateDecorateButtonUi();
                populateStyleStrip();
            });

            styleStripContainer.addView(chip);
        }
    }

    private void toggleInlinePanel(String panelName) {
        if (panelName.equals(activeInlinePanel)) {
            closeInlinePanels();
            return;
        }
        activeInlinePanel = panelName;
        inlinePanelContent.removeAllViews();
        inlinePanelScroll.setVisibility(View.VISIBLE);
        keyboardView.setVisibility(View.GONE);

        if ("emoji".equals(panelName)) {
            buildEmojiAndStickersPanel();
        } else if ("clipboard".equals(panelName)) {
            buildClipboardPanel();
        } else if ("vault".equals(panelName)) {
            buildVaultPanel();
        }
    }

    private void closeInlinePanels() {
        activeInlinePanel = "";
        if (inlinePanelContent != null) {
            inlinePanelContent.removeAllViews();
        }
        if (inlinePanelScroll != null) inlinePanelScroll.setVisibility(View.GONE);
        if (keyboardView != null) keyboardView.setVisibility(View.VISIBLE);
        lazyCustomEmojiTypeface = null;
    }

    private Typeface getLazyCustomEmojiTypeface() {
        if (!KeyboardPrefs.EMOJI_STYLE_CUSTOM.equals(prefs.getEmojiStyle())) {
            return null;
        }
        if (lazyCustomEmojiTypeface == null) {
            String path = prefs.getCustomEmojiFontPath();
            if (path != null && !path.isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    try {
                        lazyCustomEmojiTypeface = Typeface.createFromFile(f);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return lazyCustomEmojiTypeface;
    }

    private void buildEmojiAndStickersPanel() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("الإيموجي (" + prefs.getEmojiStyle() + ") وملصقاتي");
        title.setTextColor(getColor(R.color.kp_primary));
        title.setTextSize(13);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = new TextView(this);
        closeBtn.setText("أبج ✕");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        closeBtn.setBackgroundResource(R.drawable.bg_chip_selected);
        closeBtn.setOnClickListener(v -> closeInlinePanels());
        header.addView(closeBtn);
        inlinePanelContent.addView(header);

        Typeface customEmojiTf = getLazyCustomEmojiTypeface();

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(8);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridLp.topMargin = dp(8);
        grid.setLayoutParams(gridLp);

        for (String emoji : EMOJI_LIST) {
            TextView tv = new TextView(this);
            tv.setText(emoji);
            tv.setTextSize(22);
            if (customEmojiTf != null) tv.setTypeface(customEmojiTf);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(4), dp(6), dp(4), dp(6));
            tv.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(emoji, 1);
                onKeyFeedback();
            });
            GridLayout.LayoutParams cellLp = new GridLayout.LayoutParams();
            cellLp.width = 0;
            cellLp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            tv.setLayoutParams(cellLp);
            grid.addView(tv);
        }
        inlinePanelContent.addView(grid);

        TextView stkHeader = new TextView(this);
        stkHeader.setText("ملصقاتي السريعة:");
        stkHeader.setTextColor(getColor(R.color.kp_text_secondary));
        stkHeader.setTextSize(12);
        stkHeader.setPadding(0, dp(10), 0, dp(6));
        inlinePanelContent.addView(stkHeader);

        LinearLayout stkRow = new LinearLayout(this);
        stkRow.setOrientation(LinearLayout.HORIZONTAL);
        for (StickerRepository.StickerItem s : getStickerRepo().getStickers()) {
            TextView pill = new TextView(this);
            pill.setText(s.label);
            pill.setTextColor(Color.WHITE);
            pill.setTextSize(12);
            pill.setPadding(dp(10), dp(6), dp(10), dp(6));
            pill.setBackgroundResource(R.drawable.bg_chip_unselected);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(4), 0, dp(4), 0);
            pill.setLayoutParams(lp);
            pill.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(s.label + " ", 1);
                onKeyFeedback();
            });
            stkRow.addView(pill);
        }
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.addView(stkRow);
        inlinePanelContent.addView(hsv);
    }

    private void buildClipboardPanel() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("سجل الحافظة (اضغط للصق)");
        title.setTextColor(getColor(R.color.kp_primary));
        title.setTextSize(13);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = new TextView(this);
        closeBtn.setText("إغلاق ✕");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(dp(10), dp(5), dp(10), dp(5));
        closeBtn.setBackgroundResource(R.drawable.bg_chip_selected);
        closeBtn.setOnClickListener(v -> closeInlinePanels());
        header.addView(closeBtn);
        inlinePanelContent.addView(header);

        // On-demand indexed SQLite LIMIT 15 query
        List<ClipboardRepository.ClipItem> clips = getClipboardRepo().getClips(15);
        if (clips.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.ime_clipboard_empty);
            empty.setTextColor(getColor(R.color.kp_text_secondary));
            empty.setPadding(0, dp(24), 0, dp(16));
            empty.setGravity(Gravity.CENTER);
            inlinePanelContent.addView(empty);
            return;
        }

        for (ClipboardRepository.ClipItem clip : clips) {
            TextView row = new TextView(this);
            row.setText((clip.pinned ? "📌 " : "") + clip.text);
            row.setTextColor(Color.WHITE);
            row.setMaxLines(2);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackgroundResource(R.drawable.bg_chip_unselected);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            row.setLayoutParams(lp);
            row.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    String textToCommit = prefs.isDecorationEnabled()
                            ? styleEngine.transformText(clip.text, prefs.getActiveStyleId())
                            : clip.text;
                    ic.commitText(textToCommit, 1);
                }
                closeInlinePanels();
            });
            inlinePanelContent.addView(row);
        }
    }

    private void buildVaultPanel() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("🔐 Keyboard Pass — الكتابة التلقائية الآمنة");
        title.setTextColor(getColor(R.color.kp_gold));
        title.setTextSize(13);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = new TextView(this);
        closeBtn.setText("إغلاق ✕");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(dp(10), dp(5), dp(10), dp(5));
        closeBtn.setBackgroundResource(R.drawable.bg_chip_selected);
        closeBtn.setOnClickListener(v -> closeInlinePanels());
        header.addView(closeBtn);
        inlinePanelContent.addView(header);

        VaultRepository vault = getVaultRepo();
        if (!vault.isSessionUnlocked()) {
            TextView locked = new TextView(this);
            locked.setText(R.string.ime_vault_locked);
            locked.setTextColor(getColor(R.color.kp_text_secondary));
            locked.setPadding(dp(8), dp(16), dp(8), dp(12));
            inlinePanelContent.addView(locked);

            TextView unlockBtn = new TextView(this);
            unlockBtn.setText("فتح بالبصمة الآن 🔓");
            unlockBtn.setTextColor(getColor(R.color.kp_gold));
            unlockBtn.setGravity(Gravity.CENTER);
            unlockBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
            unlockBtn.setBackgroundResource(R.drawable.bg_crown_button);
            unlockBtn.setOnClickListener(v -> {
                Intent i = new Intent(this, SettingsDetailActivity.class);
                i.putExtra(SettingsDetailActivity.EXTRA_SECTION, SettingsDetailActivity.SEC_KEYBOARD_PASS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            });
            inlinePanelContent.addView(unlockBtn);
            return;
        }

        List<VaultRepository.VaultItem> items = vault.getItems();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("الخزنة فارغة — أضف حساباتك أو رقم الآيبان من Keyboard Pass.");
            empty.setTextColor(getColor(R.color.kp_text_secondary));
            empty.setPadding(0, dp(16), 0, dp(8));
            inlinePanelContent.addView(empty);
            return;
        }

        for (VaultRepository.VaultItem item : items) {
            TextView btn = new TextView(this);
            btn.setText("🔑 " + item.label + "  (" + item.usernameOrMeta + ")");
            btn.setTextColor(Color.WHITE);
            btn.setPadding(dp(12), dp(10), dp(12), dp(10));
            btn.setBackgroundResource(R.drawable.bg_chip_unselected);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(item.secretValue, 1);
                closeInlinePanels();
            });
            inlinePanelContent.addView(btn);
        }
    }

    private void runAiAction(AiKeyboardHelper.Action action) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence selected = ic.getSelectedText(0);
        boolean hadSelection = selected != null && selected.length() > 0;
        CharSequence before = hadSelection ? selected : ic.getTextBeforeCursor(300, 0);
        String text = before == null ? "" : before.toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "اكتب أو حدد نصاً أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "جاري المعالجة بالذكاء الاصطناعي...", Toast.LENGTH_SHORT).show();
        getAiHelper().processText(text, action, (success, output) -> {
            if (!success) {
                Toast.makeText(this, output, Toast.LENGTH_LONG).show();
                return;
            }
            InputConnection currentIc = getCurrentInputConnection();
            if (currentIc == null) return;
            if (hadSelection) {
                currentIc.commitText(output, 1);
            } else {
                currentIc.deleteSurroundingText(before.length(), 0);
                currentIc.commitText(output, 1);
            }
            currentWord.setLength(0);
        });
    }

    @Override
    public void onKeyOutput(String text, int code) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        if (code == KeyboardLayoutData.CODE_SPACE) {
            handleWordBoundary(ic, " ");
            return;
        }

        boolean decorateOn = prefs.isDecorationEnabled();
        String styleId = prefs.getActiveStyleId();

        String transformed = styleEngine.transformTypedChar(text, styleId, decorateOn);
        ic.commitText(transformed, 1);

        if (Character.isLetterOrDigit(text.charAt(0))) {
            currentWord.append(text);
        } else {
            currentWord.setLength(0);
        }
        refreshSuggestions();
    }

    private void handleWordBoundary(InputConnection ic, String trailing) {
        String rawWord = currentWord.toString();
        if (!rawWord.isEmpty()) {
            String expanded = wordEngine.expandShortcutExact(rawWord);
            if (expanded != null) {
                ic.deleteSurroundingText(rawWord.length(), 0);
                String out = prefs.isDecorationEnabled()
                        ? styleEngine.transformText(expanded, prefs.getActiveStyleId())
                        : expanded;
                ic.commitText(out + trailing, 1);
                previousWord = expanded;
                currentWord.setLength(0);
                refreshSuggestions();
                return;
            }

            String styleId = prefs.getActiveStyleId();
            if (prefs.isDecorationEnabled() && TextStyleEngine.isFramedStyle(styleId)) {
                CharSequence recent = ic.getTextBeforeCursor(rawWord.length() * 2 + 4, 0);
                if (recent != null && recent.length() > 0) {
                    String s = recent.toString();
                    int lastSpace = s.lastIndexOf(' ');
                    String typedSegment = lastSpace >= 0 ? s.substring(lastSpace + 1) : s;
                    if (!typedSegment.isEmpty()) {
                        ic.deleteSurroundingText(typedSegment.length(), 0);
                        ic.commitText(styleEngine.wrapFramedWord(typedSegment, styleId) + trailing, 1);
                        previousWord = rawWord;
                        currentWord.setLength(0);
                        refreshSuggestions();
                        return;
                    }
                }
            }

            previousWord = rawWord;
            currentWord.setLength(0);
        }
        ic.commitText(trailing, 1);
        refreshSuggestions();
    }

    @Override
    public void onSpecialKey(int code) {
        InputConnection ic = getCurrentInputConnection();
        switch (code) {
            case KeyboardLayoutData.CODE_DELETE:
                if (ic != null) {
                    CharSequence sel = ic.getSelectedText(0);
                    if (sel != null && sel.length() > 0) {
                        ic.commitText("", 1);
                    } else {
                        CharSequence before2 = ic.getTextBeforeCursor(2, 0);
                        if (before2 != null && before2.length() == 2
                                && Character.isSurrogatePair(before2.charAt(0), before2.charAt(1))) {
                            ic.deleteSurroundingText(2, 0);
                        } else {
                            ic.deleteSurroundingText(1, 0);
                        }
                    }
                }
                if (currentWord.length() > 0) {
                    currentWord.deleteCharAt(currentWord.length() - 1);
                }
                refreshSuggestions();
                break;

            case KeyboardLayoutData.CODE_ENTER:
                if (ic != null) {
                    EditorInfo ei = getCurrentInputEditorInfo();
                    int action = ei != null ? (ei.imeOptions & EditorInfo.IME_MASK_ACTION) : EditorInfo.IME_ACTION_NONE;
                    if (action == EditorInfo.IME_ACTION_SEARCH
                            || action == EditorInfo.IME_ACTION_SEND
                            || action == EditorInfo.IME_ACTION_GO
                            || action == EditorInfo.IME_ACTION_DONE) {
                        ic.performEditorAction(action);
                    } else {
                        ic.commitText("\n", 1);
                    }
                }
                currentWord.setLength(0);
                refreshSuggestions();
                break;

            case KeyboardLayoutData.CODE_SHIFT:
                keyboardView.toggleShift();
                break;

            case KeyboardLayoutData.CODE_SWITCH_LANG:
                if (keyboardView.getMode() == KeyboardLayoutData.KeyboardMode.ARABIC) {
                    keyboardView.setMode(KeyboardLayoutData.KeyboardMode.ENGLISH);
                    prefs.setKeyboardLanguage("en");
                } else {
                    keyboardView.setMode(KeyboardLayoutData.KeyboardMode.ARABIC);
                    prefs.setKeyboardLanguage("ar");
                }
                currentWord.setLength(0);
                refreshSuggestions();
                break;

            case KeyboardLayoutData.CODE_MODE_SYMBOLS:
                if (keyboardView.getMode() == KeyboardLayoutData.KeyboardMode.SYMBOLS_1
                        || keyboardView.getMode() == KeyboardLayoutData.KeyboardMode.SYMBOLS_2) {
                    keyboardView.setMode("en".equals(prefs.getKeyboardLanguage())
                            ? KeyboardLayoutData.KeyboardMode.ENGLISH
                            : KeyboardLayoutData.KeyboardMode.ARABIC);
                } else {
                    keyboardView.setMode(KeyboardLayoutData.KeyboardMode.SYMBOLS_1);
                }
                break;

            case KeyboardLayoutData.CODE_MODE_SYMBOLS_MORE:
                keyboardView.setMode(KeyboardLayoutData.KeyboardMode.SYMBOLS_2);
                break;

            case KeyboardLayoutData.CODE_EMOJI:
                toggleInlinePanel("emoji");
                break;
        }
    }

    @Override
    public void onSwipeWord(String rawPathChars) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        boolean isArabic = keyboardView.getMode() == KeyboardLayoutData.KeyboardMode.ARABIC;
        String matched = wordEngine.matchSwipeSequence(rawPathChars, isArabic);
        if (matched != null) {
            String out = prefs.isDecorationEnabled()
                    ? styleEngine.transformText(matched, prefs.getActiveStyleId())
                    : matched;
            ic.commitText(out + " ", 1);
            previousWord = matched;
            currentWord.setLength(0);
            refreshSuggestions();
        }
    }

    @Override
    public void onCursorMove(int steps) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        int keyCode = steps > 0 ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
        int count = Math.min(5, Math.abs(steps));
        for (int i = 0; i < count; i++) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        }
    }

    @Override
    public void onSwipeDeleteWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence before = ic.getTextBeforeCursor(40, 0);
        if (before == null || before.length() == 0) return;
        String s = before.toString();
        int idx = s.length() - 1;
        while (idx > 0 && s.charAt(idx) == ' ') idx--;
        while (idx > 0 && s.charAt(idx - 1) != ' ') idx--;
        ic.deleteSurroundingText(s.length() - idx, 0);
        currentWord.setLength(0);
        refreshSuggestions();
    }

    @Override
    public void onKeyFeedback() {
        if (prefs.isVibrationEnabled()) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                int ms = prefs.getVibrationStrengthMs();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(ms);
                }
            }
        }
        if (prefs.isKeySoundEnabled() && !KeyboardPrefs.SOUND_PACK_SILENT.equals(prefs.getSoundPack())) {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int fx = AudioManager.FX_KEYPRESS_STANDARD;
                if (KeyboardPrefs.SOUND_PACK_MECHANICAL.equals(prefs.getSoundPack())) {
                    fx = AudioManager.FX_KEYPRESS_SPACEBAR;
                } else if (KeyboardPrefs.SOUND_PACK_CRYSTAL.equals(prefs.getSoundPack())) {
                    fx = AudioManager.FX_KEY_CLICK;
                }
                am.playSoundEffect(fx, 0.5f);
            }
        }
    }

    private void refreshSuggestions() {
        if (suggestionsContainer == null) return;
        suggestionsContainer.removeAllViews();
        boolean isArabic = keyboardView == null
                || keyboardView.getMode() == KeyboardLayoutData.KeyboardMode.ARABIC;
        List<String> list = wordEngine.getSuggestions(currentWord.toString(), previousWord, isArabic);

        for (String suggestion : list) {
            TextView tv = new TextView(this);
            tv.setText(suggestion);
            tv.setTextColor(getColor(R.color.kp_text_primary));
            tv.setTextSize(13);
            tv.setPadding(dp(12), dp(4), dp(12), dp(4));
            tv.setBackgroundResource(R.drawable.bg_chip_unselected);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(26));
            lp.setMargins(dp(4), 0, dp(4), 0);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    if (currentWord.length() > 0) {
                        ic.deleteSurroundingText(currentWord.length(), 0);
                    }
                    String out = prefs.isDecorationEnabled()
                            ? styleEngine.transformText(suggestion, prefs.getActiveStyleId())
                            : suggestion;
                    ic.commitText(out + " ", 1);
                    previousWord = suggestion;
                    currentWord.setLength(0);
                    refreshSuggestions();
                }
            });
            suggestionsContainer.addView(tv);
        }
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }
}
