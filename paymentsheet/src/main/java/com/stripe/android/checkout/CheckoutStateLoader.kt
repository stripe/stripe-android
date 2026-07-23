package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Bundle
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal interface CheckoutStateLoader {
    suspend fun loadInitial(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    )

    suspend fun reload(state: CheckoutControllerState)
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutStateLoader @Inject constructor(
    private val embeddedConfigurationFactory: CheckoutEmbeddedConfigurationFactory,
    private val flagImageResolver: FlagImageResolver,
    private val paymentElementLoader: PaymentElementLoader,
    private val selectionChooser: EmbeddedSelectionChooser,
    private val stateHolder: CheckoutControllerStateHolder,
) : CheckoutStateLoader {
    override suspend fun loadInitial(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    ) {
        commit(
            configuration = configuration,
            response = checkoutSessionResponse,
            collectedDetails = CheckoutCollectedDetails(),
            carryForward = CarryForward.initial(),
        )
    }

    override suspend fun reload(state: CheckoutControllerState) {
        commit(
            configuration = state.configuration,
            response = state.checkoutSessionResponse,
            collectedDetails = state.collectedDetails,
            carryForward = CarryForward.from(state),
        )
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

        val loaderState = paymentElementLoader.load(
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                instancesKey = response.id,
                checkoutSessionResponse = response,
            ),
            integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
                isRowSelectionImmediateAction = false,
                configuration = embeddedConfig,
            ),
            metadata = PaymentElementLoader.Metadata(
                isReloadingAfterProcessDeath = false,
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        // Preserve the customer's existing selection across reloads when it's still valid, rather
        // than blindly adopting the loader's recomputed selection (reuses the embedded logic). The
        // previous selection comes from the incoming state, not a separate holder.
        val selection = selectionChooser.choose(
            paymentMethodMetadata = loaderState.paymentMethodMetadata,
            paymentMethods = loaderState.customer?.paymentMethods,
            previousSelection = carryForward.previousSelection,
            newSelection = loaderState.paymentSelection,
            newConfiguration = embeddedConfig.asCommonConfiguration(),
            formSheetAction = embeddedConfig.formSheetAction,
        )

        stateHolder.state = CheckoutControllerState(
            configuration = configuration,
            checkoutSessionResponse = response,
            flagImages = flagImages,
            collectedDetails = collectedDetails,
            paymentMethodMetadata = loaderState.paymentMethodMetadata,
            embeddedConfiguration = embeddedConfig,
            paymentSelection = selection,
            temporarySelection = carryForward.temporarySelection,
            previousNewSelections = carryForward.previousNewSelections,
        )
    }

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
