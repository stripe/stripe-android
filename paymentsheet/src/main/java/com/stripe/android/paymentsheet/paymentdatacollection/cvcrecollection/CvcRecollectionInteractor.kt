package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.ui.core.elements.CvcController
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
    scope: CoroutineScope
) : CvcRecollectionInteractor {
    private val controller = CvcController(cardBrandFlow = stateFlowOf(args.cardBrand))
    override val viewState = CvcRecollectionViewState(
        cardBrand = args.cardBrand,
        lastFour = args.lastFour,
        cvc = null,
        isTestMode = args.isTestMode,
        controller = controller
    )

    override val cvcCompletionState = controller.isComplete.mapAsStateFlow { isComplete ->
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
