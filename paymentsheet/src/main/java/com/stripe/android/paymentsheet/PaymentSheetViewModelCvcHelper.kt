package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen

internal fun PaymentSheetViewModel.shouldLaunchCvcRecollectionScreen(selection: PaymentSelection.Saved): Boolean {
    return requiresCvcRecollection(selection) {
        this.paymentMethodMetadata.value?.getPaymentMethodLayout(config.paymentMethodLayout) != PaymentSheet.PaymentMethodLayout.Horizontal &&
            navigationHandler.currentScreen.value !is PaymentSheetScreen.CvcRecollection
    }
}

internal fun PaymentSheetViewModel.shouldAttachCvc(selection: PaymentSelection.Saved): Boolean {
    return requiresCvcRecollection(selection) {
        this.paymentMethodMetadata.value?.getPaymentMethodLayout(config.paymentMethodLayout) == PaymentSheet.PaymentMethodLayout.Horizontal
    }
}

internal fun PaymentSheetViewModel.isCvcRecollectionEnabled(): Boolean {
    return paymentMethodMetadata.value?.run {
        cvcRecollectionHandler.cvcRecollectionEnabled(
            stripeIntent = stripeIntent,
        )
    } ?: false
}

private fun PaymentSheetViewModel.requiresCvcRecollection(
    selection: PaymentSelection.Saved,
    extraRequirements: () -> Boolean
): Boolean {
    return paymentMethodMetadata.value?.run {
        val requiresCvcRecollection = cvcRecollectionHandler.requiresCVCRecollection(
            stripeIntent = stripeIntent,
            paymentMethod = selection.paymentMethod,
            optionsParams = selection.paymentMethodOptionsParams,
        )

        requiresCvcRecollection && extraRequirements()
    } ?: false
}
