package com.keyboardpro.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.BuildConfig;
import com.example.R;
import com.keyboardpro.data.KeyboardPrefs;

/**
 * Main host activity for Keyboard Pro (كيبورد برو).
 * Implements Dark Mode first, RTL layout, unified top bar with Gold Crown button,
 * and a floating translucent 3-tab bottom navigation bar (Home, Settings, Requests).
 */
public class MainActivity extends AppCompatActivity {

    public static final int TAB_HOME = 0;
    public static final int TAB_SETTINGS = 1;
    public static final int TAB_REQUESTS = 2;

    private static boolean splashShownThisProcess = false;

    private KeyboardPrefs prefs;
    private int currentTab = TAB_HOME;

    private LinearLayout tabHome, tabSettings, tabRequests;
    private ImageView iconHome, iconSettings, iconRequests;
    private TextView labelHome, labelSettings, labelRequests;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = new KeyboardPrefs(this);
        applySavedNightMode(prefs.getAppThemeMode());

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        applySystemBarInsets();
        setupTopBar();
        setupFloatingBottomNav();
        maybeShowSplashOverlay();

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt("current_tab", TAB_HOME);
        }
        switchToTab(currentTab);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_tab", currentTab);
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

    private void applySystemBarInsets() {
        View topBar = findViewById(R.id.top_bar);
        View navWrapper = findViewById(R.id.floating_bottom_nav_wrapper);
        int baseTop = topBar.getPaddingTop();
        int baseBottom = navWrapper.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            topBar.setPadding(topBar.getPaddingLeft(), baseTop + bars.top,
                    topBar.getPaddingRight(), topBar.getPaddingBottom());
            navWrapper.setPadding(navWrapper.getPaddingLeft(), navWrapper.getPaddingTop(),
                    navWrapper.getPaddingRight(), baseBottom + bars.bottom);
            return windowInsets;
        });
    }

    private void setupTopBar() {
        TextView subtitle = findViewById(R.id.tv_top_subtitle);
        subtitle.setText("Keyboard Pro • v" + BuildConfig.VERSION_NAME);

        findViewById(R.id.btn_pro_crown).setOnClickListener(v -> {
            Intent i = new Intent(this, SettingsDetailActivity.class);
            i.putExtra(SettingsDetailActivity.EXTRA_SECTION, SettingsDetailActivity.SEC_PRO);
            startActivity(i);
        });
    }

    private void setupFloatingBottomNav() {
        tabHome = findViewById(R.id.nav_tab_home);
        tabSettings = findViewById(R.id.nav_tab_settings);
        tabRequests = findViewById(R.id.nav_tab_requests);

        iconHome = findViewById(R.id.nav_icon_home);
        iconSettings = findViewById(R.id.nav_icon_settings);
        iconRequests = findViewById(R.id.nav_icon_requests);

        labelHome = findViewById(R.id.nav_label_home);
        labelSettings = findViewById(R.id.nav_label_settings);
        labelRequests = findViewById(R.id.nav_label_requests);

        tabHome.setOnClickListener(v -> switchToTab(TAB_HOME));
        tabSettings.setOnClickListener(v -> switchToTab(TAB_SETTINGS));
        tabRequests.setOnClickListener(v -> switchToTab(TAB_REQUESTS));
    }

    public void switchToTab(int tabIndex) {
        currentTab = tabIndex;
        Fragment target;
        if (tabIndex == TAB_SETTINGS) {
            target = new SettingsFragment();
        } else if (tabIndex == TAB_REQUESTS) {
            target = new RequestsFragment();
        } else {
            target = new HomeFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, target)
                .commit();

        updateNavSelection(tabIndex);
    }

    private void updateNavSelection(int index) {
        int activeColor = ContextCompat.getColor(this, R.color.kp_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.kp_text_secondary);

        styleTab(tabHome, iconHome, labelHome, index == TAB_HOME, activeColor, inactiveColor);
        styleTab(tabSettings, iconSettings, labelSettings, index == TAB_SETTINGS, activeColor, inactiveColor);
        styleTab(tabRequests, iconRequests, labelRequests, index == TAB_REQUESTS, activeColor, inactiveColor);
    }

    private void styleTab(LinearLayout container, ImageView icon, TextView label,
                          boolean active, int activeColor, int inactiveColor) {
        container.setBackgroundResource(active ? R.drawable.bg_nav_active_pill : android.R.color.transparent);
        int color = active ? activeColor : inactiveColor;
        icon.setImageTintList(ColorStateList.valueOf(color));
        label.setTextColor(color);
    }

    private void maybeShowSplashOverlay() {
        View splash = findViewById(R.id.splash_overlay);
        if (prefs.isSplashScreenEnabled() && !splashShownThisProcess) {
            splashShownThisProcess = true;
            splash.setVisibility(View.VISIBLE);
            splash.setAlpha(1f);
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    splash.animate().alpha(0f).setDuration(260)
                            .withEndAction(() -> splash.setVisibility(View.GONE))
                            .start(), 650);
        }
    }
}
