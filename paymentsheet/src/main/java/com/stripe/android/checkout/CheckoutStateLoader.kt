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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
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
    suspend fun loadInitial(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    ) {
        commit(
            configuration = configuration,
            response = checkoutSessionResponse,
            collectedDetails = configuration.asInitialCollectedDetails(),
            carryForward = CarryForward.initial(),
        )
    }

    suspend fun reload(state: CheckoutControllerState) {
        commit(
            configuration = state.configuration,
            response = state.checkoutSessionResponse,
            collectedDetails = state.collectedDetails,
            carryForward = CarryForward.from(state),
        )
    }

    fun clear() {
        stateHolder.state = null
        customerStateHolder.setCustomerState(null)
    }

    private suspend fun commit(
        configuration: CheckoutController.Configuration.State,
        response: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
        carryForward: CarryForward,
    ) {
        // [CarryForward.cachedFlagImages] carries the previously resolved images forward, so they're
        // reused when the currencies haven't changed.
        val flagImages = flagImageResolver.resolve(response, cached = carryForward.cachedFlagImages)

        val embeddedConfig = embeddedConfigurationFactory.create(
            configuration = configuration,
            checkoutSessionResponse = response,
            collectedDetails = collectedDetails,
        )
        embeddedConfig.appearance.parseAppearance()

        val commonConfiguration = commonConfigurationFactory.create(
            configuration = configuration,
            checkoutSessionResponse = response,
            collectedDetails = collectedDetails,
        )

        val expressCheckoutElementConfiguration = commonConfigurationFactory.createForExpressCheckoutElement(
            configuration = configuration,
            checkoutSessionResponse = response,
            collectedDetails = collectedDetails,
        )

        val loadResults = loadPaymentElements(
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(response.id, response),
            embeddedConfiguration = embeddedConfig,
            paymentMethodLayout = configuration.paymentElementConfiguration.paymentMethodLayout.asPaymentSheet(),
            expressCheckoutElementConfiguration = expressCheckoutElementConfiguration,
        )

        // Preserve the customer's existing selection across reloads when it's still valid, rather
        // than blindly adopting the loader's recomputed selection (reuses the embedded logic). The
        // previous selection comes from the incoming state, not a separate holder.
        val selection = selectionChooser.choose(
            paymentMethodMetadata = loadResults.paymentMethodMetadata,
            paymentMethods = loadResults.customer?.paymentMethods,
            previousSelection = carryForward.previousSelection,
            newSelection = loadResults.paymentSelection,
            newConfiguration = commonConfiguration,
            formSheetAction = embeddedConfig.formSheetAction,
        )

        stateHolder.state = CheckoutControllerState(
            configuration = configuration,
            checkoutSessionResponse = response,
            flagImages = flagImages,
            collectedDetails = collectedDetails,
            paymentMethodMetadata = loadResults.paymentMethodMetadata,
            expressCheckoutElementPaymentMethodMetadata = loadResults.expressCheckoutElementPaymentMethodMetadata,
            embeddedConfiguration = embeddedConfig,
            paymentSelection = selection,
            temporarySelection = carryForward.temporarySelection,
            previousNewSelections = carryForward.previousNewSelections,
        )

        customerStateHolder.setCustomerState(loadResults.customer)
    }

    private suspend fun loadPaymentElements(
        initializationMode: PaymentElementLoader.InitializationMode,
        embeddedConfiguration: EmbeddedPaymentElement.Configuration,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
        expressCheckoutElementConfiguration: CommonConfiguration?,
    ): LoadResults = coroutineScope {
        val metadata = PaymentElementLoader.Metadata(
            isReloadingAfterProcessDeath = false,
            initializedViaCompose = false,
        )
        val paymentElementStateDeferred = async {
            paymentElementLoader.load(
                initializationMode = initializationMode,
                integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
                    isRowSelectionImmediateAction = internalRowSelectionCallback.get() != null,
                    configuration = embeddedConfiguration,
                    paymentMethodLayout = paymentMethodLayout,
                ),
                metadata = metadata,
            ).getOrThrow()
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

        val paymentElementResult = paymentElementStateDeferred.await()
        val expressCheckoutElementResult = expressCheckoutElementStateDeferred?.await()
        LoadResults(
            paymentMethodMetadata = paymentElementResult.paymentMethodMetadata,
            expressCheckoutElementPaymentMethodMetadata = expressCheckoutElementResult?.paymentMethodMetadata,
            customer = paymentElementResult.customer,
            paymentSelection = paymentElementResult.paymentSelection,
        )
    }

    private data class LoadResults(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val expressCheckoutElementPaymentMethodMetadata: PaymentMethodMetadata?,
        val customer: CustomerState?,
        val paymentSelection: PaymentSelection?,
    )

    /**
     * The fields carried from the prior state (or fresh defaults) into the next committed state, so a
     * [reload] preserves everything the load itself doesn't recompute. Collapses [commit]'s parameter
     * list into a single carrier.
     */
    private data class CarryForward(
        val cachedFlagImages: Map<String, Bitmap>?,
        val previousSelection: PaymentSelection?,
        val temporarySelection: String?,
        val previousNewSelections: Bundle,
    ) {
        companion object {
            fun initial() = CarryForward(
                cachedFlagImages = null,
                previousSelection = null,
                temporarySelection = null,
                previousNewSelections = Bundle(),
            )

            fun from(state: CheckoutControllerState) = CarryForward(
                cachedFlagImages = state.flagImages,
                previousSelection = state.paymentSelection,
                temporarySelection = state.temporarySelection,
                previousNewSelections = state.previousNewSelections,
            )
        }
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutController.Configuration.State.asInitialCollectedDetails(): CheckoutCollectedDetails {
    return CheckoutCollectedDetails(
        email = defaults.email,
        shippingName = defaults.shippingDetails?.name,
        billingName = defaults.billingDetails?.name,
        shippingAddress = defaults.shippingDetails?.address,
        billingAddress = defaults.billingDetails?.address,
    )
}
