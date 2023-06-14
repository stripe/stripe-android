package com.stripe.android.paymentsheet.utils

import android.view.View
import android.view.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.core.view.doOnAttach
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.foundation.layout.WindowInsets.Companion as ComposeWindowInsets

@Composable
internal fun EdgeToEdge(
    content: @Composable (ContentInsets) -> Unit,
) {
    TransparentNavigationBar()

    val insets = ComposeWindowInsets.systemBars.asPaddingValues()
    val contentPadding = ContentInsets(navigationBar = insets.calculateBottomPadding())

    content(contentPadding)
}

@Composable
internal fun TransparentNavigationBar() {
    val systemUiController = rememberSystemUiController()

    LaunchedEffect(systemUiController) {
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = false,
        )
    }
}

@Immutable
internal data class ContentInsets(
    val navigationBar: Dp,
)

// Taken from https://medium.com/androiddevelopers/windowinsets-listeners-to-layouts-8f9ccc8fa4d1
internal fun View.doOnApplyWindowInsets(f: (View, WindowInsets, InitialPadding) -> Unit) {
    val initialPadding = InitialPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    setOnApplyWindowInsetsListener { v, insets ->
        f(v, insets, initialPadding)
        insets
    }
    requestApplyInsetsWhenAttached()
}

internal data class InitialPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal fun View.requestApplyInsetsWhenAttached() {
    doOnAttach {
        it.requestApplyInsets()
    }
}
