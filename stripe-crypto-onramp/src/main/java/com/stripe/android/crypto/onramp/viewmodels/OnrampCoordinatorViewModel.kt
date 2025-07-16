package com.stripe.android.crypto.onramp.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.crypto.onramp.model.OnrampConfiguration

/**
 * ViewModel that stores Onramp configuration in a SavedStateHandle for
 * process death restoration.
 *
 * @property handle SavedStateHandle backing persistent state.
 */
internal class OnrampCoordinatorViewModel(
    private val handle: SavedStateHandle
) : ViewModel() {

    /**
     * The current OnrampConfiguration, persisted across process restarts.
     */
    var onRampConfiguration: OnrampConfiguration?
        get() = handle["configuration"]
        set(value) = handle.set("configuration", value)

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OnrampCoordinatorViewModel(
                handle = extras.createSavedStateHandle(),
            ) as T
        }
    }
}
