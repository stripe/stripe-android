package com.stripe.android.common.nfcscan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

/**
 * Custom saved state provider which can retain state for a composable even after it has been removed from
 * composition. Useful for composables rendered in `AnimatedContent` which removes composables completely across
 * configuration changes, preventing child composables from being able to save their state and use it to resume
 * animations after a config change.
 *
 * @param key identifier that the state should be saved under
 * @param canRetainState indicates if the holder should continue to retain state. Useful if the content has actually
 *   changed in `AnimatedContent` such as going from error content to idle content.
 */
@Composable
internal fun rememberStateRetainer(
    key: String,
    canRetainState: Boolean,
): StateRetainer {
    val saveableStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(canRetainState) {
        if (!canRetainState) {
            saveableStateHolder.removeState(key)
        }
    }

    return remember(key, saveableStateHolder) {
        StateRetainer(
            provider = { content ->
                saveableStateHolder.SaveableStateProvider(key) {
                    content()
                }
            }
        )
    }
}

internal class StateRetainer(
    val provider: @Composable (content: @Composable () -> Unit) -> Unit
)
