package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeCvcRecollectionInteractor(
    initialState: CvcRecollectionViewState
) : CvcRecollectionInteractor {
    private val _viewState = MutableStateFlow(initialState)
    override val viewState: StateFlow<CvcRecollectionViewState>
        get() = _viewState

    private val _cvcCompletion = MutableStateFlow(CvcState())
    override val cvcCompletionState: StateFlow<CvcState>
        get() = _cvcCompletion

    override fun handleViewAction(action: CvcRecollectionViewAction) = Unit
}
