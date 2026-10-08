package io.github.lorenzolubrano.portafuori.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.unit.sp
import io.github.lorenzolubrano.portafuori.ui.theme.extra
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lorenzolubrano.portafuori.ui.screens.BinEditorScreen
import io.github.lorenzolubrano.portafuori.ui.screens.BinsScreen
import io.github.lorenzolubrano.portafuori.ui.screens.CalendarScreen
import io.github.lorenzolubrano.portafuori.ui.screens.HolidaysScreen
import io.github.lorenzolubrano.portafuori.ui.screens.ImportPreviewScreen
import io.github.lorenzolubrano.portafuori.ui.screens.InfoScreen
import io.github.lorenzolubrano.portafuori.ui.screens.MoreScreen
import io.github.lorenzolubrano.portafuori.ui.screens.ProfileSettingsScreen
import io.github.lorenzolubrano.portafuori.ui.screens.ReliabilityScreen
import io.github.lorenzolubrano.portafuori.ui.screens.ScanScreen
import io.github.lorenzolubrano.portafuori.ui.screens.ShareScreen
import io.github.lorenzolubrano.portafuori.ui.screens.TodayScreen
import io.github.lorenzolubrano.portafuori.ui.screens.WelcomeScreen
import io.github.lorenzolubrano.portafuori.ui.screens.WizardScreen

@Composable
fun AppRoot(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (!state.loaded) return
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let { snackbar.showSnackbar(it); vm.message = null }
    }
    BackHandler(enabled = vm.stack.isNotEmpty() || vm.tab != Screen.Today) { vm.back() }

    val current = vm.current
    val noProfile = state.bundles.isEmpty()
    val topLevel = vm.stack.isEmpty() && !noProfile

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (topLevel) {
                NavigationBar(containerColor = extra().shell) {
                    listOf(
                        Triple(Screen.Today, "Oggi", Icons.Filled.Today),
                        Triple(Screen.Calendar, "Calendario", Icons.Filled.CalendarMonth),
                        Triple(Screen.Bins, "Bidoni", Icons.Filled.Recycling),
                        Triple(Screen.More, "Altro", Icons.Filled.MoreHoriz),
                    ).forEach { (screen, label, icon) ->
                        NavigationBarItem(
                            selected = vm.tab == screen,
                            onClick = { vm.switchTab(screen) },
                            icon = { Icon(icon, contentDescription = null) },
                            label = {
                                BasicText(
                                    label, maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium.copy(color = extra().onShell),
                                    // shrinks only when the label would not fit (large system text); never wraps or cuts
                                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.labelMedium.fontSize),
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = extra().onShell,
                                selectedIconColor = extra().shell,
                                unselectedIconColor = extra().onShell,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = if (topLevel) padding.calculateBottomPadding() else androidx.compose.ui.unit.Dp.Hairline)) {
            AnimatedContent(
                targetState = if (noProfile && current !is Screen.Wizard && current !is Screen.Scan && current !is Screen.ImportPreview && current !is Screen.Share) null else current,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { screen ->
                when (screen) {
                    null -> WelcomeScreen(vm)
                    Screen.Today -> TodayScreen(vm, state)
                    Screen.Calendar -> CalendarScreen(vm, state)
                    Screen.Bins -> BinsScreen(vm, state)
                    Screen.More -> MoreScreen(vm, state)
                    is Screen.BinEditor -> BinEditorScreen(vm, state, screen.binId)
                    is Screen.Wizard -> WizardScreen(vm)
                    Screen.Reliability -> ReliabilityScreen(vm)
                    Screen.Holidays -> HolidaysScreen(vm, state)
                    Screen.Share -> ShareScreen(vm, state)
                    Screen.ProfileSettings -> ProfileSettingsScreen(vm, state)
                    Screen.Scan -> ScanScreen(vm)
                    Screen.ImportPreview -> ImportPreviewScreen(vm, state)
                    Screen.Info -> InfoScreen(vm)
                }
            }
        }
    }
}
