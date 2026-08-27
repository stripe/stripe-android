package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal fun BaseSheetViewModel.transitionToAddPaymentScreen() {
    val interactor = createAddPaymentMethodInteractor(
        paymentMethodMetadata = requireNotNull(paymentMethodMetadata.value),
        paymentMethodMessagePromotionsHelper = null,
    )
    navigationHandler.transitionTo(AddAnotherPaymentMethod(interactor = interactor))
}
