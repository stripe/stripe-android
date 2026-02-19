package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.verticalmode.DefaultSavedPaymentMethodConfirmInteractor
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import kotlinx.coroutines.CoroutineScope


internal fun CoroutineScope.handleTapToAddCancel(
    paymentMethodMetadata: PaymentMethodMetadata,
    customerStateHolder: CustomerStateHolder,
    savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
    paymentSelection: PaymentSelection.Saved,
    determineInitialBackStack: (PaymentMethodMetadata, CustomerStateHolder) -> List<PaymentSheetScreen>,
    updateSelection: (PaymentSelection) -> Unit,
    navigationHandler: NavigationHandler<PaymentSheetScreen>,
) {
    customerStateHolder.addPaymentMethod(paymentSelection.paymentMethod)
    updateSelection(paymentSelection)
    val initialScreens = determineInitialBackStack(
        paymentMethodMetadata,
        customerStateHolder,
    )
    val newScreens = if (paymentMethodMetadata.linkState?.signupMode != null) {
        initialScreens + PaymentSheetScreen.SavedPaymentMethodConfirm(
            interactor = DefaultSavedPaymentMethodConfirmInteractor.create(
                paymentMethodMetadata = paymentMethodMetadata,
                savedPaymentMethodLinkFormHelper = savedPaymentMethodLinkFormHelper,
                initialSelection = paymentSelection,
                updateSelection = updateSelection,
                coroutineScope = this,
            ),
            isLiveMode = false // TODO: find right value.
        )
    } else {
        initialScreens
    }
    navigationHandler.resetTo(newScreens)
}