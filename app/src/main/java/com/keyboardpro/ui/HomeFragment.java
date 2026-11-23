package com.keyboardpro.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.ime.KeyboardTheme;
import com.keyboardpro.ime.MiniKeyboardPreviewView;
import com.keyboardpro.ime.ThemeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab 1 — Themes Showcase ("الرئيسية"):
 * - 2-Step Quick Setup Card
 * - Live Search by Theme Name or Color
 * - 7 Horizontal Filter Chips (الكل، زجاجية، ماتيريال، ملونة، داكنة، فاتحة، ميكانيكي)
 * - Responsive 2-column (portrait) / 3-column (landscape) grid of all 42+ unlocked live previews
 */
public class HomeFragment extends Fragment {

    private KeyboardPrefs prefs;
    private MaterialButton btnStepEnable, btnStepSelect;
    private RecyclerView rvThemes;
    private TextView tvSectionTitle;
    private ThemeGridAdapter adapter;

    private TextView[] filterChips;
    private final String[] filterKeys = {
            KeyboardTheme.FILTER_ALL,
            KeyboardTheme.FILTER_GLASS,
            KeyboardTheme.FILTER_MATERIAL,
            KeyboardTheme.FILTER_COLORFUL,
            KeyboardTheme.FILTER_DARK,
            KeyboardTheme.FILTER_LIGHT,
            KeyboardTheme.FILTER_MECHANICAL
    };

    private String selectedFilter = KeyboardTheme.FILTER_ALL;
    private String searchQuery = "";

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
        tvSectionTitle = view.findViewById(R.id.tv_themes_section_title);
        rvThemes = view.findViewById(R.id.rv_themes_grid);

        btnStepEnable.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        btnStepSelect.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });

        view.findViewById(R.id.btn_open_theme_editor).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDetailSection(SettingsDetailActivity.SEC_THEME_EDITOR);
            }
        });

        EditText etSearch = view.findViewById(R.id.et_search_themes);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString();
                refreshThemesGrid();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        filterChips = new TextView[]{
                view.findViewById(R.id.chip_filter_all),
                view.findViewById(R.id.chip_filter_glass),
                view.findViewById(R.id.chip_filter_material),
                view.findViewById(R.id.chip_filter_colorful),
                view.findViewById(R.id.chip_filter_dark),
                view.findViewById(R.id.chip_filter_light),
                view.findViewById(R.id.chip_filter_mechanical)
        };
        for (int i = 0; i < filterChips.length; i++) {
            final int idx = i;
            filterChips[i].setOnClickListener(v -> {
                selectedFilter = filterKeys[idx];
                updateChipStyles();
                refreshThemesGrid();
            });
        }

        int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        rvThemes.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        adapter = new ThemeGridAdapter();
        rvThemes.setAdapter(adapter);

        updateChipStyles();
        refreshThemesGrid();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSetupStatus();
        refreshThemesGrid();
    }

    private void refreshSetupStatus() {
        Context ctx = getContext();
        if (ctx == null) return;
        InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        boolean enabled = false;
        if (imm != null) {
            for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
                if (info.getPackageName().equals(ctx.getPackageName())) {
                    enabled = true;
                    break;
                }
            }
        }
        String defIme = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean selected = defIme != null && defIme.contains(ctx.getPackageName());

        btnStepEnable.setText(enabled ? R.string.setup_step_1_done : R.string.setup_step_1_btn);
        btnStepSelect.setText(selected ? R.string.setup_step_2_done : R.string.setup_step_2_btn);
    }

    private void updateChipStyles() {
        int active = ContextCompat.getColor(requireContext(), R.color.kp_primary);
        int inactive = ContextCompat.getColor(requireContext(), R.color.kp_text_secondary);
        for (int i = 0; i < filterChips.length; i++) {
            boolean sel = filterKeys[i].equals(selectedFilter);
            filterChips[i].setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            filterChips[i].setTextColor(sel ? active : inactive);
        }
    }

    private void refreshThemesGrid() {
        if (getContext() == null) return;
        List<KeyboardTheme> all = ThemeRegistry.getAllThemes(requireContext());
        List<KeyboardTheme> filtered = new ArrayList<>();
        for (KeyboardTheme t : all) {
            if (t.matchesFilter(selectedFilter) && t.matchesQuery(searchQuery)) {
                filtered.add(t);
            }
        }
        tvSectionTitle.setText("معرض الثيمات (" + filtered.size() + " ثيم مفتوح)");
        adapter.submit(filtered, prefs.getActiveThemeId());
    }

    private final class ThemeGridAdapter extends RecyclerView.Adapter<ThemeGridAdapter.VH> {
        private final List<KeyboardTheme> items = new ArrayList<>();
        private String activeId = "";

        void submit(List<KeyboardTheme> list, String activeThemeId) {
            items.clear();
            items.addAll(list);
            activeId = activeThemeId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            KeyboardTheme theme = items.get(pos);
            boolean isActive = theme.id.equals(activeId);

            h.preview.setTheme(theme);
            h.tvName.setText(theme.nameAr);
            h.tvMeta.setText(theme.nameEn);
            h.tvBadge.setVisibility(isActive ? View.VISIBLE : View.GONE);
            h.cardRoot.setBackgroundResource(isActive ? R.drawable.bg_card_hero : R.drawable.bg_card);

            h.cardRoot.setOnClickListener(v -> {
                prefs.setActiveThemeId(theme.id);
                activeId = theme.id;
                notifyDataSetChanged();
                Toast.makeText(requireContext(),
                        getString(R.string.theme_applied_toast, theme.nameAr), Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class VH extends RecyclerView.ViewHolder {
            final View cardRoot;
            final MiniKeyboardPreviewView preview;
            final TextView tvName;
            final TextView tvMeta;
            final TextView tvBadge;

            VH(@NonNull View v) {
                super(v);
                cardRoot = v.findViewById(R.id.theme_card_root);
                preview = v.findViewById(R.id.mini_keyboard_preview);
                tvName = v.findViewById(R.id.tv_theme_name);
                tvMeta = v.findViewById(R.id.tv_theme_meta);
                tvBadge = v.findViewById(R.id.badge_active);
            }
        }
    }
}
