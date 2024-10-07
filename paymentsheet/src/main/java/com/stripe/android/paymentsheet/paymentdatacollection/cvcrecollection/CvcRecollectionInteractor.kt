package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface CvcRecollectionInteractor {
    val viewState: StateFlow<CvcRecollectionViewState>
    val cvcCompletionState: StateFlow<CvcCompletionState>

    fun onCvcChanged(cvc: String)

    interface Factory {
        fun create(
            args: Args,
            processing: StateFlow<Boolean>,
            coroutineScope: CoroutineScope,
        ): CvcRecollectionInteractor
    }
}

internal class DefaultCvcRecollectionInteractor(
    private val lastFour: String,
    private val cardBrand: CardBrand,
    private val cvc: String,
    private val isTestMode: Boolean,
    private val processing: StateFlow<Boolean>,
    coroutineScope: CoroutineScope,
) : CvcRecollectionInteractor {
    private val _viewState = MutableStateFlow(
        CvcRecollectionViewState(
            lastFour = lastFour,
            isTestMode = isTestMode,
            cvcState = CvcState(
                cvc = cvc,
                cardBrand = cardBrand,
            ),
            isEnabled = !processing.value,
        )
    )
    override val viewState = _viewState.asStateFlow()

    init {
        coroutineScope.launch {
            processing.collect { processing ->
                _viewState.update { oldState ->
                    oldState.copy(
                        isEnabled = !processing
                    )
                }
            }
        }
    }

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

    internal object Factory : CvcRecollectionInteractor.Factory {
        override fun create(
            args: Args,
            processing: StateFlow<Boolean>,
            coroutineScope: CoroutineScope,
        ): CvcRecollectionInteractor {
            return DefaultCvcRecollectionInteractor(
                lastFour = args.lastFour,
                cardBrand = args.cardBrand,
                cvc = args.cvc,
                isTestMode = args.isTestMode,
                processing = processing,
                coroutineScope = coroutineScope,
            )
        }
    }
}
