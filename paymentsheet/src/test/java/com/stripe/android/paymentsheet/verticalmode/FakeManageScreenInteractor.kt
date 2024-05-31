package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeManageScreenInteractor(
    initialState: ManageScreenInteractor.State? = null,
    val viewActionRecorder: ViewActionRecorder<ManageScreenInteractor.ViewAction>? = null,
) : ManageScreenInteractor {
    override val state: StateFlow<ManageScreenInteractor.State> =
        stateFlowOf(
            ManageScreenInteractor.State(
                initialState?.paymentMethods ?: emptyList(),
                currentSelection = initialState?.currentSelection,
            )
        )

    override fun handleViewAction(viewAction: ManageScreenInteractor.ViewAction) {
        viewActionRecorder?.record(viewAction)
    }
}
