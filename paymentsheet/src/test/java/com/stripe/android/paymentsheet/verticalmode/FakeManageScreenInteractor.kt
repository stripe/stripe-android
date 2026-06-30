package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.Turbine
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeManageScreenInteractor(
    initialState: ManageScreenInteractor.State? = null,
    val viewActionRecorder: ViewActionRecorder<ManageScreenInteractor.ViewAction>? = null,
) : ManageScreenInteractor {
    override val isLiveMode: Boolean = true

    val closeCalls = Turbine<Unit>()

    override val state: StateFlow<ManageScreenInteractor.State> =
        stateFlowOf(
            initialState ?: ManageScreenInteractor.State(
                emptyList(),
                currentSelection = null,
                isEditing = false,
                canEdit = true,
                linkBrand = LinkBrand.Link,
            )
        )

    override fun handleViewAction(viewAction: ManageScreenInteractor.ViewAction) {
        viewActionRecorder?.record(viewAction)
    }

    override fun close() {
        closeCalls.add(Unit)
    }

    fun validate() {
        closeCalls.ensureAllEventsConsumed()
    }
}
