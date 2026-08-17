package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Bundle
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutStateLoader @Inject constructor(
    private val embeddedConfigurationFactory: CheckoutEmbeddedConfigurationFactory,
    private val commonConfigurationFactory: CheckoutCommonConfigurationFactory,
    private val flagImageResolver: FlagImageResolver,
    private val paymentElementLoader: PaymentElementLoader,
    private val selectionChooser: EmbeddedSelectionChooser,
    private val stateHolder: CheckoutControllerStateHolder,
    private val customerStateHolder: CustomerStateHolder,
) {
    suspend fun loadInitial(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
    ) {
        commit(
            configuration = configuration,
            response = checkoutSessionResponse,
            collectedDetails = configuration.asInitialCollectedDetails(),
            cachedFlagImages = null,
            latestCarryForward = { CarryForward.initial() },
        )
    }

    /**
     * Commits [checkoutSessionResponse] onto the latest committed state, applying
     * [updateCollectedDetails] to the details collected so far. Selection state is read after the
     * reload finishes so a choice written while it was in flight survives the commit.
     */
    suspend fun reload(
        checkoutSessionResponse: CheckoutSessionResponse,
        updateCollectedDetails: (CheckoutCollectedDetails) -> CheckoutCollectedDetails,
    ): Boolean {
        val state = stateHolder.state ?: return false
        return commit(
            configuration = state.configuration,
            response = checkoutSessionResponse,
            collectedDetails = updateCollectedDetails(state.collectedDetails),
            cachedFlagImages = state.flagImages,
            latestCarryForward = { stateHolder.state?.let(CarryForward::from) },
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
        cachedFlagImages: Map<String, Bitmap>?,
        latestCarryForward: () -> CarryForward?,
    ): Boolean {
        val flagImages = flagImageResolver.resolve(response, cached = cachedFlagImages)

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

        val loaderState = paymentElementLoader.load(
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                instancesKey = response.id,
                checkoutSessionResponse = response,
            ),
            integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
                isRowSelectionImmediateAction = false,
                configuration = embeddedConfig,
                paymentMethodLayout = configuration.paymentElementConfiguration.paymentMethodLayout.asPaymentSheet(),
            ),
            metadata = PaymentElementLoader.Metadata(
                isReloadingAfterProcessDeath = false,
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        // Selection writes do not use the operation gate. Read them after all suspending work so a
        // choice made while the reload was in flight survives the commit.
        val carryForward = latestCarryForward() ?: return false

        // Preserve the customer's existing selection across reloads when it's still valid, rather
        // than blindly adopting the loader's recomputed selection (reuses the embedded logic). The
        // previous selection comes from the latest committed state, not a separate holder.
        val selection = selectionChooser.choose(
            paymentMethodMetadata = loaderState.paymentMethodMetadata,
            paymentMethods = loaderState.customer?.paymentMethods,
            previousSelection = carryForward.previousSelection,
            newSelection = loaderState.paymentSelection,
            newConfiguration = commonConfiguration,
            formSheetAction = embeddedConfig.formSheetAction,
        )

        stateHolder.state = CheckoutControllerState(
            configuration = configuration,
            checkoutSessionResponse = response,
            flagImages = flagImages,
            collectedDetails = collectedDetails,
            paymentMethodMetadata = loaderState.paymentMethodMetadata,
            embeddedConfiguration = embeddedConfig,
            commonConfiguration = commonConfiguration,
            paymentSelection = selection,
            temporarySelection = carryForward.temporarySelection,
            previousNewSelections = carryForward.previousNewSelections,
        )

        customerStateHolder.setCustomerState(loaderState.customer)
        return true
    }

    /**
     * Selection fields carried from the prior state (or fresh defaults) into the next committed state.
     */
    private data class CarryForward(
        val previousSelection: PaymentSelection?,
        val temporarySelection: String?,
        val previousNewSelections: Bundle,
    ) {
        companion object {
            fun initial() = CarryForward(
                previousSelection = null,
                temporarySelection = null,
                previousNewSelections = Bundle(),
            )

            fun from(state: CheckoutControllerState) = CarryForward(
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
        shippingName = defaults.shippingDetails?.name,
        billingName = defaults.billingDetails?.name,
        shippingAddress = defaults.shippingDetails?.address,
        billingAddress = defaults.billingDetails?.address,
    )
}
