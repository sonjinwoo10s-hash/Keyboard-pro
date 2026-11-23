package com.keyboardpro.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.R;

/**
 * Tab 2 — Tools & Customization ("التخصيص"):
 * Direct elevated AMOLED cards with colored circular icons for Theme Editor,
 * Key FX (9 Animations & 5 Lighting FX), Layout Maker, Fonts, 56 Decorative Styles,
 * Stickers, and AI Keyboard (BYOK Gemini 15 Tools).
 */
public class CustomizeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customize, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout root = view.findViewById(R.id.customize_container);
        root.removeAllViews();

        root.addView(DetailUiKit.createSectionTitle(requireContext(), "استوديو التصميم والثيمات"));
        LinearLayout designCard = DetailUiKit.createCard(requireContext());
        designCard.addView(DetailUiKit.createIconActionRow(requireContext(), "🎨", Color.parseColor("#4E6BFF"),
                getString(R.string.cust_theme_editor_title), getString(R.string.cust_theme_editor_sub),
                true, v -> open(SettingsDetailActivity.SEC_THEME_EDITOR)));
        designCard.addView(DetailUiKit.createIconActionRow(requireContext(), "✨", Color.parseColor("#A855F7"),
                getString(R.string.cust_key_fx_title), getString(R.string.cust_key_fx_sub),
                true, v -> open(SettingsDetailActivity.SEC_KEY_FX)));
        designCard.addView(DetailUiKit.createIconActionRow(requireContext(), "📐", Color.parseColor("#06B6D4"),
                getString(R.string.set_appearance_title), getString(R.string.set_appearance_sub),
                false, v -> open(SettingsDetailActivity.SEC_APPEARANCE)));
        root.addView(designCard);

        root.addView(DetailUiKit.createSectionTitle(requireContext(), "التخطيطات والزخرفة والخطوط"));
        LinearLayout styleCard = DetailUiKit.createCard(requireContext());
        styleCard.addView(DetailUiKit.createIconActionRow(requireContext(), "⌨️", Color.parseColor("#10B981"),
                getString(R.string.set_layout_editor_title), getString(R.string.set_layout_editor_sub),
                true, v -> open(SettingsDetailActivity.SEC_LAYOUT_EDITOR)));
        styleCard.addView(DetailUiKit.createIconActionRow(requireContext(), "✦", Color.parseColor("#EC4899"),
                getString(R.string.set_decoration_title), getString(R.string.set_decoration_sub),
                true, v -> open(SettingsDetailActivity.SEC_DECORATION)));
        styleCard.addView(DetailUiKit.createIconActionRow(requireContext(), "🔤", Color.parseColor("#F59E0B"),
                getString(R.string.set_fonts_title), getString(R.string.set_fonts_sub),
                false, v -> open(SettingsDetailActivity.SEC_FONTS)));
        root.addView(styleCard);

        root.addView(DetailUiKit.createSectionTitle(requireContext(), "الذكاء الاصطناعي والملصقات"));
        LinearLayout toolsCard = DetailUiKit.createCard(requireContext());
        toolsCard.addView(DetailUiKit.createIconActionRow(requireContext(), "🤖", Color.parseColor("#6366F1"),
                getString(R.string.ai_keyboard_title), getString(R.string.ai_keyboard_sub),
                true, v -> open(SettingsDetailActivity.SEC_AI)));
        toolsCard.addView(DetailUiKit.createIconActionRow(requireContext(), "🖼️", Color.parseColor("#F97316"),
                getString(R.string.set_stickers_title), getString(R.string.set_stickers_sub),
                true, v -> open(SettingsDetailActivity.SEC_STICKERS)));
        toolsCard.addView(DetailUiKit.createIconActionRow(requireContext(), "😊", Color.parseColor("#EAB308"),
                getString(R.string.set_emoji_title), getString(R.string.set_emoji_sub),
                false, v -> open(SettingsDetailActivity.SEC_EMOJI)));
        root.addView(toolsCard);
    }

    private void open(String sec) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openDetailSection(sec);
        }
    }
}
