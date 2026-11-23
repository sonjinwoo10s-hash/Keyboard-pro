package com.keyboardpro.ime;

/**
 * Immutable keyboard visual theme model supporting 40+ presets across Glass, Material,
 * Colorful, Dark, Light, and Mechanical filter groups, plus custom photo backgrounds,
 * Squircle corners, 3D key depth, and Crystal Glassmorphism rendering.
 */
public class KeyboardTheme {

    public enum BackgroundType {
        SOLID,
        GRADIENT,
        CHECKERED,
        TRANSPARENT_BLUR,
        AUTO_SPLIT,
        PHOTO
    }

    public enum KeyStyleType {
        FLAT,
        ELEVATED,
        GLASS_CRYSTAL,
        NEON_OUTLINE,
        IPHONE_ROUNDED,
        MECHANICAL_3D,
        SKETCH_PENCIL
    }

    public enum Category {
        NEW,
        BASIC,
        PRO,
        MY_THEMES,
        USERS
    }

    public static final String FILTER_ALL = "all";
    public static final String FILTER_GLASS = "glass";
    public static final String FILTER_MATERIAL = "material";
    public static final String FILTER_COLORFUL = "colorful";
    public static final String FILTER_DARK = "dark";
    public static final String FILTER_LIGHT = "light";
    public static final String FILTER_MECHANICAL = "mechanical";

    public final String id;
    public final String nameAr;
    public final String nameEn;
    public final String colorTags;
    public final String filterGroup;
    public final Category category;
    public final boolean isPro;
    public final BackgroundType backgroundType;
    public final KeyStyleType keyStyleType;

    public final int bgStartColor;
    public final int bgEndColor;
    public final int keyBgColor;
    public final int keySpecialBgColor;
    public final int keyPressedColor;
    public final int keyTextColor;
    public final int keySpecialTextColor;
    public final int keyBorderColor;
    public final int accentColor;
    public final int toolbarBgColor;
    public final String photoBgPath;

    public KeyboardTheme(
            String id,
            String nameAr,
            String nameEn,
            String colorTags,
            String filterGroup,
            Category category,
            boolean isPro,
            BackgroundType backgroundType,
            KeyStyleType keyStyleType,
            int bgStartColor,
            int bgEndColor,
            int keyBgColor,
            int keySpecialBgColor,
            int keyPressedColor,
            int keyTextColor,
            int keySpecialTextColor,
            int keyBorderColor,
            int accentColor,
            int toolbarBgColor,
            String photoBgPath
    ) {
        this.id = id;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.colorTags = colorTags;
        this.filterGroup = filterGroup;
        this.category = category;
        this.isPro = isPro;
        this.backgroundType = backgroundType;
        this.keyStyleType = keyStyleType;
        this.bgStartColor = bgStartColor;
        this.bgEndColor = bgEndColor;
        this.keyBgColor = keyBgColor;
        this.keySpecialBgColor = keySpecialBgColor;
        this.keyPressedColor = keyPressedColor;
        this.keyTextColor = keyTextColor;
        this.keySpecialTextColor = keySpecialTextColor;
        this.keyBorderColor = keyBorderColor;
        this.accentColor = accentColor;
        this.toolbarBgColor = toolbarBgColor;
        this.photoBgPath = photoBgPath == null ? "" : photoBgPath;
    }

    public boolean matchesQuery(String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase();
        return nameAr.toLowerCase().contains(q)
                || nameEn.toLowerCase().contains(q)
                || colorTags.toLowerCase().contains(q);
    }

    public boolean matchesFilter(String filter) {
        if (filter == null || FILTER_ALL.equals(filter)) return true;
        return filter.equals(filterGroup);
    }
}
