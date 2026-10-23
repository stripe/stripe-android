package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Bundle
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.validateShippingCountry
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Provider

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutStateLoader @Inject constructor(
    private val embeddedConfigurationFactory: CheckoutEmbeddedConfigurationFactory,
    private val commonConfigurationFactory: CheckoutCommonConfigurationFactory,
    private val flagImageResolver: FlagImageResolver,
    private val paymentElementLoader: PaymentElementLoader,
    private val selectionChooser: EmbeddedSelectionChooser,
    private val stateHolder: CheckoutControllerStateHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
) {
    /**
     * Loads the initial state without publishing it, so [CheckoutController.configure] can finish
     * its tax update before the session becomes observable.
     */
    suspend fun loadInitial(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    ): LoadedState {
        return load(
            configuration = configuration,
            response = checkoutSessionResponse,
            collectedDetails = configuration.asInitialCollectedDetails(checkoutSessionResponse),
            carryForward = CarryForward.initial(),
        )
    }

    suspend fun reload(state: CheckoutControllerState) {
        publish(
            load(
                configuration = state.configuration,
                response = state.checkoutSessionResponse,
                collectedDetails = state.collectedDetails,
                carryForward = CarryForward.from(state),
            )
        )
    }

    fun publish(loadedState: LoadedState) {
        stateHolder.state = loadedState.state
        customerStateHolder.setCustomerState(loadedState.customer)
    }

    fun clear() {
        stateHolder.state = null
        customerStateHolder.setCustomerState(null)
    }

    private suspend fun load(
        configuration: CheckoutController.Configuration.State,
        response: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
        carryForward: CarryForward,
    ): LoadedState {
        // [CarryForward.cachedFlagImages] carries the previously resolved images forward, so they're
        // reused when the currencies haven't changed.
        val flagImages = flagImageResolver.resolve(response, cached = carryForward.cachedFlagImages)

        val paymentElementConfigs = getPaymentElementConfigs(
            configuration = configuration,
            response = response,
            collectedDetails = collectedDetails,
        )

        paymentElementConfigs?.embeddedConfig?.appearance?.parseAppearance()

        val expressCheckoutElementConfiguration = commonConfigurationFactory.createForExpressCheckoutElement(
            configuration = configuration,
            checkoutSessionResponse = response,
            collectedDetails = collectedDetails,
        )

        val loadResults = loadPaymentElements(
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                instancesKey = response.id,
                checkoutSessionResponse = response,
                collectedEmail = collectedDetails.email,
            ),
            paymentElementLoaderConfig = paymentElementConfigs?.paymentElementLoaderConfig,
            expressCheckoutElementConfiguration = expressCheckoutElementConfiguration,
        )

        val selection = getSelection(
            selectionChooser = selectionChooser,
            loadResults = loadResults,
            carryForward = carryForward,
            commonConfiguration = paymentElementConfigs?.commonConfiguration,
            embeddedConfig = paymentElementConfigs?.embeddedConfig,
        )

        val state = CheckoutControllerState(
            configuration = configuration,
            checkoutSessionResponse = response,
            flagImages = flagImages,
            collectedDetails = collectedDetails,
            paymentMethodMetadata = loadResults.paymentMethodMetadata,
            expressCheckoutElementPaymentMethodMetadata = loadResults.expressCheckoutElementPaymentMethodMetadata,
            embeddedConfiguration = paymentElementConfigs?.embeddedConfig,
            paymentSelection = selection,
            savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
            temporarySelection = carryForward.temporarySelection,
            previousNewSelections = carryForward.previousNewSelections,
            linkEagerPresentationSuppressed = carryForward.linkEagerPresentationSuppressed,
            mandateState = carryForward.mandateState,
        )
        return LoadedState(state = state, customer = loadResults.customer)
    }

    private fun getSelection(
        selectionChooser: EmbeddedSelectionChooser,
        loadResults: LoadResults,
        carryForward: CarryForward,
        commonConfiguration: CommonConfiguration?,
        embeddedConfig: EmbeddedPaymentElement.Configuration?,
    ): PaymentSelection? {
        return if (loadResults.paymentMethodMetadata == null || embeddedConfig == null || commonConfiguration == null) {
            null
        } else {
            // Preserve the customer's existing selection across reloads when it's still valid, rather
            // than blindly adopting the loader's recomputed selection (reuses the embedded logic). The
            // previous selection comes from the incoming state, not a separate holder. An empty
            // selection on reload must stay empty until the customer selects again or reconfigures.
            selectionChooser.choose(
                paymentMethodMetadata = loadResults.paymentMethodMetadata,
                paymentMethods = loadResults.customer?.paymentMethods,
                previousSelection = carryForward.previousSelection,
                newSelection = loadResults.paymentSelection.takeIf {
                    carryForward.isInitialLoad || carryForward.previousSelection != null
                },
                newConfiguration = commonConfiguration,
                formSheetAction = embeddedConfig.formSheetAction,
            )
        }
    }

    private suspend fun loadPaymentElements(
        initializationMode: PaymentElementLoader.InitializationMode,
        paymentElementLoaderConfig: PaymentElementLoader.Configuration.Embedded?,
        expressCheckoutElementConfiguration: CommonConfiguration?,
    ): LoadResults = coroutineScope {
        val metadata = PaymentElementLoader.Metadata(
            isReloadingAfterProcessDeath = false,
            initializedViaCompose = false,
        )
        val paymentElementStateDeferred = paymentElementLoaderConfig?.let {
            async {
                paymentElementLoader.load(
                    initializationMode = initializationMode,
                    integrationConfiguration = paymentElementLoaderConfig,
                    metadata = metadata,
                ).getOrThrow()
            }
        }
        val expressCheckoutElementStateDeferred = expressCheckoutElementConfiguration?.let { configuration ->
            async {
                paymentElementLoader.load(
                    initializationMode = initializationMode,
                    integrationConfiguration = PaymentElementLoader.Configuration.ExpressCheckoutElement(
                        commonConfiguration = configuration,
                    ),
                    metadata = metadata,
                ).getOrThrow()
            }
        }

        val paymentElementResult = paymentElementStateDeferred?.await()
        val expressCheckoutElementResult = expressCheckoutElementStateDeferred?.await()
        LoadResults(
            paymentMethodMetadata = paymentElementResult?.paymentMethodMetadata,
            expressCheckoutElementPaymentMethodMetadata = expressCheckoutElementResult?.paymentMethodMetadata,
            customer = paymentElementResult?.customer,
            paymentSelection = paymentElementResult?.paymentSelection,
        )
    }

    private fun getPaymentElementConfigs(
        configuration: CheckoutController.Configuration.State,
        response: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): PaymentElementConfigurations? {
        val paymentElementConfiguration = configuration.paymentElementConfiguration ?: return null

        val embeddedConfig = embeddedConfigurationFactory.create(
            configuration = configuration,
            checkoutSessionResponse = response,
            collectedDetails = collectedDetails,
        )

        val commonConfiguration = commonConfigurationFactory.create(
            configuration = configuration,
            checkoutSessionResponse = response,
            collectedDetails = collectedDetails,
        )

        if (embeddedConfig == null) {
            // TODO: report unexpected error
            return null
        }

        if (commonConfiguration == null) {
            // TODO: report unexpected error
            return null
        }

        val paymentElementLoaderConfig = PaymentElementLoader.Configuration.Embedded(
            isRowSelectionImmediateAction = internalRowSelectionCallback.get() != null,
            configuration = embeddedConfig,
            paymentMethodLayout = paymentElementConfiguration.paymentMethodLayout.asPaymentSheet(),
        )

        return PaymentElementConfigurations(
            embeddedConfig = embeddedConfig,
            commonConfiguration = commonConfiguration,
            paymentElementLoaderConfig = paymentElementLoaderConfig,
        )
    }

    data class LoadedState(
        val state: CheckoutControllerState,
        val customer: CustomerState?,
    )

    private data class LoadResults(
        val paymentMethodMetadata: PaymentMethodMetadata?,
        val expressCheckoutElementPaymentMethodMetadata: PaymentMethodMetadata?,
        val customer: CustomerState?,
        val paymentSelection: PaymentSelection?,
    )

    private data class PaymentElementConfigurations(
        val commonConfiguration: CommonConfiguration,
        val embeddedConfig: EmbeddedPaymentElement.Configuration,
        val paymentElementLoaderConfig: PaymentElementLoader.Configuration.Embedded,
    )

    /**
     * The fields carried from the prior state (or fresh defaults) into the next committed state, so a
     * [reload] preserves everything the load itself doesn't recompute. Collapses [commit]'s parameter
     * list into a single carrier.
     */
    private data class CarryForward(
        val isInitialLoad: Boolean,
        val cachedFlagImages: Map<String, Bitmap>?,
        val previousSelection: PaymentSelection?,
        val temporarySelection: String?,
        val previousNewSelections: Bundle,
        val linkEagerPresentationSuppressed: Boolean,
        val mandateState: CheckoutMandateState,
    ) {
        companion object {
            fun initial() = CarryForward(
                isInitialLoad = true,
                cachedFlagImages = null,
                previousSelection = null,
                temporarySelection = null,
                previousNewSelections = Bundle(),
                linkEagerPresentationSuppressed = false,
                mandateState = CheckoutMandateState.initial(),
            )

            fun from(state: CheckoutControllerState) = CarryForward(
                isInitialLoad = false,
                cachedFlagImages = state.flagImages,
                previousSelection = state.paymentSelection,
                temporarySelection = state.temporarySelection,
                previousNewSelections = state.previousNewSelections,
                linkEagerPresentationSuppressed = state.linkEagerPresentationSuppressed,
                mandateState = state.mandateState,
            )
        }
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutController.Configuration.State.asInitialCollectedDetails(
    checkoutSessionResponse: CheckoutSessionResponse,
): CheckoutCollectedDetails {
    val shippingDetails = defaults.shippingDetails?.takeIf { details ->
        val shippingAddress = details.address
        shippingAddressElementConfiguration == null ||
            shippingAddress == null ||
            checkoutSessionResponse.validateShippingCountry(shippingAddress.country).isSuccess
    }
    return CheckoutCollectedDetails(
        email = defaults.email,
        shippingName = shippingDetails?.name,
        shippingAddress = shippingDetails?.address,
    )
}
