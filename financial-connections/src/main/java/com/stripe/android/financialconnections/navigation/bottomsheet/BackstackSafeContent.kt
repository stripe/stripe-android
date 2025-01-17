package com.stripe.android.financialconnections.navigation.bottomsheet

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Prevents the content from being rendered until the lifecycle is ready.
 *
 * Addresses: You cannot access the NavBackStackEntry's ViewModels after the NavBackStackEntry is destroyed.
 * https://github.com/google/accompanist/issues/1487#top
 */
@Composable
internal fun LifecycleAwareContent(
    lifecycleOwner: LifecycleOwner,
    content: @Composable () -> Unit
) {
    val isReady = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> isReady.value = true
                Lifecycle.Event.ON_DESTROY -> isReady.value = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // This will typically be used on bottom sheets, where we see the NavBackStackEntry issue.
    // animating the content height will ensure we respect the bottom sheet open animation.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween())
    ) {
        if (isReady.value) { content() }
    }
}
