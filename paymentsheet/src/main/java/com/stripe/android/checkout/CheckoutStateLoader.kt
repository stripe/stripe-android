package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Bundle
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
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
    private val durationProvider: DurationProvider,
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
        val flagImages = durationProvider.measureDuration(DurationProvider.Key.CheckoutSessionResolveFlags) {
            flagImageResolver.resolve(response, cached = carryForward.cachedFlagImages)
        }

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

        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = response.id,
            checkoutSessionResponse = response,
        )
        val loaderMetadata = PaymentElementLoader.Metadata(
            isReloadingAfterProcessDeath = false,
            initializedViaCompose = false,
        )
        val loadedPaymentElements = loadPaymentElements(
            configuration = configuration,
            response = response,
            collectedDetails = collectedDetails,
            embeddedConfiguration = embeddedConfig,
            initializationMode = initializationMode,
            loaderMetadata = loaderMetadata,
        )
        val paymentElementLoaderState = loadedPaymentElements.paymentElementState

        // Preserve the customer's existing selection across reloads when it's still valid, rather
        // than blindly adopting the loader's recomputed selection (reuses the embedded logic). The
        // previous selection comes from the incoming state, not a separate holder.
        val selection = selectionChooser.choose(
            paymentMethodMetadata = paymentElementLoaderState.paymentMethodMetadata,
            paymentMethods = paymentElementLoaderState.customer?.paymentMethods,
            previousSelection = carryForward.previousSelection,
            newSelection = paymentElementLoaderState.paymentSelection,
            newConfiguration = commonConfiguration,
            formSheetAction = embeddedConfig.formSheetAction,
        )

        stateHolder.state = CheckoutControllerState(
            configuration = configuration,
            checkoutSessionResponse = response,
            flagImages = flagImages,
            collectedDetails = collectedDetails,
            paymentElementPaymentMethodMetadata = paymentElementLoaderState.paymentMethodMetadata,
            expressCheckoutElementPaymentMethodMetadata = loadedPaymentElements.expressCheckoutElementMetadata,
            embeddedConfiguration = embeddedConfig,
            paymentSelection = selection,
            temporarySelection = carryForward.temporarySelection,
            previousNewSelections = carryForward.previousNewSelections,
        )

        customerStateHolder.setCustomerState(paymentElementLoaderState.customer)
    }

    private suspend fun loadPaymentElements(
        configuration: CheckoutController.Configuration.State,
        response: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
        embeddedConfiguration: EmbeddedPaymentElement.Configuration,
        initializationMode: PaymentElementLoader.InitializationMode,
        loaderMetadata: PaymentElementLoader.Metadata,
    ): LoadedPaymentElements = coroutineScope {
        val paymentElementState = async {
            loadPaymentElement(
                configuration = configuration,
                embeddedConfiguration = embeddedConfiguration,
                initializationMode = initializationMode,
                loaderMetadata = loaderMetadata,
            )
        }
        val expressCheckoutElementMetadata = async {
            loadExpressCheckoutElementPaymentMethodMetadata(
                configuration = configuration,
                response = response,
                collectedDetails = collectedDetails,
                initializationMode = initializationMode,
                loaderMetadata = loaderMetadata,
            )
        }

        val loadedPaymentElementState = paymentElementState.await()
        LoadedPaymentElements(
            paymentElementState = loadedPaymentElementState,
            expressCheckoutElementMetadata = expressCheckoutElementMetadata.await()
                ?: loadedPaymentElementState.paymentMethodMetadata,
        )
    }

    private suspend fun loadExpressCheckoutElementPaymentMethodMetadata(
        configuration: CheckoutController.Configuration.State,
        response: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
        initializationMode: PaymentElementLoader.InitializationMode,
        loaderMetadata: PaymentElementLoader.Metadata,
    ): PaymentMethodMetadata? {
        val commonConfiguration = commonConfigurationFactory.createForExpressCheckoutElement(
            configuration = configuration,
            checkoutSessionResponse = response,
            collectedDetails = collectedDetails,
        ) ?: return null

        return durationProvider.measureDuration(
            DurationProvider.Key.CheckoutSessionLoadExpressCheckoutElement
        ) {
            paymentElementLoader.load(
                initializationMode = initializationMode,
                integrationConfiguration = PaymentElementLoader.Configuration.Checkout(
                    commonConfiguration = commonConfiguration,
                    paymentMethodLayout = configuration.paymentElementConfiguration
                        .paymentMethodLayout.asPaymentSheet(),
                ),
                metadata = loaderMetadata,
            ).getOrThrow().paymentMethodMetadata
        }
    }

    private suspend fun loadPaymentElement(
        configuration: CheckoutController.Configuration.State,
        embeddedConfiguration: EmbeddedPaymentElement.Configuration,
        initializationMode: PaymentElementLoader.InitializationMode,
        loaderMetadata: PaymentElementLoader.Metadata,
    ): PaymentElementLoader.State {
        return durationProvider.measureDuration(DurationProvider.Key.CheckoutSessionLoadPaymentElement) {
            paymentElementLoader.load(
                initializationMode = initializationMode,
                integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
                    isRowSelectionImmediateAction = internalRowSelectionCallback.get() != null,
                    configuration = embeddedConfiguration,
                    paymentMethodLayout = configuration.paymentElementConfiguration
                        .paymentMethodLayout.asPaymentSheet(),
                ),
                metadata = loaderMetadata,
            ).getOrThrow()
        }
    }

    private data class LoadedPaymentElements(
        val paymentElementState: PaymentElementLoader.State,
        val expressCheckoutElementMetadata: PaymentMethodMetadata,
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
