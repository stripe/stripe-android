package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface CvcRecollectionInteractor {
    val viewState: StateFlow<CvcRecollectionViewState>
    val cvcCompletionState: StateFlow<CvcState>

    fun handleViewAction(action: CvcRecollectionViewAction)
}

internal class DefaultCvcRecollectionInteractor(args: Args) : CvcRecollectionInteractor {
    private val _cvcCompletionState = MutableStateFlow(CvcState())
    override val cvcCompletionState: StateFlow<CvcState>
        get() = _cvcCompletionState

    private val _viewState = MutableStateFlow(
        CvcRecollectionViewState(
            cardBrand = args.cardBrand,
            lastFour = args.lastFour,
            cvc = null,
            isTestMode = args.isTestMode
        )
    )
    override val viewState: StateFlow<CvcRecollectionViewState>
        get() = _viewState

    override fun handleViewAction(action: CvcRecollectionViewAction) {
        when (action) {
            is CvcRecollectionViewAction.CvcStateChanged -> {
                _cvcCompletionState.value = action.completion
            }
            CvcRecollectionViewAction.OnBackPressed -> Unit
            is CvcRecollectionViewAction.OnConfirmPressed -> Unit
        }
    }
}
