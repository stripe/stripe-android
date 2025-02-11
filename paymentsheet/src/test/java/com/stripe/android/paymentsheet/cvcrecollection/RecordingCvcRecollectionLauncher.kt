package com.stripe.android.paymentsheet.cvcrecollection

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncher

internal class RecordingCvcRecollectionLauncher private constructor() : CvcRecollectionLauncher {
    private val launchCalls = Turbine<Call>()

    override fun launch(
        data: CvcRecollectionData,
        appearance: PaymentSheet.Appearance,
        isLiveMode: Boolean,
    ) {
        launchCalls.add(Call(data, appearance, isLiveMode))
    }

    data class Call(
        val data: CvcRecollectionData,
        val appearance: PaymentSheet.Appearance,
        val isLiveMode: Boolean,
    )

    class Scenario(
        val launcher: CvcRecollectionLauncher,
        private val launchCalls: ReceiveTurbine<Call>,
    ) {
        suspend fun awaitLaunchCall(): Call {
            return launchCalls.awaitItem()
        }
    }

    companion object {
        suspend fun test(
            test: suspend Scenario.() -> Unit
        ) {
            val launcher = RecordingCvcRecollectionLauncher()

            test(
                Scenario(
                    launcher = launcher,
                    launchCalls = launcher.launchCalls,
                )
            )

            launcher.launchCalls.ensureAllEventsConsumed()
        }

        fun noOp(): CvcRecollectionLauncher {
            return RecordingCvcRecollectionLauncher()
        }
    }
}
