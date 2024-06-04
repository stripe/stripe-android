package com.stripe.android.paymentsheet.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Launches a new coroutine and repeats `block` every time the owners lifecycle
 * is in and out of `minActiveState` lifecycle state.
 *
 * Adapted from https://github.com/google/iosched/blob/main/mobile/src/main/java/com/google/samples/apps/iosched/util/UiUtils.kt#L60
 */
internal inline fun <T> Flow<T>.launchAndCollectIn(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: (T) -> Unit
) = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(minActiveState) {
        collect {
            action(it)
        }
    }
}
