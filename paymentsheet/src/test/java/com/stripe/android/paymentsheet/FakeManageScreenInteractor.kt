package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeManageScreenInteractor(
    paymentMethods: List<DisplayableSavedPaymentMethod> = emptyList()
) : ManageScreenInteractor {
    override val state: StateFlow<ManageScreenInteractor.State> =
        stateFlowOf(
            ManageScreenInteractor.State(
                paymentMethods,
            )
        )
}
