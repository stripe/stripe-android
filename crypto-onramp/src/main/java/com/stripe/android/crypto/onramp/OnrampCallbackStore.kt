package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the latest builder-supplied callbacks alongside the retained interactor. */
@Singleton
internal class OnrampCallbackStore @Inject constructor() {
    lateinit var callbacks: OnrampCallbacks.State
        private set

    fun update(callbacks: OnrampCallbacks.State) {
        this.callbacks = callbacks
    }
}
