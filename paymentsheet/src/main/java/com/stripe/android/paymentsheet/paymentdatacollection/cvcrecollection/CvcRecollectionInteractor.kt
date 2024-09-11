package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal interface CvcRecollectionInteractor {
    val viewState: StateFlow<CvcRecollectionViewState>
    val cvcCompletionState: StateFlow<CvcCompletionState>

    fun onCvcChanged(cvc: String)
}

internal class DefaultCvcRecollectionInteractor(
    args: Args,
) : CvcRecollectionInteractor {

    private val _viewState = MutableStateFlow(
        CvcRecollectionViewState(
            lastFour = args.lastFour,
            isTestMode = args.isTestMode,
            cvcState = CvcState(
                cvc = args.cvc ?: "",
                cardBrand = args.cardBrand
            )
        )
    )
    override val viewState = _viewState.asStateFlow()

    override val cvcCompletionState = _viewState.mapAsStateFlow { state ->
        if (state.cvcState.isValid) {
            CvcCompletionState.Completed(state.cvcState.cvc)
        } else {
            CvcCompletionState.Incomplete
        }
    }

    override fun onCvcChanged(cvc: String) {
        _viewState.update { oldState ->
            oldState.copy(
                cvcState = oldState.cvcState.updateCvc(cvc)
            )
        }
    }

    internal class Factory : CvcRecollectionInteractorFactory {
        override fun create(args: Args): CvcRecollectionInteractor {
            return DefaultCvcRecollectionInteractor(args)
        }
    }
}
