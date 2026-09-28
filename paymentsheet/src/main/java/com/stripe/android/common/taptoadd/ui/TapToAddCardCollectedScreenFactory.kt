package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.spms.LinkInlineSignupAvailability
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.model.PaymentMethod
import javax.inject.Inject

internal class TapToAddCardCollectedScreenFactory @Inject constructor(
    private val tapToAddMode: TapToAddMode,
    private val linkInlineSignupAvailability: LinkInlineSignupAvailability,
    private val tapToAddCardAddedInteractorFactory: TapToAddCardAddedInteractor.Factory,
    private val tapToAddConfirmationInteractorFactory: TapToAddConfirmationInteractor.Factory,
) {
    fun create(paymentMethod: PaymentMethod): TapToAddNavigator.Screen {
        return if (
            tapToAddMode == TapToAddMode.Complete &&
            linkInlineSignupAvailability.availability() is LinkInlineSignupAvailability.Result.Unavailable
        ) {
            TapToAddNavigator.Screen.Confirmation(
                interactor = tapToAddConfirmationInteractorFactory.create(
                    paymentMethod = paymentMethod,
                    linkInput = null,
                    withTitle = true,
                )
            )
        } else {
            TapToAddNavigator.Screen.CardAdded(
                interactor = tapToAddCardAddedInteractorFactory.create(paymentMethod)
            )
        }
    }
}
