package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeManageScreenInteractor(
    displayableSavedPaymentMethods: List<DisplayableSavedPaymentMethod> = emptyList()
) : ManageScreenInteractor {
    override val state: StateFlow<ManageScreenInteractor.State> =
        stateFlowOf(ManageScreenInteractor.State(displayableSavedPaymentMethods))
}
