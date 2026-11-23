package com.keyboardpro.ime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Live mini-preview of the Arabic keyboard layout rendered on Canvas for theme cards.
 * Reuses all Paint, RectF, Path, FontMetrics, and KeyboardVisualRenderer objects
 * with ZERO object allocations inside onDraw().
 */
public class MiniKeyboardPreviewView extends View {

    private static final String[][] MINI_ARABIC_ROWS = {
            {"ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح"},
            {"ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م"},
            {"ئ", "ء", "ر", "لا", "ى", "ة", "و", "ز"}
    };

    private KeyboardTheme theme;
    private final KeyboardVisualRenderer renderer = new KeyboardVisualRenderer();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics reusableFm = new Paint.FontMetrics();

    private final Path clipPath = new Path();
    private final RectF clipBounds = new RectF();
    private final RectF reusableKeyRect = new RectF();
    private final RectF leftModRect = new RectF();
    private final RectF spaceRect = new RectF();
    private final RectF enterRect = new RectF();

    public MiniKeyboardPreviewView(Context context) {
        super(context);
        init();
    }

    public MiniKeyboardPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        lightKeyPaint.setColor(Color.WHITE);
        theme = ThemeRegistry.getBuiltInThemes().get(0);
    }

    public void setTheme(KeyboardTheme theme) {
        this.theme = theme;
        renderer.releaseShaderCaches();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (theme == null) return;
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float outerRadius = dp(12f);
        clipBounds.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(clipBounds, outerRadius, outerRadius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);

        renderer.drawBackground(canvas, w, h, theme, false, 45, true);

        float toolbarH = h * 0.14f;
        tbPaint.setColor(Color.argb(145,
                Color.red(theme.toolbarBgColor),
                Color.green(theme.toolbarBgColor),
                Color.blue(theme.toolbarBgColor)));
        canvas.drawRect(0, 0, w, toolbarH, tbPaint);

        dotPaint.setColor(theme.accentColor);
        float dotR = dp(2.2f);
        for (int i = 0; i < 4; i++) {
            canvas.drawCircle(dp(12f) + i * dp(11f), toolbarH / 2f, dotR, dotPaint);
        }

        float pad = dp(5f);
        float gap = dp(2.5f);
        float areaTop = toolbarH + dp(4f);
        float areaH = h - areaTop - pad;
        float rowH = (areaH - gap * 3f) / 4f;
        float keyCorner = dp(4f);
        textPaint.setTextSize(Math.max(dp(7.5f), rowH * 0.52f));

        for (int r = 0; r < 3; r++) {
            String[] row = MINI_ARABIC_ROWS[r];
            float keyW = (w - pad * 2f - gap * (row.length - 1)) / row.length;
            float top = areaTop + r * (rowH + gap);
            for (int c = 0; c < row.length; c++) {
                float left = pad + c * (keyW + gap);
                reusableKeyRect.set(left, top, left + keyW, top + rowH);
                boolean isLightSide = theme.backgroundType == KeyboardTheme.BackgroundType.AUTO_SPLIT
                        && reusableKeyRect.centerX() > w * 0.5f;

                if (isLightSide) {
                    canvas.drawRoundRect(reusableKeyRect, keyCorner, keyCorner, lightKeyPaint);
                    textPaint.setColor(0xFF0F172A);
                } else {
                    renderer.drawKey(canvas, reusableKeyRect, keyCorner, theme, false, false, false);
                    textPaint.setColor(theme.keyTextColor);
                }

                textPaint.getFontMetrics(reusableFm);
                canvas.drawText(row[c], reusableKeyRect.centerX(),
                        reusableKeyRect.centerY() - (reusableFm.ascent + reusableFm.descent) / 2f, textPaint);
            }
        }

        float bottomTop = areaTop + 3f * (rowH + gap);
        float sideW = (w - pad * 2f) * 0.18f;
        float spaceW = (w - pad * 2f) - sideW * 2f - gap * 2f;

        leftModRect.set(pad, bottomTop, pad + sideW, bottomTop + rowH);
        spaceRect.set(leftModRect.right + gap, bottomTop, leftModRect.right + gap + spaceW, bottomTop + rowH);
        enterRect.set(spaceRect.right + gap, bottomTop, w - pad, bottomTop + rowH);

        renderer.drawKey(canvas, leftModRect, keyCorner, theme, true, false, false);
        renderer.drawKey(canvas, spaceRect, keyCorner, theme, false, false, false);
        renderer.drawKey(canvas, enterRect, keyCorner, theme, true, false, false);

        textPaint.setColor(theme.keyTextColor);
        textPaint.setTextSize(Math.max(dp(6.5f), rowH * 0.42f));
        textPaint.getFontMetrics(reusableFm);
        canvas.drawText("العربية", spaceRect.centerX(),
                spaceRect.centerY() - (reusableFm.ascent + reusableFm.descent) / 2f, textPaint);

        canvas.restore();
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
