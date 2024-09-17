package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

internal class CvcRecollectionHandlerV2(
    private val launch: () -> Unit,
) {
    private val state = MutableStateFlow<State>(State.Idle)

    fun start(
        stripeIntent: StripeIntent,
        initializationMode: PaymentSheet.InitializationMode,
        confirmationOption: PaymentConfirmationOption.PaymentMethod.Saved
    ) {
        state.value = State.Confirming(confirmationOption)

        if (
            deferredIntentRequiresCVCRecollection(initializationMode) ||
            paymentIntentRequiresCVCRecollection(stripeIntent, initializationMode)
        ) {
            launch()
        } else {
            state.value = State.Complete(
                result = Result.Confirmed(confirmationOption)
            )
        }
    }

    fun setResult(result: CvcRecollectionResult) {
        val currentState = state.value

        if (currentState is State.Confirming) {
            val parsedResult = when (result) {
                is CvcRecollectionResult.Confirmed -> {
                    val option = currentState.confirmationOption.copy(
                        optionsParams = PaymentMethodOptionsParams.Card(
                            cvc = result.cvc,
                        )
                    )

                    Result.Confirmed(option)
                }
                is CvcRecollectionResult.Cancelled -> Result.Canceled
            }

            state.value = State.Complete(parsedResult)
        }
    }

    suspend fun await(): Result? {
        return when (val value = state.value) {
            is State.Idle -> null
            is State.Complete -> value.result
            is State.Confirming -> state.firstInstanceOf<State.Complete>().result
        }
    }

    private fun deferredIntentRequiresCVCRecollection(initializationMode: PaymentSheet.InitializationMode?): Boolean {
        return (initializationMode as? PaymentSheet.InitializationMode.DeferredIntent)
            ?.intentConfiguration?.requireCvcRecollection == true &&
            initializationMode.intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
    }

    private fun paymentIntentRequiresCVCRecollection(
        stripeIntent: StripeIntent?,
        initializationMode: PaymentSheet.InitializationMode?
    ): Boolean {
        return (stripeIntent as? PaymentIntent)?.requireCvcRecollection == true &&
            initializationMode is PaymentSheet.InitializationMode.PaymentIntent
    }

    private suspend inline fun <reified T> Flow<*>.firstInstanceOf(): T {
        return first {
            it is T
        } as T
    }

    sealed interface State {
        data object Idle : State

        data class Confirming(val confirmationOption: PaymentConfirmationOption.PaymentMethod.Saved) : State

        data class Complete(val result: Result) : State
    }

    sealed interface Result {
        data class Confirmed(val confirmationOption: PaymentConfirmationOption.PaymentMethod.Saved) : Result

        data object Canceled : Result
    }
}
