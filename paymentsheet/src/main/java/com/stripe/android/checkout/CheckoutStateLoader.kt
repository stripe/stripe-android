package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Bundle
import com.stripe.android.checkout.injection.MerchantDisplayName
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutStateLoader @Inject constructor(
    @MerchantDisplayName private val merchantDisplayName: String,
    private val flagImageResolver: FlagImageResolver,
    private val paymentElementLoader: PaymentElementLoader,
    private val selectionChooser: EmbeddedSelectionChooser,
    private val stateHolder: CheckoutControllerStateHolder,
) {
    suspend fun loadInitial(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    ) {
        commit(
            configuration = configuration,
            sessionData = InitialSessionData(checkoutSessionResponse),
            carryForward = CarryForward.initial(),
        )
    }

    suspend fun reload(state: CheckoutControllerState) {
        commit(
            configuration = state.configuration,
            sessionData = state,
            carryForward = CarryForward.from(state),
        )
    }

    private suspend fun commit(
        configuration: CheckoutController.Configuration.State,
        sessionData: CheckoutSessionData,
        carryForward: CarryForward,
    ) {
        val response = sessionData.checkoutSessionResponse

        // [CarryForward.cachedFlagImages] carries the previously resolved images forward, so they're
        // reused when the currencies haven't changed.
        val flagImages = flagImageResolver.resolve(response, cached = carryForward.cachedFlagImages)

        val baseConfig = EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
            .embeddedViewDisplaysMandateText(
                configuration.paymentElementConfiguration.embeddedViewDisplaysMandateText
            )
            .build()

        val embeddedConfig = CheckoutConfigurationMerger.EmbeddedConfiguration(baseConfig)
            .forCheckoutSession(sessionData)

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
            key = response.id,
            configuration = configuration,
            checkoutSessionResponse = response,
            flagImages = flagImages,
            collectedDetails = sessionData.toCollectedDetails(),
            integrationLaunched = carryForward.integrationLaunched,
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
        val integrationLaunched: Boolean,
    ) {
        companion object {
            fun initial() = CarryForward(
                cachedFlagImages = null,
                previousSelection = null,
                temporarySelection = null,
                previousNewSelections = Bundle(),
                integrationLaunched = false,
            )

            fun from(state: CheckoutControllerState) = CarryForward(
                cachedFlagImages = state.flagImages,
                previousSelection = state.paymentSelection,
                temporarySelection = state.temporarySelection,
                previousNewSelections = state.previousNewSelections,
                integrationLaunched = state.integrationLaunched,
            )
        }
    }

    /** The [CheckoutSessionData] for a first load: the session response with no collected details. */
    private class InitialSessionData(
        override val checkoutSessionResponse: CheckoutSessionResponse,
        private val collectedDetails: CheckoutCollectedDetails = CheckoutCollectedDetails(),
    ) : CheckoutSessionData {
        override val shippingName: String? get() = collectedDetails.shippingName
        override val billingName: String? get() = collectedDetails.billingName
        override val shippingPhoneNumber: String? get() = collectedDetails.shippingPhoneNumber
        override val billingPhoneNumber: String? get() = collectedDetails.billingPhoneNumber
        override val shippingAddress: Address.State? get() = collectedDetails.shippingAddress
        override val billingAddress: Address.State? get() = collectedDetails.billingAddress
    }
}

/** Projects the collected details carried on any [CheckoutSessionData] into a [CheckoutCollectedDetails]. */
@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionData.toCollectedDetails() = CheckoutCollectedDetails(
    shippingName = shippingName,
    billingName = billingName,
    shippingPhoneNumber = shippingPhoneNumber,
    billingPhoneNumber = billingPhoneNumber,
    shippingAddress = shippingAddress,
    billingAddress = billingAddress,
)
