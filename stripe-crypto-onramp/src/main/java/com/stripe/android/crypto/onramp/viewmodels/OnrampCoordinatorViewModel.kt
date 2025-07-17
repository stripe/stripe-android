package com.stripe.android.crypto.onramp.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationCallback
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.link.LinkController
import kotlinx.coroutines.launch

/**
 * ViewModel that stores Onramp configuration in a SavedStateHandle for
 * process death restoration.
 *
 * @property handle SavedStateHandle backing persistent state.
 */
internal class OnrampCoordinatorViewModel(
    private val handle: SavedStateHandle,
    private val linkController: LinkController
) : ViewModel() {

    /**
     * The current OnrampConfiguration, persisted across process restarts.
     */
    var onRampConfiguration: OnrampConfiguration?
        get() = handle["configuration"]
        set(value) = handle.set("configuration", value)

    /**
     * Configure the view model and associated types.
     *
     * @param configuration The OnrampConfiguration to apply.
     * @param linkController The LinkController to configure.
     * @param callback Callback receiving success or failure.
     */
    fun configure(configuration: OnrampConfiguration, callback: OnrampConfigurationCallback) {
        onRampConfiguration = configuration

        viewModelScope.launch {
            val config = LinkController.Configuration.Builder(merchantDisplayName = "").build()

            when (val result = linkController.configure(config)) {
                is LinkController.ConfigureResult.Success ->
                    callback.onResult(OnrampConfigurationResult.Completed(true))
                is LinkController.ConfigureResult.Failed ->
                    callback.onResult(OnrampConfigurationResult.Failed(result.error))
            }
        }
    }

    fun isLinkUser(email: String) {
        linkController.lookupConsumer(email)
    }

    class Factory(
        private val linkController: LinkController // pass into factory
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OnrampCoordinatorViewModel(
                handle = extras.createSavedStateHandle(),
                linkController = linkController
            ) as T
        }
    }
}
