package com.stripe.android.crypto.onramp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkController.AuthenticationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OnrampInteractor @Inject constructor(
    private val handle: SavedStateHandle,
    private val application: Application,
    private val linkController: LinkController,
    private val cryptoApiRepository: CryptoApiRepository,
) {

    private var configuration: OnrampConfiguration?
        get() = handle.get<OnrampConfiguration>(CONFIGURATION_KEY)
        set(value) = handle.set(CONFIGURATION_KEY, value)

    private var linkControllerState: LinkController.State? = null

    suspend fun configure(configuration: OnrampConfiguration) {
        this.configuration = configuration
        linkController.configure(
            LinkController.Configuration.Builder("Onramp Merchant").build()
        )
    }

    suspend fun isLinkUser(email: String): OnrampLinkLookupResult {
        val result = linkController.lookupConsumer(email)
        return when (result) {
            is LinkController.LookupConsumerResult.Success -> OnrampLinkLookupResult.Completed(
                isLinkUser = result.isConsumer
            )
            is LinkController.LookupConsumerResult.Failed -> OnrampLinkLookupResult.Failed(
                error = result.error
            )
        }
    }

    suspend fun registerNewLinkUser(info: LinkUserInfo): OnrampRegisterUserResult {
        val result = linkController.registerConsumer(
            email = info.email,
            phone = info.phone,
            country = info.country,
            name = info.fullName,
        )

        return when (result) {
            is LinkController.RegisterConsumerResult.Success -> {
                OnrampRegisterUserResult.Completed(
                    customerId = "link_consumer" // TODO create consumer
                )
            }
            is LinkController.RegisterConsumerResult.Failed -> {
                OnrampRegisterUserResult.Failed(
                    error = result.error
                )
            }
        }
    }

    suspend fun handleAuthenticationResult(
        result: AuthenticationResult
    ): OnrampVerificationResult = when (result) {
        is AuthenticationResult.Success -> {
            val secret = consumerSessionClientSecret()
            secret?.let {
                val permissionsResult = cryptoApiRepository
                    .grantPartnerMerchantPermissions(it)
                permissionsResult.fold(
                    onSuccess = { result ->
                        OnrampVerificationResult.Completed(result.id)
                    },
                    onFailure = { error ->
                        OnrampVerificationResult.Failed(error)
                    }
                )
            } ?: OnrampVerificationResult.Failed(
                IllegalStateException("Missing consumer secret")
            )
        }
        is AuthenticationResult.Failed -> OnrampVerificationResult.Failed(result.error)
        is AuthenticationResult.Canceled -> OnrampVerificationResult.Cancelled()
    }

    private fun consumerSessionClientSecret(): String? = linkController.state(application)
        .value.internalLinkAccount?.consumerSessionClientSecret

    fun onLinkControllerState(state: LinkController.State) {
        linkControllerState = state
    }

    private companion object Companion {
        private const val CONFIGURATION_KEY = "OnrampCoordinatorInteractor.configuration"
    }
}
