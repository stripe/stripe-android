package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal interface CvcRecollectionInteractor {
    val viewState: CvcRecollectionViewState
    val cvcCompletionState: StateFlow<CvcCompletionState>
}

internal class DefaultCvcRecollectionInteractor(
    args: Args,
    private val scope: CoroutineScope
) : CvcRecollectionInteractor {
    private val controller = CvcController(cardBrandFlow = stateFlowOf(args.cardBrand))
    private val element = CvcElement(
        IdentifierSpec(),
        controller
    )
    override val viewState = CvcRecollectionViewState(
        cardBrand = args.cardBrand,
        lastFour = args.lastFour,
        cvc = null,
        isTestMode = args.isTestMode,
        element = element
    )

    override val cvcCompletionState: StateFlow<CvcCompletionState>
        get() = controller.isComplete.mapAsStateFlow { isComplete ->
            if (isComplete) {
                CvcCompletionState.Completed(controller.fieldValue.value)
            } else {
                CvcCompletionState.Incomplete
            }
        }.stateIn(
            scope = scope,
            initialValue = CvcCompletionState.Incomplete,
            started = SharingStarted.Eagerly
        )
}
