package com.stripe.onramp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkController
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.onramp.callback.ConfigCallback
import com.stripe.onramp.model.DocumentUploadStatus
import com.stripe.onramp.model.KycInfo
import com.stripe.onramp.model.KycStatus
import com.stripe.onramp.model.OnRampConfiguration
import com.stripe.onramp.model.OnRampUser
import com.stripe.onramp.model.PrefillDetails
import com.stripe.onramp.repositories.CryptoRepository
import com.stripe.onramp.result.OnRampKycResult
import com.stripe.onramp.result.OnRampVerificationResult
import javax.inject.Inject

internal class DefaultOnRampCoordinator @Inject internal constructor(
    lifecycleOwner: LifecycleOwner,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val onRampCallbacks: OnRampCoordinator.OnRampCallbacks,
    private val viewModel: OnRampCoordinatorViewModel,
    private val linkApiRepository: LinkApiRepository,
    private val cryptoRepository: CryptoRepository,
) : OnRampCoordinator, DefaultLifecycleObserver {

    private val linkController: LinkController by lazy {
        LinkController.createInternal(
            activity = activityResultRegistryOwner as ComponentActivity,
            presentPaymentMethodsCallback = { _ -> 
                // No-op for now
            },
            lookupConsumerCallback = { _ -> 
                // No-op for now
            },
            createPaymentMethodCallback = { _ -> 
                // No-op for now
            },
            presentForAuthenticationCallback = ::onLinkAuthenticationResult
        )
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun configure(
        onRampConfiguration: OnRampConfiguration,
        callback: ConfigCallback
    ) {
        // Force FC lite.
        FeatureFlags.financialConnectionsFullSdkUnavailable.setEnabled(true)
        viewModel.onRampConfiguration = onRampConfiguration
        // validate the configuration and potentially make network calls
        callback.onConfigured(success = true, error = null)
    }

    override fun submitKycInfo(kycInfo: KycInfo) {
        viewModel.viewModelScope.launch {
            try {
                val result = cryptoRepository.submitKycInfo(kycInfo)
                if (result.isSuccess) {
                    onRampCallbacks.kycResultCallback.invoke(OnRampKycResult.Success)
                } else {
                    onRampCallbacks.kycResultCallback.invoke(
                        OnRampKycResult.Failed(
                            error = result.exceptionOrNull() ?: Exception("Unknown KYC submission error")
                        )
                    )
                }
            } catch (e: Exception) {
                onRampCallbacks.kycResultCallback.invoke(OnRampKycResult.Failed(error = e))
            }
        }
    }

    override fun promptForLinkAuthentication(
        emailAddress: String,
        prefillDetails: PrefillDetails?
    ) {
        // TODO call
        linkController.presentForAuthentication(emailAddress)
    }

    private fun onLinkAuthenticationResult(result: LinkController.PresentForAuthenticationResult) {
        when (result) {
            is LinkController.PresentForAuthenticationResult.Authenticated -> {
                // After successful Link authentication, fetch crypto customer info
                val linkUser = result.user
                val linkAccountId = linkUser.email
                fetchCryptoCustomerInfo(linkAccountId) { cryptoCustomer ->
                    // Return the crypto customer ID in the verification result
                    onRampCallbacks.verificationResultCallback.invoke(
                        OnRampVerificationResult.Completed(customerId = cryptoCustomer.id)
                    )
                }
            }
            is LinkController.PresentForAuthenticationResult.Canceled -> {
                // Handle cancellation - notify merchant of cancellation
                onRampCallbacks.verificationResultCallback.invoke(
                    OnRampVerificationResult.Canceled
                )
            }
            is LinkController.PresentForAuthenticationResult.Failed -> {
                // Handle failure - notify merchant of the actual error
                onRampCallbacks.verificationResultCallback.invoke(
                    OnRampVerificationResult.Failed(error = result.error)
                )
            }
        }
    }

    /**
     * Mock endpoint call to fetch crypto customer information based on Link account ID.
     * In a real implementation, this would make an API call to your backend.
     */
    private fun fetchCryptoCustomerInfo(
        linkAccountId: String,
        callback: (OnRampUser) -> Unit
    ) {
        // Mock the API call - in reality this would be a network request
        // to your backend passing the linkAccountId to get crypto customer info

        // Simulate async operation
        viewModel.viewModelScope.launch {
            try {
                // Mock crypto customer data
                val mockCryptoCustomer = OnRampUser(
                    id = "crc_${linkAccountId.hashCode().toString().takeLast(8)}",
                    kycStatus = KycStatus.NOT_SUBMITTED,
                    documentUploadStatus = DocumentUploadStatus.NOT_SUBMITTED,
                    allowedToAttemptIdentityVerification = true
                )

                // Call the callback on the main thread
                callback(mockCryptoCustomer)
            } catch (e: Exception) {
                // Handle error by calling the failed verification result
                onRampCallbacks.verificationResultCallback.invoke(
                    OnRampVerificationResult.Failed(error = e)
                )
            }
        }
    }

    companion object {
        fun getInstance(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            onRampCallbacks: OnRampCoordinator.OnRampCallbacks,
            linkElementCallbackIdentifier: String,
        ): OnRampCoordinator {
            val onRampCoordinatorViewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = OnRampCoordinatorViewModel.Factory(),
            ).get(
                key = "OnRampCoordinatorViewModel(instance = $linkElementCallbackIdentifier)",
                modelClass = OnRampCoordinatorViewModel::class.java
            )

            // Get Application from the lifecycle owner (which should be an Activity or Fragment)
            val application = when (lifecycleOwner) {
                is Fragment -> lifecycleOwner.requireActivity().application
                is ComponentActivity -> lifecycleOwner.application
                else -> throw IllegalArgumentException("LifecycleOwner must be an Activity or Fragment")
            }

            val onRampComponent: OnRampComponent =
                DaggerOnRampComponent
                    .builder()
                    .application(application)
                    .onRampCoordinatorViewModel(onRampCoordinatorViewModel)
                    .linkElementCallbackIdentifier(linkElementCallbackIdentifier)
                    .lifecycleOwner(lifecycleOwner)
                    .activityResultRegistryOwner(activityResultRegistryOwner)
                    .onRampCallbacks(onRampCallbacks)
                    .build()

            return onRampComponent.linkCoordinator
        }
    }
}
