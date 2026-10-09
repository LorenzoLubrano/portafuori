package io.github.lorenzolubrano.portafuori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lorenzolubrano.portafuori.ui.theme.extra

/** A text action on the shell (top bar): the shell's ink, faded when disabled. Plain TextButtons would take primary, the shell itself in light Linee. */
@Composable
fun ShellAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val on = extra().onShell
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = on, disabledContentColor = on.copy(alpha = 0.38f)),
    ) { Text(text) }
}

/** Top bar + scrolling column shared by every screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floating: @Composable () -> Unit = {},
    bottom: @Composable () -> Unit = {},
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro") }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = extra().shell,
                    titleContentColor = extra().onShell,
                    navigationIconContentColor = extra().onShell,
                    actionIconContentColor = extra().onShell,
                ),
            )
        },
        floatingActionButton = floating,
        bottomBar = bottom,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        val navBar = if (onBack != null) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize().padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())
                    // in landscape the system bar and the camera cut-out sit at the sides
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp + navBar),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
            // sub-screens have no tab bar: the shell runs behind the system's navigation bar, as on the main screens
            if (onBack != null) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(extra().shell))
            }
        }
    }
}
