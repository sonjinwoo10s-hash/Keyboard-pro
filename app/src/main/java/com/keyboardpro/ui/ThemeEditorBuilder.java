package com.keyboardpro.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.keyboardpro.data.BitmapUtils;
import com.keyboardpro.data.KeyboardPrefs;
import com.keyboardpro.ime.KeyboardTheme;
import com.keyboardpro.ime.MiniKeyboardPreviewView;
import com.keyboardpro.ime.ThemeRegistry;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Modular builders for:
 * 1. Professional Theme Editor (photo background, blur/opacity sliders, color swatches,
 *    Extract Colors from Photo sampler, live MiniKeyboardPreviewView, Undo/Redo stack).
 * 2. Key Press FX & Animations (9 animations, 5 lighting FX, Squircle, 3D depth, permanent glow).
 */
public final class ThemeEditorBuilder {

    public interface ThemeEditorHost {
        void pickThemeBackgroundPhoto(PhotoCallback callback);
        void refreshScreen();
    }

    public interface PhotoCallback {
        void onPhotoSaved(String localPath);
    }

    private static final class EditorState {
        final int bg1, bg2, keyBg, accent;
        final boolean glass, blur;
        final String photoPath;

        EditorState(int bg1, int bg2, int keyBg, int accent, boolean glass, boolean blur, String photoPath) {
            this.bg1 = bg1;
            this.bg2 = bg2;
            this.keyBg = keyBg;
            this.accent = accent;
            this.glass = glass;
            this.blur = blur;
            this.photoPath = photoPath == null ? "" : photoPath;
        }
    }

    private static final int[] SWATCHES = {
            0xFF000000, 0xFF141416, 0xFF0B132B, 0xFF1B1035, 0xFF062118,
            0xFF2D1022, 0xFF4E6BFF, 0xFFA855F7, 0xFFEC4899, 0xFF10B981,
            0xFF06B6D4, 0xFFF59E0B, 0xFFEF4444, 0xFFFFFFFF
    };

    private ThemeEditorBuilder() {
    }

    public static void buildThemeEditor(Context ctx, LinearLayout root, KeyboardPrefs prefs, ThemeEditorHost host) {
        final EditorState[] cur = {new EditorState(
                0xFF0B132B, 0xFF142452, 0xFF1E2942, 0xFF4E6BFF, true, false, "")};
        final Deque<EditorState> undoStack = new ArrayDeque<>();
        final Deque<EditorState> redoStack = new ArrayDeque<>();

        // Live Preview Card
        LinearLayout prevCard = DetailUiKit.createCard(ctx);
        prevCard.addView(DetailUiKit.createSectionTitle(ctx, "معاينة الثيم المباشرة"));
        MiniKeyboardPreviewView preview = new MiniKeyboardPreviewView(ctx);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, DetailUiKit.dp(ctx, 155));
        preview.setLayoutParams(plp);
        prevCard.addView(preview);

        // Undo / Redo Row
        LinearLayout historyRow = new LinearLayout(ctx);
        historyRow.setOrientation(LinearLayout.HORIZONTAL);
        historyRow.setPadding(0, DetailUiKit.dp(ctx, 10), 0, 0);

        Runnable applyPreview = () -> {
            EditorState s = cur[0];
            KeyboardTheme.BackgroundType bgType = !s.photoPath.isEmpty()
                    ? KeyboardTheme.BackgroundType.PHOTO
                    : (s.blur ? KeyboardTheme.BackgroundType.TRANSPARENT_BLUR
                    : (s.bg1 == s.bg2 ? KeyboardTheme.BackgroundType.SOLID : KeyboardTheme.BackgroundType.GRADIENT));
            preview.setTheme(new KeyboardTheme(
                    "draft", "مسودة", "Draft", "", KeyboardTheme.FILTER_DARK,
                    KeyboardTheme.Category.MY_THEMES, false, bgType,
                    s.glass ? KeyboardTheme.KeyStyleType.GLASS_CRYSTAL : KeyboardTheme.KeyStyleType.ELEVATED,
                    s.bg1, s.bg2, s.keyBg, s.accent, s.accent,
                    Color.WHITE, Color.WHITE, s.glass ? s.accent : 0x44FFFFFF,
                    s.accent, s.bg1, s.photoPath));
        };

        Runnable pushUndo = () -> {
            undoStack.push(cur[0]);
            redoStack.clear();
        };

        TextView btnUndo = DetailUiKit.createChipOption(ctx, "↶ تراجع (Undo)", false, v -> {
            if (!undoStack.isEmpty()) {
                redoStack.push(cur[0]);
                cur[0] = undoStack.pop();
                applyPreview.run();
            } else {
                Toast.makeText(ctx, "لا توجد خطوات للتراجع", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ulp.setMarginEnd(DetailUiKit.dp(ctx, 6));
        btnUndo.setLayoutParams(ulp);
        historyRow.addView(btnUndo);

        TextView btnRedo = DetailUiKit.createChipOption(ctx, "↷ إعادة (Redo)", false, v -> {
            if (!redoStack.isEmpty()) {
                undoStack.push(cur[0]);
                cur[0] = redoStack.pop();
                applyPreview.run();
            } else {
                Toast.makeText(ctx, "لا توجد خطوات للإعادة", Toast.LENGTH_SHORT).show();
            }
        });
        btnRedo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        historyRow.addView(btnRedo);
        prevCard.addView(historyRow);
        root.addView(prevCard);

        // Name & Photo Card
        LinearLayout formCard = DetailUiKit.createCard(ctx);
        EditText etName = DetailUiKit.createInput(ctx, "اسم الثيم الجديد (مثال: ليلي أزرق)", false);
        formCard.addView(etName);

        formCard.addView(DetailUiKit.createPrimaryButton(ctx, "🖼️ اختيار صورة خلفية (Photo Picker)", v ->
                host.pickThemeBackgroundPhoto(localPath -> {
                    pushUndo.run();
                    EditorState s = cur[0];
                    cur[0] = new EditorState(s.bg1, s.bg2, s.keyBg, s.accent, s.glass, s.blur, localPath);
                    applyPreview.run();
                    Toast.makeText(ctx, "تم تحميل الصورة بنجاح — يمكنك استخراج ألوانها الآن", Toast.LENGTH_SHORT).show();
                })));

        formCard.addView(DetailUiKit.createChipOption(ctx, "🎨 استخراج الألوان تلقائياً من الصورة", true, v -> {
            if (cur[0].photoPath.isEmpty()) {
                Toast.makeText(ctx, "اختر صورة خلفية أولاً لاستخراج الألوان", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bmp = BitmapUtils.decodeSampledFromFile(cur[0].photoPath, 64, 64);
            if (bmp == null) return;
            int extractedAccent = extractDominantAccent(bmp);
            int extractedBg = Color.argb(255, Color.red(extractedAccent) / 5,
                    Color.green(extractedAccent) / 5, Color.blue(extractedAccent) / 5);
            bmp.recycle();
            pushUndo.run();
            EditorState s = cur[0];
            cur[0] = new EditorState(extractedBg, extractedBg, Color.argb(170, 22, 24, 32),
                    extractedAccent, true, s.blur, s.photoPath);
            applyPreview.run();
            Toast.makeText(ctx, "تم استخراج لوحة الألوان من الصورة ✨", Toast.LENGTH_SHORT).show();
        }));

        // Color Swatches
        formCard.addView(DetailUiKit.createBodyText(ctx, "لون الخلفية الأساسي:"));
        formCard.addView(buildColorRow(ctx, color -> {
            pushUndo.run();
            EditorState s = cur[0];
            cur[0] = new EditorState(color, s.bg2, s.keyBg, s.accent, s.glass, s.blur, "");
            applyPreview.run();
        }));

        formCard.addView(DetailUiKit.createBodyText(ctx, "لون المفاتيح:"));
        formCard.addView(buildColorRow(ctx, color -> {
            pushUndo.run();
            EditorState s = cur[0];
            cur[0] = new EditorState(s.bg1, s.bg2, color, s.accent, s.glass, s.blur, s.photoPath);
            applyPreview.run();
        }));

        formCard.addView(DetailUiKit.createBodyText(ctx, "اللون التفاعلي (اللمعان وزر الإدخال):"));
        formCard.addView(buildColorRow(ctx, color -> {
            pushUndo.run();
            EditorState s = cur[0];
            cur[0] = new EditorState(s.bg1, s.bg2, s.keyBg, color, s.glass, s.blur, s.photoPath);
            applyPreview.run();
        }));

        formCard.addView(DetailUiKit.createSwitchRow(ctx, "أزرار زجاج كريستالي لامع", "حدود مضيئة وانعكاس علوي",
                cur[0].glass, checked -> {
                    pushUndo.run();
                    EditorState s = cur[0];
                    cur[0] = new EditorState(s.bg1, s.bg2, s.keyBg, s.accent, checked, s.blur, s.photoPath);
                    applyPreview.run();
                }));

        formCard.addView(DetailUiKit.createSwitchRow(ctx, "خلفية شفافة مموهة", "تمويه حقيقي على Android 12+",
                cur[0].blur, checked -> {
                    pushUndo.run();
                    EditorState s = cur[0];
                    cur[0] = new EditorState(s.bg1, s.bg2, s.keyBg, s.accent, s.glass, checked, s.photoPath);
                    applyPreview.run();
                }));

        formCard.addView(DetailUiKit.createSliderRow(ctx, "قوة التمويه / التعتيم", 0, 100,
                prefs.getBlurStrength(), "%", prefs::setBlurStrength));
        formCard.addView(DetailUiKit.createSliderRow(ctx, "شفافية المفاتيح", 25, 100,
                prefs.getKeyOpacityPercent(), "%", prefs::setKeyOpacityPercent));

        formCard.addView(DetailUiKit.createPrimaryButton(ctx, "✅ حفظ وتطبيق الثيم الآن", v -> {
            EditorState s = cur[0];
            String name = etName.getText().toString().trim();
            KeyboardTheme saved = ThemeRegistry.saveCustomTheme(ctx,
                    name.isEmpty() ? "ثيمي المخصص" : name,
                    s.bg1, s.bg2, s.keyBg, s.accent, s.glass, s.blur, s.photoPath);
            prefs.setActiveThemeId(saved.id);
            Toast.makeText(ctx, "تم حفظ وتطبيق «" + saved.nameAr + "» بنجاح 🎉", Toast.LENGTH_LONG).show();
        }));

        root.addView(formCard);
        applyPreview.run();
    }

    public static void buildKeyFx(Context ctx, LinearLayout root, KeyboardPrefs prefs, ThemeEditorHost host) {
        root.addView(DetailUiKit.createSectionTitle(ctx, "حركات الضغط على المفاتيح (9 حركات)"));
        LinearLayout animCard = DetailUiKit.createCard(ctx);
        String curAnim = prefs.getKeyAnimation();
        String[][] anims = {
                {"none", "بدون حركة"},
                {"press", "ضغطة ناعمة (Press)"},
                {"scale", "تكبير فوري (Scale)"},
                {"bounce", "قفزة مرنة (Bounce)"},
                {"sink", "غوص لأسفل (Sink)"},
                {"shake", "اهتزاز جانبي (Shake)"},
                {"tilt", "ميلان خفيف (Tilt)"},
                {"jelly", "جيلي مطاطي (Jelly)"},
                {"spin", "دوران 360° (Spin)"}
        };
        for (String[] a : anims) {
            animCard.addView(DetailUiKit.createChipOption(ctx, a[1], a[0].equals(curAnim), v -> {
                prefs.setKeyAnimation(a[0]);
                host.refreshScreen();
            }));
        }
        root.addView(animCard);

        root.addView(DetailUiKit.createSectionTitle(ctx, "تأثيرات الإضاءة عند اللمس (5 تأثيرات)"));
        LinearLayout fxCard = DetailUiKit.createCard(ctx);
        String curFx = prefs.getKeyLightingFx();
        String[][] fxs = {
                {"none", "بدون إضاءة"},
                {"glow", "توهج نيون مركزي (Glow)"},
                {"wave", "موجة دائرية (Wave)"},
                {"spread", "انتشار ضوئي واسع (Spread)"},
                {"water", "تموّج قطرة ماء مزدوج (Water)"}
        };
        for (String[] f : fxs) {
            fxCard.addView(DetailUiKit.createChipOption(ctx, f[1], f[0].equals(curFx), v -> {
                prefs.setKeyLightingFx(f[0]);
                host.refreshScreen();
            }));
        }
        root.addView(fxCard);

        root.addView(DetailUiKit.createSectionTitle(ctx, "شكل وعمق الأزرار"));
        LinearLayout shapeCard = DetailUiKit.createCard(ctx);
        shapeCard.addView(DetailUiKit.createSwitchRow(ctx, "زوايا Squircle ناعمة", "انحناءات فائقة النعومة مثل iOS",
                prefs.isSquircleCorners(), prefs::setSquircleCorners));
        shapeCard.addView(DetailUiKit.createSwitchRow(ctx, "عمق ثلاثي الأبعاد 3D", "ظلال واقعية أسفل كل مفتاح",
                prefs.isKey3dDepth(), prefs::setKey3dDepth));
        shapeCard.addView(DetailUiKit.createSwitchRow(ctx, "توهج دائم لأزرار التحكم", "إبراز حدود المسح والإدخال والرموز بلون الثيم",
                prefs.isModifierPermanentGlow(), prefs::setModifierPermanentGlow));
        shapeCard.addView(DetailUiKit.createSliderRow(ctx, "نصف قطر زوايا المفاتيح", 0, 22,
                prefs.getKeyCornerRadiusDp(), "dp", prefs::setKeyCornerRadiusDp));
        shapeCard.addView(DetailUiKit.createSliderRow(ctx, "شفافية المفاتيح", 25, 100,
                prefs.getKeyOpacityPercent(), "%", prefs::setKeyOpacityPercent));
        root.addView(shapeCard);
    }

    private interface ColorPickListener {
        void onPick(int color);
    }

    private static View buildColorRow(Context ctx, ColorPickListener listener) {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(ctx);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, DetailUiKit.dp(ctx, 4), 0, DetailUiKit.dp(ctx, 10));

        for (int col : SWATCHES) {
            View dot = new View(ctx);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(col);
            bg.setStroke(DetailUiKit.dp(ctx, 2), 0x66FFFFFF);
            dot.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    DetailUiKit.dp(ctx, 36), DetailUiKit.dp(ctx, 36));
            lp.setMarginEnd(DetailUiKit.dp(ctx, 8));
            dot.setLayoutParams(lp);
            dot.setOnClickListener(v -> listener.onPick(col));
            row.addView(dot);
        }
        hsv.addView(row);
        return hsv;
    }

    private static int extractDominantAccent(Bitmap bmp) {
        long rSum = 0, gSum = 0, bSum = 0, count = 0;
        int w = bmp.getWidth(), h = bmp.getHeight();
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                int p = bmp.getPixel(x, y);
                int r = Color.red(p), g = Color.green(p), b = Color.blue(p);
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                if (max - min > 25) {
                    rSum += r; gSum += g; bSum += b; count++;
                }
            }
        }
        if (count == 0) return 0xFF4E6BFF;
        return Color.rgb((int) (rSum / count), (int) (gSum / count), (int) (bSum / count));
    }
}
