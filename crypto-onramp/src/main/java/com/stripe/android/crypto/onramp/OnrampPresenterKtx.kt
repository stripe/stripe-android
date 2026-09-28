package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.OnrampCallbacks

internal const val DEFAULT_ONRAMP_INSTANCE_KEY = "DEFAULT_ONRAMP_INSTANCE_KEY"

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
