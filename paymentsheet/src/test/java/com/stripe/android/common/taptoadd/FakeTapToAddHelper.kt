package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeTapToAddHelper private constructor(
    override val collectedPaymentMethod: StateFlow<DisplayableSavedPaymentMethod?> = MutableStateFlow(null),
    override val hasPreviouslyAttemptedCollection: Boolean = false,
) : TapToAddHelper {
    private val collectCalls = Turbine<Unit>()

    override fun startPaymentMethodCollection() {
        collectCalls.add(Unit)
    }

    class Scenario(
        val collectCalls: ReceiveTurbine<Unit>,
        val mutableCollectedPaymentMethod: MutableStateFlow<DisplayableSavedPaymentMethod?>,
        val helper: TapToAddHelper,
    )

    companion object {
        suspend fun test(
            block: suspend Scenario.() -> Unit,
        ) {
            val collectedPaymentMethod = MutableStateFlow<DisplayableSavedPaymentMethod?>(null)
            val helper = FakeTapToAddHelper(collectedPaymentMethod)

            block(
                Scenario(
                    helper = helper,
                    mutableCollectedPaymentMethod = collectedPaymentMethod,
                    collectCalls = helper.collectCalls,
                )
            )

            helper.collectCalls.ensureAllEventsConsumed()
        }

        fun noOp(
            collectedPaymentMethod: StateFlow<DisplayableSavedPaymentMethod?> = MutableStateFlow(null),
        ) = FakeTapToAddHelper(collectedPaymentMethod)
    }
}
