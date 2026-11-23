package com.keyboardpro.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.BuildConfig;
import com.example.R;
import com.keyboardpro.data.KeyboardPrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab 3 — Searchable AMOLED Settings ("الإعدادات"):
 * Grouped into rounded elevated cards with RTL vertical blue pill section headers
 * and colored circular icons. Dynamically reads BuildConfig.VERSION_NAME.
 */
public class SettingsFragment extends Fragment {

    private static final class SettingEntry {
        final String category;
        final String icon;
        final int color;
        final String title;
        final String subtitle;
        final String sectionKey;

        SettingEntry(String category, String icon, int color, String title, String subtitle, String sectionKey) {
            this.category = category;
            this.icon = icon;
            this.color = color;
            this.title = title;
            this.subtitle = subtitle;
            this.sectionKey = sectionKey;
        }
    }

    private KeyboardPrefs prefs;
    private LinearLayout listContainer;
    private final List<SettingEntry> allEntries = new ArrayList<>();
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customize, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new KeyboardPrefs(requireContext());
        LinearLayout root = view.findViewById(R.id.customize_container);
        root.removeAllViews();

        // Top Search Input
        EditText etSearch = DetailUiKit.createInput(requireContext(), getString(R.string.search_settings_hint), false);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s == null ? "" : s.toString().trim().toLowerCase();
                renderEntries();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        root.addView(etSearch);

        // Version & App Mode Card (Always uses BuildConfig.VERSION_NAME dynamically!)
        LinearLayout topCard = DetailUiKit.createCard(requireContext());
        TextView verTitle = new TextView(requireContext());
        verTitle.setText(getString(R.string.whats_new_title));
        verTitle.setTextColor(getResources().getColor(R.color.kp_text_primary, null));
        verTitle.setTextSize(15);
        verTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        topCard.addView(verTitle);
        topCard.addView(DetailUiKit.createBodyText(requireContext(),
                getString(R.string.whats_new_sub, BuildConfig.VERSION_NAME)));

        LinearLayout themeRow = new LinearLayout(requireContext());
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        String curMode = prefs.getAppThemeMode();
        String[][] modes = {
                {KeyboardPrefs.APP_THEME_DARK, getString(R.string.theme_dark)},
                {KeyboardPrefs.APP_THEME_LIGHT, getString(R.string.theme_light)},
                {KeyboardPrefs.APP_THEME_AUTO, getString(R.string.theme_auto)}
        };
        for (String[] m : modes) {
            TextView chip = DetailUiKit.createChipOption(requireContext(), m[1], m[0].equals(curMode), v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).applyAppThemeAndRecreate(m[0]);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(DetailUiKit.dp(requireContext(), 6));
            chip.setLayoutParams(lp);
            themeRow.addView(chip);
        }
        topCard.addView(themeRow);
        root.addView(topCard);

        listContainer = new LinearLayout(requireContext());
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        populateEntries();
        renderEntries();
    }

    private void populateEntries() {
        allEntries.clear();
        String c1 = getString(R.string.cat_languages_typing);
        allEntries.add(new SettingEntry(c1, "🌐", Color.parseColor("#4E6BFF"), getString(R.string.set_languages_title), getString(R.string.set_languages_sub), SettingsDetailActivity.SEC_LANGUAGES));
        allEntries.add(new SettingEntry(c1, "🧠", Color.parseColor("#10B981"), getString(R.string.set_smart_typing_title), getString(R.string.set_smart_typing_sub), SettingsDetailActivity.SEC_SMART_TYPING));
        allEntries.add(new SettingEntry(c1, "⚡", Color.parseColor("#F59E0B"), getString(R.string.set_shortcuts_title), getString(R.string.set_shortcuts_sub), SettingsDetailActivity.SEC_SHORTCUTS));
        allEntries.add(new SettingEntry(c1, "📖", Color.parseColor("#06B6D4"), getString(R.string.set_dictionary_title), getString(R.string.set_dictionary_sub), SettingsDetailActivity.SEC_DICTIONARY));
        allEntries.add(new SettingEntry(c1, "🔤", Color.parseColor("#8B5CF6"), getString(R.string.set_translation_title), getString(R.string.set_translation_sub), SettingsDetailActivity.SEC_TRANSLATION));

        String c2 = getString(R.string.cat_appearance_design);
        allEntries.add(new SettingEntry(c2, "🎨", Color.parseColor("#EC4899"), getString(R.string.cust_theme_editor_title), getString(R.string.cust_theme_editor_sub), SettingsDetailActivity.SEC_THEME_EDITOR));
        allEntries.add(new SettingEntry(c2, "✨", Color.parseColor("#A855F7"), getString(R.string.cust_key_fx_title), getString(R.string.cust_key_fx_sub), SettingsDetailActivity.SEC_KEY_FX));
        allEntries.add(new SettingEntry(c2, "📐", Color.parseColor("#3B82F6"), getString(R.string.set_appearance_title), getString(R.string.set_appearance_sub), SettingsDetailActivity.SEC_APPEARANCE));
        allEntries.add(new SettingEntry(c2, "⌨️", Color.parseColor("#14B8A6"), getString(R.string.set_layout_editor_title), getString(R.string.set_layout_editor_sub), SettingsDetailActivity.SEC_LAYOUT_EDITOR));
        allEntries.add(new SettingEntry(c2, "🅰️", Color.parseColor("#F97316"), getString(R.string.set_fonts_title), getString(R.string.set_fonts_sub), SettingsDetailActivity.SEC_FONTS));

        String c3 = getString(R.string.cat_gestures_haptics);
        allEntries.add(new SettingEntry(c3, "👆", Color.parseColor("#0EA5E9"), getString(R.string.set_gestures_title), getString(R.string.set_gestures_sub), SettingsDetailActivity.SEC_GESTURES));
        allEntries.add(new SettingEntry(c3, "🔊", Color.parseColor("#EF4444"), getString(R.string.set_vibration_sound_title), getString(R.string.set_vibration_sound_sub), SettingsDetailActivity.SEC_HAPTICS));

        String c4 = getString(R.string.cat_clipboard_passwords);
        allEntries.add(new SettingEntry(c4, "📋", Color.parseColor("#6366F1"), getString(R.string.set_clipboard_title), getString(R.string.set_clipboard_sub), SettingsDetailActivity.SEC_CLIPBOARD));
        allEntries.add(new SettingEntry(c4, "🔐", Color.parseColor("#22C55E"), getString(R.string.set_keyboard_pass_title), getString(R.string.set_keyboard_pass_sub), SettingsDetailActivity.SEC_KEYBOARD_PASS));
        allEntries.add(new SettingEntry(c4, "🤖", Color.parseColor("#4E6BFF"), getString(R.string.ai_keyboard_title), getString(R.string.ai_keyboard_sub), SettingsDetailActivity.SEC_AI));

        String c5 = getString(R.string.cat_backup_sync);
        allEntries.add(new SettingEntry(c5, "💾", Color.parseColor("#10B981"), getString(R.string.set_local_backup_title), getString(R.string.set_local_backup_sub), SettingsDetailActivity.SEC_BACKUP));
        allEntries.add(new SettingEntry(c5, "☁️", Color.parseColor("#3B82F6"), getString(R.string.set_cloud_sync_title), getString(R.string.set_cloud_sync_sub), SettingsDetailActivity.SEC_CLOUD_SYNC));
        allEntries.add(new SettingEntry(c5, "💬", Color.parseColor("#EAB308"), getString(R.string.set_requests_shortcut_title), getString(R.string.set_requests_shortcut_sub), SettingsDetailActivity.SEC_REQUESTS));
    }

    private void renderEntries() {
        listContainer.removeAllViews();
        String lastCat = null;
        LinearLayout curCard = null;

        List<SettingEntry> visible = new ArrayList<>();
        for (SettingEntry e : allEntries) {
            if (query.isEmpty()
                    || e.title.toLowerCase().contains(query)
                    || e.subtitle.toLowerCase().contains(query)
                    || e.category.toLowerCase().contains(query)) {
                visible.add(e);
            }
        }

        for (int i = 0; i < visible.size(); i++) {
            SettingEntry e = visible.get(i);
            if (!e.category.equals(lastCat)) {
                lastCat = e.category;
                listContainer.addView(DetailUiKit.createSectionTitle(requireContext(), e.category));
                curCard = DetailUiKit.createCard(requireContext());
                listContainer.addView(curCard);
            }
            boolean hasMoreInCat = (i + 1 < visible.size()) && visible.get(i + 1).category.equals(e.category);
            if (curCard != null) {
                curCard.addView(DetailUiKit.createIconActionRow(
                        requireContext(), e.icon, e.color, e.title, e.subtitle, hasMoreInCat,
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).openDetailSection(e.sectionKey);
                            }
                        }));
            }
        }

        // Splash screen toggle at bottom
        if (query.isEmpty()) {
            listContainer.addView(DetailUiKit.createSectionTitle(requireContext(), getString(R.string.cat_general_app)));
            LinearLayout genCard = DetailUiKit.createCard(requireContext());
            genCard.addView(DetailUiKit.createSwitchRow(requireContext(),
                    getString(R.string.set_splash_title), getString(R.string.set_splash_sub),
                    prefs.isSplashScreenEnabled(), v -> {
                        prefs.setSplashScreenEnabled(v);
                        Toast.makeText(requireContext(), v ? "تم تفعيل شاشة البداية" : "تم إيقاف شاشة البداية", Toast.LENGTH_SHORT).show();
                    }));
            listContainer.addView(genCard);
        }
    }
}
