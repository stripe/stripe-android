package com.stripe.android.crypto.onramp.di

import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle

/**
 * A singleton holder for the [OnrampComponent], ensuring it is initialized only once and
 * shared across activity recreations within the same process.
 *
 * This prevents a new [OnrampComponent] (and therefore a new OnrampInteractor) from being
 * created when the ViewModel is recreated due to activity destruction without process death.
 * The cached interactor retains in-flight checkout state, so the new presenter coordinator
 * can receive the checkout result. Its SDK state is saved through each host's handle,
 * so reopening Onramp does not leave persistence attached to a destroyed host.
 */
@Suppress("RestrictedApi")
internal object OnrampComponentHolder {
    private const val KEY_SAVED_STATE = "stripe_onramp_state"

    private var component: OnrampComponent? = null

    fun getOrCreate(
        application: Application,
        savedStateHandle: SavedStateHandle,
    ): OnrampComponent {
        return synchronized(this) {
            val current = component ?: buildComponent(
                application = application,
                savedStateHandle = SavedStateHandle.createHandle(
                    restoredState = savedStateHandle.remove<Bundle>(KEY_SAVED_STATE),
                    defaultState = null,
                ),
            ).also { component = it }
            savedStateHandle.setSavedStateProvider(KEY_SAVED_STATE, current.savedStateHandle.savedStateProvider())
            current
        }
    }

    @VisibleForTesting
    fun reset() {
        synchronized(this) {
            component = null
        }
    }

    private fun buildComponent(
        application: Application,
        savedStateHandle: SavedStateHandle,
    ): OnrampComponent {
        return DaggerOnrampComponent
            .factory()
            .build(
                application = application,
                savedStateHandle = savedStateHandle,
            )
    }
}
