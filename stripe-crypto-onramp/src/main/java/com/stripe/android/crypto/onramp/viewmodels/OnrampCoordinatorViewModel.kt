package com.stripe.android.crypto.onramp.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.crypto.onramp.di.DaggerOnrampCoordinatorViewModelComponent
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that stores Onramp configuration in a SavedStateHandle for
 * process death restoration.
 *
 * @property handle SavedStateHandle backing persistent state.
 * @property onrampCallbacks Callbacks that are called when actions complete .
 * @property cryptoApiRepository The api repository to use to make crypto network calls.
 *
 */
internal class OnrampCoordinatorViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val onrampCallbacks: OnrampCallbacks,
    private val cryptoApiRepository: CryptoApiRepository
) : ViewModel() {

    /**
     * The current OnrampConfiguration, persisted across process restarts.
     */
    private var onRampConfiguration: OnrampConfiguration?
        get() = handle["configuration"]
        set(value) = handle.set("configuration", value)

    private var linkControllerState: LinkController.State? = null

    /**
     * Configure the view model and associated types.
     *
     * @param configuration The OnrampConfiguration to apply.
     */
    suspend fun configure(
        configuration: OnrampConfiguration,
        linkControllerConfigureResult: LinkController.ConfigureResult
    ): OnrampConfigurationResult {
        onRampConfiguration = configuration
        return when (linkControllerConfigureResult) {
            is LinkController.ConfigureResult.Success ->
                OnrampConfigurationResult.Completed(true)
            is LinkController.ConfigureResult.Failed ->
                OnrampConfigurationResult.Failed(linkControllerConfigureResult.error)
        }
    }

    fun onLinkControllerState(state: LinkController.State) {
        linkControllerState = state
    }

    fun handleAuthenticationResult(result: LinkController.AuthenticationResult) {
        when (result) {
            is LinkController.AuthenticationResult.Success ->
                viewModelScope.launch {
                    linkControllerState?.internalLinkAccount?.consumerSessionClientSecret?.let { secret ->
                        val permissionsResult = cryptoApiRepository.grantPartnerMerchantPermissions(secret)

                        permissionsResult.fold(
                            onSuccess = {
                                onrampCallbacks.authenticationCallback.onResult(
                                    OnrampVerificationResult.Completed(it.id)
                                )
                            },
                            onFailure = {
                                onrampCallbacks.authenticationCallback.onResult(
                                    OnrampVerificationResult.Failed(it)
                                )
                            }
                        )
                    } ?: run {
                        onrampCallbacks.authenticationCallback.onResult(
                            OnrampVerificationResult.Failed(IllegalStateException("Missing consumer secret"))
                        )
                    }
                }
            is LinkController.AuthenticationResult.Failed ->
                onrampCallbacks.authenticationCallback.onResult(
                    OnrampVerificationResult.Failed(result.error)
                )
            is LinkController.AuthenticationResult.Canceled ->
                onrampCallbacks.authenticationCallback.onResult(
                    OnrampVerificationResult.Cancelled()
                )
        }
    }

    fun handleRegisterNewUserResult(result: LinkController.RegisterConsumerResult) {
        when (result) {
            is LinkController.RegisterConsumerResult.Success ->
                viewModelScope.launch {
                    linkControllerState?.internalLinkAccount?.consumerSessionClientSecret?.let { secret ->
                        val permissionsResult = cryptoApiRepository.grantPartnerMerchantPermissions(secret)

                        permissionsResult.fold(
                            onSuccess = {
                                onrampCallbacks.registerUserCallback.onResult(
                                    OnrampRegisterUserResult.Completed(it.id)
                                )
                            },
                            onFailure = {
                                onrampCallbacks.registerUserCallback.onResult(
                                    OnrampRegisterUserResult.Failed(it)
                                )
                            }
                        )
                    } ?: run {
                        onrampCallbacks.registerUserCallback.onResult(
                            OnrampRegisterUserResult.Failed(IllegalArgumentException("Missing consumer secret"))
                        )
                    }
                }
            is LinkController.RegisterConsumerResult.Failed ->
                onrampCallbacks.registerUserCallback.onResult(
                    OnrampRegisterUserResult.Failed(result.error)
                )
        }
    }

    class Factory(
        private val onrampCallbacks: OnrampCallbacks
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return DaggerOnrampCoordinatorViewModelComponent.factory()
                .build(
                    application = extras.requireApplication(),
                    savedStateHandle = extras.createSavedStateHandle(),
                    onrampCallbacks = onrampCallbacks
                )
                .viewModel as T
        }
    }
}
