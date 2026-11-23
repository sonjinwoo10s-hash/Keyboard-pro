package com.keyboardpro.ime;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import com.keyboardpro.data.BitmapUtils;

/**
 * Low-RAM, zero-allocation-in-onDraw() Canvas visual renderer for Keyboard Pro.
 * Supports Crystal Glassmorphism, Mechanical 3D sculpted caps, Pencil Sketch outlines,
 * Neon borders, Squircle corners, Photo backgrounds (downsampled), permanent modifier glow,
 * and dynamic key press lighting FX (glow / wave / spread / water).
 */
public final class KeyboardVisualRenderer {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final RectF shadowRect = new RectF();
    private final RectF innerRect = new RectF();
    private final RectF specularRect = new RectF();
    private final Rect srcPhotoRect = new Rect();
    private final RectF dstPhotoRect = new RectF();

    private LinearGradient cachedBgGradient;
    private int cachedBgW = -1, cachedBgH = -1;
    private int cachedBgStart = 0, cachedBgEnd = 0;

    private Bitmap cachedPhotoBitmap;
    private String cachedPhotoPath = "";

    public KeyboardVisualRenderer() {
        strokePaint.setStyle(Paint.Style.STROKE);
        specularPaint.setStyle(Paint.Style.FILL);
        fxPaint.setStyle(Paint.Style.FILL);
    }

    public void releaseShaderCaches() {
        cachedBgGradient = null;
        cachedBgW = -1;
        cachedBgH = -1;
        bgPaint.setShader(null);
        if (cachedPhotoBitmap != null && !cachedPhotoBitmap.isRecycled()) {
            cachedPhotoBitmap.recycle();
        }
        cachedPhotoBitmap = null;
        cachedPhotoPath = "";
    }

    public void drawBackground(Canvas canvas, float width, float height, KeyboardTheme theme,
                               boolean forceBlur, int blurStrength, boolean isPreview) {
        int wInt = (int) width;
        int hInt = (int) height;
        if (wInt <= 0 || hInt <= 0) return;

        if (theme.backgroundType == KeyboardTheme.BackgroundType.PHOTO && !theme.photoBgPath.isEmpty()) {
            ensurePhotoLoaded(theme.photoBgPath, Math.min(wInt, 720), Math.min(hInt, 420));
            if (cachedPhotoBitmap != null && !cachedPhotoBitmap.isRecycled()) {
                srcPhotoRect.set(0, 0, cachedPhotoBitmap.getWidth(), cachedPhotoBitmap.getHeight());
                dstPhotoRect.set(0, 0, width, height);
                canvas.drawBitmap(cachedPhotoBitmap, srcPhotoRect, dstPhotoRect, bitmapPaint);
                // Scrim for legibility proportional to blurStrength
                int scrimAlpha = Math.max(40, Math.min(180, (int) (blurStrength * 1.6f)));
                bgPaint.setShader(null);
                bgPaint.setColor(Color.argb(scrimAlpha, 0, 0, 0));
                canvas.drawRect(0, 0, width, height, bgPaint);
                return;
            }
        }

        if (theme.backgroundType == KeyboardTheme.BackgroundType.AUTO_SPLIT && isPreview) {
            bgPaint.setShader(null);
            bgPaint.setColor(theme.bgStartColor);
            canvas.drawRect(0, 0, width * 0.5f, height, bgPaint);
            bgPaint.setColor(theme.bgEndColor);
            canvas.drawRect(width * 0.5f, 0, width, height, bgPaint);
            return;
        }

        if (theme.backgroundType == KeyboardTheme.BackgroundType.CHECKERED) {
            bgPaint.setShader(null);
            bgPaint.setColor(theme.bgStartColor);
            canvas.drawRect(0, 0, width, height, bgPaint);
            checkerPaint.setColor(theme.bgEndColor);
            float cell = Math.max(16f, width / 14f);
            for (float y = 0; y < height; y += cell) {
                int rowIdx = (int) (y / cell);
                for (float x = 0; x < width; x += cell) {
                    int colIdx = (int) (x / cell);
                    if ((rowIdx + colIdx) % 2 == 0) {
                        canvas.drawRect(x, y, x + cell, y + cell, checkerPaint);
                    }
                }
            }
            return;
        }

        if (theme.backgroundType == KeyboardTheme.BackgroundType.TRANSPARENT_BLUR || forceBlur) {
            int alpha = isPreview ? 220 : Math.max(90, Math.min(235, 255 - (blurStrength * 140 / 100)));
            int start = Color.argb(alpha, Color.red(theme.bgStartColor), Color.green(theme.bgStartColor), Color.blue(theme.bgStartColor));
            int end = Color.argb(alpha, Color.red(theme.bgEndColor), Color.green(theme.bgEndColor), Color.blue(theme.bgEndColor));
            ensureBgGradient(wInt, hInt, start, end);
            bgPaint.setShader(cachedBgGradient);
            canvas.drawRect(0, 0, width, height, bgPaint);
            return;
        }

        if (theme.backgroundType == KeyboardTheme.BackgroundType.GRADIENT) {
            ensureBgGradient(wInt, hInt, theme.bgStartColor, theme.bgEndColor);
            bgPaint.setShader(cachedBgGradient);
            canvas.drawRect(0, 0, width, height, bgPaint);
            return;
        }

        bgPaint.setShader(null);
        bgPaint.setColor(theme.bgStartColor);
        canvas.drawRect(0, 0, width, height, bgPaint);
    }

    public void drawKey(Canvas canvas, RectF rect, float radius, KeyboardTheme theme,
                        boolean isSpecial, boolean isPressed, boolean forceGlass) {
        drawKeyAdvanced(canvas, rect, radius, theme, isSpecial, false, isPressed, forceGlass,
                92, true, true, false);
    }

    public void drawKeyAdvanced(Canvas canvas, RectF rect, float radius, KeyboardTheme theme,
                                boolean isSpecial, boolean isAccent, boolean isPressed,
                                boolean forceGlass, int keyOpacityPct, boolean squircle,
                                boolean depth3d, boolean permanentModGlow) {
        KeyboardTheme.KeyStyleType style = forceGlass
                ? KeyboardTheme.KeyStyleType.GLASS_CRYSTAL : theme.keyStyleType;

        float r = squircle ? Math.min(radius * 1.25f, Math.min(rect.width(), rect.height()) * 0.42f) : radius;
        int baseColor = isPressed
                ? theme.keyPressedColor
                : (isAccent ? theme.accentColor : (isSpecial ? theme.keySpecialBgColor : theme.keyBgColor));

        int alphaScale = Math.max(25, Math.min(100, keyOpacityPct));
        int finalAlpha = (Color.alpha(baseColor) * alphaScale) / 100;
        int fillColor = Color.argb(finalAlpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));

        // 3D depth / drop shadow
        if (depth3d && style != KeyboardTheme.KeyStyleType.FLAT) {
            shadowRect.set(rect.left, rect.top + 3.0f, rect.right, rect.bottom + 3.0f);
            shadowPaint.setColor(style == KeyboardTheme.KeyStyleType.MECHANICAL_3D ? 0x66000000 : 0x3A000000);
            canvas.drawRoundRect(shadowRect, r, r, shadowPaint);
        }

        // Permanent glowing border on modifier / accent keys
        if (permanentModGlow && (isSpecial || isAccent)) {
            strokePaint.setStrokeWidth(2.6f);
            strokePaint.setColor(Color.argb(135, Color.red(theme.accentColor),
                    Color.green(theme.accentColor), Color.blue(theme.accentColor)));
            canvas.drawRoundRect(rect, r, r, strokePaint);
        }

        // Main key surface
        fillPaint.setColor(fillColor);
        canvas.drawRoundRect(rect, r, r, fillPaint);

        if (style == KeyboardTheme.KeyStyleType.MECHANICAL_3D) {
            // Sculpted top keycap bevel
            float inset = Math.max(2.5f, rect.height() * 0.08f);
            innerRect.set(rect.left + inset, rect.top + inset * 0.6f, rect.right - inset, rect.bottom - inset * 1.35f);
            specularPaint.setColor(isPressed ? 0x22FFFFFF : 0x28FFFFFF);
            canvas.drawRoundRect(innerRect, Math.max(2f, r - 2f), Math.max(2f, r - 2f), specularPaint);
            strokePaint.setStrokeWidth(2.0f);
            strokePaint.setColor(theme.keyBorderColor);
            canvas.drawRoundRect(rect, r, r, strokePaint);
        } else if (style == KeyboardTheme.KeyStyleType.GLASS_CRYSTAL) {
            // Top crystal specular highlight + luminous border
            specularRect.set(rect.left + 2f, rect.top + 1.5f, rect.right - 2f, rect.top + rect.height() * 0.44f);
            specularPaint.setColor(isPressed ? 0x55FFFFFF : 0x36FFFFFF);
            canvas.drawRoundRect(specularRect, r, r, specularPaint);

            strokePaint.setStrokeWidth(isPressed ? 2.8f : 1.8f);
            strokePaint.setColor(isPressed ? theme.accentColor : theme.keyBorderColor);
            canvas.drawRoundRect(rect, r, r, strokePaint);
        } else if (style == KeyboardTheme.KeyStyleType.NEON_OUTLINE) {
            strokePaint.setStrokeWidth(isPressed ? 3.2f : 2.0f);
            strokePaint.setColor(isAccent || isPressed ? theme.accentColor : theme.keyBorderColor);
            canvas.drawRoundRect(rect, r, r, strokePaint);
        } else if (style == KeyboardTheme.KeyStyleType.SKETCH_PENCIL) {
            strokePaint.setStrokeWidth(2.0f);
            strokePaint.setColor(theme.keyBorderColor);
            canvas.drawRoundRect(rect, r * 0.6f, r * 0.6f, strokePaint);
            innerRect.set(rect.left + 1.5f, rect.top + 1.5f, rect.right - 1.5f, rect.bottom - 1.5f);
            strokePaint.setStrokeWidth(1.0f);
            strokePaint.setColor(Color.argb(110, Color.red(theme.keyBorderColor),
                    Color.green(theme.keyBorderColor), Color.blue(theme.keyBorderColor)));
            canvas.drawRoundRect(innerRect, r * 0.5f, r * 0.5f, strokePaint);
        } else {
            strokePaint.setStrokeWidth(1.2f);
            strokePaint.setColor(theme.keyBorderColor);
            canvas.drawRoundRect(rect, r, r, strokePaint);
        }
    }

    public void drawLightingFx(Canvas canvas, float cx, float cy, float progress,
                               String fxType, int accentColor, float maxRadius) {
        if (fxType == null || "none".equals(fxType) || progress <= 0f || progress >= 1f) return;
        int alpha = (int) ((1f - progress) * 145);
        int col = Color.argb(alpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));

        switch (fxType) {
            case "wave":
            case "water":
                fxPaint.setStyle(Paint.Style.STROKE);
                fxPaint.setStrokeWidth("water".equals(fxType) ? 4.5f : 3f);
                fxPaint.setColor(col);
                canvas.drawCircle(cx, cy, maxRadius * (0.25f + progress * 0.9f), fxPaint);
                if ("water".equals(fxType) && progress > 0.2f) {
                    fxPaint.setStrokeWidth(2.2f);
                    canvas.drawCircle(cx, cy, maxRadius * (progress * 0.6f), fxPaint);
                }
                fxPaint.setStyle(Paint.Style.FILL);
                break;
            case "spread":
                fxPaint.setStyle(Paint.Style.FILL);
                fxPaint.setColor(col);
                canvas.drawCircle(cx, cy, maxRadius * (0.3f + progress * 1.15f), fxPaint);
                break;
            case "glow":
            default:
                fxPaint.setStyle(Paint.Style.FILL);
                fxPaint.setColor(col);
                canvas.drawCircle(cx, cy, maxRadius * (0.45f + progress * 0.35f), fxPaint);
                break;
        }
    }

    private void ensureBgGradient(int w, int h, int startColor, int endColor) {
        if (cachedBgGradient == null || cachedBgW != w || cachedBgH != h
                || cachedBgStart != startColor || cachedBgEnd != endColor) {
            cachedBgW = w;
            cachedBgH = h;
            cachedBgStart = startColor;
            cachedBgEnd = endColor;
            cachedBgGradient = new LinearGradient(0, 0, w, h, startColor, endColor, Shader.TileMode.CLAMP);
        }
    }

    private void ensurePhotoLoaded(String path, int reqW, int reqH) {
        if (path.equals(cachedPhotoPath) && cachedPhotoBitmap != null && !cachedPhotoBitmap.isRecycled()) {
            return;
        }
        if (cachedPhotoBitmap != null && !cachedPhotoBitmap.isRecycled()) {
            cachedPhotoBitmap.recycle();
        }
        cachedPhotoPath = path;
        cachedPhotoBitmap = BitmapUtils.decodeSampledFromFile(path, Math.max(64, reqW), Math.max(64, reqH));
    }
}
