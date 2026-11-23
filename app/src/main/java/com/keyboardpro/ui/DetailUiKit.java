package com.keyboardpro.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Reusable AMOLED Dark / Light Material 3 UI building blocks for Companion Screens.
 * Includes RTL section headers with vertical blue pill indicator, colored circular icon rows,
 * and the persistent Keyboard Test Modal dialog.
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

    /** RTL Section Header with vertical indigo-blue pill indicator (|). */
    public static LinearLayout createSectionTitle(Context ctx, String text) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 10);
        lp.bottomMargin = dp(ctx, 8);
        row.setLayoutParams(lp);

        View pill = new View(ctx);
        pill.setBackgroundResource(R.drawable.bg_pill_indicator);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(ctx, 4), dp(ctx, 18));
        plp.setMarginEnd(dp(ctx, 8));
        pill.setLayoutParams(plp);
        row.addView(pill);

        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(tv);
        return row;
    }

    /** Setting / Customize navigation row with colored circular icon badge. */
    public static LinearLayout createIconActionRow(Context ctx, String emojiIcon, int circleColor,
                                                   String title, String subtitle,
                                                   boolean showDivider, View.OnClickListener onClick) {
        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(ctx, 60));
        row.setPadding(dp(ctx, 4), dp(ctx, 8), dp(ctx, 4), dp(ctx, 8));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(onClick);

        TextView badge = new TextView(ctx);
        badge.setText(emojiIcon);
        badge.setTextSize(18);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.argb(42, Color.red(circleColor), Color.green(circleColor), Color.blue(circleColor)));
        bg.setStroke(dp(ctx, 1), Color.argb(120, Color.red(circleColor), Color.green(circleColor), Color.blue(circleColor)));
        badge.setBackground(bg);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(ctx, 42), dp(ctx, 42));
        blp.setMarginEnd(dp(ctx, 12));
        badge.setLayoutParams(blp);
        row.addView(badge);

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_primary));
        tvTitle.setTextSize(14.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        texts.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_secondary));
            tvSub.setTextSize(12);
            texts.addView(tvSub);
        }
        row.addView(texts);

        TextView arrow = new TextView(ctx);
        arrow.setText("‹");
        arrow.setTextColor(ContextCompat.getColor(ctx, R.color.kp_text_muted));
        arrow.setTextSize(20);
        arrow.setPadding(dp(ctx, 6), 0, 0, 0);
        row.addView(arrow);

        wrapper.addView(row);

        if (showDivider) {
            View div = new View(ctx);
            div.setBackgroundColor(ContextCompat.getColor(ctx, R.color.kp_stroke));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 1));
            dlp.setMarginStart(dp(ctx, 54));
            div.setLayoutParams(dlp);
            wrapper.addView(div);
        }
        return wrapper;
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

    /** Instant Test Keyboard modal triggered by the floating circular blue FAB on every screen. */
    public static void showKeyboardTestDialog(Context ctx) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(ctx, 18), dp(ctx, 16), dp(ctx, 18), dp(ctx, 12));
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView hint = createBodyText(ctx, "جرّب الثيم الحالي، الزخرفة الفورية (✦)، الاختصارات مثل (سع)، أو الحاسبة (15*3=)");
        box.addView(hint);

        EditText et = createInput(ctx, ctx.getString(R.string.test_keyboard_hint), false);
        et.setMinLines(3);
        et.setGravity(Gravity.TOP | Gravity.START);
        box.addView(et);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("⌨️ تجربة كيبورد برو")
                .setView(box)
                .setPositiveButton("تبديل الكيبورد", (d, w) -> {
                    InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showInputMethodPicker();
                })
                .setNegativeButton("إغلاق", null)
                .create();
        dialog.show();

        et.requestFocus();
        et.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
        }, 180);
    }

    public static int dp(Context ctx, int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics()));
    }
}
