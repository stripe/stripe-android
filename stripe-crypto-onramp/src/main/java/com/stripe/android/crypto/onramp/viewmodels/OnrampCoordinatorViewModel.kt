package com.stripe.android.crypto.onramp.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.Stripe
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import com.stripe.android.model.ConsumerSignUpConsentAction
import kotlinx.coroutines.launch

/**
 * ViewModel that stores Onramp configuration in a SavedStateHandle for
 * process death restoration.
 *
 * @property handle SavedStateHandle backing persistent state.
 * @property linkController The LinkController to configure.
 *
 */
internal class OnrampCoordinatorViewModel(
    private val handle: SavedStateHandle,
    private val linkController: LinkController,
    private val cryptoApiRepository: CryptoApiRepository,
    private val onrampCallbacks: OnrampCallbacks
) : ViewModel() {

    /**
     * The current OnrampConfiguration, persisted across process restarts.
     */
    private var onRampConfiguration: OnrampConfiguration?
        get() = handle["configuration"]
        set(value) = handle.set("configuration", value)

    /**
     * Configure the view model and associated types.
     *
     * @param configuration The OnrampConfiguration to apply.
     */
    fun configure(configuration: OnrampConfiguration) {
        onRampConfiguration = configuration

        viewModelScope.launch {
            val config = LinkController.Configuration.Builder(merchantDisplayName = "").build()

            when (val result = linkController.configure(config)) {
                is LinkController.ConfigureResult.Success ->
                    onrampCallbacks.configurationCallback.onResult(OnrampConfigurationResult.Completed(true))
                is LinkController.ConfigureResult.Failed ->
                    onrampCallbacks.configurationCallback.onResult(OnrampConfigurationResult.Failed(result.error))
            }
        }
    }

    fun isLinkUser(email: String) {
        linkController.lookupConsumer(email)
    }

    fun authenticateExistingUser(email: String) {
        linkController.authenticateExistingConsumer(email)
    }

    fun registerNewUser(info: LinkUserInfo) {
        linkController.registerConsumer(
            email = info.email,
            phone = info.phone,
            country = info.country,
            name = info.fullName,
            consentAction = ConsumerSignUpConsentAction.Implied
        )
    }

    internal fun handleConsumerLookupResult(result: LinkController.LookupConsumerResult) {
        when (result) {
            is LinkController.LookupConsumerResult.Success ->
                onrampCallbacks.linkLookupCallback.onResult(
                    OnrampLinkLookupResult.Completed(result.isConsumer)
                )
            is LinkController.LookupConsumerResult.Failed ->
                onrampCallbacks.linkLookupCallback.onResult(
                    OnrampLinkLookupResult.Failed(result.error)
                )
        }
    }

    internal fun handleAuthenticationResult(result: LinkController.AuthenticationResult) {
        when (result) {
            is LinkController.AuthenticationResult.Success ->
                viewModelScope.launch {
                    val permissionsResult = cryptoApiRepository.grantPartnerMerchantPermissions("")

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

    internal fun handleRegisterNewUserResult(result: LinkController.RegisterConsumerResult) {
        when (result) {
            is LinkController.RegisterConsumerResult.Success ->
                viewModelScope.launch {
                    val permissionsResult = cryptoApiRepository.grantPartnerMerchantPermissions("")

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
                }
            is LinkController.RegisterConsumerResult.Failed ->
                onrampCallbacks.registerUserCallback.onResult(
                    OnrampRegisterUserResult.Failed(result.error)
                )
        }
    }

    class Factory(
        private val linkController: LinkController,
        private val onrampCallbacks: OnrampCallbacks
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val cryptoApiRepository = CryptoApiRepository(
                stripeNetworkClient = DefaultStripeNetworkClient(),
                publishableKeyProvider = { "" },
                stripeAccountIdProvider = { "" },
                apiVersion = Stripe.API_VERSION,
                sdkVersion = StripeSdkVersion.VERSION,
                appInfo = Stripe.appInfo
            )

            return OnrampCoordinatorViewModel(
                handle = extras.createSavedStateHandle(),
                linkController = linkController,
                cryptoApiRepository = cryptoApiRepository,
                onrampCallbacks = onrampCallbacks
            ) as T
        }
    }
}
