package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeTapToAddHelper private constructor() : TapToAddHelper {
    private val collectCalls = Turbine<Unit>()

    override fun collect() {
        collectCalls.add(Unit)
    }

    class Scenario(
        val collectCalls: ReceiveTurbine<Unit>,
        val helper: TapToAddHelper,
    )

    companion object {
        suspend fun test(
            block: suspend Scenario.() -> Unit,
        ) {
            val helper = FakeTapToAddHelper()

            block(
                Scenario(
                    helper = helper,
                    collectCalls = helper.collectCalls,
                )
            )

            helper.collectCalls.ensureAllEventsConsumed()
        }

        fun noOp() = FakeTapToAddHelper()
    }
}
