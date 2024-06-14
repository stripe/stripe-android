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
            initialState ?: ManageScreenInteractor.State(
                emptyList(),
                currentSelection = null,
                isEditing = false,
                canDelete = true,
            )
        )

    override fun handleViewAction(viewAction: ManageScreenInteractor.ViewAction) {
        viewActionRecorder?.record(viewAction)
    }

    override fun close() { }
}
