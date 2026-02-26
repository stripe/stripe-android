package com.stripe.android.paymentsheet.example.playground

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore

@Composable
internal fun PlaygroundTheme(
    content: @Composable ColumnScope.() -> Unit,
    bottomBarContent: @Composable ColumnScope.() -> Unit,
    topBarContent: @Composable (() -> Unit)? = null
) {
    val colorScheme = if (isSystemInDarkTheme() || AppearanceStore.forceDarkMode) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                topBar = {
                    topBarContent?.invoke()
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .animateContentSize()
                            .padding(
                                paddingValues = WindowInsets.systemBars.only(
                                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                                ).asPaddingValues()
                            )
                    ) {
                        HorizontalDivider()
                        Column(
                            content = bottomBarContent,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                },
                contentWindowInsets = WindowInsets.systemBars
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                            .padding(16.dp),
                        content = content,
                    )
                }
            }
        }
    }
}
