package com.keyboardpro.ime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.keyboardpro.data.ClipboardRepository;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.ui.MainActivity;

import java.io.File;
import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;

/**
 * Ultra-lightweight Keyboard Pro InputMethodService (~30-60MB RAM):
 * - Strictly unified full-width in landscape by default + onEvaluateFullscreenMode() = false
 * - Auto-disables text decoration in password, email, and URL fields + Bank Apps exclusion list
 * - Smart Arabic intelligence (Hamza/Ta, Franco-Arabic, Inline Math, Hijri/Gregorian Dates, Azkar)
 * - Customizable pinned Top Toolbar + 26-tool "All Tools" drawer (long-press to pin/unpin)
 * - Per-action Sound & Vibration matrix (10 actions) + 2-finger gestures
 * - API 31+ hardware blur + API 26-30 alpha overlay fallback
 * - Lazy panel inflation & immediate cache cleanup in onFinishInputView() and onTrimMemory()
 */
public class KeyboardProService extends InputMethodService
        implements ProKeyboardView.OnKeyboardActionListener, ImePanelsController.Host {

    private KeyboardPrefs prefs;
    private ClipboardRepository clipRepo;
    private SmartWordEngine wordEngine;
    private TextStyleEngine styleEngine;
    private ImePanelsController panelsController;

    private LinearLayout rootLayout;
    private LinearLayout toolbarRow;
    private LinearLayout quickActionsRow;
    private HorizontalScrollView suggestionsScroll;
    private LinearLayout suggestionsContainer;
    private FrameLayout bodyContainer;
    private ProKeyboardView keyboardView;
    private View activePanelView;
    private LinearLayout iphoneBottomBar;
    private View bottomSpacerView;
    private PopupWindow popupWindow;

    private KeyboardTheme activeTheme;
    private String activeLangCode = "ar";
    private boolean isSymbols = false;
    private boolean isSymbolsPage2 = false;
    private boolean isShifted = false;
    private boolean isLandscape = false;
    private boolean sensitiveFieldActive = false;
    private boolean bankAppExcluded = false;
    private boolean selectModeActive = false;

    private final StringBuilder currentWord = new StringBuilder();
    private String previousWord = "";
    private String cachedFontPath = "";
    private Typeface cachedCustomTypeface = null;

    private ClipboardManager clipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener clipListener;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private TextToSpeech tts;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new KeyboardPrefs(this);
        clipRepo = new ClipboardRepository(this);
        wordEngine = new SmartWordEngine(this);
        styleEngine = new TextStyleEngine();
        panelsController = new ImePanelsController(this, prefs, clipRepo, this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipListener = () -> {
                if (prefs.isIncognitoMode() || sensitiveFieldActive || bankAppExcluded) return;
                ClipData cd = clipboardManager.getPrimaryClip();
                if (cd != null && cd.getItemCount() > 0 && cd.getItemAt(0).getText() != null) {
                    clipRepo.addClip(cd.getItemAt(0).getText().toString());
                }
            };
            clipboardManager.addPrimaryClipChangedListener(clipListener);
        }
    }

    /** CRITICAL: Never cover the underlying app with Fullscreen ExtractView in Landscape! */
    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public View onCreateInputView() {
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        syncActiveLanguageFromPrefs();
        activeTheme = ThemeRegistry.getThemeById(this, prefs.getActiveThemeId());

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 1. Top Toolbar (Customizable Pinned Tools + More ...)
        toolbarRow = new LinearLayout(this);
        toolbarRow.setOrientation(LinearLayout.HORIZONTAL);
        toolbarRow.setGravity(Gravity.CENTER_VERTICAL);
        toolbarRow.setPadding(dp(6), dp(4), dp(6), dp(4));
        rootLayout.addView(toolbarRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        // 2. Suggestions Bar
        suggestionsScroll = new HorizontalScrollView(this);
        suggestionsScroll.setHorizontalScrollBarEnabled(false);
        suggestionsContainer = new LinearLayout(this);
        suggestionsContainer.setOrientation(LinearLayout.HORIZONTAL);
        suggestionsContainer.setGravity(Gravity.CENTER_VERTICAL);
        suggestionsContainer.setPadding(dp(6), dp(2), dp(6), dp(2));
        suggestionsScroll.addView(suggestionsContainer);
        rootLayout.addView(suggestionsScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));

        // 3. Optional Quick Actions Row (Undo, Copy, Paste, Select All, Clear)
        quickActionsRow = new LinearLayout(this);
        quickActionsRow.setOrientation(LinearLayout.HORIZONTAL);
        quickActionsRow.setGravity(Gravity.CENTER);
        quickActionsRow.setPadding(dp(6), dp(2), dp(6), dp(2));
        rootLayout.addView(quickActionsRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        // 4. Main Body (Keyboard Canvas OR Lazy-loaded Panel)
        bodyContainer = new FrameLayout(this);
        keyboardView = new ProKeyboardView(this);
        keyboardView.setOnKeyboardActionListener(this);
        bodyContainer.addView(keyboardView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.addView(bodyContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, resolveKeyboardHeightPx()));

        // 5. Optional iPhone Bottom Bar (Globe + Mic + Hide)
        iphoneBottomBar = new LinearLayout(this);
        iphoneBottomBar.setOrientation(LinearLayout.HORIZONTAL);
        iphoneBottomBar.setGravity(Gravity.CENTER_VERTICAL);
        iphoneBottomBar.setPadding(dp(14), dp(2), dp(14), dp(4));
        rootLayout.addView(iphoneBottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        // 6. Optional Bottom Margin Spacer
        bottomSpacerView = new View(this);
        rootLayout.addView(bottomSpacerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(prefs.getBottomMarginDp())));

        applyThemeAndLayout();
        return rootLayout;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        currentWord.setLength(0);
        detectSensitiveOrExcludedContext(info);
        closePanel();
        applyThemeAndLayout();
        applyHardwareBlurIfNeeded();
        refreshSuggestions();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        dismissPopup();
        closePanel();
        if (keyboardView != null) keyboardView.releaseCaches();
        currentWord.setLength(0);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            closePanel();
            if (keyboardView != null) keyboardView.releaseCaches();
            cachedCustomTypeface = null;
            cachedFontPath = "";
        }
    }

    @Override
    public void onDestroy() {
        if (clipboardManager != null && clipListener != null) {
            clipboardManager.removePrimaryClipChangedListener(clipListener);
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    private void detectSensitiveOrExcludedContext(EditorInfo info) {
        sensitiveFieldActive = false;
        bankAppExcluded = false;
        if (info == null) return;

        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        int var = info.inputType & InputType.TYPE_MASK_VARIATION;
        if (cls == InputType.TYPE_CLASS_TEXT) {
            if (var == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || var == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                    || var == InputType.TYPE_TEXT_VARIATION_URI) {
                sensitiveFieldActive = true;
            }
        } else if (cls == InputType.TYPE_CLASS_NUMBER && var == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
            sensitiveFieldActive = true;
        }

        String pkg = info.packageName;
        if (pkg != null && !pkg.isEmpty()) {
            for (String ex : prefs.getBankExclusionApps().split(",")) {
                if (!ex.trim().isEmpty() && pkg.equalsIgnoreCase(ex.trim())) {
                    bankAppExcluded = true;
                    break;
                }
            }
        }
    }

    private void applyHardwareBlurIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && getWindow() != null && getWindow().getWindow() != null) {
            Window w = getWindow().getWindow();
            boolean wantBlur = prefs.isTransparentBlurEnabled()
                    || (activeTheme != null && activeTheme.backgroundType == KeyboardTheme.BackgroundType.TRANSPARENT_BLUR);
            try {
                w.setBackgroundBlurRadius(wantBlur ? Math.max(12, prefs.getBlurStrength() / 2) : 0);
            } catch (Exception ignored) {
            }
        }
    }

    private void applyThemeAndLayout() {
        if (rootLayout == null || keyboardView == null) return;
        activeTheme = ThemeRegistry.getThemeById(this, prefs.getActiveThemeId());
        syncActiveLanguageFromPrefs();

        rootLayout.setBackgroundColor(activeTheme.toolbarBgColor);
        toolbarRow.setVisibility(prefs.isShowToolbar() ? View.VISIBLE : View.GONE);
        suggestionsScroll.setVisibility(prefs.isSuggestionsEnabled() && !sensitiveFieldActive ? View.VISIBLE : View.GONE);
        quickActionsRow.setVisibility(prefs.isQuickActionsRowEnabled() ? View.VISIBLE : View.GONE);
        iphoneBottomBar.setVisibility(prefs.isIphoneBottomBar() ? View.VISIBLE : View.GONE);

        ViewGroup.LayoutParams bodyLp = bodyContainer.getLayoutParams();
        bodyLp.height = resolveKeyboardHeightPx();
        bodyContainer.setLayoutParams(bodyLp);

        ViewGroup.LayoutParams spLp = bottomSpacerView.getLayoutParams();
        spLp.height = dp(prefs.getBottomMarginDp());
        bottomSpacerView.setLayoutParams(spLp);

        buildToolbarButtons();
        if (prefs.isQuickActionsRowEnabled()) buildQuickActionsBar();
        if (prefs.isIphoneBottomBar()) buildIphoneBottomBar();

        loadCustomTypefaceLazily();
        keyboardView.setTheme(activeTheme);
        keyboardView.setCustomTypeface(cachedCustomTypeface);
        keyboardView.setLandscape(isLandscape);
        rebuildRows();
    }

    private void syncActiveLanguageFromPrefs() {
        List<String> enabled = prefs.getEnabledLanguages();
        String saved = prefs.getKeyboardLanguage();
        if (enabled.contains(saved)) {
            activeLangCode = saved;
        } else {
            activeLangCode = enabled.get(0);
            prefs.setKeyboardLanguage(activeLangCode);
        }
    }

    private void rebuildRows() {
        List<List<KeyboardLayoutData.KeyItem>> rows;
        if (isSymbols) {
            rows = KeyboardLayoutData.buildSymbolRows(isSymbolsPage2, activeLangCode);
        } else {
            rows = KeyboardLayoutData.buildRowsForLanguage(
                    activeLangCode,
                    prefs.isNumbersRowEnabled(),
                    prefs.isEmojiRowEnabled(),
                    isShifted,
                    prefs.getArabicLayoutType(),
                    prefs.getEnglishLayoutType(),
                    prefs.getCustomArabicLayoutJson());
        }
        keyboardView.setRows(rows);
    }

    private void buildToolbarButtons() {
        toolbarRow.removeAllViews();
        toolbarRow.setBackgroundColor(activeTheme.toolbarBgColor);

        List<String> pinned = prefs.getPinnedToolbarTools();
        for (String id : pinned) {
            addToolbarIcon(iconForTool(id), id,
                    "decorate".equals(id) && prefs.isDecorationEnabled() && !sensitiveFieldActive);
        }
        // Always include Settings (if not pinned) and More (...) button
        if (!pinned.contains("settings")) {
            addToolbarIcon("⚙️", "settings", false);
        }
        addToolbarIcon("⋯", "all_tools", activePanelView != null);
    }

    private void addToolbarIcon(String glyph, String toolId, boolean highlighted) {
        TextView tv = new TextView(this);
        tv.setText(glyph);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(highlighted ? activeTheme.accentColor : activeTheme.keyTextColor);
        if (highlighted) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.argb(55, Color.red(activeTheme.accentColor),
                    Color.green(activeTheme.accentColor), Color.blue(activeTheme.accentColor)));
            bg.setCornerRadius(dp(8));
            tv.setBackground(bg);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> {
            triggerActionFeedback("toolbar");
            openToolById(toolId);
        });
        toolbarRow.addView(tv);
    }

    private void buildQuickActionsBar() {
        quickActionsRow.removeAllViews();
        String[][] qa = {{"تحديد الكل", "select_all"}, {"نسخ", "copy"}, {"قص", "cut"}, {"لصق", "paste"}};
        for (String[] item : qa) {
            TextView tv = new TextView(this);
            tv.setText(item[0]);
            tv.setTextSize(11);
            tv.setTextColor(activeTheme.keyTextColor);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            tv.setLayoutParams(lp);
            tv.setOnClickListener(v -> performEditAction(item[1]));
            quickActionsRow.addView(tv);
        }
    }

    private void buildIphoneBottomBar() {
        iphoneBottomBar.removeAllViews();
        TextView globe = new TextView(this);
        globe.setText("🌐");
        globe.setTextSize(17);
        globe.setPadding(dp(8), 0, dp(8), 0);
        globe.setOnClickListener(v -> toggleLanguage());
        iphoneBottomBar.addView(globe);

        View spacer = new View(this);
        iphoneBottomBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView hide = new TextView(this);
        hide.setText("⌨️⬇️");
        hide.setTextSize(15);
        hide.setPadding(dp(8), 0, dp(8), 0);
        hide.setOnClickListener(v -> requestHideSelf(0));
        iphoneBottomBar.addView(hide);
    }

    private String iconForTool(String id) {
        switch (id) {
            case "ai": return "✨";
            case "clipboard": return "📋";
            case "emoji": return "😊";
            case "translate": return "🌐";
            case "resize": return "↕️";
            case "settings": return "⚙️";
            case "decorate": return "✦";
            case "text_edit": return "✏️";
            case "trackpad": return "🖱️";
            case "date_time": return "📅";
            case "calculator": return "🧮";
            case "quran": return "📖";
            case "azkar": return "📿";
            case "secret": return "🔐";
            case "stickers": return "🎨";
            case "one_handed": return "🤏";
            case "incognito": return "🕵️";
            default: return "⚡";
        }
    }

    // ---- ProKeyboardView.OnKeyboardActionListener ----

    @Override
    public void onText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || text == null) return;

        // Inline Math trigger on '='
        if ("=".equals(text) && prefs.isInlineMathEnabled() && currentWord.length() >= 3) {
            String mathRes = SmartWordEngine.tryEvaluateMath(currentWord.toString());
            if (mathRes != null) {
                ic.deleteSurroundingText(currentWord.length(), 0);
                ic.commitText(mathRes, 1);
                currentWord.setLength(0);
                refreshSuggestions();
                return;
            }
        }

        // Smart Punctuation spacing
        if (prefs.isSmartPunctuationEnabled() && (".".equals(text) || "،".equals(text) || ",".equals(text) || "؟".equals(text) || "!".equals(text))) {
            finalizeWordBeforeDelimiter(ic);
            ic.commitText(text + " ", 1);
            refreshSuggestions();
            return;
        }

        boolean canDecorate = prefs.isDecorationEnabled()
                && !sensitiveFieldActive
                && !bankAppExcluded
                && !TextStyleEngine.STYLE_NORMAL.equals(prefs.getActiveStyleId());

        String out = canDecorate
                ? styleEngine.transformTypedChar(text, prefs.getActiveStyleId(), true)
                : text;

        ic.commitText(out, 1);
        if (!text.trim().isEmpty()) {
            currentWord.append(text);
        }
        if (isShifted && !"ar".equals(activeLangCode)) {
            isShifted = false;
            rebuildRows();
        }
        refreshSuggestions();
    }

    @Override
    public void onKeyCode(int code) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (code) {
            case KeyboardLayoutData.CODE_BACKSPACE:
                deleteGraphemeBeforeCursor(ic);
                refreshSuggestions();
                break;

            case KeyboardLayoutData.CODE_SPACE:
                finalizeWordBeforeDelimiter(ic);
                ic.commitText(" ", 1);
                refreshSuggestions();
                break;

            case KeyboardLayoutData.CODE_ENTER:
                finalizeWordBeforeDelimiter(ic);
                EditorInfo ei = getCurrentInputEditorInfo();
                int action = ei != null ? (ei.imeOptions & EditorInfo.IME_MASK_ACTION) : EditorInfo.IME_ACTION_NONE;
                if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_SEND
                        || action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    ic.performEditorAction(action);
                } else {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                }
                refreshSuggestions();
                break;

            case KeyboardLayoutData.CODE_SHIFT:
                isShifted = !isShifted;
                rebuildRows();
                break;

            case KeyboardLayoutData.CODE_MODE_SYMBOLS:
                isSymbols = true;
                isSymbolsPage2 = false;
                rebuildRows();
                break;

            case KeyboardLayoutData.CODE_MODE_ALPHA:
                isSymbols = false;
                rebuildRows();
                break;

            case KeyboardLayoutData.CODE_SYMBOLS_PAGE2:
                isSymbolsPage2 = !isSymbolsPage2;
                rebuildRows();
                break;

            case KeyboardLayoutData.CODE_LANG_SWITCH:
                toggleLanguage();
                break;

            case KeyboardLayoutData.CODE_EMOJI:
                openToolById("emoji");
                break;
        }
    }

    @Override
    public void onLongPressKey(KeyboardLayoutData.KeyItem key, RectF keyBounds) {
        if (key.code == KeyboardLayoutData.CODE_ENTER && prefs.isEnterLongPressEmoji()) {
            openToolById("emoji");
            return;
        }
        if (key.popupChars == null || key.popupChars.length == 0) return;

        // Letter-first option puts base character first
        showPopupChars(key.popupChars, keyBounds);
    }

    @Override
    public void onCursorMove(int deltaSteps) {
        moveCursor(deltaSteps);
    }

    @Override
    public void onSwipeDeleteWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence before = ic.getTextBeforeCursor(40, 0);
        if (before == null || before.length() == 0) return;
        int len = before.length();
        int i = len - 1;
        while (i > 0 && Character.isWhitespace(before.charAt(i))) i--;
        while (i > 0 && !Character.isWhitespace(before.charAt(i - 1))) i--;
        ic.deleteSurroundingText(len - i, 0);
        currentWord.setLength(0);
        refreshSuggestions();
    }

    @Override
    public void onSwipeTypingResult(List<String> touchedLabels) {
        String word = wordEngine.resolveSwipePath(touchedLabels, "ar".equals(activeLangCode));
        if (word != null) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(word + " ", 1);
                previousWord = word;
                currentWord.setLength(0);
                refreshSuggestions();
            }
        }
    }

    @Override
    public void onTwoFingerSwipe(String direction) {
        String action = prefs.getTwoFingerSwipeAction(direction);
        if ("hide".equals(action)) {
            requestHideSelf(0);
        } else if (!"none".equals(action)) {
            performEditAction(action);
        }
    }

    @Override
    public void onSpacebarLanguageSwipe(int directionDelta) {
        switchLanguageByDelta(directionDelta);
    }

    @Override
    public void onKeyTouched(String matrixAction) {
        triggerActionFeedback(matrixAction);
    }

    // ---- ImePanelsController.Host ----

    @Override
    public void commitRaw(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && text != null) {
            ic.commitText(text, 1);
        }
    }

    @Override
    public void deleteEmojiOrChar(boolean isRepeat) {
        triggerActionFeedback(isRepeat ? "delete_repeat" : "delete");
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            deleteGraphemeBeforeCursor(ic);
            refreshSuggestions();
        }
    }

    @Override
    public void sendKeyEventCode(int androidKeyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, androidKeyCode));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, androidKeyCode));
        }
    }

    @Override
    public void moveCursor(int steps) {
        if (steps == 0) return;
        int key = steps > 0 ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
        int count = Math.min(Math.abs(steps), 8);
        for (int i = 0; i < count; i++) sendKeyEventCode(key);
    }

    @Override
    public void performEditAction(String action) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        switch (action) {
            case "select_all": ic.performContextMenuAction(android.R.id.selectAll); break;
            case "copy": ic.performContextMenuAction(android.R.id.copy); break;
            case "cut": ic.performContextMenuAction(android.R.id.cut); break;
            case "paste": ic.performContextMenuAction(android.R.id.paste); break;
            case "left": sendKeyEventCode(KeyEvent.KEYCODE_DPAD_LEFT); break;
            case "right": sendKeyEventCode(KeyEvent.KEYCODE_DPAD_RIGHT); break;
            case "up": sendKeyEventCode(KeyEvent.KEYCODE_DPAD_UP); break;
            case "down": sendKeyEventCode(KeyEvent.KEYCODE_DPAD_DOWN); break;
            case "select":
                selectModeActive = !selectModeActive;
                showToast(selectModeActive ? "وضع التحديد مفعّل" : "تم إيقاف التحديد");
                break;
        }
    }

    @Override
    public void openToolById(String toolId) {
        if (toolId == null) return;
        switch (toolId) {
            case "all_tools": showPanel(panelsController.buildAllToolsPanel()); break;
            case "emoji": showPanel(panelsController.buildEmojiPanel()); break;
            case "clipboard": showPanel(panelsController.buildClipboardPanel()); break;
            case "decorate": showPanel(panelsController.buildDecorationPanel()); break;
            case "ai": showPanel(panelsController.buildAiPanel()); break;
            case "translate":
                AiKeyboardHelper.AiAction tr = new AiKeyboardHelper.AiAction("tr", "ترجمة",
                        "Translate the following text into " + prefs.getTranslationTargetLang() + " and return only the translation:");
                runAiAction(tr);
                break;
            case "text_edit": showPanel(panelsController.buildTextEditPanel()); break;
            case "trackpad": showPanel(panelsController.buildTrackpadPanel()); break;
            case "calculator": showPanel(panelsController.buildCalculatorPanel()); break;
            case "date_time": showPanel(panelsController.buildDateTimePanel()); break;
            case "quran": showPanel(panelsController.buildQuranOrAzkarPanel(true)); break;
            case "azkar": showPanel(panelsController.buildQuranOrAzkarPanel(false)); break;
            case "resize": showPanel(panelsController.buildResizePanel()); break;
            case "stickers": showPanel(panelsController.buildStickersPanel()); break;
            case "secret":
                toggleSecretMessageOnField();
                break;
            case "one_handed":
                prefs.setOneHandedModeEnabled(!prefs.isOneHandedModeEnabled());
                closePanel();
                applyThemeAndLayout();
                break;
            case "split":
                prefs.setSplitModeEnabled(!prefs.isSplitModeEnabled());
                closePanel();
                applyThemeAndLayout();
                break;
            case "floating":
                prefs.setFloatingModeEnabled(!prefs.isFloatingModeEnabled());
                closePanel();
                applyThemeAndLayout();
                break;
            case "incognito":
                prefs.setIncognitoMode(!prefs.isIncognitoMode());
                showToast(prefs.isIncognitoMode() ? "تم تفعيل الوضع المتخفي" : "تم إيقاف الوضع المتخفي");
                closePanel();
                break;
            case "mute":
                boolean nextSound = !prefs.isKeySoundEnabled();
                prefs.setKeySoundEnabled(nextSound);
                showToast(nextSound ? "تم تشغيل صوت المفاتيح" : "تم كتم الصوت");
                closePanel();
                break;
            case "emoji_row":
                prefs.setEmojiRowEnabled(!prefs.isEmojiRowEnabled());
                closePanel();
                applyThemeAndLayout();
                break;
            case "iphone_bar":
                prefs.setIphoneBottomBar(!prefs.isIphoneBottomBar());
                closePanel();
                applyThemeAndLayout();
                break;
            case "quick_actions":
                prefs.setQuickActionsRowEnabled(!prefs.isQuickActionsRowEnabled());
                closePanel();
                applyThemeAndLayout();
                break;
            case "quick_paste":
                List<ClipboardRepository.ClipItem> top = clipRepo.getClips(1, true, false);
                if (!top.isEmpty()) commitRaw(top.get(0).text);
                closePanel();
                break;
            case "voice":
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
                break;
            case "tts":
                speakCurrentFieldText();
                break;
            case "hide_kb":
                requestHideSelf(0);
                break;
            case "settings": {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                break;
            }
        }
    }

    @Override
    public void refreshToolbarAndKeyboard() {
        applyThemeAndLayout();
    }

    @Override
    public void closePanel() {
        if (panelsController != null) panelsController.cancelPendingRepeats();
        if (activePanelView != null && bodyContainer != null) {
            bodyContainer.removeView(activePanelView);
            activePanelView = null;
        }
        if (keyboardView != null) keyboardView.setVisibility(View.VISIBLE);
    }

    @Override
    public void runAiAction(AiKeyboardHelper.AiAction action) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence selected = ic.getSelectedText(0);
        final boolean hasSelection = selected != null && selected.length() > 0;
        CharSequence source = hasSelection ? selected : ic.getTextBeforeCursor(500, 0);
        String text = source == null ? "" : source.toString().trim();
        if (text.isEmpty()) {
            showToast("اكتب نصاً أولاً لتطبيق «" + action.label + "»");
            return;
        }
        showToast("جاري المعالجة عبر Gemini...");
        AiKeyboardHelper.runAction(prefs, action, text, new AiKeyboardHelper.AiCallback() {
            @Override
            public void onResult(String resultText) {
                InputConnection curIc = getCurrentInputConnection();
                if (curIc == null) return;
                if (hasSelection) {
                    curIc.commitText(resultText, 1);
                } else {
                    curIc.deleteSurroundingText(text.length(), 0);
                    curIc.commitText(resultText, 1);
                }
                closePanel();
            }

            @Override
            public void onError(String errorMessage) {
                showToast(errorMessage);
            }
        });
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public KeyboardTheme getActiveKeyboardTheme() {
        return activeTheme != null ? activeTheme : ThemeRegistry.getBuiltInThemes().get(0);
    }

    @Override
    public String getActiveLanguageCode() {
        return activeLangCode;
    }

    @Override
    public boolean isLandscape() {
        return isLandscape;
    }

    /**
     * Deletes one complete user-perceived character or multi-codepoint Unicode emoji cluster
     * (handles surrogate pairs, skin-tone modifiers, flags, variation selectors, and ZWJ sequences)
     * so broken half-emojis are never left behind.
     */
    private void deleteGraphemeBeforeCursor(InputConnection ic) {
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) {
            ic.commitText("", 1);
            currentWord.setLength(0);
            return;
        }

        CharSequence before = ic.getTextBeforeCursor(32, 0);
        if (before == null || before.length() == 0) {
            ic.deleteSurroundingText(1, 0);
            return;
        }

        String text = before.toString();
        int deleteLen = 1;
        try {
            BreakIterator it = BreakIterator.getCharacterInstance(Locale.getDefault());
            it.setText(text);
            int end = it.last();
            int prev = it.previous();
            if (prev != BreakIterator.DONE && end > prev) {
                deleteLen = end - prev;
            }
        } catch (Exception ignored) {
            // Surrogate pair fallback
            int len = text.length();
            if (len >= 2 && Character.isLowSurrogate(text.charAt(len - 1))
                    && Character.isHighSurrogate(text.charAt(len - 2))) {
                deleteLen = 2;
            }
        }

        // Guard against splitting a surrogate pair
        int startIdx = text.length() - deleteLen;
        if (startIdx > 0 && startIdx < text.length()
                && Character.isLowSurrogate(text.charAt(startIdx))
                && Character.isHighSurrogate(text.charAt(startIdx - 1))) {
            deleteLen++;
        }

        ic.deleteSurroundingText(deleteLen, 0);
        if (currentWord.length() > 0) {
            int trim = Math.min(deleteLen, currentWord.length());
            currentWord.delete(currentWord.length() - trim, currentWord.length());
        }
    }

    // ---- Internal Helpers ----

    private void showPanel(View panel) {
        closePanel();
        activePanelView = panel;
        if (keyboardView != null) keyboardView.setVisibility(View.GONE);
        if (bodyContainer != null) {
            bodyContainer.addView(panel, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private void finalizeWordBeforeDelimiter(InputConnection ic) {
        if (currentWord.length() == 0) return;
        String raw = currentWord.toString();

        // Framed decorative word wrapping
        if (prefs.isDecorationEnabled() && !sensitiveFieldActive && !bankAppExcluded
                && TextStyleEngine.isFramedStyle(prefs.getActiveStyleId())) {
            String wrapped = styleEngine.wrapFramedWord(raw, prefs.getActiveStyleId());
            ic.deleteSurroundingText(raw.length(), 0);
            ic.commitText(wrapped, 1);
        } else if (!sensitiveFieldActive && !bankAppExcluded) {
            String correction = wordEngine.getAutoCommitCorrection(raw, prefs);
            if (correction != null && !correction.equals(raw)) {
                ic.deleteSurroundingText(raw.length(), 0);
                ic.commitText(correction, 1);
                triggerActionFeedback("autocorrect");
                raw = correction;
            }
            wordEngine.learnWord(raw, prefs.isIncognitoMode());
        }
        previousWord = raw;
        currentWord.setLength(0);
    }

    private void refreshSuggestions() {
        if (suggestionsContainer == null || !prefs.isSuggestionsEnabled() || sensitiveFieldActive) return;
        suggestionsContainer.removeAllViews();
        List<String> list = wordEngine.getSuggestions(currentWord.toString(), previousWord,
                "ar".equals(activeLangCode), prefs);
        for (String sug : list) {
            TextView tv = new TextView(this);
            tv.setText(sug);
            tv.setTextColor(activeTheme.keyTextColor);
            tv.setTextSize(13);
            tv.setPadding(dp(12), dp(5), dp(12), dp(5));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(activeTheme.keyBgColor);
            bg.setCornerRadius(dp(14));
            tv.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(6));
            tv.setLayoutParams(lp);
            tv.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    if (currentWord.length() > 0) ic.deleteSurroundingText(currentWord.length(), 0);
                    ic.commitText(sug + " ", 1);
                    previousWord = sug;
                    currentWord.setLength(0);
                    refreshSuggestions();
                }
            });
            suggestionsContainer.addView(tv);
        }
    }

    private void showPopupChars(String[] chars, RectF keyBounds) {
        dismissPopup();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(activeTheme.toolbarBgColor);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), activeTheme.accentColor);
        row.setBackground(bg);

        for (String ch : chars) {
            TextView tv = new TextView(this);
            tv.setText(ch);
            tv.setTextColor(activeTheme.keyTextColor);
            tv.setTextSize(16);
            tv.setPadding(dp(10), dp(6), dp(10), dp(6));
            tv.setOnClickListener(v -> {
                if ("التاريخ".equals(ch)) commitRaw(SmartWordEngine.getGregorianDate());
                else if ("الهجري".equals(ch)) commitRaw(SmartWordEngine.getHijriDate());
                else if ("الوقت".equals(ch)) commitRaw(SmartWordEngine.getCurrentTime());
                else onText(ch);
                dismissPopup();
            });
            row.addView(tv);
        }

        popupWindow = new PopupWindow(row, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popupWindow.setOutsideTouchable(true);
        if (keyboardView != null) {
            int x = (int) Math.max(dp(8), keyBounds.left - dp(24));
            int y = (int) Math.max(0, keyBounds.top - dp(48));
            popupWindow.showAtLocation(keyboardView, Gravity.TOP | Gravity.START, x, y);
        }
    }

    private void dismissPopup() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        popupWindow = null;
    }

    private void toggleLanguage() {
        switchLanguageByDelta(1);
    }

    private void switchLanguageByDelta(int delta) {
        List<String> enabled = prefs.getEnabledLanguages();
        if (enabled.size() <= 1) return;
        int curIdx = enabled.indexOf(activeLangCode);
        if (curIdx < 0) curIdx = 0;
        int nextIdx = (curIdx + delta) % enabled.size();
        if (nextIdx < 0) nextIdx += enabled.size();

        activeLangCode = enabled.get(nextIdx);
        isSymbols = false;
        isShifted = false;
        prefs.setKeyboardLanguage(activeLangCode);
        currentWord.setLength(0);
        rebuildRows();
        refreshSuggestions();
    }

    private void toggleSecretMessageOnField() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence before = ic.getTextBeforeCursor(250, 0);
        if (before == null || before.length() == 0) {
            showToast("اكتب رسالة أولاً لتشفيرها أو فك تشفيرها");
            return;
        }
        String src = before.toString();
        String toggled = SmartWordEngine.toggleSecretCipher(src);
        ic.deleteSurroundingText(src.length(), 0);
        ic.commitText(toggled, 1);
        closePanel();
    }

    private void speakCurrentFieldText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence text = ic.getSelectedText(0);
        if (text == null || text.length() == 0) text = ic.getTextBeforeCursor(300, 0);
        final String toSpeak = text == null ? "" : text.toString().trim();
        if (toSpeak.isEmpty()) {
            showToast("لا يوجد نص لقراءته");
            return;
        }
        if (tts == null) {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    tts.setLanguage("ar".equals(activeLangCode) ? new Locale("ar") : Locale.US);
                    tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "kb_tts");
                }
            });
        } else {
            tts.setLanguage("ar".equals(activeLangCode) ? new Locale("ar") : Locale.US);
            tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "kb_tts");
        }
        closePanel();
    }

    private void triggerActionFeedback(String matrixAction) {
        if (prefs.isVibrationEnabled() && prefs.isActionVibeEnabled(matrixAction) && vibrator != null) {
            int ms = prefs.getVibrationStrengthMs();
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(ms);
                }
            } catch (Exception ignored) {
            }
        }
        if (prefs.isKeySoundEnabled() && prefs.isActionSoundEnabled(matrixAction)
                && !KeyboardPrefs.SOUND_PACK_SILENT.equals(prefs.getSoundPack()) && audioManager != null) {
            float vol = prefs.getSoundVolumePct() / 100f;
            int fx = "delete".equals(matrixAction) || "delete_repeat".equals(matrixAction)
                    ? AudioManager.FX_KEYPRESS_DELETE
                    : ("space".equals(matrixAction) ? AudioManager.FX_KEYPRESS_SPACEBAR
                    : ("enter".equals(matrixAction) ? AudioManager.FX_KEYPRESS_RETURN
                    : AudioManager.FX_KEYPRESS_STANDARD));
            audioManager.playSoundEffect(fx, vol);
        }
    }

    private void loadCustomTypefaceLazily() {
        String path = prefs.getCustomUiFontPath();
        if (path.equals(cachedFontPath)) return;
        cachedFontPath = path;
        if (path.isEmpty()) {
            cachedCustomTypeface = null;
            return;
        }
        try {
            File f = new File(path);
            cachedCustomTypeface = f.exists() ? Typeface.createFromFile(f) : null;
        } catch (Exception e) {
            cachedCustomTypeface = null;
        }
    }

    private int resolveKeyboardHeightPx() {
        int heightDp = isLandscape ? prefs.getLandscapeHeightDp() : prefs.getPortraitHeightDp();
        return dp(heightDp);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
