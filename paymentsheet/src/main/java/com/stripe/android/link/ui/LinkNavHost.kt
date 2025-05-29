package com.stripe.android.link.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.stripe.android.link.utils.LinkScreenTransition

/**
 * A [NavHost] configured for use in a Link bottom sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LinkNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit
) {
    // True if the IME (soft keyboard) is animating.
    val isImeAnimating = WindowInsets.imeAnimationSource != WindowInsets.imeAnimationTarget

    // Cache the current screen size so we can size `LinkLoadingScreen` to match, reducing
    // screen size animation jumps.
    var screenSize by remember { mutableStateOf<IntSize?>(null) }
    ProvideLinkScreenSize(screenSize) {
        NavHost(
            modifier = modifier.onSizeChanged { screenSize = it },
            navController = navController,
            startDestination = startDestination,
            enterTransition = { LinkScreenTransition.targetContentEnter },
            exitTransition = { LinkScreenTransition.initialContentExit },
            // Workaround a race condition where the route changes while the soft keyboard is
            // animating in/out. Imagine navigating from screen A to B, where both screens are
            // supposed to be equal height. If the soft keyboard closes immediately after the
            // screen transition starts, the target height of screen B, which is locked in at
            // the start of the animation, will be too short.
            sizeTransform = if (isImeAnimating) {
                null
            } else {
                { LinkScreenTransition.sizeTransform }
            },
            builder = builder,
        )
    }
}

private val LocalLinkScreenSizeInternal = compositionLocalOf<DpSize?> { null }

/**
 * Current screen size rendered in [LinkNavHost].
 */
internal val LocalLinkScreenSize: CompositionLocal<DpSize?> = LocalLinkScreenSizeInternal

@Composable
private fun ProvideLinkScreenSize(size: IntSize?, content: @Composable () -> Unit) {
    val dpSize = with(LocalDensity.current) {
        size?.let { DpSize(it.width.toDp(), it.height.toDp()) }
    }
    CompositionLocalProvider(
        LocalLinkScreenSizeInternal provides dpSize,
        content = content
    )
}
