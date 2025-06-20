package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.LinkCoordinator
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection.Link
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.viewmodels.LinkCoordinatorViewModel
import javax.inject.Inject
import javax.inject.Named

internal class DefaultLinkCoordinator @Inject internal constructor(
    lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val paymentOptionCallback: (PaymentOption?) -> Unit,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val viewModel: LinkCoordinatorViewModel,
    @Named(LinkCoordinatorModule.LINK_COORDINATOR_LINK_LAUNCHER)
    private val linkPaymentLauncher: LinkPaymentLauncher,
) : LinkCoordinator, DefaultLifecycleObserver {

    /**
     * [LinkCoordinatorComponent] is held to inject into [Activity]s and created
     * after [DefaultLinkCoordinator].
     */
    lateinit var linkCoordinatorComponent: LinkCoordinatorComponent

    init {
        lifecycleOwner.lifecycle.addObserver(this)

        // Register LinkPaymentLauncher
        linkPaymentLauncher.register(
            key = LinkCoordinatorModule.LINK_COORDINATOR_LINK_LAUNCHER,
            activityResultRegistry = activityResultRegistryOwner.activityResultRegistry,
            callback = ::onLinkResult
        )
    }

    override fun configure(
        configuration: LinkCoordinator.Configuration,
        callback: LinkCoordinator.ConfigCallback
    ) {
        viewModel.configuration = configuration
        // validate the configuration and potentially make network calls
        callback.onConfigured(success = true, error = null)
    }

    override fun getPaymentOption(): PaymentOption? {
        return viewModel.paymentSelection?.let { selection -> paymentOptionFactory.create(selection) }
    }

    override fun present() {
        val configuration = viewModel.configuration
        if (configuration != null) {
            // Create LinkConfiguration from our configuration
            val linkConfiguration = LinkConfiguration(
                stripeIntent = configuration.stripeIntent,
                merchantName = configuration.merchantName,
                merchantCountryCode = "US", // TODO: Make this configurable
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = null, // TODO: Make this configurable
                    email = configuration.customerEmail,
                    phone = null, // TODO: Make this configurable
                    billingCountryCode = null, // TODO: Make this configurable
                ),
                shippingDetails = null, // TODO: Make this configurable
                passthroughModeEnabled = false,
                flags = emptyMap(),
                cardBrandChoice = null,
                cardBrandFilter = DefaultCardBrandFilter,
                useAttestationEndpointsForLink = false,
                suppress2faModal = false,
                disableRuxInFlowController = false,
                initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = configuration.stripeIntent.let { intent ->
                        when (intent) {
                            is PaymentIntent -> IntentConfiguration(
                                mode = IntentConfiguration.Mode.Payment(
                                    amount = intent.amount ?: 0,
                                    currency = intent.currency ?: "usd"
                                )
                            )
                            is SetupIntent -> IntentConfiguration(
                                mode = IntentConfiguration.Mode.Setup()
                            )
                        }
                    }
                ),
                elementsSessionId = "",
                linkMode = LinkMode.LinkCardBrand,
                allowDefaultOptIn = false,
            )

            // Present Link with basic configuration
            linkPaymentLauncher.present(
                configuration = linkConfiguration,
                linkAccountInfo = LinkAccountUpdate.Value(account = null),
                launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment = null),
                useLinkExpress = false
            )
        }
    }

    override fun confirm() {
        // TODO: Implement Link confirmation logic using injected dependencies
        // Can now use context, coroutineScope, etc.
    }

    private fun onLinkResult(result: LinkActivityResult) {
        // TODO: Handle Link result and update payment selection
        when (result) {
            is LinkActivityResult.Completed -> {
                val pm = Link(selectedPayment = result.selectedPayment)
                viewModel.paymentSelection = pm
                paymentOptionCallback.invoke(paymentOptionFactory.create(pm))
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
            activityResultCaller: ActivityResultCaller,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            paymentOptionCallback: (PaymentOption?) -> Unit,
            linkElementCallbackIdentifier: String,
        ): LinkCoordinator {
            val linkCoordinatorViewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = LinkCoordinatorViewModel.Factory(linkElementCallbackIdentifier),
            ).get(
                key = "LinkCoordinatorViewModel(instance = $linkElementCallbackIdentifier)",
                modelClass = LinkCoordinatorViewModel::class.java
            )

            val linkCoordinatorStateComponent = linkCoordinatorViewModel.linkCoordinatorStateComponent

            val linkCoordinatorComponent: LinkCoordinatorComponent =
                linkCoordinatorStateComponent.linkCoordinatorComponentBuilder
                    .lifecycleOwner(lifecycleOwner)
                    .activityResultCaller(activityResultCaller)
                    .activityResultRegistryOwner(activityResultRegistryOwner)
                    .paymentOptionCallback(paymentOptionCallback)
                    .linkElementCallbackIdentifier(linkElementCallbackIdentifier)
                    .build()
            val linkCoordinator = linkCoordinatorComponent.linkCoordinator
            linkCoordinator.linkCoordinatorComponent = linkCoordinatorComponent
            return linkCoordinator
        }
    }
} 