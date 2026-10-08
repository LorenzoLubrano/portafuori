package io.github.lorenzolubrano.portafuori

import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

class MainActivity : ComponentActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        darkBars = if (lightBarIcons(Styles.current, dark = false)) true else when (cachedTheme) {
            ThemeMode.SYSTEM -> null
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        enableEdgeToEdge(statusBarStyle, navigationBarStyle)
        applyWindowBackground(cachedTheme)
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !vm.state.value.loaded }
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            // Until the settings arrive, the cached copy is the best guess
            val theme = if (state.loaded) state.settings.theme else cachedTheme
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // System bar icons follow the in-app theme and the style's shell, not only the system theme
            val lightIcons = lightBarIcons(Styles.current, dark)
            DisposableEffect(lightIcons) {
                darkBars = lightIcons
                enableEdgeToEdge(statusBarStyle, navigationBarStyle)
                onDispose {}
            }
            LaunchedEffect(state.loaded, state.settings.theme) {
                if (state.loaded) rememberTheme(state.settings.theme)
            }
            PortafuoriTheme(theme) { AppRoot(vm) }
        }
    }

    /**
     * Keeps the synchronous copy and the window background in line with the in-app theme, and from
     * Android 12 tells the system the app's night mode. The system remembers it and uses it for the
     * splash and the starting windows it draws before the app runs (e.g. reopening from a notification
     * after the process was killed), which setSplashScreenTheme does not cover.
     */
    private fun rememberTheme(mode: ThemeMode) {
        if (uiPrefs.getString(KEY_THEME, null) != mode.name) uiPrefs.edit { putString(KEY_THEME, mode.name) }
        applyWindowBackground(mode)
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

    /** What shows before Compose draws its first frame. */
    private fun applyWindowBackground(mode: ThemeMode) {
        val color = when (mode) {
            ThemeMode.SYSTEM -> R.color.splash_bg
            ThemeMode.LIGHT -> R.color.splash_light
            ThemeMode.DARK -> R.color.splash_dark
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
        }
    }
}
