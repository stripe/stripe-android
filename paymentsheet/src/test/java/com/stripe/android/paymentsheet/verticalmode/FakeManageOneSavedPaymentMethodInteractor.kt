package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ViewActionRecorder

internal class FakeManageOneSavedPaymentMethodInteractor(
    private val paymentMethod: PaymentMethod,
    private val viewActionRecorder: ViewActionRecorder<ManageOneSavedPaymentMethodInteractor.ViewAction> =
        ViewActionRecorder(),
    val shouldShowDefaultBadge: Boolean
) : ManageOneSavedPaymentMethodInteractor {
    override val state: ManageOneSavedPaymentMethodInteractor.State
        get() = ManageOneSavedPaymentMethodInteractor.State(
            paymentMethod = paymentMethod.toDisplayableSavedPaymentMethod(shouldShowDefaultBadge),
            isLiveMode = true,
        )

    override fun handleViewAction(viewAction: ManageOneSavedPaymentMethodInteractor.ViewAction) {
        viewActionRecorder.record(viewAction)
    }
}
