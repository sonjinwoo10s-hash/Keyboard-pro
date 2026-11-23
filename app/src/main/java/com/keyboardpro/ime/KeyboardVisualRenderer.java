package com.keyboardpro.ime;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Zero-allocation reusable Canvas renderer for keyboard backgrounds and keys.
 * Pre-allocates Paint, RectF, Path, and caches LinearGradient shaders outside onDraw()
 * to eliminate GC churn and keep IME frame rendering silky smooth at low RAM.
 */
public final class KeyboardVisualRenderer {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint altPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path splitPath = new Path();
    private final RectF tempGlossRect = new RectF();
    private final RectF tempShadowRect = new RectF();

    // Cached background shader metadata to avoid allocations on every frame
    private String cachedBgKey = "";
    private Shader cachedBgShader = null;
    private float cachedGlossH = -1f;
    private float cachedGlossTop = -1f;
    private Shader cachedGlossShader = null;

    public KeyboardVisualRenderer() {
        borderPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(2.5f);
        shadowPaint.setColor(Color.argb(75, 0, 0, 0));
        frostPaint.setColor(Color.argb(120, 12, 20, 34));
    }

    public void releaseShaderCaches() {
        cachedBgKey = "";
        cachedBgShader = null;
        cachedGlossShader = null;
        bgPaint.setShader(null);
        glossPaint.setShader(null);
    }

    public void drawBackground(
            Canvas canvas,
            float width,
            float height,
            KeyboardTheme theme,
            boolean forceBlur,
            int blurStrength,
            boolean isPreviewCard
    ) {
        KeyboardTheme.BackgroundType bgType = forceBlur
                ? KeyboardTheme.BackgroundType.TRANSPARENT_BLUR
                : theme.backgroundType;

        switch (bgType) {
            case GRADIENT: {
                ensureBgGradient(width, height, theme.id + "_grad_" + width + "x" + height,
                        theme.bgStartColor, theme.bgEndColor);
                bgPaint.setShader(cachedBgShader);
                canvas.drawRect(0f, 0f, width, height, bgPaint);
                break;
            }

            case CHECKERED: {
                bgPaint.setShader(null);
                bgPaint.setColor(theme.bgStartColor);
                canvas.drawRect(0f, 0f, width, height, bgPaint);
                altPaint.setColor(theme.bgEndColor);
                float cell = Math.max(18f, width / 12f);
                int rows = (int) Math.ceil(height / cell);
                int cols = (int) Math.ceil(width / cell);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if ((r + c) % 2 == 0) {
                            canvas.drawRect(c * cell, r * cell, (c + 1) * cell, (r + 1) * cell, altPaint);
                        }
                    }
                }
                break;
            }

            case TRANSPARENT_BLUR: {
                if (isPreviewCard) {
                    ensureBgGradient(width, height, "blur_prev_" + width + "x" + height,
                            Color.parseColor("#0F172A"), Color.parseColor("#1E3A5F"));
                    bgPaint.setShader(cachedBgShader);
                    canvas.drawRect(0f, 0f, width, height, bgPaint);

                    orbPaint.setColor(Color.argb(90, 0, 212, 255));
                    canvas.drawCircle(width * 0.25f, height * 0.35f, height * 0.32f, orbPaint);
                    orbPaint.setColor(Color.argb(85, 99, 102, 241));
                    canvas.drawCircle(width * 0.78f, height * 0.65f, height * 0.36f, orbPaint);

                    canvas.drawRect(0f, 0f, width, height, frostPaint);
                } else {
                    int alpha = Math.max(70, Math.min(220, 230 - (int) (blurStrength * 1.4f)));
                    int start = Color.argb(alpha,
                            Color.red(theme.bgStartColor),
                            Color.green(theme.bgStartColor),
                            Color.blue(theme.bgStartColor));
                    int end = Color.argb(alpha,
                            Color.red(theme.bgEndColor),
                            Color.green(theme.bgEndColor),
                            Color.blue(theme.bgEndColor));
                    ensureBgGradient(width, height, theme.id + "_blur_" + alpha + "_" + width + "x" + height,
                            start, end);
                    bgPaint.setShader(cachedBgShader);
                    canvas.drawRect(0f, 0f, width, height, bgPaint);
                }
                break;
            }

            case AUTO_SPLIT: {
                bgPaint.setShader(null);
                bgPaint.setColor(theme.bgStartColor);
                canvas.drawRect(0f, 0f, width, height, bgPaint);

                if (isPreviewCard) {
                    splitPath.reset();
                    splitPath.moveTo(width * 0.55f, 0f);
                    splitPath.lineTo(width, 0f);
                    splitPath.lineTo(width, height);
                    splitPath.lineTo(width * 0.38f, height);
                    splitPath.close();
                    altPaint.setColor(theme.bgEndColor);
                    canvas.drawPath(splitPath, altPaint);

                    dividerPaint.setColor(theme.accentColor);
                    canvas.drawLine(width * 0.55f, 0f, width * 0.38f, height, dividerPaint);
                }
                break;
            }

            case SOLID:
            default:
                bgPaint.setShader(null);
                bgPaint.setColor(theme.bgStartColor);
                canvas.drawRect(0f, 0f, width, height, bgPaint);
                break;
        }
    }

    public void drawKey(
            Canvas canvas,
            RectF rect,
            float cornerRadius,
            KeyboardTheme theme,
            boolean isSpecial,
            boolean isPressed,
            boolean forceGlass
    ) {
        KeyboardTheme.KeyStyleType style = forceGlass
                ? KeyboardTheme.KeyStyleType.GLASS_CRYSTAL
                : theme.keyStyleType;

        int fillColor = isPressed
                ? theme.keyPressedColor
                : (isSpecial ? theme.keySpecialBgColor : theme.keyBgColor);

        switch (style) {
            case GLASS_CRYSTAL: {
                fillPaint.setColor(fillColor);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);

                tempGlossRect.set(rect.left + 1.5f, rect.top + 1.5f,
                        rect.right - 1.5f, rect.top + rect.height() * 0.46f);
                if (cachedGlossShader == null || cachedGlossTop != tempGlossRect.top
                        || cachedGlossH != tempGlossRect.height()) {
                    cachedGlossTop = tempGlossRect.top;
                    cachedGlossH = tempGlossRect.height();
                    cachedGlossShader = new LinearGradient(
                            0f, tempGlossRect.top, 0f, tempGlossRect.bottom,
                            Color.argb(95, 255, 255, 255), Color.argb(8, 255, 255, 255),
                            Shader.TileMode.CLAMP);
                    glossPaint.setShader(cachedGlossShader);
                }
                canvas.drawRoundRect(tempGlossRect, cornerRadius, cornerRadius, glossPaint);

                borderPaint.setStrokeWidth(isPressed ? 2.8f : 1.6f);
                borderPaint.setColor(isPressed ? theme.accentColor : theme.keyBorderColor);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint);
                break;
            }

            case NEON_OUTLINE: {
                fillPaint.setColor(fillColor);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);

                borderPaint.setStrokeWidth(isPressed ? 2.6f : 1.5f);
                borderPaint.setColor(isSpecial || isPressed ? theme.accentColor : theme.keyBorderColor);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint);
                break;
            }

            case IPHONE_ROUNDED:
            case ELEVATED: {
                tempShadowRect.set(rect.left, rect.top + 2.5f, rect.right, rect.bottom + 2.5f);
                canvas.drawRoundRect(tempShadowRect, cornerRadius, cornerRadius, shadowPaint);

                fillPaint.setColor(fillColor);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);

                borderPaint.setStrokeWidth(1f);
                borderPaint.setColor(theme.keyBorderColor);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint);
                break;
            }

            case FLAT:
            default:
                fillPaint.setColor(fillColor);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
                break;
        }
    }

    private void ensureBgGradient(float w, float h, String key, int startColor, int endColor) {
        if (!key.equals(cachedBgKey) || cachedBgShader == null) {
            cachedBgKey = key;
            cachedBgShader = new LinearGradient(0f, 0f, w, h, startColor, endColor, Shader.TileMode.CLAMP);
        }
    }
}
