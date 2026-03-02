package com.stripe.android.crypto.onramp

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

internal const val DEFAULT_ONRAMP_INSTANCE_KEY = "DEFAULT_ONRAMP_INSTANCE_KEY"

@Composable
fun onrampCallbackAttachment(
    callbacks: OnrampCallbacks
) {
    val built = remember(callbacks) { callbacks.build() }

    LaunchedEffect(DEFAULT_ONRAMP_INSTANCE_KEY, built) {
        OnrampCallbackReferences[DEFAULT_ONRAMP_INSTANCE_KEY] = built
    }

    DisposableEffect(DEFAULT_ONRAMP_INSTANCE_KEY) {
        onDispose {
            OnrampCallbackReferences.remove(DEFAULT_ONRAMP_INSTANCE_KEY)
        }
    }
}

internal object OnrampCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<String, OnrampCallbacks.State>()

    operator fun get(key: String): OnrampCallbacks.State? {
        return instanceCallbackMap[key] ?: instanceCallbackMap.values.firstOrNull()
    }

    operator fun set(key: String, callbacks: OnrampCallbacks.State) {
        instanceCallbackMap[key] = callbacks
    }

    fun remove(key: String) {
        instanceCallbackMap.remove(key)
    }
}
