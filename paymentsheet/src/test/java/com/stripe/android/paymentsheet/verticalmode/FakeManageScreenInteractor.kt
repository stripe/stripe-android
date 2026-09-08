package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.Turbine
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.ViewActionRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class FakeManageScreenInteractor(
    initialState: ManageScreenInteractor.State? = null,
    val viewActionRecorder: ViewActionRecorder<ManageScreenInteractor.ViewAction>? = null,
) : ManageScreenInteractor {
    override val isLiveMode: Boolean = true

    val closeCalls = Turbine<Unit>()

    private val _state = MutableStateFlow(
        initialState ?: ManageScreenInteractor.State(
            emptyList(),
            currentSelection = null,
            isEditing = false,
            canEdit = true,
            linkBrand = LinkBrand.Link,
        )
    )
    override val state: StateFlow<ManageScreenInteractor.State> = _state.asStateFlow()

    fun updateState(transform: (ManageScreenInteractor.State) -> ManageScreenInteractor.State) {
        _state.update(transform)
    }

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
