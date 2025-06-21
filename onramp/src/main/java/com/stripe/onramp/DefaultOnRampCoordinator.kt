package com.stripe.onramp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.onramp.repositories.CryptoRepository
import javax.inject.Inject
import javax.inject.Named

internal class DefaultOnRampCoordinator @Inject internal constructor(
    lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val paymentOptionCallback: (PaymentOption?) -> Unit,
    private val userAuthenticatedCallback: ((OnRampCoordinator.User) -> Unit)?,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val viewModel: OnRampCoordinatorViewModel,
    private val linkApiRepository: LinkApiRepository,
    private val cryptoRepository: CryptoRepository,
    @Named(OnRampCoordinatorModule.LINK_COORDINATOR_LINK_LAUNCHER)
    private val linkPaymentLauncher: LinkPaymentLauncher,
) : OnRampCoordinator, DefaultLifecycleObserver {

    init {
        lifecycleOwner.lifecycle.addObserver(this)

        // Register LinkPaymentLauncher
        linkPaymentLauncher.register(
            key = OnRampCoordinatorModule.LINK_COORDINATOR_LINK_LAUNCHER,
            activityResultRegistry = activityResultRegistryOwner.activityResultRegistry,
            callback = ::onLinkResult
        )
    }

    override fun configure(
        configuration: OnRampCoordinator.Configuration,
        callback: OnRampCoordinator.ConfigCallback
    ) {
        FeatureFlags.nativeLinkEnabled.setEnabled(true)
        viewModel.configuration = configuration
        // validate the configuration and potentially make network calls
        callback.onConfigured(success = true, error = null)
    }

    override suspend fun hasAccount(emailAddress: String): Result<Boolean> {
        // TODO this should use the Link standalone component rather than the LinkApiRepository
        return linkApiRepository.lookupConsumer(emailAddress).map { it.exists }
    }

    override suspend fun setKycInfo(kycInfo: OnRampCoordinator.KycInfo): kotlin.Result<Unit> {
        return cryptoRepository.submitKycInfo(kycInfo)
    }

    override fun promptForLinkAuthentication() {
        val configuration = viewModel.configuration
        if (configuration != null) {
            // Create LinkConfiguration from our configuration
            val linkConfiguration = LinkConfiguration(
                stripeIntent = configuration.stripeIntent,
                merchantName = configuration.merchantName,
                merchantCountryCode = "US", // TODO: Make this configurable
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = null,
                    email = null,
                    phone = null,
                    billingCountryCode = null,
                ),
                shippingDetails = null,
                passthroughModeEnabled = false,
                flags = emptyMap(),
                cardBrandChoice = null,
                cardBrandFilter = DefaultCardBrandFilter,
                useAttestationEndpointsForLink = true,
                suppress2faModal = false,
                disableRuxInFlowController = false,
                initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = configuration.stripeIntent.let { intent ->
                        when (intent) {
                            is PaymentIntent -> PaymentSheet.IntentConfiguration(
                                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                                    amount = intent.amount ?: 0,
                                    currency = intent.currency ?: "usd"
                                )
                            )
                            is SetupIntent -> PaymentSheet.IntentConfiguration(
                                mode = PaymentSheet.IntentConfiguration.Mode.Setup()
                            )
                        }
                    }
                ),
                elementsSessionId = "",
                linkMode = LinkMode.LinkCardBrand,
                allowDefaultOptIn = false,
            )

            // Present Link in Authentication mode
            linkPaymentLauncher.present(
                configuration = linkConfiguration,
                linkAccountInfo = LinkAccountUpdate.Value(account = null),
                launchMode = LinkLaunchMode.Authentication,
                useLinkExpress = false
            )
        }
    }

    private fun onLinkResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Completed -> {
                // TODO this assumes the flow is just launched for authentication.
                if (result.linkAccountUpdate is LinkAccountUpdate.Value) {
                    val linkAccount = (result.linkAccountUpdate as LinkAccountUpdate.Value)
                        .account
                    if (linkAccount != null) {
                        // Create User object from LinkAccount
                        val user = OnRampCoordinator.User(
                            email = linkAccount.email,
                            phone = linkAccount.unredactedPhoneNumber,
                            isVerified = linkAccount.isVerified,
                            completedSignup = linkAccount.completedSignup
                        )
                        userAuthenticatedCallback?.invoke(user)
                    }
                }
            }
            is LinkActivityResult.Canceled -> {
                // Handle cancellation
            }
            is LinkActivityResult.Failed -> {
                // Handle failure
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                // Handle payment method obtained
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        linkPaymentLauncher.unregister()
        super.onDestroy(owner)
    }

    companion object {
        fun getInstance(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            paymentOptionCallback: (PaymentOption?) -> Unit,
            userAuthenticatedCallback: ((OnRampCoordinator.User) -> Unit)? = null,
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
                    .paymentOptionCallback(paymentOptionCallback)
                    .userAuthenticatedCallback(userAuthenticatedCallback)
                    .build()

            return onRampComponent.linkCoordinator
        }
    }
}
