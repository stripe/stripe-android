package com.stripe.android.paymentsheet

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.DefaultCvcRecollectionInteractor
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal fun BaseSheetViewModel.transitionToAddPaymentScreen() {
    val interactor = DefaultAddPaymentMethodInteractor.create(
        viewModel = this,
        paymentMethodMetadata = requireNotNull(paymentMethodMetadata.value),
    )
    navigationHandler.transitionTo(AddAnotherPaymentMethod(interactor = interactor))
}

internal fun BaseSheetViewModel.transitionToCVCRecollection() {
    navigationHandler.transitionTo(
        target = PaymentSheetScreen.CvcRecollection(
            args = CvcRecollectionContract.Args(
                lastFour = "4546",
                cardBrand = CardBrand.Visa,
                appearance = PaymentSheet.Appearance(),
                displayMode = CvcRecollectionContract.Args.DisplayMode.PaymentScreen(true)
            ),
            interactor = DefaultCvcRecollectionInteractor {
                this.navigationHandler.pop()
            }
        )
    )
}
