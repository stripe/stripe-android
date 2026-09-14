package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import javax.inject.Inject

internal class SavedPaymentMethodConfirmScreenFactory @Inject constructor(
    private val interactorFactory: SavedPaymentMethodConfirmInteractor.Factory,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val confirmationHelper: SheetActivityConfirmationHelper,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val launchMode: EmbeddedLaunchMode,
) {
    fun create(selection: PaymentSelection.Saved) = EmbeddedNavigator.Screen.SavedPaymentMethodConfirm(
        interactor = interactorFactory.create(selection, embeddedSelectionHolder::setSelection),
        isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
        sheetActivityStateHolder = sheetActivityStateHolder,
        confirmationHelper = confirmationHelper,
        embeddedSelectionHolder = embeddedSelectionHolder,
        customerStateHolder = customerStateHolder,
        launchMode = launchMode,
    )
}
