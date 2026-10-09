package io.github.lorenzolubrano.portafuori

import android.app.ActivityOptions
import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lorenzolubrano.portafuori.data.StyleId
import io.github.lorenzolubrano.portafuori.data.StylePrefs
import io.github.lorenzolubrano.portafuori.data.ThemeMode
import io.github.lorenzolubrano.portafuori.reminders.Notifications
import io.github.lorenzolubrano.portafuori.ui.AppRoot
import io.github.lorenzolubrano.portafuori.ui.MainViewModel
import io.github.lorenzolubrano.portafuori.ui.theme.PortafuoriTheme
import io.github.lorenzolubrano.portafuori.ui.theme.Styles
import io.github.lorenzolubrano.portafuori.ui.theme.lightBarIcons
import io.github.lorenzolubrano.portafuori.ui.Screen

// Same scrims androidx.activity uses by default for 3-button navigation
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

private const val UI_PREFS = "ui"
private const val KEY_THEME = "theme"

open class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    // In-app theme once known (null = follow the system). The bar styles read it every time they
    // are applied: enableEdgeToEdge keeps the styles of its FIRST call and re-applies them on every
    // configuration change that doesn't recreate the activity, so they must never capture a value.
    private var darkBars: Boolean? = null
    private val statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark(it) }
    private val navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { isDark(it) }

    private fun isDark(res: Resources) =
        darkBars ?: (res.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

    // The theme lives in DataStore, which is read asynchronously. A synchronous copy lets the very
    // first frame (window background, bar icons) use the in-app theme instead of flashing the system one.
    private val uiPrefs by lazy { getSharedPreferences(UI_PREFS, MODE_PRIVATE) }
    private val cachedTheme by lazy {
        uiPrefs.getString(KEY_THEME, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }
    private val cachedStyle by lazy { StylePrefs.cached(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (javaClass != StyleActivities.of(cachedStyle)) {
            // Not this style's activity: hand the intent over without drawing. The starting window
            // already showing moves to the new activity.
            super.onCreate(savedInstanceState)
            val target = StyleActivities.of(cachedStyle)
            val launch = if (StyleActivities.carriesLaunchIntent(restored = savedInstanceState != null)) intent else Intent()
            val next = Intent(launch).setClass(this, target).setFlags(StyleActivities.forwardFlags(launch.action, launch.flags))
            startActivity(next, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle())
            finish()
            return
        }
        val splash = installSplashScreen()
        darkBars = if (lightBarIcons(Styles.of(cachedStyle, this), dark = false)) true else when (cachedTheme) {
            ThemeMode.SYSTEM -> null
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        enableEdgeToEdge(statusBarStyle, navigationBarStyle)
        applyWindowBackground(cachedTheme, cachedStyle)
        super.onCreate(savedInstanceState)
        // Linee's activity keeps the system's own Back (and its animation home). Registered before the content,
        // so the app's own Back handling always comes first.
        if (javaClass != MainActivity::class.java) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (StyleActivities.backKeepsInBackground(javaClass, isTaskRoot)) {
                        moveTaskToBack(true)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
        }
        splash.setKeepOnScreenCondition { !vm.state.value.loaded }
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            // Until the settings arrive, the cached copy is the best guess
            val theme = if (state.loaded) state.settings.theme else cachedTheme
            val styleId = if (state.loaded) state.settings.style else cachedStyle
            val style = remember(styleId) { Styles.of(styleId, this) }
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // System bar icons follow the in-app theme and the style's shell, not only the system theme
            val lightIcons = lightBarIcons(style, dark)
            DisposableEffect(lightIcons) {
                darkBars = lightIcons
                enableEdgeToEdge(statusBarStyle, navigationBarStyle)
                onDispose {}
            }
            LaunchedEffect(state.loaded, state.settings.theme, state.settings.style) {
                if (state.loaded) rememberLook(state.settings.theme, state.settings.style)
            }
            PortafuoriTheme(theme, style) { AppRoot(vm) }
        }
    }

    /**
     * Keeps the synchronous copies and the window background in line with the in-app theme and style, and from
     * Android 12 tells the system the app's night mode. The system remembers it and uses it for the
     * splash and the starting windows it draws before the app runs (e.g. reopening from a notification
     * after the process was killed), which setSplashScreenTheme does not cover.
     */
    private fun rememberLook(mode: ThemeMode, style: StyleId) {
        if (uiPrefs.getString(KEY_THEME, null) != mode.name) uiPrefs.edit { putString(KEY_THEME, mode.name) }
        StylePrefs.remember(this, style)
        applyWindowBackground(mode, style)
        // the system's own splash, from Android 13, in the colours of the style
        if (Build.VERSION.SDK_INT >= 33) {
            splashScreen.setSplashScreenTheme(
                when (style) {
                    StyleId.LINEE -> R.style.Theme_App_Starting
                    StyleId.ORIGINALE -> R.style.Theme_App_Starting_Originale
                    StyleId.ANDROID -> R.style.Theme_App_Starting_Android
                },
            )
        }
        if (StyleActivities.reopens(javaClass, style, finishing = isFinishing)) {
            // A new style lives in its own activity: reopen there, on the same tab, with the choice in view
            val next = Intent(this, StyleActivities.of(style))
            if (vm.tab == Screen.More) next.putExtra(Notifications.EXTRA_OPEN, "style")
            startActivity(next, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
            finish()
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val night = when (mode) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
            // Always set it: nightMode reports the system setting, not the app's own
            getSystemService(UiModeManager::class.java).setApplicationNightMode(night)
        }
    }

    /** What shows before Compose draws its first frame, in the colours of the style. */
    private fun applyWindowBackground(mode: ThemeMode, style: StyleId) {
        val color = when (style) {
            StyleId.LINEE -> when (mode) { ThemeMode.SYSTEM -> R.color.splash_bg; ThemeMode.LIGHT -> R.color.splash_light; ThemeMode.DARK -> R.color.splash_dark }
            StyleId.ORIGINALE -> when (mode) { ThemeMode.SYSTEM -> R.color.splash_originale_bg; ThemeMode.LIGHT -> R.color.splash_originale_light; ThemeMode.DARK -> R.color.splash_originale_dark }
            StyleId.ANDROID -> when (mode) { ThemeMode.SYSTEM -> R.color.splash_android_bg; ThemeMode.LIGHT -> R.color.splash_android_light; ThemeMode.DARK -> R.color.splash_android_dark }
        }
        window.setBackgroundDrawable(getColor(color).toDrawable())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Opening the app is also a recovery point for the alarm chain (e.g. after a force stop)
        vm.refresh()
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val stream = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                when {
                    stream != null -> vm.importUri(this, stream)
                    text != null -> vm.importText(text)
                }
            }
            Intent.ACTION_VIEW -> intent.data?.let { vm.importUri(this, it) }
        }
        when (intent.getStringExtra(Notifications.EXTRA_OPEN)) {
            "holidays" -> vm.open(Screen.Holidays)
            "today" -> vm.switchTab(Screen.Today)
            "more" -> vm.switchTab(Screen.More)
            "style" -> { vm.switchTab(Screen.More); vm.showStyle = true }
        }
    }
}
