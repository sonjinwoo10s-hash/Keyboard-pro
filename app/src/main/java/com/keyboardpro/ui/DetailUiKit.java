package com.keyboardpro.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Reusable Material 3 Dark/Light UI building blocks for Settings sub-screens.
 */
public final class DetailUiKit {

    public interface OnToggleListener {
        void onChanged(boolean checked);
    }

    public interface OnSliderListener {
        void onChanged(int value);
    }

    private DetailUiKit() {
    }

    public static LinearLayout createCard(Context ctx) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int p = dp(ctx, 14);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 12);
        card.setLayoutParams(lp);
        return card;
    }

    public static TextView createSectionTitle(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(ctx, R.color.kp_primary));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 6);
        lp.bottomMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static TextView createBodyText(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_secondary));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static LinearLayout createSwitchRow(Context ctx, String title, String subtitle,
                                               boolean checked, OnToggleListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(ctx, 6), 0, dp(ctx, 6));

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMarginEnd(dp(ctx, 10));
        texts.setLayoutParams(tlp);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        texts.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_secondary));
            tvSub.setTextSize(11.5f);
            texts.addView(tvSub);
        }
        row.addView(texts);

        MaterialSwitch sw = new MaterialSwitch(ctx);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onChanged(isChecked));
        row.addView(sw);
        return row;
    }

    public static LinearLayout createSliderRow(Context ctx, String label, int min, int max,
                                               int current, String unit, OnSliderListener listener) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(ctx, 6), 0, dp(ctx, 6));

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvLabel = new TextView(ctx);
        tvLabel.setText(label);
        tvLabel.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
        tvLabel.setTextSize(13.5f);
        top.addView(tvLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvVal = new TextView(ctx);
        tvVal.setText(current + " " + unit);
        tvVal.setTextColor(ContextCompat.getColor(ctx, R.color.kp_primary));
        tvVal.setTextSize(13);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(tvVal);
        box.addView(top);

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(max - min);
        seekBar.setProgress(Math.max(0, current - min));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int val = min + progress;
                tvVal.setText(val + " " + unit);
                if (fromUser) listener.onChanged(val);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        box.addView(seekBar);
        return box;
    }

    public static EditText createInput(Context ctx, String hint, boolean password) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setBackgroundResource(R.drawable.bg_search_bar);
        et.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
        et.setHintTextColor(ContextCompat.getColor(ctx, R.color.kp_text_muted));
        et.setTextSize(13.5f);
        et.setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10));
        et.setMinHeight(dp(ctx, 48));
        if (password) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 8);
        et.setLayoutParams(lp);
        return et;
    }

    public static MaterialButton createPrimaryButton(Context ctx, String text, View.OnClickListener listener) {
        MaterialButton btn = new MaterialButton(ctx);
        btn.setText(text);
        btn.setTextColor(ContextCompat.getColor(ctx, R.color.kp_on_primary));
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 48));
        lp.topMargin = dp(ctx, 6);
        btn.setLayoutParams(lp);
        return btn;
    }

    public static TextView createChipOption(Context ctx, String text, boolean selected, View.OnClickListener listener) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10));
        tv.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tv.setTextColor(ContextCompat.getColor(ctx, selected ? R.color.kp_primary : R.color.kp_text_primary));
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 6);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static int dp(Context ctx, int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics()));
    }
}
