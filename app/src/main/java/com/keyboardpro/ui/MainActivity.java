package com.keyboardpro.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.keyboardpro.data.KeyboardPrefs;

/**
 * Main AMOLED Dark-first companion host featuring:
 * - 3-Tab Floating Bottom Navigation (1. Themes, 2. Customize, 3. Settings)
 * - Persistent Floating Circular Blue Keyboard Test FAB at bottom-left
 * - Edge-to-Edge WindowInsets handling
 */
public class MainActivity extends AppCompatActivity {

    private static final int TAB_HOME = 0;
    private static final int TAB_CUSTOMIZE = 1;
    private static final int TAB_SETTINGS = 2;

    private KeyboardPrefs prefs;
    private int currentTab = TAB_HOME;

    private LinearLayout tabHome, tabCustomize, tabSettings;
    private ImageView iconHome, iconCustomize, iconSettings;
    private TextView labelHome, labelCustomize, labelSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = new KeyboardPrefs(this);
        applySavedNightMode(prefs.getAppThemeMode());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setupEdgeToEdgeInsets();
        bindViews();
        maybeShowSplash();

        if (savedInstanceState == null) {
            selectTab(TAB_HOME);
        }
    }

    public static void applySavedNightMode(String mode) {
        if (KeyboardPrefs.APP_THEME_LIGHT.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (KeyboardPrefs.APP_THEME_AUTO.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    private void setupEdgeToEdgeInsets() {
        View topBar = findViewById(R.id.top_bar);
        View bottomWrapper = findViewById(R.id.floating_bottom_nav_wrapper);
        View fab = findViewById(R.id.fab_test_keyboard);
        int baseTop = topBar.getPaddingTop();
        int baseBottom = bottomWrapper.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topBar.setPadding(topBar.getPaddingLeft(), baseTop + bars.top,
                    topBar.getPaddingRight(), topBar.getPaddingBottom());
            bottomWrapper.setPadding(bottomWrapper.getPaddingLeft(), bottomWrapper.getPaddingTop(),
                    bottomWrapper.getPaddingRight(), baseBottom + bars.bottom);
            if (fab.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams flp =
                        (android.widget.FrameLayout.LayoutParams) fab.getLayoutParams();
                flp.bottomMargin = DetailUiKit.dp(this, 92) + bars.bottom;
                fab.setLayoutParams(flp);
            }
            return insets;
        });
    }

    private void bindViews() {
        tabHome = findViewById(R.id.nav_tab_home);
        tabCustomize = findViewById(R.id.nav_tab_customize);
        tabSettings = findViewById(R.id.nav_tab_settings);

        iconHome = findViewById(R.id.nav_icon_home);
        iconCustomize = findViewById(R.id.nav_icon_customize);
        iconSettings = findViewById(R.id.nav_icon_settings);

        labelHome = findViewById(R.id.nav_label_home);
        labelCustomize = findViewById(R.id.nav_label_customize);
        labelSettings = findViewById(R.id.nav_label_settings);

        tabHome.setOnClickListener(v -> selectTab(TAB_HOME));
        tabCustomize.setOnClickListener(v -> selectTab(TAB_CUSTOMIZE));
        tabSettings.setOnClickListener(v -> selectTab(TAB_SETTINGS));

        findViewById(R.id.btn_pro_crown).setOnClickListener(v -> openDetailSection(SettingsDetailActivity.SEC_PRO));
        findViewById(R.id.fab_test_keyboard).setOnClickListener(v -> DetailUiKit.showKeyboardTestDialog(this));
    }

    public void selectTab(int tabIndex) {
        currentTab = tabIndex;
        Fragment target;
        if (tabIndex == TAB_CUSTOMIZE) {
            target = new CustomizeFragment();
        } else if (tabIndex == TAB_SETTINGS) {
            target = new SettingsFragment();
        } else {
            target = new HomeFragment();
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, target)
                .commit();

        updateNavHighlight();
    }

    private void updateNavHighlight() {
        int active = ContextCompat.getColor(this, R.color.kp_primary);
        int inactive = ContextCompat.getColor(this, R.color.kp_text_secondary);

        styleNavTab(tabHome, iconHome, labelHome, currentTab == TAB_HOME, active, inactive);
        styleNavTab(tabCustomize, iconCustomize, labelCustomize, currentTab == TAB_CUSTOMIZE, active, inactive);
        styleNavTab(tabSettings, iconSettings, labelSettings, currentTab == TAB_SETTINGS, active, inactive);
    }

    private void styleNavTab(LinearLayout tab, ImageView icon, TextView label,
                             boolean selected, int activeColor, int inactiveColor) {
        tab.setBackgroundResource(selected ? R.drawable.bg_nav_active_pill : 0);
        label.setTextColor(selected ? activeColor : inactiveColor);
        icon.setColorFilter(selected ? activeColor : inactiveColor);
    }

    public void openDetailSection(String sectionKey) {
        Intent intent = new Intent(this, SettingsDetailActivity.class);
        intent.putExtra(SettingsDetailActivity.EXTRA_SECTION, sectionKey);
        startActivity(intent);
    }

    public void applyAppThemeAndRecreate(String mode) {
        prefs.setAppThemeMode(mode);
        applySavedNightMode(mode);
        recreate();
    }

    private void maybeShowSplash() {
        if (!prefs.isSplashScreenEnabled()) return;
        View splash = findViewById(R.id.splash_overlay);
        splash.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                splash.animate().alpha(0f).setDuration(300)
                        .withEndAction(() -> splash.setVisibility(View.GONE)).start(), 750);
    }
}
