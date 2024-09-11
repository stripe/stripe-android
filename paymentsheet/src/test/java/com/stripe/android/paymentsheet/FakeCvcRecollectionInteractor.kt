package com.stripe.android.paymentsheet

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcCompletionState
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionViewState
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeCvcRecollectionInteractor : CvcRecollectionInteractor {
    private val _viewState = MutableStateFlow(
        value = CvcRecollectionViewState(
            lastFour = "4242",
            isTestMode = true,
            cvcState = CvcState(
                cvc = "",
                cardBrand = CardBrand.Visa
            )
        )
    )
    override val viewState: StateFlow<CvcRecollectionViewState> = _viewState

    private val _cvcCompletionState = MutableStateFlow<CvcCompletionState>(CvcCompletionState.Incomplete)

    override val cvcCompletionState = _cvcCompletionState

    override fun onCvcChanged(cvc: String) = Unit

    fun updateCompletionState(state: CvcCompletionState) {
        _cvcCompletionState.value = state
    }
}
