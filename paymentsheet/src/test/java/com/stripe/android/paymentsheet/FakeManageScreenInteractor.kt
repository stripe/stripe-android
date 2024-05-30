package com.stripe.android.paymentsheet

import com.google.common.truth.Truth
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeManageScreenInteractor(
    initialState: ManageScreenInteractor.State? = null,
    val viewActionRecorder: ViewActionRecorder? = null,
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

    class ViewActionRecorder {
        private val _viewActions: MutableList<ManageScreenInteractor.ViewAction> = mutableListOf()
        val viewActions: List<ManageScreenInteractor.ViewAction>
            get() = _viewActions.toList()

        fun record(viewAction: ManageScreenInteractor.ViewAction) {
            _viewActions += viewAction
        }

        fun consume(viewAction: ManageScreenInteractor.ViewAction) {
            Truth.assertThat(_viewActions.size).isGreaterThan(0)
            Truth.assertThat(_viewActions[0]).isEqualTo(viewAction)
            _viewActions.removeAt(0)
        }
    }
}
