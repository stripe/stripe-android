package com.stripe.android.financialconnections.navigation.bottomsheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    // Track the lifecycle to update the readiness state
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

    // Render content only when ready
    AnimatedVisibility(
        visible = isReady.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        content()
    }
}
