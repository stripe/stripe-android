package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen

internal fun PaymentSheetViewModel.shouldLaunchCvcRecollectionScreen(): Boolean {
    return requiresCvcRecollection {
        config.paymentMethodLayout != PaymentSheet.PaymentMethodLayout.Horizontal &&
            navigationHandler.currentScreen.value !is PaymentSheetScreen.CvcRecollection
    }
}

internal fun PaymentSheetViewModel.shouldAttachCvc(): Boolean {
    return requiresCvcRecollection {
        config.paymentMethodLayout == PaymentSheet.PaymentMethodLayout.Horizontal
    }
}

internal fun PaymentSheetViewModel.isCvcRecollectionEnabled(): Boolean {
    return cvcRecollectionHandler.cvcRecollectionEnabled(
        stripeIntent = paymentMethodMetadata.value?.stripeIntent,
        initializationMode = args.initializationMode
    )
}

private fun PaymentSheetViewModel.requiresCvcRecollection(extraRequirements: () -> Boolean): Boolean {
    return cvcRecollectionHandler.requiresCVCRecollection(
        stripeIntent = paymentMethodMetadata.value?.stripeIntent,
        paymentSelection = selection.value,
        initializationMode = args.initializationMode,
        extraRequirements = extraRequirements
    )
}
