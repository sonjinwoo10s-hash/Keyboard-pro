package com.keyboardpro.ime;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance Canvas keyboard view with ZERO allocations inside onDraw().
 * - Strictly unified full-width in both Portrait and Landscape by default (no split gap).
 * - Optional Split, Floating, One-Handed, and Landscape Right-Side Numpad modes (all OFF by default).
 * - 9 key press animations (none, shake, tilt, bounce, scale, press, jelly, spin, sink).
 * - 5 key lighting FX (none, glow, wave, spread, water).
 * - Swipe typing, Spacebar cursor/language swipe, Swipe-to-delete, Long-press popups, and 2-finger swipes.
 */
public class ProKeyboardView extends View {

    public interface OnKeyboardActionListener {
        void onText(String text);
        void onKeyCode(int code);
        void onLongPressKey(KeyboardLayoutData.KeyItem key, RectF keyBounds);
        void onCursorMove(int deltaSteps);
        void onSwipeDeleteWord();
        void onSwipeTypingResult(List<String> touchedLabels);
        void onTwoFingerSwipe(String direction);
        void onSpacebarLanguageSwipe(int directionDelta);
        void onKeyTouched(String matrixAction);
    }

    private static final class KeyRect {
        KeyboardLayoutData.KeyItem key;
        final RectF rect = new RectF();
    }

    private static final String[][] NUMPAD_KEYS = {
            {"7", "8", "9"},
            {"4", "5", "6"},
            {"1", "2", "3"},
            {"0", ".", "⌫"}
    };

    private OnKeyboardActionListener listener;
    private KeyboardTheme theme;
    private final KeyboardPrefs prefs;
    private final KeyboardVisualRenderer renderer = new KeyboardVisualRenderer();

    private final List<KeyRect> keyRects = new ArrayList<>(64);
    private List<List<KeyboardLayoutData.KeyItem>> currentRows = new ArrayList<>();

    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swipePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint popupBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint popupTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics reusableFm = new Paint.FontMetrics();
    private final Path swipePath = new Path();
    private final RectF popupRect = new RectF();
    private final RectF drawRect = new RectF();

    private KeyRect activeKey = null;
    private KeyRect lastAnimKey = null;
    private long pressAnimStartMs = 0L;
    private float fxCenterX = 0f, fxCenterY = 0f;
    private long fxStartMs = 0L;

    private float downX, downY;
    private float lastSpaceX;
    private boolean isSpaceSwiping = false;
    private boolean isDeleteSwiping = false;
    private boolean isSwipeTyping = false;
    private boolean longPressTriggered = false;
    private boolean isLandscape = false;
    private final List<String> swipeTouchedLabels = new ArrayList<>();

    // Two-finger gesture tracking
    private boolean twoFingerActive = false;
    private float twoFingerStartX, twoFingerStartY;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable longPressRunnable = () -> {
        if (activeKey != null && !isSpaceSwiping && !isSwipeTyping && !isDeleteSwiping) {
            longPressTriggered = true;
            if (listener != null) {
                listener.onKeyTouched("long_press");
                listener.onLongPressKey(activeKey.key, activeKey.rect);
            }
            invalidate();
        }
    };

    private final Runnable repeatDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (activeKey != null && activeKey.key.code == KeyboardLayoutData.CODE_BACKSPACE && !isDeleteSwiping) {
                if (listener != null) {
                    listener.onKeyTouched("delete_repeat");
                    listener.onKeyCode(KeyboardLayoutData.CODE_BACKSPACE);
                }
                handler.postDelayed(this, 65);
            }
        }
    };

    public ProKeyboardView(Context context) {
        this(context, null);
    }

    public ProKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.prefs = new KeyboardPrefs(context);
        this.theme = ThemeRegistry.getThemeById(context, prefs.getActiveThemeId());
        initPaints();
    }

    private void initPaints() {
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        hintPaint.setTextAlign(Paint.Align.RIGHT);
        hintPaint.setTextSize(sp(9f));

        swipePaint.setStyle(Paint.Style.STROKE);
        swipePaint.setStrokeCap(Paint.Cap.ROUND);
        swipePaint.setStrokeJoin(Paint.Join.ROUND);
        swipePaint.setStrokeWidth(dp(5f));

        popupTextPaint.setTextAlign(Paint.Align.CENTER);
        popupTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        popupTextPaint.setTextSize(sp(24f));
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
        this.listener = listener;
    }

    public void setCustomTypeface(Typeface tf) {
        Typeface finalTf = tf != null ? tf : Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        labelPaint.setTypeface(finalTf);
        popupTextPaint.setTypeface(finalTf);
        invalidate();
    }

    public void setTheme(KeyboardTheme theme) {
        this.theme = theme;
        renderer.releaseShaderCaches();
        invalidate();
    }

    public void setLandscape(boolean landscape) {
        this.isLandscape = landscape;
        computeKeyRects(getWidth(), getHeight());
        invalidate();
    }

    public void setRows(List<List<KeyboardLayoutData.KeyItem>> rows) {
        this.currentRows = rows != null ? rows : new ArrayList<>();
        computeKeyRects(getWidth(), getHeight());
        invalidate();
    }

    public void releaseCaches() {
        handler.removeCallbacksAndMessages(null);
        activeKey = null;
        renderer.releaseShaderCaches();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeKeyRects(w, h);
    }

    private void computeKeyRects(int viewW, int viewH) {
        keyRects.clear();
        if (viewW <= 0 || viewH <= 0 || currentRows.isEmpty()) return;

        float outerPadX = dp(4f);
        float outerPadY = dp(4f);
        float hGap = dp(4f);
        float vGap = dp(5f);

        float usableLeft = outerPadX;
        float usableWidth = viewW - outerPadX * 2f;

        // Optional One-Handed mode (82% width docked right for RTL)
        if (!isLandscape && prefs.isOneHandedModeEnabled()) {
            float targetW = usableWidth * 0.84f;
            usableLeft = viewW - outerPadX - targetW;
            usableWidth = targetW;
        } else if (prefs.isFloatingModeEnabled()) {
            float targetW = usableWidth * 0.88f;
            usableLeft = (viewW - targetW) / 2f;
            usableWidth = targetW;
        }

        // Optional Landscape Right-Side Numpad (OFF by default -> unified full-width!)
        boolean showNumpad = isLandscape && prefs.isLandscapeNumpadEnabled();
        float numpadWidth = showNumpad ? usableWidth * 0.24f : 0f;
        float mainAreaWidth = showNumpad ? (usableWidth - numpadWidth - hGap * 2f) : usableWidth;

        // Optional Split Mode (OFF by default -> strictly unified zero-gap!)
        boolean split = prefs.isSplitModeEnabled() && viewW >= dp(460f);
        float splitGap = split ? usableWidth * 0.14f : 0f;

        int rowCount = currentRows.size();
        float rowHeight = (viewH - outerPadY * 2f - vGap * (rowCount - 1)) / rowCount;

        for (int r = 0; r < rowCount; r++) {
            List<KeyboardLayoutData.KeyItem> row = currentRows.get(r);
            if (row.isEmpty()) continue;

            float totalWeight = 0f;
            for (KeyboardLayoutData.KeyItem k : row) totalWeight += k.weight;

            float totalGaps = hGap * (row.size() - 1) + (split ? splitGap : 0f);
            float availRowW = mainAreaWidth - totalGaps;
            float x = usableLeft;
            float top = outerPadY + r * (rowHeight + vGap);
            int midIdx = row.size() / 2;

            for (int c = 0; c < row.size(); c++) {
                if (split && c == midIdx) {
                    x += splitGap;
                }
                KeyboardLayoutData.KeyItem k = row.get(c);
                float kw = availRowW * (k.weight / totalWeight);
                KeyRect kr = new KeyRect();
                kr.key = k;
                kr.rect.set(x, top, x + kw, top + rowHeight);
                keyRects.add(kr);
                x += kw + hGap;
            }
        }

        // Append right-side Numpad keys if enabled in landscape
        if (showNumpad) {
            float numLeft = usableLeft + mainAreaWidth + hGap * 2f;
            float nRowH = (viewH - outerPadY * 2f - vGap * 3f) / 4f;
            float nColW = (numpadWidth - hGap * 2f) / 3f;
            for (int r = 0; r < 4; r++) {
                float top = outerPadY + r * (nRowH + vGap);
                for (int c = 0; c < 3; c++) {
                    String lbl = NUMPAD_KEYS[r][c];
                    KeyboardLayoutData.KeyItem item = "⌫".equals(lbl)
                            ? KeyboardLayoutData.KeyItem.special("⌫", KeyboardLayoutData.CODE_BACKSPACE, 1f, false)
                            : KeyboardLayoutData.KeyItem.ch(lbl, 1f, null, null);
                    KeyRect kr = new KeyRect();
                    kr.key = item;
                    float left = numLeft + c * (nColW + hGap);
                    kr.rect.set(left, top, left + nColW, top + nRowH);
                    keyRects.add(kr);
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (theme == null) return;
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        boolean forceBlur = prefs.isTransparentBlurEnabled();
        int blurStrength = prefs.getBlurStrength();
        boolean forceGlass = prefs.isGlassKeysForced();
        int keyOpacity = prefs.getKeyOpacityPercent();
        boolean squircle = prefs.isSquircleCorners();
        boolean depth3d = prefs.isKey3dDepth();
        boolean permGlow = prefs.isModifierPermanentGlow();
        String animType = prefs.getKeyAnimation();
        String lightingFx = prefs.getKeyLightingFx();

        renderer.drawBackground(canvas, w, h, theme, forceBlur, blurStrength, false);

        float cornerRadius = dp(prefs.getKeyCornerRadiusDp());
        float baseTextPx = sp(isLandscape ? prefs.getLandscapeKeyTextSp() : prefs.getPortraitKeyTextSp());
        float specialTextPx = baseTextPx * 0.80f;

        long now = System.currentTimeMillis();
        boolean keepAnimating = false;

        // Lighting FX layer behind keys
        if (!"none".equals(lightingFx) && fxStartMs > 0L) {
            float fxProg = (now - fxStartMs) / 280f;
            if (fxProg < 1f) {
                renderer.drawLightingFx(canvas, fxCenterX, fxCenterY, fxProg, lightingFx,
                        theme.accentColor, dp(68f));
                keepAnimating = true;
            } else {
                fxStartMs = 0L;
            }
        }

        for (int i = 0; i < keyRects.size(); i++) {
            KeyRect kr = keyRects.get(i);
            boolean pressed = (kr == activeKey);
            drawRect.set(kr.rect);

            // Apply 9 Key Press Animations
            boolean applyAnim = (kr == activeKey || (kr == lastAnimKey && (now - pressAnimStartMs) < 180L));
            float rotDeg = 0f;
            if (applyAnim && !"none".equals(animType)) {
                float p = Math.min(1f, (now - pressAnimStartMs) / 180f);
                if (p < 1f) keepAnimating = true;
                float wave = (float) Math.sin(p * Math.PI);
                switch (animType) {
                    case "press":
                        drawRect.inset(dp(1.5f) * wave, dp(1.5f) * wave);
                        break;
                    case "scale":
                        drawRect.inset(-dp(2.5f) * wave, -dp(2.5f) * wave);
                        break;
                    case "bounce":
                        drawRect.offset(0f, -dp(4.5f) * wave);
                        break;
                    case "sink":
                        drawRect.offset(0f, dp(3.5f) * wave);
                        break;
                    case "shake":
                        drawRect.offset((float) Math.sin(p * Math.PI * 4f) * dp(3f), 0f);
                        break;
                    case "tilt":
                        rotDeg = wave * 7f;
                        break;
                    case "jelly":
                        drawRect.inset(-dp(3f) * wave, dp(2f) * wave);
                        break;
                    case "spin":
                        rotDeg = p * 360f;
                        break;
                    default:
                        break;
                }
            }

            if (rotDeg != 0f) {
                canvas.save();
                canvas.rotate(rotDeg, drawRect.centerX(), drawRect.centerY());
            }

            renderer.drawKeyAdvanced(canvas, drawRect, cornerRadius, theme,
                    kr.key.isSpecial, kr.key.isAccent, pressed, forceGlass,
                    keyOpacity, squircle, depth3d, permGlow);

            // Secondary hint label (top-right corner)
            if (kr.key.secondaryLabel != null && !kr.key.secondaryLabel.isEmpty()) {
                hintPaint.setColor(Color.argb(145, Color.red(theme.keyTextColor),
                        Color.green(theme.keyTextColor), Color.blue(theme.keyTextColor)));
                canvas.drawText(kr.key.secondaryLabel, drawRect.right - dp(4f), drawRect.top + dp(11f), hintPaint);
            }

            // Primary key label
            boolean isWideLabel = kr.key.label.length() > 2;
            labelPaint.setTextSize(isWideLabel ? specialTextPx * 0.85f
                    : (kr.key.isSpecial ? specialTextPx : baseTextPx));
            labelPaint.setColor(kr.key.isAccent ? Color.WHITE
                    : (kr.key.isSpecial ? theme.keySpecialTextColor : theme.keyTextColor));
            labelPaint.getFontMetrics(reusableFm);
            float textY = drawRect.centerY() - (reusableFm.ascent + reusableFm.descent) / 2f;
            canvas.drawText(kr.key.label, drawRect.centerX(), textY, labelPaint);

            if (rotDeg != 0f) {
                canvas.restore();
            }
        }

        // Swipe trail
        if (isSwipeTyping && !swipePath.isEmpty()) {
            swipePaint.setColor(Color.argb(190, Color.red(theme.accentColor),
                    Color.green(theme.accentColor), Color.blue(theme.accentColor)));
            canvas.drawPath(swipePath, swipePaint);
        }

        // Floating key preview bubble on press
        if (activeKey != null && !activeKey.key.isSpecial
                && activeKey.key.code != KeyboardLayoutData.CODE_SPACE
                && !isSwipeTyping && !longPressTriggered) {
            float pw = Math.max(activeKey.rect.width() * 1.25f, dp(42f));
            float ph = activeKey.rect.height() * 1.15f;
            float cx = activeKey.rect.centerX();
            float left = Math.max(dp(4f), Math.min(w - pw - dp(4f), cx - pw / 2f));
            float bottom = activeKey.rect.top - dp(4f);
            float top = Math.max(dp(2f), bottom - ph);
            popupRect.set(left, top, left + pw, bottom);
            popupBgPaint.setColor(theme.accentColor);
            canvas.drawRoundRect(popupRect, dp(10f), dp(10f), popupBgPaint);
            popupTextPaint.setColor(Color.WHITE);
            popupTextPaint.getFontMetrics(reusableFm);
            canvas.drawText(activeKey.key.label, popupRect.centerX(),
                    popupRect.centerY() - (reusableFm.ascent + reusableFm.descent) / 2f, popupTextPaint);
        }

        if (keepAnimating) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        // Two-finger gesture detection
        if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() == 2) {
            twoFingerActive = true;
            twoFingerStartX = (event.getX(0) + event.getX(1)) * 0.5f;
            twoFingerStartY = (event.getY(0) + event.getY(1)) * 0.5f;
            handler.removeCallbacks(longPressRunnable);
            handler.removeCallbacks(repeatDeleteRunnable);
            activeKey = null;
            invalidate();
            return true;
        }
        if (twoFingerActive) {
            if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                float endX = event.getPointerCount() >= 2 ? (event.getX(0) + event.getX(1)) * 0.5f : event.getX(0);
                float endY = event.getPointerCount() >= 2 ? (event.getY(0) + event.getY(1)) * 0.5f : event.getY(0);
                float dx = endX - twoFingerStartX;
                float dy = endY - twoFingerStartY;
                float thresh = dp(42f);
                if (listener != null && (Math.abs(dx) > thresh || Math.abs(dy) > thresh)) {
                    if (Math.abs(dx) > Math.abs(dy)) {
                        listener.onTwoFingerSwipe(dx > 0 ? "right" : "left");
                    } else {
                        listener.onTwoFingerSwipe(dy > 0 ? "down" : "up");
                    }
                }
                twoFingerActive = false;
            }
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                downX = x;
                downY = y;
                lastSpaceX = x;
                isSpaceSwiping = false;
                isDeleteSwiping = false;
                isSwipeTyping = false;
                longPressTriggered = false;
                swipePath.reset();
                swipeTouchedLabels.clear();

                activeKey = findKeyAt(x, y);
                if (activeKey != null) {
                    lastAnimKey = activeKey;
                    pressAnimStartMs = System.currentTimeMillis();
                    fxCenterX = activeKey.rect.centerX();
                    fxCenterY = activeKey.rect.centerY();
                    fxStartMs = pressAnimStartMs;

                    if (listener != null) {
                        listener.onKeyTouched(resolveMatrixAction(activeKey.key));
                    }
                    if (activeKey.key.code == KeyboardLayoutData.CODE_BACKSPACE) {
                        handler.postDelayed(repeatDeleteRunnable, 340);
                    } else if (activeKey.key.code == KeyboardLayoutData.CODE_ENTER
                            || (activeKey.key.popupChars != null && activeKey.key.popupChars.length > 0)) {
                        handler.postDelayed(longPressRunnable, prefs.getLongPressDelayMs());
                    }
                    if (!activeKey.key.isSpecial && activeKey.key.code == 0) {
                        swipePath.moveTo(x, y);
                        swipeTouchedLabels.add(activeKey.key.output);
                    }
                }
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (activeKey == null) return true;
                float dx = x - downX;
                float dy = y - downY;

                // Spacebar horizontal swipe: switch language (next/previous) or cursor control
                if (activeKey.key.code == KeyboardLayoutData.CODE_SPACE) {
                    String mode = prefs.getSpacebarSwipeMode();
                    float swipeThreshold = dp(38f);
                    if ("lang".equals(mode)) {
                        float deltaX = x - lastSpaceX;
                        if (Math.abs(deltaX) >= swipeThreshold && Math.abs(dx) > Math.abs(dy)) {
                            isSpaceSwiping = true;
                            lastSpaceX = x;
                            int directionDelta = deltaX > 0 ? 1 : -1;
                            if (listener != null) {
                                listener.onKeyTouched("function");
                                listener.onSpacebarLanguageSwipe(directionDelta);
                            }
                        }
                    } else if ("cursor".equals(mode)) {
                        float stepPx = dp(18f);
                        float delta = x - lastSpaceX;
                        if (Math.abs(delta) >= stepPx) {
                            isSpaceSwiping = true;
                            int steps = (int) (delta / stepPx);
                            lastSpaceX = x;
                            if (listener != null) listener.onCursorMove(steps);
                        }
                    }
                    return true;
                }

                // Swipe-to-delete from Backspace
                if (activeKey.key.code == KeyboardLayoutData.CODE_BACKSPACE && prefs.isSwipeToDeleteEnabled()) {
                    if (Math.abs(dx) > dp(34f)) {
                        isDeleteSwiping = true;
                        handler.removeCallbacks(repeatDeleteRunnable);
                    }
                    return true;
                }

                // Swipe typing across letter keys
                if (prefs.isSwipeTypingEnabled() && !activeKey.key.isSpecial && activeKey.key.code == 0) {
                    if (Math.hypot(dx, dy) > dp(24f)) {
                        isSwipeTyping = true;
                        handler.removeCallbacks(longPressRunnable);
                        swipePath.lineTo(x, y);
                        KeyRect over = findKeyAt(x, y);
                        if (over != null && !over.key.isSpecial && over.key.code == 0) {
                            String lbl = over.key.output;
                            if (swipeTouchedLabels.isEmpty() || !swipeTouchedLabels.get(swipeTouchedLabels.size() - 1).equals(lbl)) {
                                swipeTouchedLabels.add(lbl);
                            }
                        }
                        invalidate();
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP: {
                handler.removeCallbacks(longPressRunnable);
                handler.removeCallbacks(repeatDeleteRunnable);

                if (activeKey != null) {
                    if (isDeleteSwiping) {
                        if (listener != null) {
                            listener.onKeyTouched("swipe_delete");
                            listener.onSwipeDeleteWord();
                        }
                    } else if (isSwipeTyping && swipeTouchedLabels.size() >= 2) {
                        if (listener != null) listener.onSwipeTypingResult(new ArrayList<>(swipeTouchedLabels));
                    } else if (!isSpaceSwiping && !longPressTriggered) {
                        if (listener != null) {
                            if (activeKey.key.code != 0) {
                                listener.onKeyCode(activeKey.key.code);
                            } else if (activeKey.key.output != null && !activeKey.key.output.isEmpty()) {
                                listener.onText(activeKey.key.output);
                            }
                        }
                    }
                }
                activeKey = null;
                isSwipeTyping = false;
                swipePath.reset();
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                handler.removeCallbacks(repeatDeleteRunnable);
                activeKey = null;
                isSwipeTyping = false;
                swipePath.reset();
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private String resolveMatrixAction(KeyboardLayoutData.KeyItem key) {
        if (key.code == KeyboardLayoutData.CODE_SPACE) return "space";
        if (key.code == KeyboardLayoutData.CODE_ENTER) return "enter";
        if (key.code == KeyboardLayoutData.CODE_BACKSPACE) return "delete";
        if (key.isSpecial) return "function";
        return "letters";
    }

    private KeyRect findKeyAt(float x, float y) {
        for (int i = 0; i < keyRects.size(); i++) {
            KeyRect kr = keyRects.get(i);
            if (kr.rect.contains(x, y)) return kr;
        }
        return null;
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }
}
