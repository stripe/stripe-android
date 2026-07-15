package com.stripe.android.checkout

import com.stripe.android.checkout.injection.MerchantDisplayName
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

/**
 * Loads the payment element for a [CheckoutControllerState] and, on success, atomically commits it
 * to the [stateHolder], [confirmationStateHolder], and [selectionHolder]. Extracted from
 * [CheckoutController] so the load-and-commit flow can be unit tested in isolation from the
 * controller.
 */
@OptIn(CheckoutSessionPreview::class)
internal class CheckoutStateLoader @Inject constructor(
    @MerchantDisplayName private val merchantDisplayName: String,
    private val flagImageResolver: FlagImageResolver,
    private val paymentElementLoader: PaymentElementLoader,
    private val selectionChooser: EmbeddedSelectionChooser,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val confirmationStateHolder: CheckoutConfirmationStateHolder,
    private val stateHolder: CheckoutControllerStateHolder,
) {
    /**
     * Loads the payment element for [state] and, on success, atomically commits the resolved state.
     * Throws if the load fails, leaving the previously committed state untouched (the commits are
     * the final step, so a failure short-circuits before any holder is mutated).
     */
    suspend fun load(state: CheckoutControllerState) {
        val response = state.checkoutSessionResponse

        val resolvedState = state.copy(
            // [state] carries the previously resolved flag images forward (a mutation copies them
            // from the committed state), so they're reused when the currencies haven't changed.
            flagImages = flagImageResolver.resolve(response, cached = state.flagImages),
        )

        val baseConfig = EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
            .embeddedViewDisplaysMandateText(
                resolvedState.configuration.paymentElementConfiguration.embeddedViewDisplaysMandateText
            )
            .billingDetailsCollectionConfiguration(
                resolvedState.configuration.paymentElementConfiguration.billingDetailsCollectionConfiguration
                    .asPaymentSheet()
            )
            .build()

        val embeddedConfig = CheckoutConfigurationMerger.EmbeddedConfiguration(baseConfig)
            .forCheckoutSession(resolvedState)

        val loaderState = paymentElementLoader.load(
            initializationMode = resolvedState.initializationMode,
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
        // than blindly adopting the loader's recomputed selection (reuses the embedded logic).
        val selection = selectionChooser.choose(
            paymentMethodMetadata = loaderState.paymentMethodMetadata,
            paymentMethods = loaderState.customer?.paymentMethods,
            previousSelection = selectionHolder.selection.value,
            newSelection = loaderState.paymentSelection,
            newConfiguration = embeddedConfig.asCommonConfiguration(),
            formSheetAction = embeddedConfig.formSheetAction,
        )

        stateHolder.state = resolvedState
        confirmationStateHolder.state = CheckoutConfirmationStateHolder.State(
            paymentMethodMetadata = loaderState.paymentMethodMetadata,
            selection = selection,
            configuration = embeddedConfig,
        )
        selectionHolder.set(selection)
    }
}
