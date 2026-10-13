package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinators share the cached component's interactor and callback key. Keep the latest
 * builder-supplied callbacks here so an older coordinator cannot restore stale callbacks.
 */
@Singleton
internal class OnrampCallbackStore @Inject constructor(
    private val onrampCallbackIdentifier: String,
) {
    private lateinit var callbacks: OnrampCallbacks.State

    fun update(callbacks: OnrampCallbacks.State) {
        this.callbacks = callbacks
        restore()
    }

    fun restore() {
        OnrampCallbackReferences[onrampCallbackIdentifier] = callbacks
    }
}
