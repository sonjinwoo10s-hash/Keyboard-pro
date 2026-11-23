package com.keyboardpro.ime;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.keyboardpro.data.KeyboardPrefs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight, low-RAM custom Canvas keyboard view.
 * - Strictly NO-SPLIT full-width layout in both portrait and landscape (zero center gap).
 * - Independent Portrait & Landscape heights and key text scaling.
 * - ZERO object allocations inside onDraw() via pre-allocated Paint, RectF, and FontMetrics.
 * - Supports swipe typing trail, spacebar cursor slide, swipe-to-delete, and long-press popups.
 */
public class ProKeyboardView extends View {

    public interface OnKeyboardActionListener {
        void onKeyOutput(String text, int code);
        void onSpecialKey(int code);
        void onSwipeWord(String rawPathChars);
        void onCursorMove(int steps);
        void onSwipeDeleteWord();
        void onKeyFeedback();
    }

    private static class RenderedKey {
        final KeyboardLayoutData.KeySpec spec;
        final RectF rect;

        RenderedKey(KeyboardLayoutData.KeySpec spec, RectF rect) {
            this.spec = spec;
            this.rect = rect;
        }
    }

    private KeyboardPrefs prefs;
    private KeyboardTheme theme;
    private KeyboardLayoutData.KeyboardMode mode = KeyboardLayoutData.KeyboardMode.ARABIC;
    private boolean shifted = false;
    private OnKeyboardActionListener listener;

    private final KeyboardVisualRenderer renderer = new KeyboardVisualRenderer();
    private final List<RenderedKey> renderedKeys = new ArrayList<>(48);

    // Pre-allocated drawing objects (zero allocations in onDraw)
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint popupBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint popupTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swipeTrailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics reusableFm = new Paint.FontMetrics();
    private final RectF reusablePopRect = new RectF();
    private final RectF reusableBarRect = new RectF();
    private final RectF reusableCellRect = new RectF();

    private RenderedKey activeKey = null;
    private RenderedKey longPressKey = null;
    private int selectedAltIndex = 0;

    private float downX, downY;
    private float lastCursorX;
    private boolean isSpaceSliding = false;
    private boolean isDeleteSwiping = false;
    private boolean isSwipeTyping = false;
    private final Path swipePath = new Path();
    private final StringBuilder swipeChars = new StringBuilder();
    private String lastSwipedChar = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable longPressRunnable = () -> {
        if (activeKey != null && activeKey.spec.longPressAlts != null
                && activeKey.spec.longPressAlts.length > 0 && !isSwipeTyping) {
            longPressKey = activeKey;
            selectedAltIndex = 0;
            invalidate();
        }
    };

    private final Runnable repeatDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (activeKey != null && activeKey.spec.code == KeyboardLayoutData.CODE_DELETE && !isDeleteSwiping) {
                if (listener != null) {
                    listener.onSpecialKey(KeyboardLayoutData.CODE_DELETE);
                }
                handler.postDelayed(this, 65);
            }
        }
    };

    public ProKeyboardView(Context context) {
        super(context);
        init(context);
    }

    public ProKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        prefs = new KeyboardPrefs(context);
        theme = ThemeRegistry.getThemeById(context, prefs.getActiveThemeId());

        textPaint.setTextAlign(Paint.Align.CENTER);
        popupTextPaint.setTextAlign(Paint.Align.CENTER);

        swipeTrailPaint.setStyle(Paint.Style.STROKE);
        swipeTrailPaint.setStrokeCap(Paint.Cap.ROUND);
        swipeTrailPaint.setStrokeJoin(Paint.Join.ROUND);
        swipeTrailPaint.setStrokeWidth(dp(4.5f));
        reloadConfig();
    }

    public void setListener(OnKeyboardActionListener listener) {
        this.listener = listener;
    }

    public void setMode(KeyboardLayoutData.KeyboardMode mode) {
        this.mode = mode;
        this.shifted = false;
        computeKeyRects(getWidth(), getHeight());
        invalidate();
    }

    public KeyboardLayoutData.KeyboardMode getMode() {
        return mode;
    }

    public void toggleShift() {
        this.shifted = !this.shifted;
        computeKeyRects(getWidth(), getHeight());
        invalidate();
    }

    public void releaseCaches() {
        handler.removeCallbacks(longPressRunnable);
        handler.removeCallbacks(repeatDeleteRunnable);
        activeKey = null;
        longPressKey = null;
        swipePath.reset();
        swipeChars.setLength(0);
        renderer.releaseShaderCaches();
    }

    public void reloadConfig() {
        theme = ThemeRegistry.getThemeById(getContext(), prefs.getActiveThemeId());
        renderer.releaseShaderCaches();
        String customFont = prefs.getCustomUiFontPath();
        Typeface tf = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        if (customFont != null && !customFont.isEmpty()) {
            File f = new File(customFont);
            if (f.exists()) {
                try {
                    tf = Typeface.createFromFile(f);
                } catch (Exception ignored) {
                }
            }
        }
        textPaint.setTypeface(tf);
        popupTextPaint.setTypeface(tf);
        swipeTrailPaint.setColor(theme.accentColor);
        requestLayout();
        computeKeyRects(getWidth(), getHeight());
        invalidate();
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int fullWidth = MeasureSpec.getSize(widthMeasureSpec);
        int targetHeightDp = isLandscape() ? prefs.getLandscapeHeightDp() : prefs.getPortraitHeightDp();
        int targetHeightPx = (int) dp(targetHeightDp);
        setMeasuredDimension(fullWidth, targetHeightPx);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        renderer.releaseShaderCaches();
        computeKeyRects(w, h);
    }

    private void computeKeyRects(int width, int height) {
        renderedKeys.clear();
        if (width <= 0 || height <= 0) return;

        List<List<KeyboardLayoutData.KeySpec>> rows = KeyboardLayoutData.buildRows(
                mode,
                shifted,
                prefs.isNumbersRowEnabled(),
                prefs.isIphoneLayoutEnabled(),
                prefs.getCustomArabicLayoutJson()
        );

        int rowCount = rows.size();
        float padH = dp(isLandscape() ? 4f : 3f);
        float padV = dp(isLandscape() ? 3f : 4f);
        float keyGapX = dp(isLandscape() ? 4f : 3.5f);
        float keyGapY = dp(isLandscape() ? 4f : 5.5f);

        float usableH = height - padV * 2f;
        float rowH = (usableH - keyGapY * (rowCount - 1)) / rowCount;

        for (int r = 0; r < rowCount; r++) {
            List<KeyboardLayoutData.KeySpec> row = rows.get(r);
            float totalWeight = 0f;
            for (KeyboardLayoutData.KeySpec k : row) {
                totalWeight += k.weight;
            }

            float usableW = width - padH * 2f - keyGapX * (row.size() - 1);
            float x = padH;
            float top = padV + r * (rowH + keyGapY);

            for (KeyboardLayoutData.KeySpec k : row) {
                float keyW = (k.weight / totalWeight) * usableW;
                renderedKeys.add(new RenderedKey(k, new RectF(x, top, x + keyW, top + rowH)));
                x += keyW + keyGapX;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        renderer.drawBackground(
                canvas, w, h, theme,
                prefs.isTransparentBlurEnabled(),
                prefs.getBlurStrength(),
                false
        );

        float corner = dp(prefs.getKeyCornerRadiusDp());
        float keyTextPx = sp(isLandscape() ? prefs.getLandscapeKeyTextSp() : prefs.getPortraitKeyTextSp());
        boolean forceGlass = prefs.isGlassKeysForced();

        int count = renderedKeys.size();
        for (int i = 0; i < count; i++) {
            RenderedKey rk = renderedKeys.get(i);
            boolean pressed = (rk == activeKey);
            renderer.drawKey(canvas, rk.rect, corner, theme, rk.spec.isSpecial, pressed, forceGlass);

            boolean isSpace = rk.spec.code == KeyboardLayoutData.CODE_SPACE;
            textPaint.setTextSize(isSpace ? keyTextPx * 0.68f : (rk.spec.isSpecial ? keyTextPx * 0.85f : keyTextPx));
            textPaint.setColor(rk.spec.isSpecial ? theme.keySpecialTextColor : theme.keyTextColor);

            textPaint.getFontMetrics(reusableFm);
            float textY = rk.rect.centerY() - (reusableFm.ascent + reusableFm.descent) / 2f;
            canvas.drawText(rk.spec.label, rk.rect.centerX(), textY, textPaint);
        }

        if (isSwipeTyping) {
            swipeTrailPaint.setColor(theme.accentColor);
            canvas.drawPath(swipePath, swipeTrailPaint);
        }

        if (activeKey != null && !activeKey.spec.isSpecial
                && activeKey.spec.code != KeyboardLayoutData.CODE_SPACE
                && longPressKey == null && !isSwipeTyping) {
            float popW = Math.max(activeKey.rect.width() * 1.25f, dp(42f));
            float popH = activeKey.rect.height() * 1.15f;
            float left = Math.max(dp(4f), Math.min(w - popW - dp(4f), activeKey.rect.centerX() - popW / 2f));
            float top = Math.max(dp(2f), activeKey.rect.top - popH - dp(6f));
            reusablePopRect.set(left, top, left + popW, top + popH);

            popupBgPaint.setColor(theme.accentColor);
            canvas.drawRoundRect(reusablePopRect, corner + dp(3f), corner + dp(3f), popupBgPaint);

            popupTextPaint.setColor(0xFF07131A);
            popupTextPaint.setTextSize(keyTextPx * 1.3f);
            popupTextPaint.getFontMetrics(reusableFm);
            canvas.drawText(activeKey.spec.label, reusablePopRect.centerX(),
                    reusablePopRect.centerY() - (reusableFm.ascent + reusableFm.descent) / 2f, popupTextPaint);
        }

        if (longPressKey != null && longPressKey.spec.longPressAlts != null) {
            String[] alts = longPressKey.spec.longPressAlts;
            float itemW = dp(38f);
            float barW = itemW * alts.length + dp(8f);
            float barH = dp(44f);
            float left = Math.max(dp(4f), Math.min(w - barW - dp(4f), longPressKey.rect.centerX() - barW / 2f));
            float top = Math.max(dp(2f), longPressKey.rect.top - barH - dp(6f));
            reusableBarRect.set(left, top, left + barW, top + barH);

            popupBgPaint.setColor(0xFF182235);
            canvas.drawRoundRect(reusableBarRect, dp(12f), dp(12f), popupBgPaint);

            for (int i = 0; i < alts.length; i++) {
                reusableCellRect.set(left + dp(4f) + i * itemW, top + dp(4f),
                        left + dp(4f) + (i + 1) * itemW, top + barH - dp(4f));
                if (i == selectedAltIndex) {
                    popupBgPaint.setColor(theme.accentColor);
                    canvas.drawRoundRect(reusableCellRect, dp(8f), dp(8f), popupBgPaint);
                    popupTextPaint.setColor(0xFF07131A);
                } else {
                    popupTextPaint.setColor(Color.WHITE);
                }
                popupTextPaint.setTextSize(keyTextPx);
                popupTextPaint.getFontMetrics(reusableFm);
                canvas.drawText(alts[i], reusableCellRect.centerX(),
                        reusableCellRect.centerY() - (reusableFm.ascent + reusableFm.descent) / 2f, popupTextPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = x;
                downY = y;
                lastCursorX = x;
                isSpaceSliding = false;
                isDeleteSwiping = false;
                isSwipeTyping = false;
                longPressKey = null;
                swipePath.reset();
                swipeChars.setLength(0);
                lastSwipedChar = "";

                activeKey = findKeyAt(x, y);
                if (activeKey != null) {
                    if (listener != null) listener.onKeyFeedback();
                    if (activeKey.spec.code == KeyboardLayoutData.CODE_DELETE) {
                        handler.postDelayed(repeatDeleteRunnable, 360);
                    } else if (!activeKey.spec.isSpecial && activeKey.spec.code != KeyboardLayoutData.CODE_SPACE) {
                        swipePath.moveTo(x, y);
                        swipeChars.append(activeKey.spec.output);
                        lastSwipedChar = activeKey.spec.output;
                        handler.postDelayed(longPressRunnable, 340);
                    }
                    invalidate();
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (activeKey == null) return true;

                if (longPressKey != null && longPressKey.spec.longPressAlts != null) {
                    String[] alts = longPressKey.spec.longPressAlts;
                    float dx = x - longPressKey.rect.centerX();
                    int offset = Math.round(dx / dp(36f));
                    selectedAltIndex = Math.max(0, Math.min(alts.length - 1, (alts.length / 2) + offset));
                    invalidate();
                    return true;
                }

                if (activeKey.spec.code == KeyboardLayoutData.CODE_SPACE && prefs.isCursorControlEnabled()) {
                    float stepPx = dp(18f);
                    float dx = x - lastCursorX;
                    if (Math.abs(dx) >= stepPx) {
                        isSpaceSliding = true;
                        int steps = (int) (dx / stepPx);
                        lastCursorX = x;
                        if (listener != null) listener.onCursorMove(steps);
                    }
                    return true;
                }

                if (activeKey.spec.code == KeyboardLayoutData.CODE_DELETE && prefs.isSwipeToDeleteEnabled()) {
                    if (downX - x > dp(42f) && !isDeleteSwiping) {
                        isDeleteSwiping = true;
                        handler.removeCallbacks(repeatDeleteRunnable);
                        if (listener != null) listener.onSwipeDeleteWord();
                    }
                    return true;
                }

                if (prefs.isSwipeTypingEnabled() && !activeKey.spec.isSpecial
                        && activeKey.spec.code != KeyboardLayoutData.CODE_SPACE) {
                    float dist = (float) Math.hypot(x - downX, y - downY);
                    if (dist > dp(22f)) {
                        isSwipeTyping = true;
                        handler.removeCallbacks(longPressRunnable);
                        swipePath.lineTo(x, y);
                        RenderedKey hovered = findKeyAt(x, y);
                        if (hovered != null && !hovered.spec.isSpecial
                                && hovered.spec.code != KeyboardLayoutData.CODE_SPACE
                                && !hovered.spec.output.equals(lastSwipedChar)) {
                            swipeChars.append(hovered.spec.output);
                            lastSwipedChar = hovered.spec.output;
                        }
                        invalidate();
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                handler.removeCallbacks(longPressRunnable);
                handler.removeCallbacks(repeatDeleteRunnable);

                if (event.getActionMasked() == MotionEvent.ACTION_UP && activeKey != null && listener != null) {
                    if (longPressKey != null && longPressKey.spec.longPressAlts != null) {
                        String alt = longPressKey.spec.longPressAlts[selectedAltIndex];
                        listener.onKeyOutput(alt, alt.codePointAt(0));
                    } else if (isSwipeTyping && swipeChars.length() >= 2) {
                        listener.onSwipeWord(swipeChars.toString());
                    } else if (!isSpaceSliding && !isDeleteSwiping) {
                        if (activeKey.spec.isSpecial) {
                            listener.onSpecialKey(activeKey.spec.code);
                        } else {
                            listener.onKeyOutput(activeKey.spec.output, activeKey.spec.code);
                        }
                    }
                }

                activeKey = null;
                longPressKey = null;
                isSwipeTyping = false;
                isSpaceSliding = false;
                isDeleteSwiping = false;
                swipePath.reset();
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private RenderedKey findKeyAt(float x, float y) {
        int count = renderedKeys.size();
        for (int i = 0; i < count; i++) {
            RenderedKey rk = renderedKeys.get(i);
            if (rk.rect.contains(x, y)) return rk;
        }
        RenderedKey nearest = null;
        float minD = Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            RenderedKey rk = renderedKeys.get(i);
            float dx = x - rk.rect.centerX();
            float dy = y - rk.rect.centerY();
            float d = dx * dx + dy * dy;
            if (d < minD) {
                minD = d;
                nearest = rk;
            }
        }
        return nearest;
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }
}
