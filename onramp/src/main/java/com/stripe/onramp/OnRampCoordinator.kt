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
import com.stripe.onramp.model.DocumentUploadStatus
import com.stripe.onramp.model.KycInfo
import com.stripe.onramp.model.KycStatus
import com.stripe.onramp.model.OnRampConfiguration
import com.stripe.onramp.model.OnRampUser
import com.stripe.onramp.model.PrefillDetails
import com.stripe.onramp.repositories.CryptoRepository
import com.stripe.onramp.result.OnRampConfigureResult
import com.stripe.onramp.result.OnRampIdentityVerificationResult
import com.stripe.onramp.result.OnRampKycResult
import com.stripe.onramp.result.OnRampLookupResult
import com.stripe.onramp.result.OnRampRegistrationResult
import com.stripe.onramp.result.OnRampSetWalletAddressResult
import com.stripe.onramp.result.OnRampVerificationResult
import javax.inject.Inject

class OnRampCoordinator @Inject internal constructor(
    lifecycleOwner: LifecycleOwner,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val onRampCallbacks: OnRampCallbacks,
    private val viewModel: OnRampCoordinatorViewModel,
    private val linkApiRepository: LinkApiRepository,
    private val cryptoRepository: CryptoRepository,
) : DefaultLifecycleObserver {

    private val linkController: LinkController by lazy {
        LinkController.createInternal(
            activity = activityResultRegistryOwner as ComponentActivity,
            presentPaymentMethodsCallback = { _ ->
                // No-op for now
            },
            lookupConsumerCallback = ::onLinkLookupResult,
            createPaymentMethodCallback = { _ ->
                // No-op for now
            },
            presentForAuthenticationCallback = ::onLinkAuthenticationResult,
            registerNewLinkUserCallback = ::onLinkRegistrationResult
        )
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * Configure the LinkCoordinator with Link configuration.
     *
     * @param onRampConfiguration The OnRamp configuration to use.
     */
    fun configure(
        onRampConfiguration: OnRampConfiguration
    ) {
        // Force FC lite.
        FeatureFlags.financialConnectionsFullSdkUnavailable.setEnabled(true)
        viewModel.onRampConfiguration = onRampConfiguration
        // validate the configuration and potentially make network calls
        onRampCallbacks.configureCallback(OnRampConfigureResult.Success)
    }

    /**
     * Register a new Link user for OnRamp.
     *
     * @param email The email address of the new user.
     * @param name The full name of the new user.
     * @param phone The phone number of the new user.
     * @param country The country code of the new user.
     */
    fun registerNewLinkUser(
        email: String,
        name: String?,
        phone: String,
        country: String
    ) {
        linkController.registerNewLinkUser(email, name, phone, country)
    }

    /**
     * Check if an email belongs to an existing Link user.
     * The result will be delivered via the lookup callback.
     *
     * @param email The email address to check
     */
    fun isLinkUser(email: String) {
        linkController.lookupConsumer(email)
    }

    /**
     * Set the wallet address for the user.
     * The result will be delivered via the setWalletAddress callback.
     *
     * @param walletAddress The wallet address to set
     */
    fun setWalletAddress(walletAddress: String) {
        // TODO: Call crypto endpoint to set wallet address
        viewModel.viewModelScope.launch {
            try {
                // TODO: Implement actual crypto API call
                // val result = cryptoRepository.setWalletAddress(walletAddress)

                // For now, simulate success
                onRampCallbacks.setWalletAddressCallback.invoke(OnRampSetWalletAddressResult.Success)
            } catch (e: Exception) {
                onRampCallbacks.setWalletAddressCallback.invoke(
                    OnRampSetWalletAddressResult.Failed(error = e)
                )
            }
        }
    }

    /**
     * Present Link to the customer for authentication.
     *
     * @param emailAddress The email address for authentication.
     * @param prefillDetails Optional prefill details for signup if the email does not exist.
     */
    fun promptForLinkAuthentication(
        emailAddress: String,
        prefillDetails: PrefillDetails? = null,
    ) {
        // TODO call
        linkController.presentForAuthentication(emailAddress)
    }

    /**
     * Submit KYC information for the customer.
     * Result will be delivered via the kycResultCallback provided during initialization.
     *
     * @param kycInfo The KYC information to submit.
     */
    fun submitKycInfo(kycInfo: KycInfo) {
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

    /**
     * Puts the user through a Stripe Identity photo ID verification flow.
     * 
     * Stripe Identity has SDKs for iOS, Android, and web which this project leverages.
     * This method will:
     * 1. Make an API request to create an Identity VerificationSession
     * 2. Associate the VerificationSession with the Link consumer
     * 3. Initialize the Stripe Identity SDK with the client secret and ephemeral API key
     * 4. Present the Identity SDK UI, which returns with cancelled/completed/failed state
     * 5. Poll an endpoint to know when verification is complete, then return updated account state
     * 
     * Result will be delivered via the identityVerificationCallback provided during initialization.
     * 
     * @param merchantIcon Optional merchant icon for customization
     * @param backgroundColor Optional background color for customization
     */
    fun promptForIdentityVerification(
        merchantIcon: String? = null,
        backgroundColor: String? = null
    ) {
        viewModel.viewModelScope.launch {
            try {
                // TODO: Implement the full Identity verification flow:
                // 1. Make API request to server to create VerificationSession
                // 2. Associate with Link consumer  
                // 3. Get client secret and ephemeral API key
                // 4. Initialize Stripe Identity SDK
                // 5. Present Identity UI
                // 6. Poll for completion and get updated account state
                onRampCallbacks.identityVerificationCallback.invoke(
                    OnRampIdentityVerificationResult.Completed()
                )
            } catch (e: Exception) {
                onRampCallbacks.identityVerificationCallback.invoke(
                    OnRampIdentityVerificationResult.Failed(error = e)
                )
            }
        }
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

    private fun onLinkRegistrationResult(result: LinkController.RegisterNewLinkUserResult) {
        when (result) {
            is LinkController.RegisterNewLinkUserResult.Success -> {
                // After successful Link registration, create a crypto customer ID and return it
                val linkUser = result.user
                val customerId = "crc_${linkUser.email.hashCode().toString().takeLast(8)}"
                onRampCallbacks.registrationCallback.invoke(
                    OnRampRegistrationResult.Success(customerId = customerId)
                )
            }
            is LinkController.RegisterNewLinkUserResult.Failed -> {
                // Handle failure - notify merchant of the actual error
                onRampCallbacks.registrationCallback.invoke(
                    OnRampRegistrationResult.Failed(error = result.error)
                )
            }
        }
    }

    private fun onLinkLookupResult(result: LinkController.LookupConsumerResult) {
        when (result) {
            is LinkController.LookupConsumerResult.Success -> {
                // Convert LinkController lookup result to OnRamp lookup result
                onRampCallbacks.lookupCallback.invoke(
                    OnRampLookupResult.Success(
                        email = result.email,
                        isLinkUser = result.isConsumer
                    )
                )
            }
            is LinkController.LookupConsumerResult.Failed -> {
                // Handle failure - notify merchant of the actual error
                onRampCallbacks.lookupCallback.invoke(
                    OnRampLookupResult.Failed(
                        email = result.email,
                        error = result.error
                    )
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

    /**
     * Builder utility to create [OnRampCoordinator] with callbacks.
     *
     * @param onRampCallbacks The callbacks for handling OnRamp events.
     */
    class Builder(
        private val onRampCallbacks: OnRampCallbacks
    ) {

        /**
         * Returns a [OnRampCoordinator].
         *
         * @param activity The Activity that is presenting [OnRampCoordinator].
         */
        fun build(activity: ComponentActivity): OnRampCoordinator {
            return create(
                viewModelStoreOwner = activity,
                lifecycleOwner = activity,
                activityResultRegistryOwner = activity,
                linkElementCallbackIdentifier = "LinkCoordinator"
            )
        }

        /**
         * Returns a [OnRampCoordinator].
         *
         * @param fragment The Fragment that is presenting [OnRampCoordinator].
         */
        fun build(fragment: Fragment): OnRampCoordinator {
            return create(
                viewModelStoreOwner = fragment,
                lifecycleOwner = fragment,
                activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner) ?: fragment.requireActivity(),
                linkElementCallbackIdentifier = "LinkCoordinator"
            )
        }

        private fun create(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            linkElementCallbackIdentifier: String
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

    /**
     * Callbacks for handling OnRamp events.
     */
    data class OnRampCallbacks private constructor(
        val verificationResultCallback: (OnRampVerificationResult) -> Unit,
        val kycResultCallback: (OnRampKycResult) -> Unit,
        val configureCallback: (OnRampConfigureResult) -> Unit,
        val registrationCallback: (OnRampRegistrationResult) -> Unit,
        val lookupCallback: (OnRampLookupResult) -> Unit,
        val setWalletAddressCallback: (OnRampSetWalletAddressResult) -> Unit,
        val identityVerificationCallback: (OnRampIdentityVerificationResult) -> Unit
    ) {
        class Builder {
            private var verificationResultCallback: ((OnRampVerificationResult) -> Unit)? = null
            private var kycResultCallback: ((OnRampKycResult) -> Unit)? = null
            private var configureCallback: ((OnRampConfigureResult) -> Unit)? = null
            private var registrationCallback: ((OnRampRegistrationResult) -> Unit)? = null
            private var lookupCallback: ((OnRampLookupResult) -> Unit)? = null
            private var setWalletAddressCallback: ((OnRampSetWalletAddressResult) -> Unit)? = null
            private var identityVerificationCallback: ((OnRampIdentityVerificationResult) -> Unit)? = null

            fun verificationResultCallback(callback: (OnRampVerificationResult) -> Unit) = apply {
                this.verificationResultCallback = callback
            }

            fun kycResultCallback(callback: (OnRampKycResult) -> Unit) = apply {
                this.kycResultCallback = callback
            }

            fun configureCallback(callback: (OnRampConfigureResult) -> Unit) = apply {
                this.configureCallback = callback
            }

            fun registrationCallback(callback: (OnRampRegistrationResult) -> Unit) = apply {
                this.registrationCallback = callback
            }

            fun lookupCallback(callback: (OnRampLookupResult) -> Unit) = apply {
                this.lookupCallback = callback
            }

            fun setWalletAddressCallback(callback: (OnRampSetWalletAddressResult) -> Unit) = apply {
                this.setWalletAddressCallback = callback
            }

            fun identityVerificationCallback(callback: (OnRampIdentityVerificationResult) -> Unit) = apply {
                this.identityVerificationCallback = callback
            }

            fun build(): OnRampCallbacks {
                return OnRampCallbacks(
                    verificationResultCallback = verificationResultCallback
                        ?: throw IllegalArgumentException("verificationResultCallback is required"),
                    kycResultCallback = kycResultCallback
                        ?: throw IllegalArgumentException("kycResultCallback is required"),
                    configureCallback = configureCallback
                        ?: throw IllegalArgumentException("configureCallback is required"),
                    registrationCallback = registrationCallback
                        ?: throw IllegalArgumentException("registrationCallback is required"),
                    lookupCallback = lookupCallback
                        ?: throw IllegalArgumentException("lookupCallback is required"),
                    setWalletAddressCallback = setWalletAddressCallback
                        ?: throw IllegalArgumentException("setWalletAddressCallback is required"),
                    identityVerificationCallback = identityVerificationCallback
                        ?: throw IllegalArgumentException("identityVerificationCallback is required")
                )
            }
        }
    }
}
