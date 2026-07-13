package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LoadedPaymentSelectionResolver
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import javax.inject.Inject

/**
 * Adapts [PaymentSelectionUpdater] to the loader's [LoadedPaymentSelectionResolver] contract so
 * FlowController's selection-preservation logic runs inside [PaymentElementLoader.load].
 */
internal class PaymentSelectionUpdaterResolver @Inject constructor(
    private val updater: PaymentSelectionUpdater,
) : LoadedPaymentSelectionResolver {
    override fun resolve(
        state: PaymentElementLoader.State,
        integrationConfiguration: PaymentElementLoader.Configuration,
        reconfigureContext: PaymentElementLoader.ReconfigureContext?,
    ): PaymentSelection? {
        val configuration = (integrationConfiguration as? PaymentElementLoader.Configuration.PaymentSheet)
            ?.configuration ?: return state.paymentSelection

        return updater(
            selection = reconfigureContext?.previousSelection,
            previousConfig = reconfigureContext?.previousPaymentSheetConfig,
            newState = PaymentSheetState.Full(state),
            newConfig = configuration,
            walletButtonsAlreadyShown = reconfigureContext?.walletButtonsAlreadyShown ?: false,
        )
    }
}
