package com.keyboardpro.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.BuildConfig;
import com.example.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.keyboardpro.data.KeyboardPrefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Screen 2: Settings (الإعدادات).
 * Features search bar, App Language & Theme card (ليلي / نهاري / تلقائي),
 * What's New & AI Keyboard cards, and all 7 categorized setting groups.
 */
public class SettingsFragment extends Fragment {

    private static class SettingItem {
        final String emoji;
        final String title;
        final String subtitle;
        final String sectionKey;
        final boolean isSwitch;

        SettingItem(String emoji, String title, String subtitle, String sectionKey, boolean isSwitch) {
            this.emoji = emoji;
            this.title = title;
            this.subtitle = subtitle;
            this.sectionKey = sectionKey;
            this.isSwitch = isSwitch;
        }
    }

    private static class SettingCategory {
        final String title;
        final List<SettingItem> items;

        SettingCategory(String title, List<SettingItem> items) {
            this.title = title;
            this.items = items;
        }
    }

    private KeyboardPrefs prefs;
    private LinearLayout categoriesContainer;
    private TextView btnDark, btnLight, btnAuto, btnLangAr, btnLangEn;
    private String searchQuery = "";
    private List<SettingCategory> categories;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new KeyboardPrefs(requireContext());
        categoriesContainer = view.findViewById(R.id.settings_categories_container);

        btnDark = view.findViewById(R.id.btn_theme_dark);
        btnLight = view.findViewById(R.id.btn_theme_light);
        btnAuto = view.findViewById(R.id.btn_theme_auto);
        btnLangAr = view.findViewById(R.id.btn_lang_ar);
        btnLangEn = view.findViewById(R.id.btn_lang_en);

        TextView whatsNewSub = view.findViewById(R.id.tv_whats_new_sub);
        whatsNewSub.setText(getString(R.string.whats_new_sub, BuildConfig.VERSION_NAME));

        view.findViewById(R.id.card_whats_new).setOnClickListener(v -> showWhatsNewDialog());
        view.findViewById(R.id.card_ai_keyboard).setOnClickListener(v -> openDetail(SettingsDetailActivity.SEC_AI));

        setupThemeAndLanguageCard();
        buildCategoriesData();

        EditText etSearch = view.findViewById(R.id.et_settings_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase();
                renderCategories();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        renderCategories();
    }

    private void setupThemeAndLanguageCard() {
        refreshThemeLangChips();

        btnDark.setOnClickListener(v -> updateThemeMode(KeyboardPrefs.APP_THEME_DARK));
        btnLight.setOnClickListener(v -> updateThemeMode(KeyboardPrefs.APP_THEME_LIGHT));
        btnAuto.setOnClickListener(v -> updateThemeMode(KeyboardPrefs.APP_THEME_AUTO));

        btnLangAr.setOnClickListener(v -> {
            prefs.setAppLanguage("ar");
            refreshThemeLangChips();
            Toast.makeText(requireContext(), "تم تفعيل اللغة العربية (RTL)", Toast.LENGTH_SHORT).show();
        });
        btnLangEn.setOnClickListener(v -> {
            prefs.setAppLanguage("en");
            refreshThemeLangChips();
            Toast.makeText(requireContext(), "English layout preference saved", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateThemeMode(String mode) {
        prefs.setAppThemeMode(mode);
        refreshThemeLangChips();
        MainActivity.applySavedNightMode(mode);
    }

    private void refreshThemeLangChips() {
        String mode = prefs.getAppThemeMode();
        styleChip(btnDark, KeyboardPrefs.APP_THEME_DARK.equals(mode));
        styleChip(btnLight, KeyboardPrefs.APP_THEME_LIGHT.equals(mode));
        styleChip(btnAuto, KeyboardPrefs.APP_THEME_AUTO.equals(mode));

        String lang = prefs.getAppLanguage();
        styleChip(btnLangAr, "ar".equals(lang));
        styleChip(btnLangEn, "en".equals(lang));
    }

    private void styleChip(TextView tv, boolean active) {
        tv.setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tv.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.kp_primary : R.color.kp_text_secondary));
    }

    private void buildCategoriesData() {
        categories = Arrays.asList(
                new SettingCategory(getString(R.string.cat_languages_typing), Arrays.asList(
                        new SettingItem("🌐", getString(R.string.set_languages_title), getString(R.string.set_languages_sub), SettingsDetailActivity.SEC_LANGUAGES, false),
                        new SettingItem("🧠", getString(R.string.set_smart_typing_title), getString(R.string.set_smart_typing_sub), SettingsDetailActivity.SEC_SMART_TYPING, false),
                        new SettingItem("🌍", getString(R.string.set_translation_title), getString(R.string.set_translation_sub), SettingsDetailActivity.SEC_TRANSLATION, false),
                        new SettingItem("📖", getString(R.string.set_dictionary_title), getString(R.string.set_dictionary_sub), SettingsDetailActivity.SEC_DICTIONARY, false),
                        new SettingItem("⚡", getString(R.string.set_shortcuts_title), getString(R.string.set_shortcuts_sub), SettingsDetailActivity.SEC_SHORTCUTS, false)
                )),
                new SettingCategory(getString(R.string.cat_appearance_design), Arrays.asList(
                        new SettingItem("🎨", getString(R.string.set_appearance_title), getString(R.string.set_appearance_sub), SettingsDetailActivity.SEC_APPEARANCE, false),
                        new SettingItem("🔤", getString(R.string.set_fonts_title), getString(R.string.set_fonts_sub), SettingsDetailActivity.SEC_FONTS, false),
                        new SettingItem("⌨️", getString(R.string.set_layout_editor_title), getString(R.string.set_layout_editor_sub), SettingsDetailActivity.SEC_LAYOUT_EDITOR, false)
                )),
                new SettingCategory(getString(R.string.cat_emoji_stickers), Arrays.asList(
                        new SettingItem("😎", getString(R.string.set_emoji_title), getString(R.string.set_emoji_sub), SettingsDetailActivity.SEC_EMOJI, false),
                        new SettingItem("🖼️", getString(R.string.set_stickers_title), getString(R.string.set_stickers_sub), SettingsDetailActivity.SEC_STICKERS, false),
                        new SettingItem("✨", getString(R.string.set_decoration_title), getString(R.string.set_decoration_sub), SettingsDetailActivity.SEC_DECORATION, false)
                )),
                new SettingCategory(getString(R.string.cat_gestures_haptics), Arrays.asList(
                        new SettingItem("👆", getString(R.string.set_gestures_title), getString(R.string.set_gestures_sub), SettingsDetailActivity.SEC_GESTURES, false),
                        new SettingItem("🔊", getString(R.string.set_vibration_sound_title), getString(R.string.set_vibration_sound_sub), SettingsDetailActivity.SEC_HAPTICS, false)
                )),
                new SettingCategory(getString(R.string.cat_clipboard_passwords), Arrays.asList(
                        new SettingItem("📋", getString(R.string.set_clipboard_title), getString(R.string.set_clipboard_sub), SettingsDetailActivity.SEC_CLIPBOARD, false),
                        new SettingItem("🔐", getString(R.string.set_keyboard_pass_title), getString(R.string.set_keyboard_pass_sub), SettingsDetailActivity.SEC_KEYBOARD_PASS, false)
                )),
                new SettingCategory(getString(R.string.cat_backup_sync), Arrays.asList(
                        new SettingItem("💾", getString(R.string.set_local_backup_title), getString(R.string.set_local_backup_sub), SettingsDetailActivity.SEC_BACKUP, false),
                        new SettingItem("☁️", getString(R.string.set_cloud_sync_title), getString(R.string.set_cloud_sync_sub), SettingsDetailActivity.SEC_CLOUD_SYNC, false)
                )),
                new SettingCategory(getString(R.string.cat_general_app), Arrays.asList(
                        new SettingItem("💬", getString(R.string.set_requests_shortcut_title), getString(R.string.set_requests_shortcut_sub), "NAV_REQUESTS", false),
                        new SettingItem("🚀", getString(R.string.set_splash_title), getString(R.string.set_splash_sub), "SPLASH_SWITCH", true)
                ))
        );
    }

    private void renderCategories() {
        categoriesContainer.removeAllViews();
        int textPrimary = ContextCompat.getColor(requireContext(), R.color.kp_text_primary);
        int textSecondary = ContextCompat.getColor(requireContext(), R.color.kp_text_secondary);
        int accent = ContextCompat.getColor(requireContext(), R.color.kp_primary);

        for (SettingCategory cat : categories) {
            List<SettingItem> matched = new ArrayList<>();
            for (SettingItem item : cat.items) {
                if (searchQuery.isEmpty()
                        || cat.title.toLowerCase().contains(searchQuery)
                        || item.title.toLowerCase().contains(searchQuery)
                        || item.subtitle.toLowerCase().contains(searchQuery)) {
                    matched.add(item);
                }
            }
            if (matched.isEmpty()) continue;

            TextView catHeader = new TextView(requireContext());
            catHeader.setText(cat.title);
            catHeader.setTextColor(accent);
            catHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            catHeader.setTypeface(catHeader.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            hlp.topMargin = dp(14);
            hlp.bottomMargin = dp(6);
            catHeader.setLayoutParams(hlp);
            categoriesContainer.addView(catHeader);

            LinearLayout groupCard = new LinearLayout(requireContext());
            groupCard.setOrientation(LinearLayout.VERTICAL);
            groupCard.setBackgroundResource(R.drawable.bg_card);
            groupCard.setPadding(dp(12), dp(6), dp(12), dp(6));

            for (int i = 0; i < matched.size(); i++) {
                SettingItem item = matched.get(i);
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight(dp(58));
                row.setPadding(dp(4), dp(8), dp(4), dp(8));

                TextView icon = new TextView(requireContext());
                icon.setText(item.emoji);
                icon.setTextSize(20);
                icon.setGravity(Gravity.CENTER);
                icon.setBackgroundResource(R.drawable.bg_search_bar);
                row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

                LinearLayout texts = new LinearLayout(requireContext());
                texts.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tlp.setMarginStart(dp(12));
                texts.setLayoutParams(tlp);

                TextView title = new TextView(requireContext());
                title.setText(item.title);
                title.setTextColor(textPrimary);
                title.setTextSize(14);
                title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
                texts.addView(title);

                TextView sub = new TextView(requireContext());
                sub.setText(item.subtitle);
                sub.setTextColor(textSecondary);
                sub.setTextSize(11.5f);
                texts.addView(sub);
                row.addView(texts);

                if (item.isSwitch) {
                    MaterialSwitch sw = new MaterialSwitch(requireContext());
                    sw.setChecked(prefs.isSplashScreenEnabled());
                    sw.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setSplashScreenEnabled(isChecked));
                    row.addView(sw);
                } else {
                    TextView chevron = new TextView(requireContext());
                    chevron.setText("‹");
                    chevron.setTextColor(textSecondary);
                    chevron.setTextSize(22);
                    chevron.setPadding(dp(6), 0, dp(4), 0);
                    row.addView(chevron);

                    row.setClickable(true);
                    row.setFocusable(true);
                    row.setOnClickListener(v -> {
                        if ("NAV_REQUESTS".equals(item.sectionKey)) {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).switchToTab(MainActivity.TAB_REQUESTS);
                            }
                        } else {
                            openDetail(item.sectionKey);
                        }
                    });
                }

                groupCard.addView(row);

                if (i < matched.size() - 1) {
                    View divider = new View(requireContext());
                    divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kp_stroke));
                    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    dlp.setMarginStart(dp(52));
                    groupCard.addView(divider, dlp);
                }
            }

            categoriesContainer.addView(groupCard);
        }
    }

    private void openDetail(String section) {
        Intent i = new Intent(requireContext(), SettingsDetailActivity.class);
        i.putExtra(SettingsDetailActivity.EXTRA_SECTION, section);
        startActivity(i);
    }

    private void showWhatsNewDialog() {
        String msg = "الإصدار " + BuildConfig.VERSION_NAME + ":\n\n"
                + "• محرك الزخرفة والخطوط الفوري (TextStyleEngine): زخارف عربية (تطويل، تشكيل، إطارات ꧁ ꧂ و ❖ ❖) + 12 خط إنجليزي باليونيكود.\n"
                + "• منع تقسيم الكيبورد تماماً في الوضع الأفقي + إلغاء وضع الشاشة الكاملة (ExtractView).\n"
                + "• ثيمات الكريستال والزجاج المضيء والخلفية الشفافة المموهة (Blur).\n"
                + "• خزنة Keyboard Pass محمية ببصمة الإصبع (BiometricPrompt).\n"
                + "• كيبورد الذكاء الاصطناعي بمفتاح API مجاني خاص بك + النسخ الاحتياطي السحابي.";

        new AlertDialog.Builder(requireContext())
                .setTitle("ما الجديد في كيبورد برو ✨")
                .setMessage(msg)
                .setPositiveButton("رائع", null)
                .show();
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }
}
