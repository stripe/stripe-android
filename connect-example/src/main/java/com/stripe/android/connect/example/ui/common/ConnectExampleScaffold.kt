package com.stripe.android.connect.example.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Composable
fun ConnectExampleScaffold(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = navigationIcon,
                actions = actions,
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) { content() }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun ConnectExampleScaffoldPreview() {
    ConnectExampleScaffold(
        title = "Title",
        content = {
            Text("Content")
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ConnectExampleScaffoldWithNavigationIconPreview() {
    ConnectExampleScaffold(
        title = "Title",
        navigationIcon = { BackIconButton(onClick = { }) },
        content = {
            Text("Content")
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ConnectExampleScaffoldWithActionsPreview() {
    ConnectExampleScaffold(
        title = "Title",
        actions = { CustomizeAppearanceIconButton(onClick = { }) },
        content = {
            Text("Content")
        }
    )
}
