package com.keyboardpro.ime;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.keyboardpro.data.ClipboardRepository;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.data.StickerRepository;

import java.util.List;

/**
 * Lazily constructs all in-keyboard expandable panels on demand (zero upfront RAM cost)
 * and releases them immediately when closed or inside onFinishInputView() / onTrimMemory().
 * Includes:
 * - "All Tools" Drawer (26 tools, long-press any tool to pin/unpin to top toolbar!)
 * - Emoji Panel with 3x/5x/7x repeat strip
 * - Clipboard Panel (3-day expiry, pin, delete, sort)
 * - Live Decoration Panel (26 Arabic + 30 English styles)
 * - AI Keyboard Panel (15 Gemini actions + custom tools)
 * - Inline Text Editing Pad (Arrows, Select, Copy, Cut, Paste, Select All)
 * - Trackpad Mode (touchpad cursor navigation)
 * - Inline Calculator
 * - Quranic Verse Search & Azkar Panel
 * - Quick Stickers & Resize Panel
 */
public class ImePanelsController {

    public interface Host {
        void commitRaw(String text);
        void deleteEmojiOrChar(boolean isRepeat);
        void sendKeyEventCode(int androidKeyCode);
        void moveCursor(int steps);
        void performEditAction(String action);
        void openToolById(String toolId);
        void refreshToolbarAndKeyboard();
        void closePanel();
        void runAiAction(AiKeyboardHelper.AiAction action);
        void showToast(String msg);
        KeyboardTheme getActiveKeyboardTheme();
        String getActiveLanguageCode();
        boolean isLandscape();
    }

    public static final String[][] ALL_TOOLS = {
            {"text_edit", "✏️", "تحرير النص"},
            {"trackpad", "🖱️", "التراك باد"},
            {"ai", "✨", "الذكاء AI"},
            {"clipboard", "📋", "الحافظة"},
            {"emoji", "😊", "الإيموجي"},
            {"decorate", "✦", "الزخرفة"},
            {"translate", "🌐", "الترجمة"},
            {"date_time", "📅", "التاريخ والوقت"},
            {"calculator", "🧮", "الحاسبة"},
            {"quran", "📖", "آيات قرآنية"},
            {"azkar", "📿", "الأذكار"},
            {"secret", "🔐", "رسالة سرية"},
            {"one_handed", "🤏", "يد واحدة"},
            {"split", "⬌", "تقسيم"},
            {"floating", "🪟", "عائم"},
            {"resize", "↕️", "تغيير الحجم"},
            {"stickers", "🎨", "الملصقات"},
            {"voice", "🎙️", "إدخال صوتي"},
            {"tts", "🔊", "قراءة النص"},
            {"incognito", "🕵️", "وضع متخفي"},
            {"quick_paste", "📌", "لصق سريع"},
            {"mute", "🔇", "كتم الصوت"},
            {"emoji_row", "😀", "صف الإيموجي"},
            {"iphone_bar", "📱", "شريط آيفون"},
            {"quick_actions", "⚡", "شريط سريع"},
            {"hide_kb", "⬇️", "إخفاء"}
    };

    private static final String[] EMOJI_CAT_ICONS = {"😀", "❤️", "👍", "🔥", "🌙"};
    private static final String[][] EMOJI_CATEGORIES = {
            {
                    "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","😉","😊","😇","🥰","😍",
                    "🤩","😘","😗","😚","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔",
                    "🫡","🤐","🤨","😐","😑","😶","😏","😒","🙄","😬","😌","😔","😪","😴",
                    "😷","🤒","🤕","🤢","🤮","🥵","🥶","😵","🤯","🤠","🥳","😎","🤓","🧐",
                    "😕","😟","🙁","😮","😯","😲","😳","🥺","🥹","😦","😢","😭","😤","😠"
            },
            {
                    "❤️","🧡","💛","💚","💙","💜","🤎","🖤","🤍","🩷","🩵","🩶","💔","❣️",
                    "💕","💞","💓","💗","💖","💘","💝","💟","❤️‍🔥","❤️‍🩹","🫶","💋","💯","💢",
                    "💥","💫","💦","💨","💬","💭","🗯️","💤","💌","💐","🌹","🥀","🌺","🌸",
                    "🌷","🌻","🌼","🦋","✨","⭐","🌟","🌠","💎","👑","💍","🎁","🎀","🎈"
            },
            {
                    "👍","👎","👌","🤌","🤏","✌️","🤞","🫰","🤟","🤘","🤙","👈","👉","👆",
                    "👇","☝️","✋","🤚","🖐️","🖖","👋","🤚","💪","🙏","🤝","👏","🙌","👐",
                    "🤲","🫶","✍️","💅","🤳","🙋","🙆","🙅","🤷","🤦","👨‍👩‍👧‍👦","👨‍💻","👩‍💻","🧑‍🎓"
            },
            {
                    "🔥","✨","⚡","🚀","🎯","🎉","🎊","🏆","🥇","🥈","🥉","⚽","🏀","🎮",
                    "🎧","🎵","🎶","📸","🎬","📱","💻","⌨️","💡","🔑","🔒","🔓","📌","📍",
                    "✅","❌","⚠️","❓","❗","♻️","🔔","📣","💬","📢","🕒","📅","💰","💳"
            },
            {
                    "🌙","🕌","🕋","📿","🤲","☀️","🌤️","⛅","🌧️","⛈️","🌈","❄️","🌊","🌍",
                    "🇸🇦","🇪🇬","🇦🇪","🇰🇼","🇶🇦","🇯🇴","🇲🇦","🇩🇿","🇹🇳","🇵🇸","🇱🇧","🇮🇶","🇴🇲","🇧🇭",
                    "☕","🍵","🥤","🍕","🍔","🍟","🍰","🍫","🍎","🍓","🍉","🍇","🌴","🌵"
            }
    };

    private final Context ctx;
    private final KeyboardPrefs prefs;
    private final ClipboardRepository clipRepo;
    private final Host host;
    private final Handler repeatHandler = new Handler(Looper.getMainLooper());
    private boolean isEmojiDeleteHeld = false;

    private final Runnable emojiDeleteRepeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (isEmojiDeleteHeld) {
                host.deleteEmojiOrChar(true);
                repeatHandler.postDelayed(this, 75);
            }
        }
    };

    private String lastTappedEmoji = "❤️";
    private int selectedEmojiCategory = 0;
    private String calcExpr = "";

    public ImePanelsController(Context ctx, KeyboardPrefs prefs, ClipboardRepository clipRepo, Host host) {
        this.ctx = ctx;
        this.prefs = prefs;
        this.clipRepo = clipRepo;
        this.host = host;
    }

    public void cancelPendingRepeats() {
        isEmojiDeleteHeld = false;
        repeatHandler.removeCallbacks(emojiDeleteRepeatRunnable);
    }

    public View buildAllToolsPanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "جميع الأدوات (اضغط مطولاً للتثبيت في الشريط)", theme);

        ScrollView scroll = new ScrollView(ctx);
        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(host.isLandscape() ? 6 : 4);
        grid.setPadding(dp(6), dp(4), dp(6), dp(8));

        List<String> pinned = prefs.getPinnedToolbarTools();
        for (String[] tool : ALL_TOOLS) {
            final String id = tool[0];
            boolean isPinned = pinned.contains(id);

            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(4), dp(8), dp(4), dp(8));
            cell.setBackground(pillBg(isPinned ? withAlpha(theme.accentColor, 65) : theme.keyBgColor,
                    isPinned ? theme.accentColor : theme.keyBorderColor, 12));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            cell.setLayoutParams(lp);

            TextView ico = new TextView(ctx);
            ico.setText(tool[1]);
            ico.setTextSize(18);
            ico.setGravity(Gravity.CENTER);
            cell.addView(ico);

            TextView lbl = new TextView(ctx);
            lbl.setText(tool[2] + (isPinned ? " ★" : ""));
            lbl.setTextColor(theme.keyTextColor);
            lbl.setTextSize(10);
            lbl.setGravity(Gravity.CENTER);
            lbl.setMaxLines(1);
            cell.addView(lbl);

            cell.setOnClickListener(v -> host.openToolById(id));
            cell.setOnLongClickListener(v -> {
                prefs.togglePinnedToolbarTool(id);
                host.refreshToolbarAndKeyboard();
                host.showToast(prefs.getPinnedToolbarTools().contains(id)
                        ? "تم تثبيت «" + tool[2] + "» في الشريط العلوي"
                        : "تمت إزالة «" + tool[2] + "» من الشريط العلوي");
                host.openToolById("all_tools");
                return true;
            });
            grid.addView(cell);
        }
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    public View buildEmojiPanel() {
        cancelPendingRepeats();
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);

        // Quick repeat strip (3x / 5x / 7x)
        LinearLayout repeatBar = new LinearLayout(ctx);
        repeatBar.setOrientation(LinearLayout.HORIZONTAL);
        repeatBar.setGravity(Gravity.CENTER_VERTICAL);
        repeatBar.setPadding(dp(8), dp(4), dp(8), dp(4));
        repeatBar.setBackgroundColor(theme.toolbarBgColor);

        final TextView[] repeatChips = new TextView[3];
        final int[] counts = {3, 5, 7};
        for (int i = 0; i < counts.length; i++) {
            final int n = counts[i];
            TextView rep = createChip(repeatEmoji(lastTappedEmoji, n), theme, i == 0);
            rep.setOnClickListener(v -> host.commitRaw(repeatEmoji(lastTappedEmoji, n)));
            repeatChips[i] = rep;
            repeatBar.addView(rep);
        }
        if (prefs.isEmojiRepeatSuggestions()) {
            root.addView(repeatBar);
        }

        // Scrollable Emoji Grid (weight=1 so bottom bar stays pinned!)
        ScrollView scroll = new ScrollView(ctx);
        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(host.isLandscape() ? 10 : 7);
        grid.setPadding(dp(6), dp(4), dp(6), dp(6));
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Fixed Bottom Bar: [ABC / أ ب]  [Emoji Category Tabs]  [Persistent Backspace ⌫]
        LinearLayout bottomBar = new LinearLayout(ctx);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setBackgroundColor(theme.toolbarBgColor);
        bottomBar.setPadding(dp(6), dp(4), dp(6), dp(4));
        bottomBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        String langCode = host.getActiveLanguageCode();
        String backLabel = "ar".equals(langCode) ? "أ ب" : ("ru".equals(langCode) ? "АБВ" : "ABC");
        TextView btnAbc = new TextView(ctx);
        btnAbc.setText(backLabel);
        btnAbc.setTextColor(theme.keySpecialTextColor);
        btnAbc.setTextSize(13);
        btnAbc.setTypeface(null, Typeface.BOLD);
        btnAbc.setGravity(Gravity.CENTER);
        btnAbc.setBackground(pillBg(theme.keySpecialBgColor, theme.keyBorderColor, 10));
        LinearLayout.LayoutParams abcLp = new LinearLayout.LayoutParams(dp(56), dp(38));
        abcLp.setMargins(dp(2), 0, dp(4), 0);
        btnAbc.setLayoutParams(abcLp);
        btnAbc.setOnClickListener(v -> {
            cancelPendingRepeats();
            host.closePanel();
        });
        bottomBar.addView(btnAbc);

        // Category Tabs
        final TextView[] catViews = new TextView[EMOJI_CAT_ICONS.length];
        Runnable populateGrid = () -> {
            grid.removeAllViews();
            int catIdx = Math.max(0, Math.min(EMOJI_CATEGORIES.length - 1, selectedEmojiCategory));
            for (int c = 0; c < catViews.length; c++) {
                if (catViews[c] != null) {
                    boolean active = (c == catIdx);
                    catViews[c].setBackground(active
                            ? pillBg(withAlpha(theme.accentColor, 65), theme.accentColor, 10)
                            : null);
                }
            }
            for (String em : EMOJI_CATEGORIES[catIdx]) {
                TextView tv = new TextView(ctx);
                tv.setText(em);
                tv.setTextSize(22);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(dp(4), dp(6), dp(4), dp(6));
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = 0;
                lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                tv.setLayoutParams(lp);
                tv.setOnClickListener(v -> {
                    lastTappedEmoji = em;
                    for (int i = 0; i < counts.length; i++) {
                        repeatChips[i].setText(repeatEmoji(em, counts[i]));
                    }
                    host.commitRaw(em);
                });
                grid.addView(tv);
            }
            scroll.scrollTo(0, 0);
        };

        for (int i = 0; i < EMOJI_CAT_ICONS.length; i++) {
            final int idx = i;
            TextView tab = new TextView(ctx);
            tab.setText(EMOJI_CAT_ICONS[i]);
            tab.setTextSize(17);
            tab.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(38), 1f);
            tlp.setMargins(dp(2), 0, dp(2), 0);
            tab.setLayoutParams(tlp);
            tab.setOnClickListener(v -> {
                selectedEmojiCategory = idx;
                populateGrid.run();
            });
            catViews[i] = tab;
            bottomBar.addView(tab);
        }

        // Persistent Backspace / Delete (⌫) Button with Tap + Long-Press Continuous Repeat Delete
        TextView btnBackspace = new TextView(ctx);
        btnBackspace.setText("⌫");
        btnBackspace.setTextColor(theme.keySpecialTextColor);
        btnBackspace.setTextSize(18);
        btnBackspace.setTypeface(null, Typeface.BOLD);
        btnBackspace.setGravity(Gravity.CENTER);
        btnBackspace.setBackground(pillBg(theme.keySpecialBgColor, theme.keyBorderColor, 10));
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(56), dp(38));
        delLp.setMargins(dp(4), 0, dp(2), 0);
        btnBackspace.setLayoutParams(delLp);

        btnBackspace.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    btnBackspace.setBackground(pillBg(theme.keyPressedColor, theme.accentColor, 10));
                    isEmojiDeleteHeld = true;
                    host.deleteEmojiOrChar(false);
                    repeatHandler.removeCallbacks(emojiDeleteRepeatRunnable);
                    repeatHandler.postDelayed(emojiDeleteRepeatRunnable, 340);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    btnBackspace.setBackground(pillBg(theme.keySpecialBgColor, theme.keyBorderColor, 10));
                    cancelPendingRepeats();
                    return true;
                default:
                    return false;
            }
        });
        bottomBar.addView(btnBackspace);

        root.addView(bottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        populateGrid.run();
        return root;
    }

    public View buildClipboardPanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "الحافظة الذكية (تثبيت وحذف فوري)", theme);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(4), dp(8), dp(8));

        List<ClipboardRepository.ClipItem> clips = clipRepo.getClips(
                24, prefs.isClipboardSortNewest(), prefs.isClipboardAutoDelete3Days());
        if (clips.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("الحافظة فارغة — انسخ أي نص وسيظهر هنا فوراً");
            empty.setTextColor(theme.keyTextColor);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(24), dp(12), dp(24));
            list.addView(empty);
        } else {
            for (ClipboardRepository.ClipItem item : clips) {
                LinearLayout row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(10), dp(8), dp(10), dp(8));
                row.setBackground(pillBg(theme.keyBgColor, item.pinned ? theme.accentColor : theme.keyBorderColor, 10));
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = dp(6);
                row.setLayoutParams(rlp);

                TextView txt = new TextView(ctx);
                txt.setText((item.pinned ? "📌 " : "") + item.text);
                txt.setTextColor(theme.keyTextColor);
                txt.setTextSize(13);
                txt.setMaxLines(2);
                txt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                txt.setOnClickListener(v -> host.commitRaw(item.text));
                row.addView(txt);

                TextView pinBtn = createChip(item.pinned ? "إلغاء" : "تثبيت", theme, item.pinned);
                pinBtn.setOnClickListener(v -> {
                    clipRepo.togglePin(item.id);
                    host.openToolById("clipboard");
                });
                row.addView(pinBtn);

                TextView delBtn = createChip("🗑", theme, false);
                delBtn.setOnClickListener(v -> {
                    clipRepo.deleteClip(item.id);
                    host.openToolById("clipboard");
                });
                row.addView(delBtn);

                list.addView(row);
            }
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    public View buildDecorationPanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "الزخرفة الفورية (26 عربي + 30 إنجليزي)", theme);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(4), dp(8), dp(8));

        String activeId = prefs.getActiveStyleId();
        for (TextStyleEngine.StyleOption opt : TextStyleEngine.getAllStyles()) {
            boolean selected = opt.id.equals(activeId) && prefs.isDecorationEnabled();
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));
            row.setBackground(pillBg(selected ? withAlpha(theme.accentColor, 85) : theme.keyBgColor,
                    selected ? theme.accentColor : theme.keyBorderColor, 10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(5);
            row.setLayoutParams(lp);

            TextView name = new TextView(ctx);
            name.setText(opt.displayName);
            name.setTextColor(theme.keyTextColor);
            name.setTextSize(12);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(name);

            TextView preview = new TextView(ctx);
            preview.setText(opt.samplePreview);
            preview.setTextColor(theme.accentColor);
            preview.setTextSize(12);
            row.addView(preview);

            row.setOnClickListener(v -> {
                if (TextStyleEngine.STYLE_NORMAL.equals(opt.id)) {
                    prefs.setDecorationEnabled(false);
                    prefs.setActiveStyleId(TextStyleEngine.STYLE_NORMAL);
                } else {
                    prefs.setDecorationEnabled(true);
                    prefs.setActiveStyleId(opt.id);
                }
                host.refreshToolbarAndKeyboard();
                host.closePanel();
            });
            list.addView(row);
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    public View buildAiPanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "كيبورد الذكاء الاصطناعي (Gemini BYOK)", theme);

        ScrollView scroll = new ScrollView(ctx);
        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(host.isLandscape() ? 4 : 3);
        grid.setPadding(dp(6), dp(4), dp(6), dp(8));

        for (AiKeyboardHelper.AiAction action : AiKeyboardHelper.getAllActions(prefs)) {
            TextView btn = new TextView(ctx);
            btn.setText(action.label);
            btn.setTextColor(theme.keyTextColor);
            btn.setTextSize(12);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(6), dp(12), dp(6), dp(12));
            btn.setBackground(pillBg(theme.keyBgColor, theme.keyBorderColor, 10));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> host.runAiAction(action));
            grid.addView(btn);
        }
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    public View buildTextEditPanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "لوحة تحرير النص المتقدمة", theme);

        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(3);
        grid.setPadding(dp(10), dp(6), dp(10), dp(10));
        String[][] actions = {
                {"تحديد الكل", "select_all"}, {"▲ أعلى", "up"}, {"نسخ", "copy"},
                {"◀ يسار", "left"}, {"تحديد", "select"}, {"يمين ▶", "right"},
                {"قص", "cut"}, {"▼ أسفل", "down"}, {"لصق", "paste"}
        };
        for (String[] a : actions) {
            TextView btn = new TextView(ctx);
            btn.setText(a[0]);
            btn.setTextColor(theme.keyTextColor);
            btn.setTextSize(13);
            btn.setTypeface(null, Typeface.BOLD);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(8), dp(14), dp(8), dp(14));
            btn.setBackground(pillBg(theme.keyBgColor, theme.keyBorderColor, 12));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> host.performEditAction(a[1]));
            grid.addView(btn);
        }
        root.addView(grid);
        return root;
    }

    public View buildTrackpadPanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "وضع التراك باد — اسحب إصبعك لتحريك المؤشر", theme);

        TextView pad = new TextView(ctx);
        pad.setText("🖱️ اسحب يميناً أو يساراً لتحريك المؤشر بدقة");
        pad.setTextColor(theme.keyTextColor);
        pad.setGravity(Gravity.CENTER);
        pad.setBackground(pillBg(theme.keyBgColor, theme.accentColor, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(dp(12), dp(8), dp(12), dp(12));
        pad.setLayoutParams(lp);

        final float[] lastX = {0f};
        pad.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                lastX[0] = ev.getX();
            } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                float dx = ev.getX() - lastX[0];
                float step = dp(16);
                if (Math.abs(dx) >= step) {
                    host.moveCursor((int) (dx / step));
                    lastX[0] = ev.getX();
                }
            }
            return true;
        });
        root.addView(pad);
        return root;
    }

    public View buildCalculatorPanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "الحاسبة المدمجة", theme);

        TextView display = new TextView(ctx);
        display.setText(calcExpr.isEmpty() ? "0" : calcExpr);
        display.setTextColor(theme.accentColor);
        display.setTextSize(20);
        display.setTypeface(null, Typeface.BOLD);
        display.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        display.setPadding(dp(12), dp(6), dp(12), dp(6));
        root.addView(display);

        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(4);
        grid.setPadding(dp(8), dp(2), dp(8), dp(8));
        String[] keys = {
                "7","8","9","÷",
                "4","5","6","×",
                "1","2","3","-",
                "C","0","=","+"
        };
        for (String k : keys) {
            TextView b = new TextView(ctx);
            b.setText(k);
            b.setTextColor("=".equals(k) ? Color.WHITE : theme.keyTextColor);
            b.setTextSize(16);
            b.setTypeface(null, Typeface.BOLD);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(4), dp(10), dp(4), dp(10));
            b.setBackground(pillBg("=".equals(k) ? theme.accentColor : theme.keyBgColor, theme.keyBorderColor, 10));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                if ("C".equals(k)) {
                    calcExpr = "";
                } else if ("=".equals(k)) {
                    String res = SmartWordEngine.tryEvaluateMath(calcExpr);
                    if (res != null) {
                        host.commitRaw(res);
                        calcExpr = res;
                    }
                } else {
                    calcExpr += k;
                }
                display.setText(calcExpr.isEmpty() ? "0" : calcExpr);
            });
            grid.addView(b);
        }
        root.addView(grid);
        return root;
    }

    public View buildDateTimePanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "إدراج التاريخ والوقت (ميلادي وهجري)", theme);

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(12));

        String[] items = {
                "📅 التاريخ الميلادي: " + SmartWordEngine.getGregorianDate(),
                "🌙 التاريخ الهجري: " + SmartWordEngine.getHijriDate(),
                "⏰ الوقت الحالي: " + SmartWordEngine.getCurrentTime(),
                "🗓️ كامل: " + SmartWordEngine.getHijriDate() + " — " + SmartWordEngine.getGregorianDate()
        };
        String[] values = {
                SmartWordEngine.getGregorianDate(),
                SmartWordEngine.getHijriDate(),
                SmartWordEngine.getCurrentTime(),
                SmartWordEngine.getHijriDate() + " (" + SmartWordEngine.getGregorianDate() + ")"
        };
        for (int i = 0; i < items.length; i++) {
            final String val = values[i];
            TextView tv = createListRow(items[i], theme);
            tv.setOnClickListener(v -> {
                host.commitRaw(val);
                host.closePanel();
            });
            list.addView(tv);
        }
        root.addView(list);
        return root;
    }

    public View buildQuranOrAzkarPanel(boolean isQuran) {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, isQuran ? "آيات قرآنية سريعة الإدراج" : "الأذكار والأدعية السريعة", theme);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(10), dp(6), dp(10), dp(10));

        List<String> entries = isQuran ? SmartWordEngine.searchQuranVerses("") : SmartWordEngine.getAllAzkar();
        for (String line : entries) {
            TextView tv = createListRow(line, theme);
            tv.setOnClickListener(v -> {
                host.commitRaw(line);
                host.closePanel();
            });
            list.addView(tv);
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    public View buildResizePanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "تعديل ارتفاع الكيبورد والهامش السفلي", theme);

        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(12), dp(16), dp(12));

        boolean land = host.isLandscape();
        int curH = land ? prefs.getLandscapeHeightDp() : prefs.getPortraitHeightDp();
        TextView hLbl = new TextView(ctx);
        hLbl.setText((land ? "ارتفاع الوضع الأفقي: " : "ارتفاع الوضع الطولي: ") + curH + "dp");
        hLbl.setTextColor(theme.keyTextColor);
        body.addView(hLbl);

        SeekBar sbH = new SeekBar(ctx);
        sbH.setMax(land ? 130 : 190);
        sbH.setProgress(Math.max(0, curH - (land ? 130 : 190)));
        sbH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = p + (land ? 130 : 190);
                if (land) prefs.setLandscapeHeightDp(v); else prefs.setPortraitHeightDp(v);
                hLbl.setText((land ? "ارتفاع الوضع الأفقي: " : "ارتفاع الوضع الطولي: ") + v + "dp");
                host.refreshToolbarAndKeyboard();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        body.addView(sbH);

        TextView mLbl = new TextView(ctx);
        mLbl.setText("الهامش السفلي: " + prefs.getBottomMarginDp() + "dp");
        mLbl.setTextColor(theme.keyTextColor);
        mLbl.setPadding(0, dp(12), 0, 0);
        body.addView(mLbl);

        SeekBar sbM = new SeekBar(ctx);
        sbM.setMax(48);
        sbM.setProgress(prefs.getBottomMarginDp());
        sbM.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                prefs.setBottomMarginDp(p);
                mLbl.setText("الهامش السفلي: " + p + "dp");
                host.refreshToolbarAndKeyboard();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        body.addView(sbM);

        root.addView(body);
        return root;
    }

    public View buildStickersPanel() {
        KeyboardTheme theme = host.getActiveKeyboardTheme();
        LinearLayout root = createPanelRoot(theme);
        addPanelHeader(root, "ملصقاتي ورموز الزخرفة السريعة", theme);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(10), dp(6), dp(10), dp(10));

        // Quick decorative symbol strip
        HorizontalScrollView hsv = new HorizontalScrollView(ctx);
        LinearLayout symRow = new LinearLayout(ctx);
        symRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String sym : prefs.getCustomSymbols().split("\\s+")) {
            if (sym.trim().isEmpty()) continue;
            TextView chip = createChip(sym, theme, false);
            chip.setOnClickListener(v -> host.commitRaw(sym));
            symRow.addView(chip);
        }
        hsv.addView(symRow);
        list.addView(hsv);

        StickerRepository sRepo = new StickerRepository(ctx);
        List<StickerRepository.StickerItem> stickers = sRepo.getStickers();
        if (stickers.isEmpty()) {
            TextView hint = createListRow("أنشئ ملصقاتك النصية والمصوّرة من التطبيق ← التخصيص ← ملصقاتي", theme);
            list.addView(hint);
        } else {
            for (StickerRepository.StickerItem st : stickers) {
                TextView row = createListRow("🎨 " + st.label, theme);
                row.setOnClickListener(v -> host.commitRaw(st.label));
                list.addView(row);
            }
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    private LinearLayout createPanelRoot(KeyboardTheme theme) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.bgStartColor);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return root;
    }

    private void addPanelHeader(LinearLayout parent, String title, KeyboardTheme theme) {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(6), dp(12), dp(6));
        bar.setBackgroundColor(theme.toolbarBgColor);

        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextColor(theme.keyTextColor);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(tv);

        TextView close = createChip("✕ الكيبورد", theme, true);
        close.setOnClickListener(v -> host.closePanel());
        bar.addView(close);

        parent.addView(bar);
    }

    private TextView createListRow(String text, KeyboardTheme theme) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(theme.keyTextColor);
        tv.setTextSize(13);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(pillBg(theme.keyBgColor, theme.keyBorderColor, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView createChip(String text, KeyboardTheme theme, boolean accent) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(accent ? Color.WHITE : theme.keyTextColor);
        tv.setTextSize(12);
        tv.setPadding(dp(10), dp(5), dp(10), dp(5));
        tv.setBackground(pillBg(accent ? theme.accentColor : theme.keyBgColor, theme.keyBorderColor, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(6));
        tv.setLayoutParams(lp);
        return tv;
    }

    private GradientDrawable pillBg(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static String repeatEmoji(String em, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(em);
        return sb.toString();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics());
    }
}
