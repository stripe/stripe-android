package com.stripe.android.crypto.onramp.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle

/**
 * A singleton holder for the [OnrampComponent], ensuring it is initialized only once and
 * shared across activity recreations within the same process.
 *
 * This prevents a new [OnrampComponent] (and therefore a new OnrampInteractor) from being
 * created when the ViewModel is recreated due to activity destruction without process death.
 * The cached interactor retains in-flight checkout state, so the new presenter coordinator
 * can observe its StateFlow and receive the checkout result.
 */
internal object OnrampComponentHolder {

    @Volatile
    private var component: OnrampComponent? = null

    fun getOrCreate(
        application: Application,
        savedStateHandle: SavedStateHandle,
    ): OnrampComponent {
        return component ?: synchronized(this) {
            component ?: buildComponent(
                application,
                savedStateHandle,
            ).also { component = it }
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
