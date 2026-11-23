package com.keyboardpro.ime;

/**
 * Immutable keyboard visual theme model supporting solid, gradient, checkered,
 * transparent blur, and auto light/dark split backgrounds, plus flat, elevated,
 * neon outline, iPhone rounded, and glowing glass/crystal keys.
 */
public class KeyboardTheme {

    public enum BackgroundType {
        SOLID,
        GRADIENT,
        CHECKERED,
        TRANSPARENT_BLUR,
        AUTO_SPLIT
    }

    public enum KeyStyleType {
        FLAT,
        ELEVATED,
        GLASS_CRYSTAL,
        NEON_OUTLINE,
        IPHONE_ROUNDED
    }

    public enum Category {
        NEW,
        BASIC,
        PRO,
        MY_THEMES,
        USERS
    }

    public final String id;
    public final String nameAr;
    public final String nameEn;
    public final String colorTags;
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

    public KeyboardTheme(
            String id,
            String nameAr,
            String nameEn,
            String colorTags,
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
            int toolbarBgColor
    ) {
        this.id = id;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.colorTags = colorTags;
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
    }

    public boolean matchesQuery(String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase();
        return nameAr.toLowerCase().contains(q)
                || nameEn.toLowerCase().contains(q)
                || colorTags.toLowerCase().contains(q);
    }
}
