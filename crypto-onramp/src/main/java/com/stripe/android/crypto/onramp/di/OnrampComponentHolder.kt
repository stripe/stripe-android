package com.stripe.android.crypto.onramp.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.crypto.onramp.OnrampSessionClientSecretProvider
import com.stripe.android.crypto.onramp.model.OnrampCallbacks

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
        onrampCallbacks: OnrampCallbacks,
        checkoutHandler: OnrampSessionClientSecretProvider
    ): OnrampComponent {
        return component ?: synchronized(this) {
            component ?: buildComponent(
                application,
                savedStateHandle,
                onrampCallbacks,
                checkoutHandler
            ).also { component = it }
        }
    }

    fun clear() {
        synchronized(this) {
            component = null
        }
    }

    private fun buildComponent(
        application: Application,
        savedStateHandle: SavedStateHandle,
        onrampCallbacks: OnrampCallbacks,
        checkoutHandler: OnrampSessionClientSecretProvider
    ): OnrampComponent {
        return DaggerOnrampComponent
            .factory()
            .build(
                application = application,
                savedStateHandle = savedStateHandle,
                onrampCallbacks = onrampCallbacks,
                checkoutHandler = checkoutHandler
            )
    }
}
