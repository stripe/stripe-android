package com.stripe.android.crypto.onramp.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.crypto.onramp.di.DaggerOnrampCoordinatorViewModelComponent
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampContinuations
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that stores Onramp configuration in a SavedStateHandle for
 * process death restoration.
 *
 * @property handle SavedStateHandle backing persistent state.
 * @property continuations Manager for handling async operation continuations.
 * @property cryptoApiRepository The api repository to use to make crypto network calls.
 *
 */
internal class OnrampCoordinatorViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val continuations: OnrampContinuations,
    private val cryptoApiRepository: CryptoApiRepository
) : ViewModel() {

    /**
     * The current OnrampConfiguration, persisted across process restarts.
     */
    private var onRampConfiguration: OnrampConfiguration?
        get() = handle["configuration"]
        set(value) = handle.set("configuration", value)

    private val _configurationFlow =
        MutableSharedFlow<LinkController.Configuration>(replay = 1)

    /**
     * A flow that receives the built configuration model when `[configure]` is called.
     */
    internal val configurationFlow = _configurationFlow.asSharedFlow()

    /**
     * Configure the view model and associated types.
     *
     * @param configuration The OnrampConfiguration to apply.
     * @return The result of the configuration operation.
     */
    internal suspend fun configure(configuration: OnrampConfiguration): OnrampConfigurationResult {
        onRampConfiguration = configuration

        viewModelScope.launch {
            val config = LinkController.Configuration.Builder(merchantDisplayName = "").build()
            _configurationFlow.emit(config)
        }

        return continuations.awaitConfiguration()
    }

    internal fun onLinkControllerConfigureResult(result: LinkController.ConfigureResult) {
        when (result) {
            is LinkController.ConfigureResult.Success ->
                continuations.completeConfiguration(OnrampConfigurationResult.Completed(true))
            is LinkController.ConfigureResult.Failed ->
                continuations.completeConfiguration(OnrampConfigurationResult.Failed(result.error))
        }
    }

    internal fun handleConsumerLookupResult(result: LinkController.LookupConsumerResult) {
        when (result) {
            is LinkController.LookupConsumerResult.Success ->
                continuations.completeLookup(OnrampLinkLookupResult.Completed(result.isConsumer))
            is LinkController.LookupConsumerResult.Failed ->
                continuations.completeLookup(OnrampLinkLookupResult.Failed(result.error))
        }
    }

    internal fun handleAuthenticationResult(result: LinkController.AuthenticationResult, secret: String?) {
        when (result) {
            is LinkController.AuthenticationResult.Success ->
                viewModelScope.launch {
                    secret?.let {
                        val permissionsResult = cryptoApiRepository.grantPartnerMerchantPermissions(secret)

                        permissionsResult.fold(
                            onSuccess = {
                                continuations.completeAuthentication(OnrampVerificationResult.Completed(it.id))
                            },
                            onFailure = {
                                continuations.completeAuthentication(OnrampVerificationResult.Failed(it))
                            }
                        )
                    } ?: run {
                        continuations.completeAuthentication(
                            OnrampVerificationResult.Failed(IllegalStateException("Missing consumer secret"))
                        )
                    }
                }
            is LinkController.AuthenticationResult.Failed ->
                continuations.completeAuthentication(OnrampVerificationResult.Failed(result.error))
            is LinkController.AuthenticationResult.Canceled ->
                continuations.completeAuthentication(OnrampVerificationResult.Cancelled())
        }
    }

    internal fun handleRegisterNewUserResult(result: LinkController.RegisterConsumerResult, secret: String?) {
        when (result) {
            is LinkController.RegisterConsumerResult.Success ->
                viewModelScope.launch {
                    secret?.let {
                        val permissionsResult = cryptoApiRepository.grantPartnerMerchantPermissions(secret)

                        permissionsResult.fold(
                            onSuccess = {
                                continuations.completeRegistration(OnrampRegisterUserResult.Completed(it.id))
                            },
                            onFailure = {
                                continuations.completeRegistration(OnrampRegisterUserResult.Failed(it))
                            }
                        )
                    } ?: run {
                        continuations.completeRegistration(
                            OnrampRegisterUserResult.Failed(IllegalArgumentException("Missing consumer secret"))
                        )
                    }
                }
            is LinkController.RegisterConsumerResult.Failed ->
                continuations.completeRegistration(OnrampRegisterUserResult.Failed(result.error))
        }
    }

    internal class Factory(
        private val continuations: OnrampContinuations
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return DaggerOnrampCoordinatorViewModelComponent.factory()
                .build(
                    application = extras.requireApplication(),
                    savedStateHandle = extras.createSavedStateHandle(),
                    continuations = continuations
                )
                .viewModel as T
        }
    }
}
