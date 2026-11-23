package com.keyboardpro.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.ime.KeyboardTheme;
import com.keyboardpro.ime.MiniKeyboardPreviewView;
import com.keyboardpro.ime.ThemeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 1: Home (الرئيسية - Themes & Setup).
 * Features Quick Setup Card (2 steps), live test box, real-time theme search,
 * 4 sub-tabs (الثيمات / برو / ثيماتي / المستخدمين), categorized live mini-preview theme grids,
 * and a custom theme creator dialog.
 */
public class HomeFragment extends Fragment {

    private static final int SUBTAB_THEMES = 0;
    private static final int SUBTAB_PRO = 1;
    private static final int SUBTAB_MY_THEMES = 2;
    private static final int SUBTAB_USERS = 3;

    private KeyboardPrefs prefs;
    private int selectedSubTab = SUBTAB_THEMES;
    private String searchQuery = "";

    private MaterialButton btnStepEnable, btnStepSelect, btnCreateTheme;
    private TextView tvSetupProgress;
    private TextView tabThemes, tabPro, tabMyThemes, tabUsers;
    private LinearLayout sectionsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new KeyboardPrefs(requireContext());

        btnStepEnable = view.findViewById(R.id.btn_step_enable);
        btnStepSelect = view.findViewById(R.id.btn_step_select);
        btnCreateTheme = view.findViewById(R.id.btn_create_custom_theme);
        tvSetupProgress = view.findViewById(R.id.tv_setup_progress);

        tabThemes = view.findViewById(R.id.subtab_themes);
        tabPro = view.findViewById(R.id.subtab_pro);
        tabMyThemes = view.findViewById(R.id.subtab_my_themes);
        tabUsers = view.findViewById(R.id.subtab_users);
        sectionsContainer = view.findViewById(R.id.themes_sections_container);

        btnStepEnable.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        });

        btnStepSelect.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
        });

        EditText etSearch = view.findViewById(R.id.et_theme_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString();
                renderThemes();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        tabThemes.setOnClickListener(v -> selectSubTab(SUBTAB_THEMES));
        tabPro.setOnClickListener(v -> selectSubTab(SUBTAB_PRO));
        tabMyThemes.setOnClickListener(v -> selectSubTab(SUBTAB_MY_THEMES));
        tabUsers.setOnClickListener(v -> selectSubTab(SUBTAB_USERS));

        btnCreateTheme.setOnClickListener(v -> showCreateThemeDialog());

        updateSetupStatus();
        selectSubTab(SUBTAB_THEMES);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSetupStatus();
        renderThemes();
    }

    private void updateSetupStatus() {
        if (!isAdded()) return;
        Context ctx = requireContext();
        String pkg = ctx.getPackageName();
        boolean isEnabled = false;
        boolean isSelected = false;

        InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
                if (pkg.equals(info.getPackageName())) {
                    isEnabled = true;
                    break;
                }
            }
        }
        String defaultIme = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (defaultIme != null && defaultIme.contains(pkg)) {
            isSelected = true;
        }

        int doneCount = (isEnabled ? 1 : 0) + (isSelected ? 1 : 0);
        tvSetupProgress.setText(doneCount + " / 2");
        btnStepEnable.setText(isEnabled ? R.string.setup_step_1_done : R.string.setup_step_1_btn);
        btnStepSelect.setText(isSelected ? R.string.setup_step_2_done : R.string.setup_step_2_btn);
    }

    private void selectSubTab(int index) {
        selectedSubTab = index;
        styleSubTab(tabThemes, index == SUBTAB_THEMES, false);
        styleSubTab(tabPro, index == SUBTAB_PRO, true);
        styleSubTab(tabMyThemes, index == SUBTAB_MY_THEMES, false);
        styleSubTab(tabUsers, index == SUBTAB_USERS, false);
        btnCreateTheme.setVisibility(index == SUBTAB_MY_THEMES ? View.VISIBLE : View.GONE);
        renderThemes();
    }

    private void styleSubTab(TextView tv, boolean active, boolean isProTab) {
        tv.setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        int color = active
                ? ContextCompat.getColor(requireContext(), isProTab ? R.color.kp_gold : R.color.kp_primary)
                : ContextCompat.getColor(requireContext(), isProTab ? R.color.kp_gold : R.color.kp_text_secondary);
        tv.setTextColor(color);
    }

    private void renderThemes() {
        if (!isAdded()) return;
        sectionsContainer.removeAllViews();
        List<KeyboardTheme> all = ThemeRegistry.getAllThemes(requireContext());

        if (selectedSubTab == SUBTAB_THEMES) {
            List<KeyboardTheme> newThemes = filter(all, t -> t.category == KeyboardTheme.Category.NEW);
            List<KeyboardTheme> basicThemes = filter(all, t -> t.category == KeyboardTheme.Category.BASIC);
            addSection(getString(R.string.section_new), newThemes);
            addSection(getString(R.string.section_basic), basicThemes);
        } else if (selectedSubTab == SUBTAB_PRO) {
            List<KeyboardTheme> proThemes = filter(all, t -> t.isPro);
            addSection("ثيمات برو الكريستالية والضبابية 👑", proThemes);
        } else if (selectedSubTab == SUBTAB_MY_THEMES) {
            List<KeyboardTheme> myThemes = filter(all, t -> t.category == KeyboardTheme.Category.MY_THEMES);
            if (myThemes.isEmpty()) {
                addEmptyMessage("لم تقم بإنشاء ثيم مخصص بعد. اضغط على «+ إنشاء ثيم جديد» لتصميم ثيمك الخاص!");
            } else {
                addSection(getString(R.string.section_custom), myThemes);
            }
        } else if (selectedSubTab == SUBTAB_USERS) {
            List<KeyboardTheme> userThemes = filter(all, t -> t.category == KeyboardTheme.Category.USERS);
            addSection(getString(R.string.section_community), userThemes);
        }

        if (sectionsContainer.getChildCount() == 0) {
            addEmptyMessage("لا توجد ثيمات مطابقة لنتيجة البحث.");
        }
    }

    private interface ThemePredicate {
        boolean test(KeyboardTheme theme);
    }

    private List<KeyboardTheme> filter(List<KeyboardTheme> source, ThemePredicate predicate) {
        List<KeyboardTheme> result = new ArrayList<>();
        for (KeyboardTheme t : source) {
            if (predicate.test(t) && t.matchesQuery(searchQuery)) {
                result.add(t);
            }
        }
        return result;
    }

    private void addSection(String title, List<KeyboardTheme> themes) {
        if (themes.isEmpty()) return;

        TextView header = new TextView(requireContext());
        header.setText(title);
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.kp_text_primary));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(12);
        hlp.bottomMargin = dp(8);
        header.setLayoutParams(hlp);
        sectionsContainer.addView(header);

        // Build clean 2-column rows
        String activeId = prefs.getActiveThemeId();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < themes.size(); i += 2) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = dp(10);
            row.setLayoutParams(rlp);

            View card1 = createThemeCard(inflater, themes.get(i), activeId);
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp1.setMarginEnd(dp(5));
            row.addView(card1, lp1);

            if (i + 1 < themes.size()) {
                View card2 = createThemeCard(inflater, themes.get(i + 1), activeId);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp2.setMarginStart(dp(5));
                row.addView(card2, lp2);
            } else {
                View spacer = new View(requireContext());
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, 1, 1f);
                lp2.setMarginStart(dp(5));
                row.addView(spacer, lp2);
            }
            sectionsContainer.addView(row);
        }
    }

    private View createThemeCard(LayoutInflater inflater, KeyboardTheme theme, String activeId) {
        View card = inflater.inflate(R.layout.item_theme_card, sectionsContainer, false);
        MiniKeyboardPreviewView preview = card.findViewById(R.id.mini_keyboard_preview);
        TextView name = card.findViewById(R.id.tv_theme_name);
        TextView meta = card.findViewById(R.id.tv_theme_meta);
        View crownBadge = card.findViewById(R.id.badge_pro_crown);
        View activeBadge = card.findViewById(R.id.badge_active);

        preview.setTheme(theme);
        name.setText(theme.nameAr);
        meta.setText(describeThemeStyle(theme));
        crownBadge.setVisibility(theme.isPro ? View.VISIBLE : View.GONE);

        boolean isActive = theme.id.equals(activeId);
        activeBadge.setVisibility(isActive ? View.VISIBLE : View.GONE);
        card.setBackgroundResource(isActive ? R.drawable.bg_chip_selected : R.drawable.bg_card);

        card.setOnClickListener(v -> {
            prefs.setActiveThemeId(theme.id);
            prefs.setTransparentBlurEnabled(theme.backgroundType == KeyboardTheme.BackgroundType.TRANSPARENT_BLUR);
            prefs.setGlassKeysForced(theme.keyStyleType == KeyboardTheme.KeyStyleType.GLASS_CRYSTAL);
            Toast.makeText(requireContext(),
                    getString(R.string.theme_applied_toast, theme.nameAr),
                    Toast.LENGTH_SHORT).show();
            renderThemes();
        });
        return card;
    }

    private String describeThemeStyle(KeyboardTheme theme) {
        String bg;
        switch (theme.backgroundType) {
            case GRADIENT: bg = "تدرج"; break;
            case CHECKERED: bg = "شطرنج"; break;
            case TRANSPARENT_BLUR: bg = "تمويه شفاف"; break;
            case AUTO_SPLIT: bg = "تلقائي ليلي/نهاري"; break;
            default: bg = "لون صلب"; break;
        }
        String key = theme.keyStyleType == KeyboardTheme.KeyStyleType.GLASS_CRYSTAL ? " • كريستال" : "";
        return bg + key;
    }

    private void addEmptyMessage(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.kp_text_secondary));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(16), dp(32), dp(16), dp(32));
        sectionsContainer.addView(tv);
    }

    private void showCreateThemeDialog() {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(14), dp(20), dp(8));

        EditText etName = new EditText(requireContext());
        etName.setHint("اسم الثيم المخصص (مثلاً: أزرق ملكي)");
        form.addView(etName);

        final int[] bgChoices = {
                Color.parseColor("#0D1321"), Color.parseColor("#1B102E"),
                Color.parseColor("#09241D"), Color.parseColor("#2A1215")
        };
        final int[] accentChoices = {
                Color.parseColor("#00D4FF"), Color.parseColor("#F5C542"),
                Color.parseColor("#EC4899"), Color.parseColor("#10B981")
        };
        final int[] chosenBgIdx = {0};
        final int[] chosenAccentIdx = {0};

        TextView lblColors = new TextView(requireContext());
        lblColors.setText("اختر لوحة الألوان:");
        lblColors.setPadding(0, dp(10), 0, dp(6));
        form.addView(lblColors);

        LinearLayout swatchRow = new LinearLayout(requireContext());
        swatchRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] presetLabels = {"سماوي داكن", "بنفسجي ذهبي", "زمردي وردي", "ياقوتي أخضر"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView pill = new TextView(requireContext());
            pill.setText(presetLabels[i]);
            pill.setTextSize(11);
            pill.setTextColor(Color.WHITE);
            pill.setBackgroundColor(bgChoices[i]);
            pill.setPadding(dp(8), dp(8), dp(8), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            pill.setGravity(Gravity.CENTER);
            pill.setOnClickListener(v -> {
                chosenBgIdx[0] = idx;
                chosenAccentIdx[0] = idx;
                Toast.makeText(requireContext(), "تم اختيار: " + presetLabels[idx], Toast.LENGTH_SHORT).show();
            });
            swatchRow.addView(pill, lp);
        }
        form.addView(swatchRow);

        CheckBox cbGlass = new CheckBox(requireContext());
        cbGlass.setText("مفاتيح زجاجية / كريستال مضيئة");
        cbGlass.setChecked(true);
        form.addView(cbGlass);

        CheckBox cbBlur = new CheckBox(requireContext());
        cbBlur.setText("خلفية شفافة مموهة (Blur)");
        form.addView(cbBlur);

        new AlertDialog.Builder(requireContext())
                .setTitle("إنشاء ثيم مخصص")
                .setView(form)
                .setPositiveButton("حفظ وتطبيق", (d, w) -> {
                    int bg1 = bgChoices[chosenBgIdx[0]];
                    int accent = accentChoices[chosenAccentIdx[0]];
                    int bg2 = Color.rgb(
                            Math.min(255, Color.red(bg1) + 22),
                            Math.min(255, Color.green(bg1) + 26),
                            Math.min(255, Color.blue(bg1) + 38));
                    int keyBg = Color.argb(cbGlass.isChecked() ? 70 : 255,
                            Math.min(255, Color.red(bg1) + 32),
                            Math.min(255, Color.green(bg1) + 38),
                            Math.min(255, Color.blue(bg1) + 52));
                    KeyboardTheme created = ThemeRegistry.saveCustomTheme(
                            requireContext(), etName.getText().toString(),
                            bg1, bg2, keyBg, accent, cbGlass.isChecked(), cbBlur.isChecked());
                    prefs.setActiveThemeId(created.id);
                    renderThemes();
                    Toast.makeText(requireContext(), "تم إنشاء وتطبيق ثيمك!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }
}
