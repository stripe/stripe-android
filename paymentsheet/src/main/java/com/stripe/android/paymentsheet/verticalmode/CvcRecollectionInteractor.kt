package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionResult

internal interface CvcRecollectionInteractor {

    fun handleEffect(effect: Effect)

    sealed interface Effect {
        data class SendCvcRecollectionEffect(val result: CvcRecollectionResult) : Effect
    }
}

internal class DefaultCvcRecollectionInteractor(
    private val onCvcRecollectionResult: (CvcRecollectionResult) -> Unit,
) : CvcRecollectionInteractor {
    override fun handleEffect(effect: CvcRecollectionInteractor.Effect) {
        when (effect) {
            is CvcRecollectionInteractor.Effect.SendCvcRecollectionEffect -> onCvcRecollectionResult(effect.result)
        }
    }
}
