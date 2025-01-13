package com.stripe.android.paymentsheet.cvcrecollection

import androidx.activity.result.ActivityResultLauncher
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncherFactory

internal class RecordingCvcRecollectionLauncherFactory private constructor(
    private val launcher: CvcRecollectionLauncher,
) : CvcRecollectionLauncherFactory {
    private val createCalls = Turbine<Call>()

    override fun create(
        activityResultLauncher: ActivityResultLauncher<CvcRecollectionContract.Args>
    ): CvcRecollectionLauncher {
        createCalls.add(Call(activityResultLauncher))

        return launcher
    }

    data class Call(
        val activityResultLauncher: ActivityResultLauncher<CvcRecollectionContract.Args>
    )

    class Scenario(
        val factory: CvcRecollectionLauncherFactory,
        private val createCalls: ReceiveTurbine<Call>,
    ) {
        suspend fun awaitCreateCall(): Call {
            return createCalls.awaitItem()
        }
    }

    companion object {
        suspend fun test(
            launcher: CvcRecollectionLauncher = RecordingCvcRecollectionLauncher.noOp(),
            test: suspend Scenario.() -> Unit
        ) {
            val factory = RecordingCvcRecollectionLauncherFactory(launcher)

            test(
                Scenario(
                    factory = factory,
                    createCalls = factory.createCalls,
                )
            )

            factory.createCalls.ensureAllEventsConsumed()
        }

        fun noOp(): CvcRecollectionLauncherFactory {
            return RecordingCvcRecollectionLauncherFactory(RecordingCvcRecollectionLauncher.noOp())
        }
    }
}
