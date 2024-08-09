package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

internal interface CvcRecollectionInteractor {
    val viewState: StateFlow<CvcRecollectionViewState>
    val cvcCompletionState: StateFlow<CvcState>
}

internal class DefaultCvcRecollectionInteractor(
    args: Args,
    private val scope: CoroutineScope
) : CvcRecollectionInteractor {
    private val _viewState = MutableStateFlow(
        CvcRecollectionViewState(
            cardBrand = args.cardBrand,
            lastFour = args.lastFour,
            cvc = null,
            isTestMode = args.isTestMode,
            element = CvcElement(
                IdentifierSpec(),
                CvcController(cardBrandFlow = stateFlowOf(args.cardBrand))
            )
        )
    )
    override val viewState: StateFlow<CvcRecollectionViewState>
        get() = _viewState

    override val cvcCompletionState: StateFlow<CvcState>
        get() = _viewState.flatMapLatest {
            combine(
                it.element.controller.fieldValue,
                it.element.controller.isComplete
            ) { cvc, isComplete ->
                CvcState(cvc, isComplete)
            }
        }.stateIn(
            scope = scope,
            initialValue = CvcState(),
            started = SharingStarted.Eagerly
        )
}
