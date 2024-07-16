package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal fun BaseSheetViewModel.transitionToAddPaymentScreen() {
    navigationHandler.transitionTo(
        AddAnotherPaymentMethod(interactor = DefaultAddPaymentMethodInteractor.create(this))
    )
}
